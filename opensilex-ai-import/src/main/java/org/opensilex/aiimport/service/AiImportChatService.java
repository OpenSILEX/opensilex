//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.create.AiImportCreationService;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.mapping.MappingService;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionService;
import org.opensilex.aiimport.resolve.SharedResourceVariableLookup;
import org.opensilex.aiimport.service.dto.ChatMessage;
import org.opensilex.aiimport.service.dto.ToolCall;
import org.opensilex.aiimport.service.tools.AiTool;
import org.opensilex.aiimport.service.tools.ToolContext;
import org.opensilex.aiimport.workbook.WorkbookReader;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.core.CoreModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.displayable.DisplayableServiceUnavailableException;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Drives one conversation: reads the file, resolves it against the instance, and runs the exchange
 * with the language model, executing the lookups the model asks for.
 * <p>
 * The loop is bounded. A model that keeps asking for tools instead of answering is stopped and the
 * user is told, rather than the request running until the timeout.
 *
 * @author Arnaud Charleroy
 */
public class AiImportChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiImportChatService.class);

    /**
     * The opening turn is a message we send on the user's behalf, so that the first thing the user
     * sees is an analysis rather than an empty conversation.
     */
    /**
     * How many completed turns keep their verbose tool results.
     * <p>
     * A tool result can run to thousands of characters, and the whole history is resent on every
     * message, so without this a long conversation grows without bound. Older results are replaced
     * by a one-line note: the assistant already used them to write its answer, which stays.
     */
    private static final int TURNS_KEEPING_TOOL_RESULTS = 2;

    /**
     * Below this length a tool result is left alone: shortening it would save nothing.
     */
    private static final int COMPACTED_TOOL_RESULT_LENGTH = 200;

    private static final String OPENING_INSTRUCTION =
            "Describe what this file contains, then state what is already in the instance and what "
                    + "is not, and ask me about what is missing or contradictory.";

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel currentUser;
    private final AiImportConfig config;
    private final LlmService llm;
    private final ObjectMapper mapper;
    private final ImportProfileRegistry profiles;
    private final ToolRegistry toolRegistry;
    private final PromptBuilder promptBuilder;
    private final SharedResourceVariableLookup sharedResources;
    private final MappingService mappingService = new MappingService();

    public AiImportChatService(SPARQLService sparql,
                               MongoDBService nosql,
                               FileStorageService fs,
                               AccountModel currentUser,
                               AiImportConfig config,
                               LlmService llm,
                               ObjectMapper mapper,
                               CoreModule coreModule) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.currentUser = currentUser;
        this.config = config;
        this.llm = llm;
        this.mapper = mapper;
        this.profiles = new ImportProfileRegistry();
        this.toolRegistry = new ToolRegistry(mapper);
        this.promptBuilder = new PromptBuilder(mapper, config.sampleRowsPerSheet());
        this.sharedResources = config.searchSharedResourceInstances() && coreModule != null
                ? new SharedResourceVariableLookup(coreModule, currentUser.getLanguage())
                : null;
    }

    public ImportProfileRegistry getProfiles() {
        return profiles;
    }

    /**
     * Reads the file, resolves it, and asks the assistant for its opening analysis.
     *
     * @param requestedProfileId a profile chosen by the user, or {@code null} to detect it
     */
    public void start(AiImportSession session, File file, String originalFileName,
                      String requestedProfileId) throws WorkbookReadException {
        WorkbookStructure workbook = new WorkbookReader().read(file, originalFileName);
        ImportProfile profile = selectProfile(workbook, requestedProfileId);

        session.setFileName(originalFileName)
                .setWorkbook(workbook)
                .setProfileId(profile.getId());

        analyse(session, profile);

        session.getHistory().add(ChatMessage.user(OPENING_INSTRUCTION));

        AiImportMessage opening = AiImportMessage.assistant(null);
        session.getTranscript().add(opening);
        answerInto(session, opening);
    }

    /**
     * Re-reads the instance and rebuilds the system prompt, without losing the conversation. Called
     * after the user has created what was missing.
     */
    public void revalidate(AiImportSession session) {
        ImportProfile profile = profiles.getById(session.getProfileId())
                .orElseGet(() -> profiles.select(session.getWorkbook()));
        analyse(session, profile);
    }

    /**
     * Adds a user message and returns the assistant's reply.
     */
    public AiImportMessage ask(AiImportSession session, String content) {
        compactOldToolResults(session);

        session.getTranscript().add(AiImportMessage.user(content));
        session.getHistory().add(ChatMessage.user(content));

        AiImportMessage reply = AiImportMessage.assistant(null);
        session.getTranscript().add(reply);
        answerInto(session, reply);

        // A draft made during this turn belongs to this turn, so its card renders under this reply.
        CreationProposal proposal = session.getPendingProposal();
        if (proposal != null && proposal.getStatus() == CreationProposal.Status.PENDING) {
            reply.setProposalId(proposal.getId());
        }
        return reply;
    }

    /**
     * Shortens the tool results of turns older than the last few.
     * <p>
     * The message is kept rather than removed: an OpenAI-compatible endpoint rejects a conversation
     * where an assistant message asks for a tool call that no result answers.
     */
    private void compactOldToolResults(AiImportSession session) {
        List<ChatMessage> history = session.getHistory();

        int userTurnsSeen = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            if (ChatMessage.ROLE_USER.equals(message.getRole())) {
                userTurnsSeen++;
                continue;
            }
            if (userTurnsSeen < TURNS_KEEPING_TOOL_RESULTS) {
                continue;
            }
            if (ChatMessage.ROLE_TOOL.equals(message.getRole())
                    && message.getContent() != null
                    && message.getContent().length() > COMPACTED_TOOL_RESULT_LENGTH) {
                message.setContent("{\"note\":\"Result of an earlier lookup, shortened. Call the "
                        + "tool again if you need it.\"}");
            }
        }
    }

    //#region analysis

    private ImportProfile selectProfile(WorkbookStructure workbook, String requestedProfileId) {
        if (requestedProfileId != null && !requestedProfileId.isEmpty()) {
            Optional<ImportProfile> requested = profiles.getById(requestedProfileId);
            if (requested.isPresent()) {
                return requested.get();
            }
            LOGGER.warn("Unknown import profile {}, falling back to detection", requestedProfileId);
        }
        return profiles.select(workbook);
    }

    /**
     * Extracts the names from the file, checks them against the instance, and rewrites the system
     * prompt from the result.
     */
    private void analyse(AiImportSession session, ImportProfile profile) {
        ExtractedImportPlan plan = profile.extract(session.getWorkbook());
        ResolutionReport report = new ResolutionService(sparql, nosql, fs, currentUser, sharedResources)
                .resolve(plan);

        session.setPlan(plan).setReport(report);
        session.setMappings(mappingService.map(session.getWorkbook(), profile, report));
        session.setDataPoints(profile.extractDataPoints(session.getWorkbook()));
        session.setEvents(profile.extractEvents(session.getWorkbook()));
        session.replaceSystemPrompt(promptBuilder.build(session.getWorkbook(), profile, report,
                session.getMappings(), creationRequirements(session), currentUser.getLanguage()));
    }

    /**
     * @return what each creation would take, computed with the same service the page uses.
     * <p>
     * Handed to the prompt so the assistant describes the form that exists rather than the one it
     * imagines. Left empty on failure: an assistant with no creation section says less, whereas one
     * with a stale section confidently misleads.
     */
    private List<CreationRequirements> creationRequirements(AiImportSession session) {
        AiImportCreationService creationService =
                new AiImportCreationService(sparql, nosql, fs, currentUser);
        List<CreationRequirements> requirements = new ArrayList<>();
        for (CreationTarget target : CreationTarget.values()) {
            try {
                requirements.add(creationService.requirementsFor(target, session));
            } catch (RuntimeException e) {
                LOGGER.warn("Could not compute what creating a {} would take", target, e);
            }
        }
        return requirements;
    }

    //#endregion

    //#region the model exchange

    /**
     * Runs the exchange until the model answers instead of asking for a lookup, and writes the
     * answer into the transcript message.
     * <p>
     * The two ways this can end without an answer — the model unreachable, or the tool-call limit
     * reached — are written as the module's own message, with a translation key, because they are
     * not the assistant speaking. Neither is an error page: the file has been read, the instance
     * checked and the mapping computed without the model, and all of that is still on screen.
     */
    private void answerInto(AiImportSession session, AiImportMessage into) {
        int maxIterations = Math.max(1, llm.getMaxToolIterations());

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            ChatMessage reply;
            try {
                reply = llm.complete(session.getHistory(), toolRegistry.getDefinitions(),
                        session.getTokenUsage()::add);
            } catch (DisplayableServiceUnavailableException e) {
                LOGGER.error("The language model could not be reached", e);
                into.setContentKey("AiImport.chat.assistantUnreachable")
                        .setContent("The assistant could not be reached. The file analysis beside "
                                + "this conversation is still valid: it was produced without the "
                                + "assistant, and the instance was checked without it too.");
                return;
            }
            session.getHistory().add(reply);

            if (!reply.hasToolCalls()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Conversation {} so far: {}", session.getId(), session.getTokenUsage());
                }
                into.setContent(reply.getContent() == null ? "" : reply.getContent());
                return;
            }
            runToolCalls(session, reply.getToolCalls(), into.getLookups());
        }

        LOGGER.warn("The assistant reached the tool-call limit for session {}", session.getId());
        into.setContentKey("AiImport.chat.tooManyLookups")
                .setContent("The assistant kept looking things up without reaching a conclusion. "
                        + "Try asking a narrower question, or look at the analysis beside this "
                        + "conversation.");
    }

    private void runToolCalls(AiImportSession session, List<ToolCall> toolCalls, List<String> lookups) {
        ToolContext context = new ToolContext(sparql, nosql, fs, currentUser,
                session.getWorkbook(), sharedResources, session.getReport(), session.getMappings(),
                session, new AiImportCreationService(sparql, nosql, fs, currentUser));

        for (ToolCall call : toolCalls) {
            String name = call.getFunctionName();
            Object result;

            Optional<AiTool> tool = toolRegistry.get(name);
            if (!tool.isPresent()) {
                LOGGER.warn("The assistant asked for the unknown tool {}", name);
                result = singleton("error", "There is no tool named '" + name + "'.");
            } else {
                lookups.add(describe(name, call.getFunctionArguments()));
                result = execute(tool.get(), call, context);
            }

            session.getHistory().add(ChatMessage.toolResult(call.getId(), name, asJson(result)));
        }
    }

    private Object execute(AiTool tool, ToolCall call, ToolContext context) {
        JsonNode arguments;
        try {
            String raw = call.getFunctionArguments();
            arguments = raw == null || raw.isEmpty()
                    ? mapper.createObjectNode()
                    : mapper.readTree(raw);
        } catch (Exception e) {
            LOGGER.warn("The assistant sent malformed arguments to {}", tool.getName(), e);
            return singleton("error", "The arguments were not valid JSON.");
        }

        try {
            return tool.execute(arguments, context);
        } catch (Exception e) {
            // A failed lookup is told to the model, not raised: it can then say so, or try
            // something else, instead of the whole request failing.
            LOGGER.warn("The tool {} failed", tool.getName(), e);
            return singleton("error", "The lookup failed: " + e.getMessage());
        }
    }

    //#endregion

    //#region helpers

    /**
     * @return a one-line description of a lookup, shown under the assistant's answer
     */
    private String describe(String toolName, String rawArguments) {
        StringBuilder description = new StringBuilder(toolName);
        if (rawArguments == null || rawArguments.isEmpty()) {
            return description.toString();
        }
        try {
            JsonNode arguments = mapper.readTree(rawArguments);
            List<String> parts = new ArrayList<>();
            arguments.fieldNames().forEachRemaining(field ->
                    parts.add(field + "=" + arguments.get(field).asText()));
            if (!parts.isEmpty()) {
                description.append('(').append(String.join(", ", parts)).append(')');
            }
        } catch (Exception e) {
            LOGGER.debug("Could not describe the arguments of {}", toolName, e);
        }
        return description.toString();
    }

    private Map<String, Object> singleton(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private String asJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            LOGGER.error("Could not serialise a tool result", e);
            return "{\"error\":\"The result could not be serialised.\"}";
        }
    }

    //#endregion
}
