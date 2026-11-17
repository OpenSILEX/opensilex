//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api;

import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.create.AiImportCreationService;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.service.AiImportChatService;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.service.store.AiImportSessionStore;
import org.opensilex.aiimport.service.store.FileTemplateStore;
import org.opensilex.aiimport.service.store.SavedFile;
import org.opensilex.aiimport.service.store.SavedSession;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.core.CoreModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;

/**
 * How an endpoint of the module reaches a conversation: finding it, storing it, and the services
 * that act on it. Shared by every API class of the module, so that each can stay about one thing.
 * <p>
 * Nothing here is new behaviour: it is what {@link AiImportAPI} did on its own, made available to
 * the classes that cannot inherit from it.
 *
 * @author Arnaud Charleroy
 */
public class SessionAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAccess.class);

    private static final String SESSION_NOT_FOUND =
            "No open conversation carries this identifier. It may have expired, or belong to "
                    + "another account.";

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AiImportConfig config;
    private final LlmService llm;
    private final AiImportSessionCache sessions;
    private final CoreModule coreModule;
    private final AccountModel currentUser;

    public SessionAccess(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                         AiImportConfig config, LlmService llm, AiImportSessionCache sessions,
                         CoreModule coreModule, AccountModel currentUser) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.config = config;
        this.llm = llm;
        this.sessions = sessions;
        this.coreModule = coreModule;
        this.currentUser = currentUser;
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public AiImportSessionStore store() {
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
    public Optional<AiImportSession> find(String sessionId) {
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
            readFiles(store, saved.get(), session);
            chat().restore(session, session.getProfileId());
            sessions.put(session);
            return Optional.of(session);
        } catch (IOException | WorkbookReadException | RuntimeException e) {
            // A session that cannot be rebuilt is reported as absent rather than half restored.
            LOGGER.warn("Could not resume import conversation {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Reads the stored files of a conversation again. One made before a conversation could hold
     * several has its only file under the conversation itself: it is stored again under an
     * identifier, so that the conversation is from then on like every other.
     */
    private void readFiles(AiImportSessionStore store, SavedSession saved, AiImportSession session)
            throws IOException, WorkbookReadException {
        if (saved.getFiles().isEmpty()) {
            File copy = temporaryCopy(session.getFileName(), store.readFile(session.getId()));
            try {
                SessionFile file = SessionFile.of(session.getFileName(),
                        SheetSources.read(copy, session.getFileName()));
                store.saveFile(session.getId(), file.getId(), copy);
                session.getFiles().add(file);
            } finally {
                Files.deleteIfExists(copy.toPath());
            }
            return;
        }
        for (SavedFile stored : saved.getFiles()) {
            File copy = temporaryCopy(stored.getName(), store.readFile(session.getId(), stored.getId()));
            try {
                session.getFiles().add(new SessionFile(stored.getId(), stored.getName(),
                        SheetSources.read(copy, stored.getName())));
            } finally {
                Files.deleteIfExists(copy.toPath());
            }
        }
    }

    /**
     * A temporary file of the kind the name says, which is what tells the reader how to read it.
     * Only the extension is taken from the name, and only when a reader exists for it.
     */
    private static File temporaryCopy(String fileName, byte[] content) throws IOException {
        String extension = SheetSources.forFile(fileName)
                .map(source -> source.extensions().stream()
                        .filter(candidate -> fileName.toLowerCase(Locale.ROOT).endsWith("." + candidate))
                        .findFirst().orElse(source.extensions().get(0)))
                .orElseThrow(() -> new IOException("No reader for the stored file " + fileName));
        File copy = File.createTempFile("ai-import-", "." + extension);
        Files.write(copy.toPath(), content);
        return copy;
    }

    /**
     * Stores the session after a step that changed it. A failure costs the ability to resume, not
     * the step the user just took, so it is logged rather than returned.
     */
    public void persist(AiImportSession session) {
        try {
            store().save(session);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not store import conversation {}", session.getId(), e);
        }
    }

    public FileTemplateStore templates() {
        return new FileTemplateStore(nosql, ObjectMapperContextResolver.getObjectMapper());
    }

    public AiImportChatService chat() {
        return new AiImportChatService(sparql, nosql, fs, currentUser, config, llm,
                ObjectMapperContextResolver.getObjectMapper(), coreModule);
    }

    public AiImportCreationService creation() {
        return new AiImportCreationService(sparql, nosql, fs, currentUser, coreModule);
    }

    //#region the answers every endpoint gives the same way

    public Response disabled() {
        return new ErrorResponse(Response.Status.SERVICE_UNAVAILABLE,
                "The import assistant is not enabled.",
                "Set 'ai-import.enabled' and configure 'ai-import.llm' in the instance configuration.")
                .getResponse();
    }

    public Response notFound() {
        return new ErrorResponse(Response.Status.NOT_FOUND, "Unknown conversation", SESSION_NOT_FOUND)
                .getResponse();
    }

    public Response badRequest(String title, String detail) {
        return new ErrorResponse(Response.Status.BAD_REQUEST, title, detail).getResponse();
    }

    //#endregion
}
