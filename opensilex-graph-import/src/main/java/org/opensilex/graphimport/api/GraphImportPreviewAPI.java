//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.OpenSilex;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.api.dto.PreviewDTO;
import org.opensilex.graphimport.api.dto.PreviewNodeDTO;
import org.opensilex.graphimport.api.dto.PreviewRowsDTO;
import org.opensilex.graphimport.preview.ImportPreview;
import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.graphimport.preview.NodeState;
import org.opensilex.graphimport.preview.PreviewGrid;
import org.opensilex.graphimport.preview.PreviewTurtle;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * What an import would do to the instance, compared with what it holds: figures, the sheets a page at
 * a time, one resource at a time, and the whole as Turtle. Nothing here writes to the instance.
 * <p>
 * The preview is worked out once and kept with the import while the import stays as it is; asking it
 * afresh reads the instance again.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH)
public class GraphImportPreviewAPI {

    public static final String TURTLE_MEDIA_TYPE = "text/turtle";

    private static final String PREVIEW_PATH = GraphImportAPI.SESSIONS_PATH + "/{sessionId}/preview";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private GraphImportConfig config;

    @Inject
    private GraphImportSessionCache sessions;

    @Inject
    private OpenSilex opensilex;

    @GET
    @Path(PREVIEW_PATH)
    @ApiOperation(
            value = "Preview what an import would do to the instance",
            notes = "Compares each resource of the import with what exists, within the user's rights: how many would "
                    + "be created, completed, left as they are, or cannot be settled, and how the sheets hang together. "
                    + "Nothing is written."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The figures and the map of the sheets", response = PreviewDTO.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response preview(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Read the instance again rather than give back the preview last worked out")
            @QueryParam("afresh") @DefaultValue("false") boolean afresh
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        ImportPreview preview = interpretations().preview(session.get(), afresh);
        return new SingleObjectResponse<>(PreviewDTO.fromModel(sessionId, preview)).getResponse();
    }

    @GET
    @Path(PREVIEW_PATH + "/sheets/{sheet}/rows")
    @ApiOperation(
            value = "Preview a sheet of an import, a page at a time",
            notes = "Its rows as written, each filled cell with what the import would do with it, each row with the "
                    + "resource it is. At most 100 rows a page."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A page of the sheet", response = PreviewRowsDTO.class),
            @ApiResponse(code = 400, message = "An invalid page or filter", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or sheet", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response previewRows(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Sheet name, as the import shows it", required = true) @PathParam("sheet") @NotNull
            String sheet,
            @ApiParam(value = "Keep the rows whose resource is in this state: CREATED, COMPLETED, UNCHANGED or CONFLICT")
            @QueryParam("state") String state,
            @ApiParam(value = "Keep only the rows whose resource the file must be corrected for")
            @QueryParam("to_fix") @DefaultValue("false") boolean toFix,
            @ApiParam(value = "Page number, from 0", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Rows per page, at most 100", example = "20") @QueryParam("page_size") @DefaultValue("20")
            @Min(1) @Max(PreviewGrid.MAX_PAGE_SIZE) int pageSize
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        // The sheet as the assistant reads it, with the columns and sheets the user added.
        Optional<SheetStructure> structure = session.flatMap(found -> found.getReadWorkbook().workbook()
                .getSheet(sheet));
        if (structure.isEmpty()) {
            return access.notFound();
        }
        NodeState kept = null;
        if (state != null && !state.isBlank()) {
            try {
                kept = NodeState.valueOf(state);
            } catch (IllegalArgumentException e) {
                return access.badRequest("Unknown state", "The state " + state
                        + " is none of CREATED, COMPLETED, UNCHANGED and CONFLICT.");
            }
        }
        ImportPreview preview = interpretations().preview(session.get(), false);
        PreviewGrid.Page rows = PreviewGrid.page(preview, structure.get(), kept, toFix, page, pageSize);
        return new SingleObjectResponse<>(PreviewRowsDTO.fromModel(sheet, rows, page, pageSize)).getResponse();
    }

    @GET
    @Path(PREVIEW_PATH + "/nodes/{node}")
    @ApiOperation(
            value = "Preview a resource of an import",
            notes = "Its values merged across sheets, each with the sheet and row it comes from and what the import "
                    + "would do with it, what the existing resource holds instead, and the problems of the file about "
                    + "it."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The resource", response = PreviewNodeDTO.class),
            @ApiResponse(code = 404, message = "Unknown import or resource", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response previewNode(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "The index of the resource, as the rows of the preview give it", required = true,
                    example = "0") @PathParam("node") int node
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        Interpretations interpretations = interpretations();
        Optional<NodeComparison> found = interpretations.preview(session.get(), false).node(node);
        if (found.isEmpty()) {
            return access.notFound();
        }
        return new SingleObjectResponse<>(interpretations.describe(found.get())).getResponse();
    }

    @GET
    @Path(PREVIEW_PATH + "/turtle")
    @ApiOperation(
            value = "Export what an import describes, as Turtle",
            notes = "Every resource with its classes and the values the file gives it, its rows merged. A resource "
                    + "that does not exist yet has an indicative URI. Nothing is written to the instance."
    )
    @ApiProtected
    @Produces({TURTLE_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The graph, in Turtle"),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response exportTurtle(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        ImportPreview preview = interpretations().preview(session.get(), false);
        return Response.ok(new PreviewTurtle().write(preview, SPARQLService.getPrefixes()), TURTLE_MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + fileName(session.get().getTitle()) + "\"")
                .build();
    }

    /** A file name made of the import's title, with nothing in it a header could misread. */
    static String fileName(String title) {
        String base = title == null ? "" : title.replaceFirst("\\.[A-Za-z0-9]{1,5}$", "");
        String safe = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.length() > 80) {
            safe = safe.substring(0, 80);
        }
        return (safe.isBlank() ? "import" : safe) + "-preview.ttl";
    }

    private Interpretations interpretations() throws Exception {
        return new Interpretations(sparql, nosql, fs, opensilex, currentUser);
    }
}
