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
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.api.dto.AiImportSessionDTO;
import org.opensilex.aiimport.api.dto.AssistantStatusDTO;
import org.opensilex.aiimport.api.dto.BulkValidationDTO;
import org.opensilex.aiimport.api.dto.ChatMessageDTO;
import org.opensilex.aiimport.api.dto.ChatQuestionDTO;
import org.opensilex.aiimport.api.dto.ColumnMappingDTO;
import org.opensilex.aiimport.api.dto.CreationRequestDTO;
import org.opensilex.aiimport.api.dto.CreationProposalDTO;
import org.opensilex.aiimport.api.dto.CreationRequirementsDTO;
import org.opensilex.aiimport.api.dto.CreationResultDTO;
import org.opensilex.aiimport.api.dto.ImportProfileDTO;
import org.opensilex.aiimport.api.dto.MatchConfirmationDTO;
import org.opensilex.aiimport.api.dto.ObjectSheetDTO;
import org.opensilex.aiimport.api.dto.ObjectSheetPlanDTO;
import org.opensilex.aiimport.api.dto.ObjectSheetsValidationDTO;
import org.opensilex.aiimport.api.dto.ResolutionReportDTO;
import org.opensilex.aiimport.api.dto.SavedSessionDTO;
import org.opensilex.aiimport.api.dto.TypePropertyDTO;
import org.opensilex.aiimport.create.AiImportCreationService;
import org.opensilex.aiimport.create.CreationFieldException;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.CreationOutcome;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.bulk.BulkOutcome;
import org.opensilex.aiimport.create.bulk.ScientificObjectBulkImport;
import org.opensilex.aiimport.create.objects.ObjectSheet;
import org.opensilex.aiimport.create.objects.ObjectSheetPlan;
import org.opensilex.aiimport.create.objects.ObjectSheets;
import org.opensilex.aiimport.create.objects.TypeProperties;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.resolve.CorrectionStore;
import org.opensilex.aiimport.resolve.LearnedCorrection;
import org.opensilex.aiimport.resolve.ReportCategory;
import org.opensilex.aiimport.resolve.ResolutionService;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.service.AiImportChatService;
import org.opensilex.aiimport.service.AiImportMessage;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.store.AiImportSessionStore;
import org.opensilex.aiimport.service.store.SavedSession;
import org.opensilex.core.CoreModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

/**
 * Conversational preparation of a spreadsheet import.
 * <p>
 * A session is opened by uploading a file. The module reads it, checks every name it finds against
 * this instance, and holds a conversation about what is there and what is missing. Nothing is
 * written to the databases: the deliverable is the report, and the user acts on it.
 *
 * @author Arnaud Charleroy
 */
