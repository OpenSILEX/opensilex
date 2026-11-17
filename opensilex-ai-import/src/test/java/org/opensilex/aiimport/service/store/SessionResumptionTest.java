//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFiles;
import org.opensilex.aiimport.api.SessionAccess;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.custom.CustomWorkbooks;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;
import org.opensilex.aiimport.service.AiImportChatService;
import org.opensilex.aiimport.service.AiImportMessage;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.TestConfig;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.aiimport.service.dto.ChatMessage;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Leaving a conversation and coming back: the stored work and the stored file make the session
 * again, the report is recomputed against the instance, and the assistant is not asked anything —
 * its endpoint here does not even exist.
 *
 * @author Arnaud Charleroy
 */
public class SessionResumptionTest extends AbstractMongoIntegrationTest {

    private static AccountModel admin;

    @BeforeClass
    public static void anAdministrator() {
        admin = new AccountModel();
        admin.setUri(URI.create("test:id/account/ai-import-resume"));
        admin.setLanguage("en");
        admin.setAdmin(true);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(AiImportSessionStore.COLLECTION);
    }

    private AiImportSessionStore store() {
        return new AiImportSessionStore(getMongoDBService(), getFs(),
                ObjectMapperContextResolver.getObjectMapper(), 30);
    }

    private AiImportChatService chat() {
        // An endpoint nobody listens on: resuming must not need the language model.
        TestConfig config = TestConfig.pointingAt("http://127.0.0.1:1/v1");
        return new AiImportChatService(getSparqlService(), getMongoDBService(), getFs(), admin,
                config, new LlmService(config), ObjectMapperContextResolver.getObjectMapper(), null);
    }

    @Test
    public void aStoredConversationIsRebuiltFromItsFileWithoutTheAssistant() throws Exception {
        AiImportSession left = new AiImportSession(UUID.randomUUID().toString(), admin.getUri());
        left.setFileName(WorkbookFixture.VITIS_FILE_NAME).setProfileId("vitis-explorer");
        left.getHistory().add(ChatMessage.system("the prompt of a month ago"));
        left.getHistory().add(ChatMessage.user("Quelles variables manquent ?"));
        left.getHistory().add(ChatMessage.assistant("Trois."));
        left.getTranscript().add(AiImportMessage.user("Quelles variables manquent ?"));
        left.getTranscript().add(AiImportMessage.assistant("Trois."));
        store().saveFile(left.getId(), WorkbookFixture.vitisFile());
        store().save(left);

        // What the API does when the session is no longer in memory.
        AiImportSession resumed = store().load(left.getId(), admin.getUri())
                .orElseThrow(AssertionError::new).toSession(admin.getUri());
        File workbook = File.createTempFile("resume-", ".xlsx");
        try {
            Files.write(workbook.toPath(), store().readFile(left.getId()));
            chat().restore(resumed, workbook, resumed.getFileName(), resumed.getProfileId());
        } finally {
            Files.deleteIfExists(workbook.toPath());
        }

        assertEquals("vitis-explorer", resumed.getProfileId());
        assertNotNull("the file was read again", resumed.getWorkbook());
        assertNotNull("the report was recomputed", resumed.getReport());
        assertFalse("the mapping was recomputed", resumed.getMappings().isEmpty());

        // The conversation is intact, and argues from a prompt rebuilt from the current report.
        assertEquals(2, resumed.getTranscript().size());
        assertEquals(3, resumed.getHistory().size());
        assertEquals(ChatMessage.ROLE_SYSTEM, resumed.getHistory().get(0).getRole());
        assertNotEquals("the prompt of a month ago", resumed.getHistory().get(0).getContent());
        assertEquals(1, resumed.getHistory().stream()
                .filter(message -> ChatMessage.ROLE_SYSTEM.equals(message.getRole())).count());
        assertEquals("Trois.", resumed.getHistory().get(2).getContent());
        assertEquals("no model call was made", 0, resumed.getTokenUsage().getCalls());
    }

