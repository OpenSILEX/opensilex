//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.service.AiImportChatService;
import org.opensilex.aiimport.service.AiImportMessage;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.TestConfig;
import org.opensilex.aiimport.service.dto.ChatMessage;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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
}
