//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps sessions beyond the memory cache, so that a user can leave one and come back to it.
 * <p>
 * A session is a MongoDB document — its owner, its title, when it was last active, and its own state
 * as JSON — and its files in the platform's file storage. The store never looks inside the state: an
 * assistant decides what it keeps, and a new one needs no change here, only its own collection.
 * <p>
 * Expiry is done here rather than with a TTL index, which would delete the document and leave its
 * files behind where nothing would ever find them again. The sweep deletes both, and runs whenever
 * a list of sessions is read.
 *
 * @param <S> the state an assistant keeps of a session
 * @author Arnaud Charleroy
 */
public class SnapshotSessionStore<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotSessionStore.class);

    static final String ID = "_id";
    static final String OWNER = "owner";
    static final String TITLE = "title";
    static final String CREATED_AT = "created_at";
    static final String UPDATED_AT = "updated_at";
    static final String FILE_IDS = "file_ids";
    static final String SNAPSHOT = "snapshot";

    /**
     * The collections already given their indexes since the server started.
     */
    private static final Set<String> INDEXED = ConcurrentHashMap.newKeySet();

    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final ObjectMapper mapper;
    private final String collectionName;
    private final String fsPrefix;
    private final Class<S> snapshotType;
    private final Duration retention;

    /**
     * @param collectionName the MongoDB collection of this kind of session
     * @param fsPrefix       where its files live in the file storage
     * @param snapshotType   the class its state is read back as
     * @param retentionDays  how long a session is kept after its last activity
     */
    public SnapshotSessionStore(MongoDBService nosql, FileStorageService fs, ObjectMapper mapper,
                                String collectionName, String fsPrefix, Class<S> snapshotType, int retentionDays) {
        this.nosql = nosql;
        this.fs = fs;
        this.mapper = mapper;
        this.collectionName = collectionName;
        this.fsPrefix = fsPrefix;
        this.snapshotType = snapshotType;
        this.retention = Duration.ofDays(Math.max(1, retentionDays));
    }

    //#region writing

    /**
     * Stores a session's current state, replacing the previous one. Called after every step that
     * changes it, so that leaving the page never loses more than the step in progress.
     */
    public void save(StoredSession<S> session) throws IOException {
        Document document = new Document(ID, session.id())
                .append(OWNER, session.owner().toString())
                .append(TITLE, session.title())
                .append(CREATED_AT, Date.from(session.createdAt()))
                .append(UPDATED_AT, Date.from(Instant.now()))
                .append(FILE_IDS, new ArrayList<>(session.fileIds()))
                .append(SNAPSHOT, mapper.writeValueAsString(session.snapshot()));
        collection().replaceOne(Filters.eq(ID, session.id()), document, new ReplaceOptions().upsert(true));
    }

    /**
     * Deletes a session and its files.
     *
     * @return false when the owner has no such session
     */
    public boolean delete(String sessionId, URI owner) {
        if (sessionId == null || owner == null) {
            return false;
        }
        Document document = collection().findOneAndDelete(ownedBy(sessionId, owner));
        if (document != null) {
            deleteFiles(document);
        }
        return document != null;
    }

    /**
     * Stores a file of a session under the identifier the server gave it.
     */
    public void saveFile(String sessionId, String fileId, File file) throws IOException {
        fs.writeFile(fsPrefix, filePath(sessionId, fileId), file);
    }

    /**
     * Deletes the stored copy of a file taken out of a session.
     */
    public void deleteFile(String sessionId, String fileId) {
        try {
            Path path = filePath(sessionId, fileId);
            if (fs.exist(fsPrefix, path)) {
                fs.delete(fsPrefix, path);
            }
        } catch (IOException e) {
            // Nothing will offer this file again: a leftover only costs disk.
            LOGGER.warn("Could not delete a stored file of session {}", sessionId, e);
        }
    }

    //#endregion

    //#region reading

    /**
     * The owner's sessions, most recently active first. Expired ones are deleted first, so they are
     * never offered.
     */
    public List<StoredSessionSummary> list(URI owner) {
        sweepExpired();
        List<StoredSessionSummary> summaries = new ArrayList<>();
        for (Document document : collection().find(Filters.eq(OWNER, owner.toString()))
                .projection(new Document(SNAPSHOT, 0))
                .sort(Sorts.descending(UPDATED_AT))) {
            summaries.add(new StoredSessionSummary(
                    document.getString(ID),
                    document.getString(TITLE),
                    instantOf(document.getDate(CREATED_AT)),
                    instantOf(document.getDate(UPDATED_AT))));
        }
        return summaries;
    }

    /**
     * @return the state of a session, only when it belongs to {@code owner} and has not expired.
     * Someone else's session is indistinguishable from one that never existed.
     */
    public Optional<S> load(String sessionId, URI owner) throws IOException {
        if (sessionId == null || owner == null) {
            return Optional.empty();
        }
        Document document = collection().find(ownedBy(sessionId, owner)).first();
        if (document == null || isExpired(document)) {
            return Optional.empty();
        }
        return Optional.of(mapper.readValue(document.getString(SNAPSHOT), snapshotType));
    }

    /**
     * A file of a session. Only called once {@link #load} has checked the owner, with an identifier
     * the state gave.
     */
    public byte[] readFile(String sessionId, String fileId) throws IOException {
        return fs.readFileAsByteArray(fsPrefix, filePath(sessionId, fileId));
    }

    //#endregion

    //#region expiry

    /**
     * Deletes every session inactive for longer than the retention, with its files.
     *
     * @return how many were deleted
     */
    public int sweepExpired() {
        Bson expired = Filters.lt(UPDATED_AT, Date.from(Instant.now().minus(retention)));
        List<String> expiredIds = new ArrayList<>();
        for (Document document : collection().find(expired).projection(new Document(ID, 1))) {
            expiredIds.add(document.getString(ID));
        }
        int deleted = 0;
        for (String sessionId : expiredIds) {
            Document document = collection().findOneAndDelete(Filters.eq(ID, sessionId));
            if (document != null) {
                deleteFiles(document);
                deleted++;
            }
        }
        if (deleted > 0) {
            LOGGER.info("Deleted {} session(s) of {} inactive for more than {} days", deleted, collectionName,
                    retention.toDays());
        }
        return deleted;
    }

    private boolean isExpired(Document document) {
        Date updated = document.getDate(UPDATED_AT);
        return updated != null && updated.toInstant().isBefore(Instant.now().minus(retention));
    }

    //#endregion

    //#region helpers

    private MongoCollection<Document> collection() {
        MongoCollection<Document> collection = nosql.getDatabase().getCollection(collectionName);
        if (INDEXED.add(collectionName)) {
            collection.createIndex(Indexes.compoundIndex(Indexes.ascending(OWNER), Indexes.descending(UPDATED_AT)));
            collection.createIndex(Indexes.ascending(UPDATED_AT));
        }
        return collection;
    }

    private static Bson ownedBy(String sessionId, URI owner) {
        return Filters.and(Filters.eq(ID, sessionId), Filters.eq(OWNER, owner.toString()));
    }

    /**
     * Built from two identifiers the server generated, never from a name the user gave.
     */
    private static Path filePath(String sessionId, String fileId) {
        return Paths.get("sessions", sessionId, "files", fileId);
    }

    private void deleteFiles(Document document) {
        String sessionId = document.getString(ID);
        for (String fileId : document.getList(FILE_IDS, String.class, List.of())) {
            deleteFile(sessionId, fileId);
        }
    }

    private static Instant instantOf(Date date) {
        return date == null ? null : date.toInstant();
    }

    //#endregion
}