    /**
     * What the user said of their file is part of the conversation: it comes back with it, and the
     * report is worked out from it, not from what the module would have guessed.
     */
    @Test
    public void aDescriptionMadeByTheUserComesBackWithTheConversation() throws Exception {
        File workbook = WorkbookFiles.write("described-", CustomWorkbooks.flatSheet());
        AiImportSession left = new AiImportSession(UUID.randomUUID().toString(), admin.getUri());
        left.setFileName("essai.xlsx").setProfileId(UserDefinedProfile.ID);
        left.getDescription().setExperiment("test:id/experiment/described", "Described trial");
        left.getDescription().getSheets().put("essai", new SheetDescription("essai")
                .setKind(SheetKind.OBJECTS_AND_DATA)
                .setType(URI.create("http://www.opensilex.org/vocabulary/oeso#Plot")).setFactor("Traitement"));
        left.getDescription().getSheets().get("essai").getRoles().put("nb_grappes", DescribedRole.UNREAD);
        left.getDescription().getSheets().get("essai").getProperties().put("remarque", "http://example.org/p");
        try {
            store().saveFile(left.getId(), workbook);
            store().save(left);

            AiImportSession resumed = store().load(left.getId(), admin.getUri())
                    .orElseThrow(AssertionError::new).toSession(admin.getUri());
            chat().restore(resumed, workbook, resumed.getFileName(), resumed.getProfileId());

            assertEquals(UserDefinedProfile.ID, resumed.getProfileId());
            assertEquals("Described trial", resumed.getDescription().getExperimentName());
            SheetDescription sheet = resumed.getDescription().find("essai").orElseThrow(AssertionError::new);
            assertEquals(SheetKind.OBJECTS_AND_DATA, sheet.getKind());
            assertEquals("http://www.opensilex.org/vocabulary/oeso#Plot", sheet.getType().toString());
            assertEquals("Traitement", sheet.getFactor());
            assertEquals(DescribedRole.UNREAD, sheet.getRoles().get("nb_grappes"));
            assertEquals("http://example.org/p", sheet.getProperties().get("remarque"));
            List<String> variables = resumed.getPlan().getVariables().stream()
                    .map(variable -> variable.getColumnKey()).collect(Collectors.toList());
            assertTrue("the column the user left unread is not asked of the instance: " + variables,
                    variables.contains("hauteur_cm") && !variables.contains("nb_grappes"));
            assertEquals("no model call was made", 0, resumed.getTokenUsage().getCalls());
        } finally {
            Files.deleteIfExists(workbook.toPath());
        }
    }

    private SessionAccess access() {
        TestConfig config = TestConfig.pointingAt("http://127.0.0.1:1/v1");
        return new SessionAccess(getSparqlService(), getMongoDBService(), getFs(), config, new LlmService(config),
                new AiImportSessionCache(config), null, admin);
    }

    private static File csv(String content) throws Exception {
        File file = File.createTempFile("resume-", ".csv");
        Files.writeString(file.toPath(), content);
        return file;
    }

    /**
     * Each file of a conversation is stored under its own identifier, and every one is read again:
     * the sheets are those of all of them, in the order the files were added.
     */
    @Test
    public void everyFileOfAConversationComesBack() throws Exception {
        File plan = csv("plot_id;variete\nP01;Syrah\n");
        File measures = csv("plot_id;date;hauteur_cm\nP01;2026-06-12;84\n");
        AiImportSession left = new AiImportSession(UUID.randomUUID().toString(), admin.getUri());
        left.setFileName("plan.csv").setProfileId(UserDefinedProfile.ID);
        try {
            store().saveFile(left.getId(), "file-1", plan);
            store().saveFile(left.getId(), "file-2", measures);
            left.getFiles().add(new SessionFile("file-1", "plan.csv", SheetSources.read(plan, "plan.csv")));
            left.getFiles().add(new SessionFile("file-2", "mesures.csv", SheetSources.read(measures, "mesures.csv")));
            store().save(left);

            AiImportSession resumed = access().find(left.getId()).orElseThrow(AssertionError::new);

            assertEquals(List.of("file-1", "file-2"),
                    resumed.getFiles().stream().map(SessionFile::getId).collect(Collectors.toList()));
            assertEquals(List.of("plan", "mesures"), resumed.getWorkbook().getSheetNames());
            assertEquals("plan.csv", resumed.getFileName());
            assertEquals("no model call was made", 0, resumed.getTokenUsage().getCalls());
        } finally {
            Files.deleteIfExists(plan.toPath());
            Files.deleteIfExists(measures.toPath());
        }
    }

    /**
     * A conversation stored before it could hold several files kept its only one under the
     * conversation itself. It still resumes, and is stored again the way the others are.
     */
    @Test
    public void aConversationStoredWithASingleFileStillResumes() throws Exception {
        AiImportSession left = new AiImportSession(UUID.randomUUID().toString(), admin.getUri());
        left.setFileName(WorkbookFixture.VITIS_FILE_NAME).setProfileId("vitis-explorer");
        store().saveFile(left.getId(), WorkbookFixture.vitisFile());
        store().save(left);

        AiImportSession resumed = access().find(left.getId()).orElseThrow(AssertionError::new);

        assertEquals(1, resumed.getFiles().size());
        assertEquals("vitis-explorer", resumed.getProfileId());
        String fileId = resumed.getFiles().get(0).getId();
        assertTrue("its file is now stored under an identifier",
                store().readFile(left.getId(), fileId).length > 0);

        store().save(resumed);
        assertEquals(fileId, store().load(left.getId(), admin.getUri()).orElseThrow(AssertionError::new)
                .getFiles().get(0).getId());
        assertTrue(store().delete(left.getId(), admin.getUri()));
    }
}
