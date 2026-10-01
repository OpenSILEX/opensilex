//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Keeps import conversations beyond the memory cache, so that a user can leave one and resume it.
 * <p>
 * A session is two things: a MongoDB document holding the stored work ({@link SavedSession}, as
 * JSON) with who owns it and when it was last active, and the uploaded workbook in the platform's
 * file storage. Nothing else is kept: the report is recomputed from the file on resumption.
 * <p>
 * Expiry is done here rather than with a MongoDB TTL index. A TTL index would delete the document
 * and leave its workbook behind in the file storage, where nothing would ever find it again; the
 * sweep deletes both, and runs whenever the list of sessions is read.
 *
 * @author Arnaud Charleroy
 */
public class AiImportSessionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiImportSessionStore.class);

    public static final String COLLECTION = "ai_import_sessions";

    /**
     * Where the workbooks live in the platform's file storage.
     */
    public static final String FS_PREFIX = "ai-import";

    static final String ID = "_id";
    static final String OWNER = "owner";
    static final String FILE_NAME = "file_name";
    static final String PROFILE_ID = "profile_id";
    static final String CREATED_AT = "created_at";
    static final String UPDATED_AT = "updated_at";
    static final String MESSAGE_COUNT = "message_count";
    static final String SNAPSHOT = "snapshot";

    private static volatile boolean indexed;

    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final ObjectMapper mapper;
    private final Duration retention;

    /**
     * @param retentionDays how long a session is kept after its last activity
     */
    public AiImportSessionStore(MongoDBService nosql, FileStorageService fs, ObjectMapper mapper,
                                int retentionDays) {
        this.nosql = nosql;
        this.fs = fs;
        this.mapper = mapper;
        this.retention = Duration.ofDays(Math.max(1, retentionDays));
    }

    //#region writing

    /**
     * Stores the uploaded workbook, once, when the conversation opens.
     */
    public void saveFile(String sessionId, File file) throws IOException {
        fs.writeFile(FS_PREFIX, filePath(sessionId), file);
    }

    /**
     * Stores the session's current state, replacing the previous one. Called after every step that
     * changes it, so that leaving the page never loses more than the step in progress.
     */
    public void save(AiImportSession session) throws IOException {
        Document document = new Document(ID, session.getId())
                .append(OWNER, session.getAccountUri().toString())
                .append(FILE_NAME, session.getFileName())
                .append(PROFILE_ID, session.getProfileId())
                .append(CREATED_AT, Date.from(session.getCreatedAt()))
                .append(UPDATED_AT, Date.from(Instant.now()))
                .append(MESSAGE_COUNT, session.getTranscript().size())
                .append(SNAPSHOT, mapper.writeValueAsString(SavedSession.of(session)));
        collection().replaceOne(Filters.eq(ID, session.getId()), document,
                new ReplaceOptions().upsert(true));
    }

    /**
     * Deletes a session and its workbook.
     *
     * @return false when there was no such session for this owner
     */
    public boolean delete(String sessionId, URI owner) {
        long deleted = collection().deleteOne(ownedBy(sessionId, owner)).getDeletedCount();
        if (deleted > 0) {
            deleteFile(sessionId);
        }
        return deleted > 0;
    }

    //#endregion

    //#region reading

    /**
     * The owner's stored sessions, most recently active first. Expired ones are deleted first, so
     * they are never offered for resumption.
     */
    public List<SavedSessionSummary> list(URI owner) {
        sweepExpired();
        List<SavedSessionSummary> summaries = new ArrayList<>();
        for (Document document : collection().find(Filters.eq(OWNER, owner.toString()))
                .projection(new Document(SNAPSHOT, 0))
                .sort(Sorts.descending(UPDATED_AT))) {
            summaries.add(new SavedSessionSummary(
                    document.getString(ID),
                    document.getString(FILE_NAME),
                    document.getString(PROFILE_ID),
                    instantOf(document.getDate(CREATED_AT)),
                    instantOf(document.getDate(UPDATED_AT)),
                    document.getInteger(MESSAGE_COUNT, 0)));
        }
        return summaries;
    }

    /**
     * @return the stored work of a session, only when it belongs to {@code owner}. Someone else's
     * session is indistinguishable from one that never existed, as in the memory cache.
     */
    public Optional<SavedSession> load(String sessionId, URI owner) throws IOException {
        if (sessionId == null || owner == null) {
            return Optional.empty();
        }
        Document document = collection().find(ownedBy(sessionId, owner)).first();
        if (document == null || isExpired(document)) {
            return Optional.empty();
        }
        return Optional.of(mapper.readValue(document.getString(SNAPSHOT), SavedSession.class));
    }

    /**
     * The workbook of a session. Only called after {@link #load} has checked the owner.
     */
    public byte[] readFile(String sessionId) throws IOException {
        return fs.readFileAsByteArray(FS_PREFIX, filePath(sessionId));
    }

    //#endregion

    //#region expiry

    /**
     * Deletes every session inactive for longer than the retention, with its workbook.
     *
     * @return how many were deleted
     */
    public int sweepExpired() {
        Bson expired = Filters.lt(UPDATED_AT, Date.from(Instant.now().minus(retention)));
        List<String> expiredIds = new ArrayList<>();
        for (Document document : collection().find(expired).projection(new Document(ID, 1))) {
            expiredIds.add(document.getString(ID));
        }
        for (String sessionId : expiredIds) {
            collection().deleteOne(Filters.eq(ID, sessionId));
            deleteFile(sessionId);
        }
        if (!expiredIds.isEmpty()) {
            LOGGER.info("Deleted {} import conversation(s) inactive for more than {} days",
                    expiredIds.size(), retention.toDays());
        }
        return expiredIds.size();
    }

    private boolean isExpired(Document document) {
        Date updated = document.getDate(UPDATED_AT);
        return updated != null && updated.toInstant().isBefore(Instant.now().minus(retention));
    }

    //#endregion

    //#region helpers

    private MongoCollection<Document> collection() {
        MongoCollection<Document> collection = nosql.getDatabase().getCollection(COLLECTION);
        if (!indexed) {
            synchronized (AiImportSessionStore.class) {
                if (!indexed) {
                    collection.createIndex(Indexes.compoundIndex(Indexes.ascending(OWNER),
                            Indexes.descending(UPDATED_AT)));
                    collection.createIndex(Indexes.ascending(UPDATED_AT));
                    indexed = true;
                }
            }
        }
        return collection;
    }

    private Bson ownedBy(String sessionId, URI owner) {
        return Filters.and(Filters.eq(ID, sessionId), Filters.eq(OWNER, owner.toString()));
    }

    /**
     * Built from the session identifier only — a UUID the server generated — never from the name
     * the user gave the file.
     */
    private Path filePath(String sessionId) {
        return Paths.get("sessions", sessionId, "workbook.xlsx");
    }

    private void deleteFile(String sessionId) {
        try {
            if (fs.exist(FS_PREFIX, filePath(sessionId))) {
                fs.delete(FS_PREFIX, filePath(sessionId));
            }
        } catch (IOException e) {
            // The document is gone, so nothing will offer this file again; a leftover is only disk.
            LOGGER.warn("Could not delete the workbook of import conversation {}", sessionId, e);
        }
    }

    private static Instant instantOf(Date date) {
        return date == null ? null : date.toInstant();
    }

    //#endregion
}
