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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.api.dto.GraphImportSessionDTO;
import org.opensilex.graphimport.api.dto.StoredImportDTO;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.graphimport.session.SessionFiles;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The imports of the experimental assistant: opening one with a file, listing the stored ones,
 * resuming and deleting one. Nothing here writes to the instance.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH)
public class GraphImportAPI {

    public static final String PATH = "/graph-import";

    /**
     * The swagger tag, which also names the generated TypeScript service: {@code GraphImportService}.
     */
    public static final String API_TAG = "GraphImport";

    public static final String SESSIONS_PATH = "sessions";

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphImportAPI.class);

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private GraphImportConfig config;

    @Inject
    private GraphImportSessionCache sessions;

    @POST
    @Path(SESSIONS_PATH)
    @ApiOperation(
            value = "Open an import with a spreadsheet or a CSV file",
            notes = "Reads the file and stores it, so that the import can be resumed. Nothing is written to "
                    + "the instance."
    )
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Import opened", response = GraphImportSessionDTO.class),
            @ApiResponse(code = 400, message = "A file that is missing, too large, of another kind, or unreadable",
                    response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response createSession(
            @ApiParam(value = "Spreadsheet or CSV file", required = true, type = "file")
            @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        GraphImportAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        String fileName = fileNameOf(fileDetail);
        Optional<Response> rejected = access.reject(file, fileName);
        if (rejected.isPresent()) {
            return rejected.get();
        }

        SessionFile read;
        try {
            read = SessionFiles.read(file, fileName);
        } catch (WorkbookReadException e) {
            LOGGER.warn("Could not read the uploaded file {}", fileName, e);
            return access.unreadable(fileName);
        }

        GraphImportSession session = access.create();
        session.add(read);
        try {
            // Stored from the start, so that closing the page never loses the import.
            access.store().saveFile(session.getId(), read.getId(), file);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not store the file of import {}", session.getId(), e);
        }
        access.persist(session);

        return Response.status(Response.Status.CREATED)
                .entity(new SingleObjectResponse<>(GraphImportSessionDTO.fromModel(session)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path(SESSIONS_PATH)
    @ApiOperation(
            value = "List the user's stored imports",
            notes = "Each import is stored as it goes, with its files, and kept for the configured number of "
                    + "days after its last change."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The stored imports, most recent first",
                    response = StoredImportDTO.class, responseContainer = "List"),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response listSessions() {
        GraphImportAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        List<StoredImportDTO> stored = access.store().list(currentUser.getUri()).stream()
                .map(summary -> StoredImportDTO.fromModel(summary, config.savedSessionDays()))
                .toList();
        return new PaginatedListResponse<>(stored).getResponse();
    }

    @GET
    @Path(SESSIONS_PATH + "/{sessionId}")
    @ApiOperation(
            value = "Get an import, with the sheets of its files",
            notes = "An import no longer in memory is read back from its stored files."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The import", response = GraphImportSessionDTO.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response getSession(
            @ApiParam(value = "Import identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        GraphImportAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        return new SingleObjectResponse<>(GraphImportSessionDTO.fromModel(session.get())).getResponse();
    }

    @DELETE
    @Path(SESSIONS_PATH + "/{sessionId}")
    @ApiOperation(value = "Delete an import, with its stored files")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Import deleted"),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response deleteSession(
            @ApiParam(value = "Import identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        GraphImportAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        if (!access.delete(sessionId)) {
            return access.notFound();
        }
        return new SingleObjectResponse<>(sessionId).getResponse();
    }

    static String fileNameOf(FormDataContentDisposition fileDetail) {
        return fileDetail == null || fileDetail.getFileName() == null ? "uploaded-file" : fileDetail.getFileName();
    }

    private GraphImportAccess access() {
        return new GraphImportAccess(nosql, fs, config, sessions, currentUser);
    }
}
