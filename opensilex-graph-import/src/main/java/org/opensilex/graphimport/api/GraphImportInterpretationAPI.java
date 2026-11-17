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
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.api.dto.InterpretationDTO;
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
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * What the files of an import hold: the class of each sheet, the column identifying its rows, and the
 * property of each column, each with the evidence behind it. Nothing here writes to the instance.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH + "/{sessionId}/interpretation")
public class GraphImportInterpretationAPI {

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
    @ApiOperation(
            value = "Interpret the sheets and columns of an import",
            notes = "Suggests the class of each sheet, its identifier column and the property of each column, "
                    + "with the evidence behind each suggestion, and the user's decisions in their place. Existing "
                    + "instances are recognised only among those the user may see; scientific objects only in "
                    + "the experiment chosen for the import."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The interpretation", response = InterpretationDTO.class),
            @ApiResponse(code = 403, message = "The chosen experiment is no longer readable by the user",
                    response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response getInterpretation(
            @ApiParam(value = "Import identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        InterpretationDTO interpretation = new Interpretations(sparql, nosql, fs, opensilex, currentUser)
                .interpret(session.get());
        return new SingleObjectResponse<>(interpretation).getResponse();
    }
}
