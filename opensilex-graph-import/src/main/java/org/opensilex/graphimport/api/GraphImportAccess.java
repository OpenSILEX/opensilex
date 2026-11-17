//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import org.apache.commons.io.FilenameUtils;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.graphimport.session.SessionFiles;
import org.opensilex.graphimport.session.store.GraphImportSnapshot;
import org.opensilex.graphimport.session.store.SnapshotSessionStore;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * How an endpoint of the module reaches an import: finding it in memory or in the store, storing it
 * again, and the answers every endpoint gives the same way. Built per request, like the services it
 * holds.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportAccess {

    /**
     * The MongoDB collection of the module's imports, apart from the other assistant's.
     */
    public static final String COLLECTION = "graph_import_sessions";

    /**
     * Where the files of the imports live in the platform's file storage.
     */
    public static final String FS_PREFIX = GraphImportModule.CONFIG_ID;

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphImportAccess.class);

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final GraphImportConfig config;
    private final GraphImportSessionCache sessions;
    private final AccountModel currentUser;

    public GraphImportAccess(MongoDBService nosql, FileStorageService fs, GraphImportConfig config,
                             GraphImportSessionCache sessions, AccountModel currentUser) {
        this.nosql = nosql;
        this.fs = fs;
        this.config = config;
        this.sessions = sessions;
        this.currentUser = currentUser;
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public SnapshotSessionStore<GraphImportSnapshot> store() {
        return new SnapshotSessionStore<>(nosql, fs, ObjectMapperContextResolver.getObjectMapper(), COLLECTION,
                FS_PREFIX, GraphImportSnapshot.class, config.savedSessionDays());
    }

    public GraphImportSession create() {
        return sessions.create(currentUser.getUri());
    }

    /**
     * The user's import: from memory, or else read back from the store with its files.
     * <p>
     * An import that cannot be read back is reported as absent rather than half restored.
     */
    public Optional<GraphImportSession> find(String sessionId) {
        Optional<GraphImportSession> open = sessions.get(sessionId, currentUser.getUri());
        if (open.isPresent()) {
            return open;
        }
        try {
            Optional<GraphImportSession> resumed = SessionFiles.resume(store(), sessionId, currentUser.getUri());
            resumed.ifPresent(sessions::put);
            return resumed;
        } catch (IOException | WorkbookReadException | RuntimeException e) {
            LOGGER.warn("Could not resume import {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Stores the import after a step that changed it. A failure costs the ability to resume, not the
     * step the user just took, so it is logged rather than returned.
     */
    public void persist(GraphImportSession session) {
        try {
            store().save(GraphImportSnapshot.envelope(session));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not store import {}", session.getId(), e);
        }
    }

    /**
     * Forgets an import, in memory and in the store, with its files.
     *
     * @return false when the user has no such import
     */
    public boolean delete(String sessionId) {
        boolean open = sessions.get(sessionId, currentUser.getUri()).isPresent();
        if (open) {
            sessions.remove(sessionId);
        }
        boolean stored = store().delete(sessionId, currentUser.getUri());
        return open || stored;
    }

    /**
     * Rejects a file before it is opened, so that a wrong upload gets a clear answer instead of a
     * parser error, and an oversized one is never read into memory.
     *
     * @return the answer to send, or empty when the file is acceptable
     */
    public Optional<Response> reject(File file, String fileName) {
        if (file == null) {
            return Optional.of(badRequest("No file was uploaded.", "Attach the file in the 'file' part."));
        }
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        if (!SheetSources.extensions().contains(extension)) {
            return Optional.of(badRequest("Only spreadsheets and CSV files are accepted.",
                    "The file " + fileName + " has the extension '" + extension + "'. Accepted extensions are "
                            + String.join(", ", SheetSources.extensions()) + "."));
        }
        if (file.length() > config.maxFileSizeMb() * BYTES_PER_MB) {
            return Optional.of(badRequest("The file is too large.",
                    "The file is " + (file.length() / BYTES_PER_MB) + " MB, and the limit is "
                            + config.maxFileSizeMb() + " MB."));
        }
        return Optional.empty();
    }

    //#region the answers every endpoint gives the same way

    public Response disabled() {
        return new ErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "The experimental import assistant is "
                + "not enabled.", "Set 'graph-import.enabled' to true in the instance configuration.")
                .getResponse();
    }

    public Response notFound() {
        return new ErrorResponse(Response.Status.NOT_FOUND, "Unknown import",
                "No import carries this identifier. It may have expired, or belong to another account.")
                .getResponse();
    }

    public Response unreadable(String fileName) {
        return badRequest("The file could not be read.",
                "Check that " + fileName + " is a valid workbook or CSV file and is not password protected.");
    }

    public Response badRequest(String title, String detail) {
        return new ErrorResponse(Response.Status.BAD_REQUEST, title, detail).getResponse();
    }

    //#endregion
}
