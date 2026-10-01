//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.junit.Test;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.objects.ObjectSheetPlan;
import org.opensilex.aiimport.resolve.ReportCategory;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportMessage;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.dto.ChatMessage;
import org.opensilex.aiimport.service.dto.ToolCall;
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
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Storing and resuming an import conversation, on an embedded MongoDB and the platform's file
 * storage — what a user relies on when they close the page and come back days later.
 *
 * @author Arnaud Charleroy
 */
public class AiImportSessionStoreTest extends AbstractMongoIntegrationTest {

    private static final URI ALICE = URI.create("test:id/account/alice");
    private static final URI BOB = URI.create("test:id/account/bob");
    private static final URI PROJECT = URI.create("test:id/project/vitadapt");
    private static final URI PLOT_TYPE = URI.create("http://www.opensilex.org/vocabulary/oeso#Plot");

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(AiImportSessionStore.COLLECTION);
    }

    private AiImportSessionStore store() {
        return new AiImportSessionStore(getMongoDBService(), getFs(),
                ObjectMapperContextResolver.getObjectMapper(), 30);
    }

    /**
     * A conversation with some of everything the user can leave behind: messages, a tool call and
     * its result, a confirmed name, an applied draft and a pending one.
     */
    private AiImportSession aConversation(URI owner) {
        AiImportSession session = new AiImportSession(UUID.randomUUID().toString(), owner);
        session.setFileName("Vitis_2020.xlsx").setProfileId("vitis-explorer");

        session.getHistory().add(ChatMessage.system("prompt"));
        session.getHistory().add(ChatMessage.user("Crée le projet"));
        session.getHistory().add(ChatMessage.assistant(null).setToolCalls(List.of(new ToolCall()
                .setId("call-1")
                .setFunction(new ToolCall.FunctionCall().setName("get_report").setArguments("{}")))));
        session.getHistory().add(ChatMessage.toolResult("call-1", "get_report", "{\"ok\":true}"));
        session.getTranscript().add(AiImportMessage.user("Crée le projet"));
        session.getTranscript().add(AiImportMessage.assistant("Voici le brouillon.")
                .setLookups(List.of("get_report")));

        session.getConfirmedMatches().confirm(ReportCategory.PROJECTS, "Vitis Adapt.",
                new ResourceReference(PROJECT, "Vitis Adaptation"));

        CreationProposal applied = new CreationProposal("p-1", CreationTarget.PROJECT)
                .put("name", "Vitis Adaptation", CreationProposal.FieldSource.FILE)
                .setRationale("from the cartouche")
                .setStatus(CreationProposal.Status.APPLIED)
                .setResultUri(PROJECT);
        CreationProposal pending = new CreationProposal("p-2", CreationTarget.EXPERIMENT)
                .put("name", "Vigne Nord 2024", CreationProposal.FieldSource.ASSISTANT);
        pending.getMissingRequired().add("objective");
        session.restoreProposal(applied);
        session.restoreProposal(pending);
        session.setPendingProposal(pending);

        session.getObjectPlans().put("Cartouche_Fixe", new ObjectSheetPlan("Cartouche_Fixe")
                .setType(PLOT_TYPE).setIncluded(false).map("Rang", "x").map("Remarque", ""));
        return session;
    }

    @Test
    public void aConversationComesBackWithAllItsWork() throws Exception {
        AiImportSession original = aConversation(ALICE);
        store().save(original);

        AiImportSession resumed = store().load(original.getId(), ALICE)
                .orElseThrow(AssertionError::new)
                .toSession(ALICE);

        assertEquals(original.getId(), resumed.getId());
        assertEquals("Vitis_2020.xlsx", resumed.getFileName());
        assertEquals("vitis-explorer", resumed.getProfileId());
        assertEquals(original.getCreatedAt().truncatedTo(ChronoUnit.MILLIS),
                resumed.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));

        // The model's side of the conversation, tool call and result included.
        assertEquals(4, resumed.getHistory().size());
        assertEquals("call-1", resumed.getHistory().get(2).getToolCalls().get(0).getId());
        assertEquals("get_report", resumed.getHistory().get(3).getName());
        // The user's side.
        assertEquals(2, resumed.getTranscript().size());
        assertEquals(List.of("get_report"), resumed.getTranscript().get(1).getLookups());

        assertEquals(PROJECT, resumed.getConfirmedMatches()
                .lookup(ReportCategory.PROJECTS, "vitis adapt").orElseThrow(AssertionError::new).getUri());

        CreationProposal applied = resumed.getProposal("p-1").orElseThrow(AssertionError::new);
        assertEquals(CreationProposal.Status.APPLIED, applied.getStatus());
        assertEquals("from the cartouche", applied.getRationale());
        assertEquals(PROJECT, applied.getResultUri());
        assertEquals(CreationProposal.FieldSource.FILE, applied.getFieldSources().get("name"));

        assertEquals("p-2", resumed.getPendingProposal().getId());
        assertEquals(List.of("objective"), resumed.getPendingProposal().getMissingRequired());

        // What was chosen for the object sheets: the type, the sheet left out, each column.
        ObjectSheetPlan plan = resumed.getObjectPlans().get("Cartouche_Fixe");
        assertEquals(PLOT_TYPE, plan.getType());
        assertFalse(plan.isIncluded());
        assertEquals("x", plan.getMapping().get("Rang"));
        assertEquals("", plan.getMapping().get("Remarque"));
    }

    @Test
    public void theWorkbookIsKeptAndDeletedWithItsConversation() throws Exception {
        AiImportSession session = aConversation(ALICE);
        byte[] content = {'P', 'K', 3, 4, 42};
        File upload = File.createTempFile("upload-", ".xlsx");
        Files.write(upload.toPath(), content);

        store().saveFile(session.getId(), upload);
        store().save(session);
        assertArrayEquals(content, store().readFile(session.getId()));

        assertTrue(store().delete(session.getId(), ALICE));
        assertFalse(store().load(session.getId(), ALICE).isPresent());
        assertFalse(getFs().exist(AiImportSessionStore.FS_PREFIX,
                Paths.get("sessions", session.getId(), "workbook.xlsx")));
        Files.deleteIfExists(upload.toPath());
    }

    /**
     * Someone else's conversation is indistinguishable from one that never existed.
     */
    @Test
    public void aConversationIsOnlyItsOwners() throws Exception {
        AiImportSession session = aConversation(ALICE);
        store().save(session);

        assertFalse(store().load(session.getId(), BOB).isPresent());
        assertTrue(store().list(BOB).isEmpty());
        assertFalse(store().delete(session.getId(), BOB));
        assertTrue(store().load(session.getId(), ALICE).isPresent());
    }

    @Test
    public void theListShowsTheMostRecentFirst() throws Exception {
        AiImportSession older = aConversation(ALICE);
        AiImportSession newer = aConversation(ALICE);
        store().save(older);
        store().save(newer);
        ageBy(older.getId(), 2);

        List<SavedSessionSummary> list = store().list(ALICE);
        assertEquals(2, list.size());
        assertEquals(newer.getId(), list.get(0).getSessionId());
        assertEquals("Vitis_2020.xlsx", list.get(0).getFileName());
        assertEquals(2, list.get(0).getMessageCount());
    }

    /**
     * Thirty days without activity, and the conversation goes, file included — a TTL index would
     * have left the file behind.
     */
    @Test
    public void anInactiveConversationExpiresWithItsFile() throws Exception {
        AiImportSession session = aConversation(ALICE);
        File upload = File.createTempFile("upload-", ".xlsx");
        Files.write(upload.toPath(), new byte[]{1, 2, 3});
        store().saveFile(session.getId(), upload);
        store().save(session);
        ageBy(session.getId(), 31);

        assertFalse("expired, so not resumable even before the sweep",
                store().load(session.getId(), ALICE).isPresent());
        assertEquals(1, store().sweepExpired());
        assertTrue(store().list(ALICE).isEmpty());
        assertFalse(getFs().exist(AiImportSessionStore.FS_PREFIX,
                Paths.get("sessions", session.getId(), "workbook.xlsx")));
        Files.deleteIfExists(upload.toPath());
    }

    @Test
    public void aSessionWithoutADraftHasNoPendingOne() throws Exception {
        AiImportSession session = new AiImportSession(UUID.randomUUID().toString(), ALICE);
        session.setFileName("empty.xlsx");
        store().save(session);

        AiImportSession resumed = store().load(session.getId(), ALICE)
                .orElseThrow(AssertionError::new).toSession(ALICE);
        assertNull(resumed.getPendingProposal());
        assertTrue(resumed.getProposals().isEmpty());
    }

    private void ageBy(String sessionId, int days) {
        getMongoDBService().getDatabase().getCollection(AiImportSessionStore.COLLECTION)
                .updateOne(Filters.eq(AiImportSessionStore.ID, sessionId),
                        Updates.set(AiImportSessionStore.UPDATED_AT,
                                Date.from(Instant.now().minus(days, ChronoUnit.DAYS))));
    }
}
