//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session.store;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Keeping sessions between two visits, on an embedded MongoDB and the platform's file storage.
 *
 * @author Arnaud Charleroy
 */
public class SnapshotSessionStoreTest extends AbstractMongoIntegrationTest {

    private static final String COLLECTION = "test_snapshot_sessions";
    private static final String OTHER_COLLECTION = "test_other_snapshot_sessions";
    private static final String FS_PREFIX = "test-snapshot-sessions";
    private static final URI ALICE = URI.create("test:id/account/alice");
    private static final URI BOB = URI.create("test:id/account/bob");
    private static final int RETENTION_DAYS = 30;

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(COLLECTION, OTHER_COLLECTION);
    }

    private static SnapshotSessionStore<GraphImportSnapshot> store(String collection) {
        return new SnapshotSessionStore<>(getMongoDBService(), getFs(), ObjectMapperContextResolver.getObjectMapper(),
                collection, FS_PREFIX, GraphImportSnapshot.class, RETENTION_DAYS);
    }

    private static StoredSession<GraphImportSnapshot> aSession(URI owner, String title, String... fileIds) {
        GraphImportSnapshot snapshot = new GraphImportSnapshot();
        snapshot.setCreatedAt(Instant.parse("2026-11-14T08:00:00Z"));
        for (String fileId : fileIds) {
            snapshot.getFiles().add(new GraphImportSnapshot.StoredFile(fileId, fileId + ".csv"));
        }
        return new StoredSession<>(UUID.randomUUID().toString(), owner, title, snapshot.getCreatedAt(),
                List.of(fileIds), snapshot);
    }

    private static File aFile(String content) throws Exception {
        File file = Files.createTempFile("snapshot-", ".csv").toFile();
        Files.write(file.toPath(), content.getBytes());
        file.deleteOnExit();
        return file;
    }

    private static boolean stored(String sessionId, String fileId) throws Exception {
        return getFs().exist(FS_PREFIX, Paths.get("sessions", sessionId, "files", fileId));
    }

    @Test
    public void aSessionIsReadBackOnlyByItsOwner() throws Exception {
        SnapshotSessionStore<GraphImportSnapshot> store = store(COLLECTION);
        StoredSession<GraphImportSnapshot> session = aSession(ALICE, "plan.csv", "f-1");

        store.save(session);

        Optional<GraphImportSnapshot> read = store.load(session.id(), ALICE);
        assertTrue(read.isPresent());
        assertEquals(session.snapshot().getCreatedAt(), read.get().getCreatedAt());
        assertEquals("f-1.csv", read.get().getFiles().get(0).getName());
        assertFalse("someone else's session reads as absent", store.load(session.id(), BOB).isPresent());
        assertFalse(store.load(null, ALICE).isPresent());
    }

    @Test
    public void theListShowsTheOwnersSessionsMostRecentlyChangedFirst() throws Exception {
        SnapshotSessionStore<GraphImportSnapshot> store = store(COLLECTION);
        StoredSession<GraphImportSnapshot> older = aSession(ALICE, "older.csv");
        StoredSession<GraphImportSnapshot> newer = aSession(ALICE, "newer.csv");
        store.save(older);
        store.save(newer);
        store.save(aSession(BOB, "bob.csv"));
        getMongoDBService().getDatabase().getCollection(COLLECTION).updateOne(
                Filters.eq(SnapshotSessionStore.ID, older.id()),
                Updates.set(SnapshotSessionStore.UPDATED_AT, Date.from(Instant.now().minus(1, ChronoUnit.HOURS))));

        List<StoredSessionSummary> list = store.list(ALICE);

        assertEquals(2, list.size());
        assertEquals("newer.csv", list.get(0).title());
        assertEquals("older.csv", list.get(1).title());
        assertEquals(older.createdAt(), list.get(1).createdAt());
    }

    @Test
    public void filesAreStoredReadAndDeletedWithTheirSession() throws Exception {
        SnapshotSessionStore<GraphImportSnapshot> store = store(COLLECTION);
        StoredSession<GraphImportSnapshot> session = aSession(ALICE, "plan.csv", "f-1", "f-2");
        store.save(session);
        store.saveFile(session.id(), "f-1", aFile("id;name\n1;a\n"));
        store.saveFile(session.id(), "f-2", aFile("id;value\n1;2\n"));

        assertArrayEquals("id;name\n1;a\n".getBytes(), store.readFile(session.id(), "f-1"));

        store.deleteFile(session.id(), "f-2");
        assertFalse(stored(session.id(), "f-2"));
        store.deleteFile(session.id(), "never-stored");

        assertFalse("only the owner deletes", store.delete(session.id(), BOB));
        assertTrue(store.delete(session.id(), ALICE));
        assertFalse(stored(session.id(), "f-1"));
        assertFalse(store.load(session.id(), ALICE).isPresent());
        assertFalse(store.delete(session.id(), ALICE));
        assertFalse(store.delete(null, ALICE));
    }

    @Test
    public void aSessionInactiveForLongerThanTheRetentionIsDeletedWithItsFiles() throws Exception {
        SnapshotSessionStore<GraphImportSnapshot> store = store(COLLECTION);
        StoredSession<GraphImportSnapshot> old = aSession(ALICE, "old.csv", "f-1");
        StoredSession<GraphImportSnapshot> recent = aSession(ALICE, "recent.csv");
        store.save(old);
        store.save(recent);
        store.saveFile(old.id(), "f-1", aFile("id\n1\n"));
        getMongoDBService().getDatabase().getCollection(COLLECTION).updateOne(
                Filters.eq(SnapshotSessionStore.ID, old.id()),
                Updates.set(SnapshotSessionStore.UPDATED_AT,
                        Date.from(Instant.now().minus(RETENTION_DAYS + 1L, ChronoUnit.DAYS))));

        assertFalse("an expired session is not read back", store.load(old.id(), ALICE).isPresent());
        List<StoredSessionSummary> list = store.list(ALICE);

        assertEquals(1, list.size());
        assertEquals(recent.id(), list.get(0).id());
        assertFalse(stored(old.id(), "f-1"));
        assertEquals("nothing left to sweep", 0, store.sweepExpired());
    }

    @Test
    public void twoKindsOfSessionKeptInTwoCollectionsDoNotSeeEachOther() throws Exception {
        StoredSession<GraphImportSnapshot> session = aSession(ALICE, "plan.csv");
        store(COLLECTION).save(session);

        assertTrue(store(OTHER_COLLECTION).list(ALICE).isEmpty());
        assertFalse(store(OTHER_COLLECTION).load(session.id(), ALICE).isPresent());
    }
}
