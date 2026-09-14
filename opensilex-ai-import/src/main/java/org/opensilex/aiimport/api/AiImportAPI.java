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
import org.opensilex.aiimport.api.dto.ChatMessageDTO;
import org.opensilex.aiimport.api.dto.ChatQuestionDTO;
import org.opensilex.aiimport.api.dto.ColumnMappingDTO;
import org.opensilex.aiimport.api.dto.CreationRequestDTO;
import org.opensilex.aiimport.api.dto.CreationProposalDTO;
import org.opensilex.aiimport.api.dto.CreationRequirementsDTO;
import org.opensilex.aiimport.api.dto.CreationResultDTO;
import org.opensilex.aiimport.api.dto.ImportProfileDTO;
import org.opensilex.aiimport.api.dto.ResolutionReportDTO;
import org.opensilex.aiimport.api.dto.UnresolvedRowDTO;
import org.opensilex.aiimport.create.AiImportCreationService;
import org.opensilex.aiimport.create.CreationFieldException;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.DataInsertionResult;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.service.AiImportChatService;
import org.opensilex.aiimport.service.AiImportMessage;
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
    public static final String MESSAGES_PATH = "messages";
    public static final String REVALIDATE_PATH = "revalidate";
    public static final String REPORT_PATH = "report";
    public static final String MAPPING_PATH = "mapping";
    public static final String REQUIREMENTS_PATH = "creation-requirements";
    public static final String CREATE_PATH = "create";
    public static final String CANCEL_PATH = "cancel-proposal";

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

        return Response.status(Response.Status.CREATED)
                .entity(new SingleObjectResponse<>(toDto(session)))
                .type(MediaType.APPLICATION_JSON)
                .build();
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
        Optional<AiImportSession> session = sessions.get(sessionId, currentUser.getUri());
        if (!session.isPresent()) {
            return notFound();
        }
        return new SingleObjectResponse<>(toDto(session.get())).getResponse();
    }

    @DELETE
    @Path(SESSIONS_PATH + "/{sessionId}")
    @ApiOperation(value = "Close a conversation and forget the file it was about")
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
        Optional<AiImportSession> session = sessions.get(sessionId, currentUser.getUri());
        if (!session.isPresent()) {
            return notFound();
        }
        sessions.remove(sessionId);
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
        Optional<AiImportSession> session = sessions.get(question.getSessionId(), currentUser.getUri());
        if (!session.isPresent()) {
            return notFound();
        }

        AiImportMessage reply = newChatService().ask(session.get(), question.getContent());
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
        Optional<AiImportSession> session = sessions.get(sessionId, currentUser.getUri());
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
        Optional<AiImportSession> session = sessions.get(sessionId, currentUser.getUri());
        if (!session.isPresent()) {
            return notFound();
        }

        newChatService().revalidate(session.get());
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
        Optional<AiImportSession> session = sessions.get(sessionId, currentUser.getUri());
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
        Optional<AiImportSession> session = sessions.get(sessionId, currentUser.getUri());
        if (!session.isPresent()) {
            return notFound();
        }
        CreationTarget parsed = parseTarget(target);
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
        Optional<AiImportSession> found = sessions.get(request.getSessionId(), currentUser.getUri());
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
            switch (target) {
                case PROJECT:
                    result.setUri(creationService.createProject(session, values));
                    break;
                case EXPERIMENT:
                    result.setUri(creationService.createExperiment(session, values));
                    break;
                case VARIABLE:
                    result.setInsertedCount(creationService.importVariables(session, values));
                    break;
                case EVENT:
                    result.setInsertedCount(creationService.createEvents(session, values));
                    break;
                case DATA:
                default:
                    DataInsertionResult insertion = creationService.insertData(session, values);
                    result.setInsertedCount(insertion.getInsertedCount());
                    if (insertion.isRefused()) {
                        result.setRefused(true)
                                .setUnresolvedCount(insertion.getUnresolvedCount())
                                .setUnresolved(
                                        UnresolvedRowDTO.fromModels(insertion.getUnresolved()));
                    }
                    break;
            }
        } catch (CreationFieldException e) {
            // Named field, named reason: the user is told where to go, not what the storage said.
            return badRequest("The field '" + e.getField() + "' cannot be used as it stands.",
                    e.getMessage());
        } catch (IllegalArgumentException e) {
            // Field-shaped problems the card should have caught, e.g. an unparseable date.
            return badRequest("A field could not be used as it stands.", e.getMessage());
        }

        result.setProposalId(proposal.getId());
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
        Optional<AiImportSession> found = sessions.get(request.getSessionId(), currentUser.getUri());
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

    private CreationTarget parseTarget(String target) {
        if (target == null) {
            return null;
        }
        try {
            return CreationTarget.valueOf(target.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Response unknownTarget(String target) {
        return badRequest("Unknown creation target.",
                "'" + target + "' is not one of PROJECT, EXPERIMENT or DATA.");
    }

    private AiImportCreationService newCreationService() {
        return new AiImportCreationService(sparql, nosql, fs, currentUser, coreModule);
    }

    //#endregion

    //#region helpers

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
