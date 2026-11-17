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
import org.opensilex.aiimport.api.dto.CreationDraftRequestDTO;
import org.opensilex.aiimport.api.dto.CreationProposalDTO;
import org.opensilex.aiimport.api.dto.FacilityDraftDTO;
import org.opensilex.aiimport.api.dto.FactorPlanDTO;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.ProposalBuilder;
import org.opensilex.aiimport.create.SessionFacts;
import org.opensilex.aiimport.create.drafts.FacilityDrafts;
import org.opensilex.aiimport.create.drafts.FactorPlanner;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.SessionProfile;
import org.opensilex.core.CoreModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creating from a conversation without asking the assistant anything: the draft the assistant
 * would have proposed is made here from what the files say, and the platform's forms are opened
 * with what the files say of each factor and each facility. Nothing is written by any of these
 * calls; the user confirms on the card or in the form, as for every creation.
 *
 * @author Arnaud Charleroy
 */
@Api(AiImportAPI.API_TAG)
@Path(AiImportAPI.PATH)
public class CreationDraftAPI {

    public static final String DRAFTS_PATH = "drafts";
    public static final String FACTORS_PATH = "factors";
    public static final String FACILITIES_PATH = "facilities";

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

    @POST
    @Path(DRAFTS_PATH)
    @ApiOperation(
            value = "Draft a creation from what the files say",
            notes = "The draft the assistant would have proposed, made without it: every field the files can "
                    + "answer is filled, the others are asked on the card. The draft becomes the conversation's "
                    + "pending one; creating it is a separate, confirmed call."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The draft", response = CreationProposalDTO.class),
            @ApiResponse(code = 400, message = "Not a creation, or a field it does not have", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response draft(
            @ApiParam(value = "What to draft, and where", required = true)
            @NotNull @Valid CreationDraftRequestDTO request
    ) {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(request.getSessionId());
        if (!found.isPresent()) {
            return access.notFound();
        }
        Optional<CreationTarget> target = CreationTarget.parse(request.getTarget());
        if (!target.isPresent()) {
            return access.badRequest("Not something to create.",
                    "'" + request.getTarget() + "' is none of " + CreationTarget.choices() + ".");
        }

        AiImportSession session = found.get();
        CreationProposal proposal;
        try {
            proposal = new ProposalBuilder(access.creation()).build(session, target.get(), request.getValues(), null);
        } catch (IllegalArgumentException e) {
            return access.badRequest("Not a value of this creation.", e.getMessage());
        }
        session.setPendingProposal(proposal);
        access.persist(session);
        CreationRequirements requirements = access.creation().requirementsFor(target.get(), session);
        return new SingleObjectResponse<>(CreationProposalDTO.fromModel(proposal, requirements)).getResponse();
    }

    @GET
    @Path(AiImportAPI.SESSIONS_PATH + "/{sessionId}/" + DRAFTS_PATH + "/" + FACTORS_PATH)
    @ApiOperation(
            value = "The factors to declare in an experiment, one plan per factor",
            notes = "Each factor the files name, set beside what the experiment holds: created when it has none "
                    + "of that name, completed when it lacks levels, complete otherwise. The factor form is opened "
                    + "with the plan. Nothing is written."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The factors", response = FactorPlanDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 400, message = "No experiment to declare them in", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getFactorPlans(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "The experiment; the one the files name when left out")
            @QueryParam("experiment") URI experiment
    ) throws Exception {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(sessionId);
        if (!found.isPresent()) {
            return access.notFound();
        }
        AiImportSession session = found.get();
        URI target = experiment != null ? experiment : new SessionFacts(session).firstFoundUri(
                session.getReport() == null ? null : session.getReport().getExperiments()).orElse(null);
        if (target == null) {
            return access.badRequest("No experiment.", "Factors are declared in an experiment: none is named "
                    + "by the files or chosen, and none was given.");
        }
        return new PaginatedListResponse<>(new FactorPlanner(sparql, currentUser.getLanguage())
                .plan(SessionProfile.of(session).map(profile -> profile.extractFactorLevels(session.getWorkbook()))
                        .orElse(List.of()), target)
                .stream().map(FactorPlanDTO::fromModel).collect(Collectors.toList())).getResponse();
    }

    @GET
    @Path(AiImportAPI.SESSIONS_PATH + "/{sessionId}/" + DRAFTS_PATH + "/" + FACILITIES_PATH)
    @ApiOperation(
            value = "The facilities the sheets of facilities describe",
            notes = "One per named row, with the type the user chose and the value of each property they matched "
                    + "a column to. The facility form is opened with them. Nothing is written."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The facilities", response = FacilityDraftDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getFacilityDrafts(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(sessionId);
        if (!found.isPresent()) {
            return access.notFound();
        }
        return new PaginatedListResponse<>(FacilityDrafts.of(found.get()).stream()
                .map(FacilityDraftDTO::fromModel).collect(Collectors.toList())).getResponse();
    }

    private SessionAccess access() {
        return new SessionAccess(sparql, nosql, fs, config, llm, sessions, coreModule, currentUser);
    }
}
