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
import org.opensilex.graphimport.api.dto.ColumnReadingDTO;
import org.opensilex.graphimport.api.dto.ValidationDTO;
import org.opensilex.graphimport.binding.ColumnReading;
import org.opensilex.graphimport.binding.ImportDecisions;
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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The graph an import gives once its rows are merged, and what is wrong with it, cell by cell. Nothing
 * here writes to the instance.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH)
public class GraphImportValidationAPI {

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
    @Path(GraphImportAPI.SESSIONS_PATH + "/{sessionId}/validation")
    @ApiOperation(
            value = "Check the graph of an import",
            notes = "Merges the rows sharing an identifier, resolves the links, reads the values for their datatype and "
                    + "checks the restrictions of the ontology. Every problem is counted; at most 500 are listed."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The graph's figures and problems", response = ValidationDTO.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response validate(
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
        return new SingleObjectResponse<>(interpretations().validate(session.get())).getResponse();
    }

    @PUT
    @Path("column_readings")
    @ApiOperation(
            value = "Say how the dates of a column are written",
            notes = "The order of day and month, where the values cannot tell, and the offset of times written without "
                    + "one. The decisions about the sheet's class and columns are kept."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The graph checked again", response = ValidationDTO.class),
            @ApiResponse(code = 400, message = "A column the sheet does not have", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or sheet", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response readColumn(
            @ApiParam(value = "How the column is written", required = true) @NotNull @Valid ColumnReadingDTO dto
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(dto.getSessionId());
        Optional<SheetStructure> sheet = session.flatMap(found -> found.getWorkbook().getSheet(dto.getSheet()));
        if (sheet.isEmpty()) {
            return access.notFound();
        }
        if (dto.getColumn() >= sheet.get().getHeaders().size()) {
            return access.badRequest("This reading cannot be kept",
                    "The sheet has no column " + (dto.getColumn() + 1) + ".");
        }
        ImportDecisions.SheetDecisions decided = session.get().getDecisions().of(dto.getSheet());
        Map<Integer, ColumnReading> readings = new HashMap<>(decided.readings());
        readings.put(dto.getColumn(), dto.toModel());
        session.get().decide(dto.getSheet(), decided.withReadings(readings));
        access.persist(session.get());
        return new SingleObjectResponse<>(interpretations().validate(session.get())).getResponse();
    }

    private Interpretations interpretations() throws Exception {
        return new Interpretations(sparql, nosql, fs, opensilex, currentUser);
    }
}