@Api(AiImportAPI.API_TAG)
@Path(AiImportAPI.PATH)
public class AiImportAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiImportAPI.class);

    public static final String PATH = "/ai-import";

    /**
     * The swagger tag, which also names the generated TypeScript service: {@code AiImportService}.
     */
    public static final String API_TAG = "AiImport";

    public static final String SESSIONS_PATH = "sessions";
    public static final String PROFILES_PATH = "profiles";
    public static final String ASSISTANT_PATH = "assistant";
    public static final String MESSAGES_PATH = "messages";
    public static final String REVALIDATE_PATH = "revalidate";
    public static final String REPORT_PATH = "report";
    public static final String MAPPING_PATH = "mapping";
    public static final String REQUIREMENTS_PATH = "creation-requirements";
    public static final String CREATE_PATH = "create";
    public static final String CANCEL_PATH = "cancel-proposal";
    public static final String CONFIRM_MATCH_PATH = "matches";
    public static final String FORGET_MATCH_PATH = "matches/forget";
    public static final String BIND_CREATED_PATH = "matches/created";
    public static final String REMEMBER_CORRECTION_PATH = "corrections";
    public static final String FORGET_CORRECTION_PATH = "corrections/forget";
    public static final String OBJECT_SHEETS_PATH = "object-sheets";
    public static final String OBJECT_SHEETS_VALIDATE_PATH = "object-sheets/validate";
    public static final String TYPE_PROPERTIES_PATH = "object-types/properties";

    private static final List<String> ACCEPTED_EXTENSIONS = Arrays.asList("xlsx", "xlsm", "xls");

    private static final String SESSION_NOT_FOUND =
            "No open conversation carries this identifier. It may have expired, or belong to "
                    + "another account.";

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

    //#region profiles

    @GET
    @Path(PROFILES_PATH)
    @ApiOperation(value = "List the file families the assistant recognises")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Known import profiles", response = ImportProfileDTO.class,
                    responseContainer = "List")
    })
    public Response getProfiles() {
        List<ImportProfileDTO> profiles = new ArrayList<>();
        for (ImportProfile profile : new ImportProfileRegistry().getProfiles()) {
            profiles.add(ImportProfileDTO.fromModel(profile));
        }
        return new PaginatedListResponse<>(profiles).getResponse();
    }

    @GET
    @Path(ASSISTANT_PATH)
    @ApiOperation(value = "Tell whether the language model can hold a conversation",
            notes = "Asks the configured endpoint for its models, which costs no token. The page folds "
                    + "the conversation away when it cannot take place; the rest works without it.")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The assistant's status", response = AssistantStatusDTO.class)
    })
    public Response getAssistantStatus() {
        return new SingleObjectResponse<>(AssistantStatusDTO.of(llm.isEnable(), llm.isReachable()))
                .getResponse();
    }

    //#endregion

    //#region sessions

    @POST
    @Path(SESSIONS_PATH)
    @ApiOperation(
            value = "Upload a spreadsheet and open a conversation about it",
            notes = "Reads the file, recognises its family, checks every name it holds against this "
                    + "instance, and returns the assistant's opening analysis. Nothing is written."
    )
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Conversation opened", response = AiImportSessionDTO.class),
            @ApiResponse(code = 400, message = "The file could not be read as a spreadsheet",
                    response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "No language model is configured",
                    response = ErrorResponse.class)
    })
    public Response createSession(
            @ApiParam(value = "Spreadsheet to analyse", required = true, type = "file")
            @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @ApiParam(value = "Force a file family instead of detecting it", example = "vitis-explorer")
            @QueryParam("profile") String profileId
    ) {
        if (!config.enabled()) {
            return disabled();
        }
        if (file == null) {
            return badRequest("No file was uploaded.", "Attach the spreadsheet in the 'file' part.");
        }

        String fileName = fileDetail == null || fileDetail.getFileName() == null
                ? "uploaded-file"
                : fileDetail.getFileName();

        Response rejected = rejectUnsuitableFile(file, fileName);
        if (rejected != null) {
            return rejected;
        }

        AiImportSession session = sessions.create(currentUser.getUri());
        try {
            newChatService().start(session, file, fileName, profileId);
        } catch (WorkbookReadException e) {
            sessions.remove(session.getId());
            LOGGER.warn("Could not read the uploaded file {}", fileName, e);
            return badRequest("The file could not be read as a spreadsheet.",
                    "Check that " + fileName + " is a valid .xlsx workbook and is not password protected.");
        } catch (RuntimeException e) {
            sessions.remove(session.getId());
            throw e;
        }

        try {
            // Stored from the start, so that closing the page never loses the conversation.
            store().saveFile(session.getId(), file);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not store the workbook of import conversation {}", session.getId(), e);
        }
        persist(session);

        return Response.status(Response.Status.CREATED)
                .entity(new SingleObjectResponse<>(toDto(session)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path(SESSIONS_PATH)
    @ApiOperation(
            value = "List the user's stored import conversations",
            notes = "Every conversation is stored as it goes, with its file, and kept for the "
                    + "configured number of days after its last activity. Any of them can be "
                    + "resumed by reading it like an open one."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The stored conversations, most recent first",
                    response = SavedSessionDTO.class, responseContainer = "List")
    })
    public Response listSavedSessions() {
        if (!config.enabled()) {
            return disabled();
        }
        List<SavedSessionDTO> saved = new ArrayList<>();
        store().list(currentUser.getUri()).forEach(summary ->
                saved.add(SavedSessionDTO.fromModel(summary, config.savedSessionDays())));
        return new PaginatedListResponse<>(saved).getResponse();
    }

    @GET
    @Path(SESSIONS_PATH + "/{sessionId}")
    @ApiOperation(value = "Get an open conversation, with its analysis and its history")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The conversation", response = AiImportSessionDTO.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getSession(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        Optional<AiImportSession> session = findSession(sessionId);
        if (!session.isPresent()) {
            return notFound();
        }
        return new SingleObjectResponse<>(toDto(session.get())).getResponse();
    }

    @DELETE
    @Path(SESSIONS_PATH + "/{sessionId}")
    @ApiOperation(value = "Close a conversation and delete it, with its stored file")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Conversation closed"),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response deleteSession(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        // Not through findSession: deleting a stored conversation must not rebuild it first.
        boolean open = sessions.get(sessionId, currentUser.getUri()).isPresent();
        if (open) {
            sessions.remove(sessionId);
        }
        boolean stored = store().delete(sessionId, currentUser.getUri());
        if (!open && !stored) {
            return notFound();
        }
        return new SingleObjectResponse<>(sessionId).getResponse();
    }

    //#endregion

    //#region conversation

    @POST
    @Path(MESSAGES_PATH)
    @ApiOperation(
            value = "Ask the assistant a question about the uploaded file",
            notes = "The assistant may query this instance while answering. Every lookup it performs "
                    + "is listed on the reply."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The assistant's reply", response = ChatMessageDTO.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "No language model is configured",
                    response = ErrorResponse.class)
    })
    public Response askQuestion(
            @ApiParam(value = "The question, and the conversation it belongs to", required = true)
            @NotNull @Valid ChatQuestionDTO question
    ) {
        if (!config.enabled()) {
            return disabled();
        }
        Optional<AiImportSession> session = findSession(question.getSessionId());
        if (!session.isPresent()) {
            return notFound();
        }

        AiImportMessage reply = newChatService().ask(session.get(), question.getContent());
        persist(session.get());
        return new SingleObjectResponse<>(ChatMessageDTO.fromModel(reply)).getResponse();
    }

    //#endregion

    //#region report

    @GET
    @Path(SESSIONS_PATH + "/{sessionId}/" + REPORT_PATH)
    @ApiOperation(value = "Get the validation report of a conversation")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The report", response = ResolutionReportDTO.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getReport(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        Optional<AiImportSession> session = findSession(sessionId);
        if (!session.isPresent()) {
            return notFound();
        }
        return new SingleObjectResponse<>(
                ResolutionReportDTO.fromModel(session.get().getReport())).getResponse();
    }

    @POST
    @Path(SESSIONS_PATH + "/{sessionId}/" + REVALIDATE_PATH)
    @ApiOperation(
            value = "Check the file against the instance again",
            notes = "Use it after creating what the report said was missing. The conversation is "
                    + "kept, and the assistant argues from the new state."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The refreshed report", response = ResolutionReportDTO.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response revalidate(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        Optional<AiImportSession> session = findSession(sessionId);
        if (!session.isPresent()) {
            return notFound();
        }

        newChatService().revalidate(session.get());
        persist(session.get());
        return new SingleObjectResponse<>(
                ResolutionReportDTO.fromModel(session.get().getReport())).getResponse();
    }

    //#endregion

    //#region mapping

    @GET
    @Path(SESSIONS_PATH + "/{sessionId}/" + MAPPING_PATH)
    @ApiOperation(
            value = "Get how each column was mapped, and where the values disagree with it",
            notes = "Computed from the recognised template and from this instance, never by the "
                    + "assistant, so that the same file always maps the same way."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The column mapping", response = ColumnMappingDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getMapping(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        Optional<AiImportSession> session = findSession(sessionId);
        if (!session.isPresent()) {
            return notFound();
        }
        List<ColumnMappingDTO> mappings = new ArrayList<>();
        for (ColumnMapping mapping : session.get().getMappings()) {
            mappings.add(ColumnMappingDTO.fromModel(mapping));
        }
        return new PaginatedListResponse<>(mappings).getResponse();
    }

    //#endregion

    //#region creation

    @GET
    @Path(SESSIONS_PATH + "/{sessionId}/" + REQUIREMENTS_PATH)
    @ApiOperation(
            value = "Ask what creating a project, an experiment or the data would take",
            notes = "Returns the form fields with the values the file suggests, and what stands in "
                    + "the way. Use it to build the form before the user fills anything."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "What the creation would take",
                    response = CreationRequirementsDTO.class),
            @ApiResponse(code = 400, message = "Unknown target", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getCreationRequirements(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "PROJECT, EXPERIMENT or DATA", required = true, example = "EXPERIMENT")
            @QueryParam("target") @NotNull String target
    ) {
        Optional<AiImportSession> session = findSession(sessionId);
        if (!session.isPresent()) {
            return notFound();
        }
        CreationTarget parsed = CreationTarget.parse(target).orElse(null);
        if (parsed == null) {
            return unknownTarget(target);
        }

        CreationRequirements requirements = newCreationService()
                .requirementsFor(parsed, session.get());
        return new SingleObjectResponse<>(CreationRequirementsDTO.fromModel(requirements)).getResponse();
    }

    @POST
    @Path(CREATE_PATH)
    @ApiOperation(
            value = "Confirm a creation the assistant proposed",
            notes = "Writes the draft named by proposal_id, after revalidating it against the "
                    + "current state of the instance. Refused if a required field is empty, if "
                    + "something blocks it, or if the draft was already applied. The report is "
                    + "recomputed and returned."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created", response = CreationResultDTO.class),
            @ApiResponse(code = 400, message = "A required field is missing, something blocks it, "
                    + "or the draft was already applied", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation or draft",
                    response = ErrorResponse.class)
    })
    public Response create(
            @ApiParam(value = "The draft to confirm, with any correction", required = true)
            @NotNull @Valid CreationRequestDTO request
    ) throws Exception {
        if (!config.enabled()) {
            return disabled();
        }
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();

        Optional<CreationProposal> drafted = session.getProposal(request.getProposalId());
        if (!drafted.isPresent()) {
            return notFound();
        }
        CreationProposal proposal = drafted.get();
        if (proposal.getStatus() != CreationProposal.Status.PENDING) {
            // Confirming twice would create twice. The status is what prevents it.
            return badRequest("This draft was already handled.",
                    "Its state is " + proposal.getStatus().name().toLowerCase()
                            + ". Ask the assistant for a new one.");
        }

        // The card's values win where they were edited; the rest of the draft stands.
        Map<String, String> values = new LinkedHashMap<>(proposal.getFields());
        if (request.getValues() != null) {
            values.putAll(request.getValues());
        }

        CreationTarget target = proposal.getTarget();
        AiImportCreationService creationService = newCreationService();

        // Revalidated rather than trusted: the instance can have changed since the draft was made.
        CreationRequirements requirements = creationService.requirementsFor(target, session);
        if (!requirements.isAvailable()) {
            return badRequest("This cannot be created yet.",
                    "What stands in the way: " + String.join("; ", requirements.getBlockers()));
        }
        String missing = firstEmptyRequiredField(requirements, values);
        if (missing != null) {
            return badRequest("A required field is missing.",
                    "The field '" + missing + "' has to be filled before creating.");
        }

        CreationResultDTO result = new CreationResultDTO().setTarget(target.name());
        try {
            CreationOutcome outcome = creationService.apply(target, session, values);
            if (outcome.getUri() != null) {
                result.setUri(outcome.getUri());
            } else {
                result.setInsertedCount(outcome.getInsertedCount());
            }
            if (outcome.isRefused()) {
                result.setRefused(true).setValidation(BulkValidationDTO.of(target.name(),
                        outcome.getRowsChecked(), outcome.getErrors(), session.getWorkbook()));
            }
            result.setBatches(outcome.getBatches());
        } catch (CreationFieldException e) {
            // Named field, named reason: the user is told where to go, not what the storage said.
            return badRequest("The field '" + e.getField() + "' cannot be used as it stands.",
                    e.getMessage());
        } catch (IllegalArgumentException e) {
            // Field-shaped problems the card should have caught, e.g. an unparseable date.
            return badRequest("A field could not be used as it stands.", e.getMessage());
        }

        result.setProposalId(proposal.getId());
        if (result.isRefused() && result.getInsertedCount() != null && result.getInsertedCount() > 0) {
            // Interrupted between batches: some data is in the instance. Confirming the draft again
            // would write those batches a second time, under a new provenance the duplicate check
            // cannot catch — so the draft is closed, and the batches written are named for the user
            // to keep or delete from the data import history.
            proposal.setStatus(CreationProposal.Status.CANCELLED)
                    .setInsertedCount(result.getInsertedCount());
            if (proposal == session.getPendingProposal()) {
                session.setPendingProposal(null);
            }
            persist(session);
            return Response.ok(new SingleObjectResponse<>(result))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (result.isRefused()) {
            // Nothing was written, so the draft stays open: the user fixes what is named and
            // validates again. Recomputing the report would be pointless, the instance is as it was.
            return Response.ok(new SingleObjectResponse<>(result))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        proposal.setStatus(CreationProposal.Status.APPLIED)
                .setResultUri(result.getUri())
                .setInsertedCount(result.getInsertedCount());
        if (proposal == session.getPendingProposal()) {
            session.setPendingProposal(null);
        }
        // The instance just changed, so the report the user is looking at is stale.
        newChatService().revalidate(session);
        persist(session);
        result.setReport(ResolutionReportDTO.fromModel(session.getReport()));

        return Response.status(Response.Status.CREATED)
                .entity(new SingleObjectResponse<>(result))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Path(CANCEL_PATH)
    @ApiOperation(value = "Discard a creation the assistant proposed")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Draft discarded"),
            @ApiResponse(code = 404, message = "Unknown conversation or draft",
                    response = ErrorResponse.class)
    })
    public Response cancelProposal(
            @ApiParam(value = "The draft to discard", required = true)
            @NotNull @Valid CreationRequestDTO request
    ) {
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        Optional<CreationProposal> drafted = found.get().getProposal(request.getProposalId());
        if (!drafted.isPresent()) {
            return notFound();
        }
        CreationProposal proposal = drafted.get();
        if (proposal.getStatus() == CreationProposal.Status.PENDING) {
            proposal.setStatus(CreationProposal.Status.CANCELLED);
        }
        if (proposal == found.get().getPendingProposal()) {
            found.get().setPendingProposal(null);
        }
        persist(found.get());
        return new SingleObjectResponse<>(proposal.getId()).getResponse();
    }

    /**
     * @return the name of the first required field left empty, or {@code null} when all are filled
     */
    private String firstEmptyRequiredField(CreationRequirements requirements,
                                           Map<String, String> values) {
        for (var field : requirements.getFields()) {
            String value = values == null ? null : values.get(field.getName());
            if (!field.isSatisfiedBy(value)) {
                return field.getName();
            }
        }
        return null;
    }

    private Response unknownTarget(String target) {
        return badRequest("Unknown creation target.",
                "'" + target + "' is not one of " + CreationTarget.choices() + ".");
    }

    private AiImportCreationService newCreationService() {
        return new AiImportCreationService(sparql, nosql, fs, currentUser, coreModule);
    }

    //#endregion

    //#region object sheets

    @GET
    @Path(SESSIONS_PATH + "/{sessionId}/" + OBJECT_SHEETS_PATH)
    @ApiOperation(
            value = "List the sheets whose rows would become scientific objects",
            notes = "Each with its rows, the type its objects would take, what each column would "
                    + "become, and what that type contradicts in the mapping. Nothing is written."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The object sheets", response = ObjectSheetDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getObjectSheets(
            @ApiParam(value = "Conversation identifier", required = true) @PathParam("sessionId") @NotNull String sessionId
    ) throws Exception {
        if (!config.enabled()) {
            return disabled();
        }
        Optional<AiImportSession> found = findSession(sessionId);
        if (!found.isPresent()) {
            return notFound();
        }
        List<ObjectSheetDTO> sheets = new ArrayList<>();
        for (ObjectSheet sheet : ObjectSheets.of(found.get())) {
            sheets.add(objectSheetDto(sheet));
        }
        return new PaginatedListResponse<>(sheets).getResponse();
    }

    @POST
    @Path(OBJECT_SHEETS_PATH)
    @ApiOperation(
            value = "Choose the type of a sheet's objects, and what its columns become",
            notes = "Kept with the conversation, and when it is resumed. Only the fields sent change; "
                    + "a column not sent keeps its mapping. The name column always stays the name."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The sheet as it now stands", response = ObjectSheetDTO.class),
            @ApiResponse(code = 400, message = "Not a scientific object type, an unknown column, or a "
                    + "target that is neither a URI nor a position", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation or sheet", response = ErrorResponse.class)
    })
    public Response updateObjectSheet(
            @ApiParam(value = "The choices for the sheet", required = true) @NotNull @Valid ObjectSheetPlanDTO request
    ) throws Exception {
        if (!config.enabled()) {
            return disabled();
        }
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();
        Optional<ObjectSheet> sheet = ObjectSheets.named(session, request.getSheet());
        if (!sheet.isPresent()) {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Unknown object sheet",
                    "No sheet named '" + request.getSheet() + "' lists objects in this file.").getResponse();
        }
        if (request.getRdfType() != null && !new TypeProperties(currentUser.getLanguage()).isObjectType(request.getRdfType())) {
            return badRequest("Not a scientific object type.",
                    "'" + request.getRdfType() + "' is not a type of scientific object in this instance.");
        }
        Map<String, String> mapping = request.getMapping() == null ? Map.of() : request.getMapping();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (!sheet.get().getHeaders().contains(entry.getKey())) {
                return badRequest("Unknown column.", "The sheet '" + request.getSheet()
                        + "' has no column '" + entry.getKey() + "'.");
            }
            if (!isTarget(entry.getValue())) {
                return badRequest("Not a target.", "'" + entry.getValue() + "' is neither a property URI nor "
                        + "a position (x, y); leave it empty for a column that is not written.");
            }
        }

        ObjectSheetPlan plan = session.getObjectPlans().computeIfAbsent(request.getSheet(), ObjectSheetPlan::new);
        if (request.getRdfType() != null) {
            plan.setType(request.getRdfType());
        }
        if (request.getIncluded() != null) {
            plan.setIncluded(request.getIncluded());
        }
        mapping.forEach(plan::map);
        persist(session);

        return new SingleObjectResponse<>(objectSheetDto(ObjectSheets.named(session, request.getSheet()).get()))
                .getResponse();
    }

    @GET
    @Path(TYPE_PROPERTIES_PATH)
    @ApiOperation(
            value = "List the properties a scientific object type accepts",
            notes = "The ones the platform's scientific-object import accepts for that type: what a "
                    + "column of an object sheet can be mapped to."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The properties", response = TypePropertyDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 400, message = "Not a scientific object type", response = ErrorResponse.class)
    })
    public Response getTypeProperties(
            @ApiParam(value = "Scientific object type", required = true, example = "vocabulary:Plot")
            @QueryParam("type") @NotNull URI type
    ) throws Exception {
        TypeProperties properties = new TypeProperties(currentUser.getLanguage());
        if (!properties.isObjectType(type)) {
            return badRequest("Not a scientific object type.",
                    "'" + type + "' is not a type of scientific object in this instance.");
        }
        List<TypePropertyDTO> dtos = new ArrayList<>();
        properties.of(type).forEach(property -> dtos.add(TypePropertyDTO.fromModel(property)));
        return new PaginatedListResponse<>(dtos).getResponse();
    }

    @POST
    @Path(OBJECT_SHEETS_VALIDATE_PATH)
    @ApiOperation(
            value = "Check the object sheets against the platform, without writing anything",
            notes = "The same check the creation makes before it writes: the module's own — a type "
                    + "missing, a name that resolves to nothing — then the platform's scientific-object "
                    + "import, row by row. The errors are given on the workbook's own rows."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "What the check found", response = BulkValidationDTO.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response validateObjectSheets(
            @ApiParam(value = "The conversation and the experiment", required = true)
            @NotNull @Valid ObjectSheetsValidationDTO request
    ) throws Exception {
        if (!config.enabled()) {
            return disabled();
        }
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();
        Map<String, String> values = new LinkedHashMap<>();
        values.put(ScientificObjectBulkImport.EXPERIMENT, request.getExperiment().toString());
        BulkOutcome outcome = new ScientificObjectBulkImport(sparql, nosql, fs, currentUser).validate(session, values);
        return new SingleObjectResponse<>(BulkValidationDTO.of(CreationTarget.SCIENTIFIC_OBJECTS.name(),
                outcome.getRowsChecked(), outcome.getErrors(), session.getWorkbook())).getResponse();
    }

    /**
     * The sheet, with what its type contradicts in its mapping — or that its type is none.
     */
    private ObjectSheetDTO objectSheetDto(ObjectSheet sheet) throws Exception {
        List<ReportMessage> problems = new ArrayList<>();
        if (sheet.getType() != null) {
            TypeProperties properties = new TypeProperties(currentUser.getLanguage());
            if (properties.isObjectType(sheet.getType())) {
                problems.addAll(ObjectSheets.problemsOf(sheet, properties.of(sheet.getType())));
            } else {
                problems.add(ReportMessage.of(ObjectSheets.PROBLEM + "notObjectType",
                                "The type stated for this sheet is not a scientific object type.")
                        .with("sheet", sheet.getName())
                        .with("type", sheet.getType().toString()));
            }
        }
        return ObjectSheetDTO.fromModel(sheet, problems);
    }

    /**
     * A target is a property URI, a position, or nothing.
     */
    private static boolean isTarget(String target) {
        if (target == null || target.isEmpty() || ObjectTargets.isPosition(target)) {
            return true;
        }
        try {
            return URI.create(target).isAbsolute();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    //#endregion

    //#region near matches

    @POST
    @Path(CONFIRM_MATCH_PATH)
    @ApiOperation(
            value = "Confirm which existing resource a misspelt name of the file means",
            notes = "Only one of the suggestions the report made for that name is accepted. The "
                    + "confirmation holds for the rest of the conversation, survives revalidation, "
                    + "and makes the name resolve as if it had been spelt right. Nothing is renamed, "
                    + "in the file or in the instance."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The refreshed report", response = ResolutionReportDTO.class),
            @ApiResponse(code = 400, message = "Not one of the suggestions", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response confirmMatch(
            @ApiParam(value = "The name and the suggestion it means", required = true)
            @NotNull @Valid MatchConfirmationDTO request
    ) {
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();

        Optional<ReportCategory> category = ReportCategory.fromKey(request.getCategory());
        if (!category.isPresent() || !category.get().allowsNearMatching()) {
            return badRequest("This category takes no confirmation.",
                    "'" + request.getCategory() + "' is not a category whose names can be matched "
                            + "by spelling.");
        }
        // The user chooses among what the resolution proposed, never an arbitrary URI: the
        // suggestions were read through the DAOs, under this user's access rights.
        Optional<ResourceReference> suggestion = session.getReport() == null
                ? Optional.empty()
                : session.getReport().suggestion(category.get(), request.getValue(), request.getUri());
        if (!suggestion.isPresent()) {
            return badRequest("Not one of the suggestions.",
                    "Pick the resource among those the report suggested for '" + request.getValue()
                            + "'.");
        }

        session.getConfirmedMatches().confirm(category.get(), request.getValue(),
                new ResourceReference(suggestion.get().getUri(), suggestion.get().getName()));
        newChatService().revalidate(session);
        persist(session);
        return new SingleObjectResponse<>(ResolutionReportDTO.fromModel(session.getReport()))
                .getResponse();
    }

    @POST
    @Path(BIND_CREATED_PATH)
    @ApiOperation(
            value = "Bind a name of the file to the resource just created for it",
            notes = "Called after a platform creation form, opened from a row of the report, has "
                    + "created its resource. The form may have renamed it — the variable form names "
                    + "a variable after its components — so the row is bound to the created URI "
                    + "rather than found again by its name. The URI is read back under the current "
                    + "user's rights before it is accepted. Nothing is renamed."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The refreshed report", response = ResolutionReportDTO.class),
            @ApiResponse(code = 400, message = "Not a resource this user can see", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response bindCreatedResource(
            @ApiParam(value = "The name of the file and the resource created for it", required = true)
            @NotNull @Valid MatchConfirmationDTO request
    ) {
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();

        Optional<ReportCategory> category = ReportCategory.fromKey(request.getCategory());
        if (!category.isPresent() || !category.get().allowsNearMatching() || request.getUri() == null) {
            return badRequest("This category takes no binding.",
                    "'" + request.getCategory() + "' is not a category whose rows can be bound to a "
                            + "resource.");
        }
        Optional<ResourceReference> created = new ResolutionService(sparql, nosql, fs, currentUser, null)
                .visibleResource(category.get(), request.getUri());
        if (!created.isPresent()) {
            return badRequest("Not a resource you can see.",
                    "No " + category.get().getKey() + " with this URI is visible to you.");
        }

        session.getConfirmedMatches().confirm(category.get(), request.getValue(), created.get());
        newChatService().revalidate(session);
        persist(session);
        return new SingleObjectResponse<>(ResolutionReportDTO.fromModel(session.getReport()))
                .getResponse();
    }

    @POST
    @Path(FORGET_MATCH_PATH)
    @ApiOperation(
            value = "Take back a confirmation",
            notes = "The name goes back to missing, with its suggestions, as before the confirmation."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The refreshed report", response = ResolutionReportDTO.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response forgetMatch(
            @ApiParam(value = "The name whose confirmation to take back", required = true)
            @NotNull @Valid MatchConfirmationDTO request
    ) {
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();

        ReportCategory.fromKey(request.getCategory()).ifPresent(category ->
                session.getConfirmedMatches().forget(category, request.getValue()));
        newChatService().revalidate(session);
        persist(session);
        return new SingleObjectResponse<>(ResolutionReportDTO.fromModel(session.getReport()))
                .getResponse();
    }

    @POST
    @Path(REMEMBER_CORRECTION_PATH)
    @ApiOperation(
            value = "Teach the instance what a misspelt name means, for every later import",
            notes = "The misspelling is stored as a skos:hiddenLabel of the resource, with who taught "
                    + "it and when. Later imports, by anyone, recognise it outright and say on whose "
                    + "word. Needs the right to modify that kind of resource, and only a suggestion "
                    + "or a confirmed match of that very name is accepted."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The refreshed report", response = ResolutionReportDTO.class),
            @ApiResponse(code = 400, message = "Not a suggestion or a confirmed match", response = ErrorResponse.class),
            @ApiResponse(code = 403, message = "Not allowed to modify this kind of resource", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response rememberCorrection(
            @ApiParam(value = "The name and the resource it means", required = true)
            @NotNull @Valid MatchConfirmationDTO request
    ) throws Exception {
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();

        Optional<ReportCategory> category = ReportCategory.fromKey(request.getCategory());
        if (!category.isPresent() || category.get().getModificationCredential() == null) {
            return badRequest("This category takes no correction.",
                    "'" + request.getCategory() + "' is not a category whose names can be taught.");
        }
        if (!mayModify(category.get())) {
            return new ErrorResponse(Response.Status.FORBIDDEN,
                    "Not allowed to teach this correction.",
                    "A remembered correction applies to everyone's imports, so it takes the right "
                            + "to modify this kind of resource.").getResponse();
        }
        Optional<ResourceReference> meant = session.getReport() == null
                ? Optional.empty()
                : session.getReport().suggestionOrConfirmedMatch(
                        category.get(), request.getValue(), request.getUri());
        if (!meant.isPresent()) {
            return badRequest("Not a suggestion or a confirmed match.",
                    "Pick the resource among those the report proposed for '" + request.getValue()
                            + "'.");
        }

        correctionStore().remember(category.get(), request.getValue(), meant.get().getUri(),
                meant.get().getName(), currentUser.getUri(), displayName());
        // The session no longer needs its own confirmation: the instance now knows.
        session.getConfirmedMatches().forget(category.get(), request.getValue());
        newChatService().revalidate(session);
        persist(session);
        return new SingleObjectResponse<>(ResolutionReportDTO.fromModel(session.getReport()))
                .getResponse();
    }

    @POST
    @Path(FORGET_CORRECTION_PATH)
    @ApiOperation(
            value = "Make the instance forget a correction it was taught",
            notes = "For everyone: later imports treat the misspelling as unknown again. Needs the "
                    + "right to modify that kind of resource."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The refreshed report", response = ResolutionReportDTO.class),
            @ApiResponse(code = 403, message = "Not allowed to modify this kind of resource", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation or correction", response = ErrorResponse.class)
    })
    public Response forgetCorrection(
            @ApiParam(value = "The name whose correction to forget", required = true)
            @NotNull @Valid MatchConfirmationDTO request
    ) throws Exception {
        Optional<AiImportSession> found = findSession(request.getSessionId());
        if (!found.isPresent()) {
            return notFound();
        }
        AiImportSession session = found.get();

        Optional<ReportCategory> category = ReportCategory.fromKey(request.getCategory());
        if (!category.isPresent() || category.get().getModificationCredential() == null) {
            return badRequest("This category takes no correction.",
                    "'" + request.getCategory() + "' is not a category whose names can be taught.");
        }
        if (!mayModify(category.get())) {
            return new ErrorResponse(Response.Status.FORBIDDEN,
                    "Not allowed to forget this correction.",
                    "Forgetting a correction changes everyone's imports, so it takes the right to "
                            + "modify this kind of resource.").getResponse();
        }
        CorrectionStore store = correctionStore();
        Optional<LearnedCorrection> correction = store.load().lookup(category.get(), request.getValue());
        if (!correction.isPresent()) {
            return new ErrorResponse(Response.Status.NOT_FOUND, "No such correction.",
                    "The instance remembers no correction for '" + request.getValue() + "'.")
                    .getResponse();
        }
        store.forget(correction.get());
        newChatService().revalidate(session);
        persist(session);
        return new SingleObjectResponse<>(ResolutionReportDTO.fromModel(session.getReport()))
                .getResponse();
    }

    private CorrectionStore correctionStore() {
        return new CorrectionStore(sparql, CorrectionStore.graphFor(sparql.getBaseURI()));
    }

    /**
     * Administrators, or holders of the category's modification right.
     */
    private boolean mayModify(ReportCategory category) throws Exception {
        if (Boolean.TRUE.equals(currentUser.isAdmin())) {
            return true;
        }
        return new AccountDAO(sparql).getCredentialList(currentUser.getUri())
                .contains(category.getModificationCredential());
    }

    /**
     * A name people recognise on a correction, rather than an account URI.
     */
    private String displayName() {
        PersonModel person = currentUser.getLinkedPerson();
        if (person != null && (person.getFirstName() != null || person.getLastName() != null)) {
            return ((person.getFirstName() == null ? "" : person.getFirstName()) + " "
                    + (person.getLastName() == null ? "" : person.getLastName())).trim();
        }
        return currentUser.getName();
    }

    //#endregion

    //#region helpers

    private AiImportSessionStore store() {
        return new AiImportSessionStore(nosql, fs, ObjectMapperContextResolver.getObjectMapper(),
                config.savedSessionDays());
    }

    /**
     * The user's session: from the memory cache, or else resumed from storage.
     * <p>
     * A conversation left for longer than the cache keeps it is still the user's, so every endpoint
     * finds it the same way and none of them has to know where it was. Resuming reads the file again
     * and recomputes the report against the instance as it is now, without asking the assistant
     * anything.
     */
    private Optional<AiImportSession> findSession(String sessionId) {
        Optional<AiImportSession> cached = sessions.get(sessionId, currentUser.getUri());
        if (cached.isPresent()) {
            return cached;
        }
        try {
            AiImportSessionStore store = store();
            Optional<SavedSession> saved = store.load(sessionId, currentUser.getUri());
            if (!saved.isPresent()) {
                return Optional.empty();
            }
            AiImportSession session = saved.get().toSession(currentUser.getUri());
            File workbook = File.createTempFile("ai-import-", ".xlsx");
            try {
                Files.write(workbook.toPath(), store.readFile(sessionId));
                newChatService().restore(session, workbook, session.getFileName(),
                        session.getProfileId());
            } finally {
                Files.deleteIfExists(workbook.toPath());
            }
            sessions.put(session);
            return Optional.of(session);
        } catch (IOException | WorkbookReadException | RuntimeException e) {
            // A session that cannot be rebuilt is reported as absent rather than half restored.
            LOGGER.warn("Could not resume import conversation {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Stores the session after a step that changed it. A failure costs the ability to resume, not
     * the step the user just took, so it is logged rather than returned.
     */
    private void persist(AiImportSession session) {
        try {
            store().save(session);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not store import conversation {}", session.getId(), e);
        }
    }

    private AiImportChatService newChatService() {
        return new AiImportChatService(sparql, nosql, fs, currentUser, config, llm,
                ObjectMapperContextResolver.getObjectMapper(), coreModule);
    }

    private AiImportSessionDTO toDto(AiImportSession session) {
        CreationRequirements requirements = null;
        if (session.getPendingProposal() != null) {
            requirements = newCreationService()
                    .requirementsFor(session.getPendingProposal().getTarget(), session);
        }
        return AiImportSessionDTO.fromModel(session, config.sampleRowsPerSheet(), requirements);
    }

    /**
     * Rejects a file before it is opened, so that a wrong upload gives a clear answer instead of a
     * parser error, and so that an oversized one is never read into memory.
     */
    private Response rejectUnsuitableFile(File file, String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        if (!ACCEPTED_EXTENSIONS.contains(extension)) {
            return badRequest("Only spreadsheets are accepted.",
                    "The file " + fileName + " has the extension '" + extension + "'. Accepted "
                            + "extensions are " + String.join(", ", ACCEPTED_EXTENSIONS) + ".");
        }

        long maxBytes = (long) config.maxFileSizeMb() * 1024L * 1024L;
        if (file.length() > maxBytes) {
            return badRequest("The file is too large.",
                    "The file is " + (file.length() / (1024 * 1024)) + " MB, and the limit is "
                            + config.maxFileSizeMb() + " MB.");
        }
        return null;
    }

    private Response disabled() {
        return new ErrorResponse(Response.Status.SERVICE_UNAVAILABLE,
                "The import assistant is not enabled.",
                "Set 'ai-import.enabled' and configure 'ai-import.llm' in the instance configuration.")
                .getResponse();
    }

    private Response notFound() {
        return new ErrorResponse(Response.Status.NOT_FOUND, "Unknown conversation", SESSION_NOT_FOUND)
                .getResponse();
    }

    private Response badRequest(String title, String detail) {
        return new ErrorResponse(Response.Status.BAD_REQUEST, title, detail).getResponse();
    }

    //#endregion
}
