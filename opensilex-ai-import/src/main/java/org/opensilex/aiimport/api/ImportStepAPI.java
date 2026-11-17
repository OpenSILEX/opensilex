//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.api.dto.ImportStepDTO;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.core.CoreModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
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
import java.util.stream.Collectors;

/**
 * The order an import is declared in — an experiment, then its factors, then their levels, then the
 * objects and the data — and where each step of a conversation stands. Read-only: the guided path
 * of the screen is drawn from it, and the creations themselves refuse what comes too early.
 *
 * @author Arnaud Charleroy
 */
@Api(AiImportAPI.API_TAG)
@Path(AiImportAPI.PATH + "/" + AiImportAPI.SESSIONS_PATH + "/{sessionId}/" + ImportStepAPI.PATH)
public class ImportStepAPI {

    public static final String PATH = "steps";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private AiImportConfig config;

    @Inject
    private LlmService llm;

    @Inject
    private AiImportSessionCache sessions;

    @Inject
    private CoreModule coreModule;

    @GET
    @ApiOperation(
            value = "Where each step of the import stands",
            notes = "In the order the steps are declared in. A step is done when everything the files name for it "
                    + "exists on the instance, blocked when a step it depends on is not done, and not applicable "
                    + "when the files name nothing for it. Nothing is written."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The steps", response = ImportStepDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getSteps(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        SessionAccess access = new SessionAccess(sparql, nosql, fs, config, llm, sessions, coreModule, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(sessionId);
        if (!found.isPresent()) {
            return access.notFound();
        }
        return new PaginatedListResponse<>(access.creation().stepsOf(found.get()).stream()
                .map(ImportStepDTO::fromModel).collect(Collectors.toList())).getResponse();
    }
}
