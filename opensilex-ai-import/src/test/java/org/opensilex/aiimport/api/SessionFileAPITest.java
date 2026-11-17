//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.service.StubLlmEndpoint;
import org.opensilex.aiimport.service.store.AiImportSessionStore;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * An import made of several files, end to end: a CSV opens the conversation, others are added and
 * taken away, and the sheets of all of them are read together.
 *
 * @author Arnaud Charleroy
 */
public class SessionFileAPITest extends AbstractMongoIntegrationTest {

    private static final String BASE = AiImportAPI.PATH + "/";
    private static final String PLAN = "plot_id;variete;traitement\nP01;Syrah;T1\nP02;Grenache;T2\n";
    private static final String MEASURES = "plot_id;date;hauteur_cm\nP01;2026-06-12;84\nP02;2026-06-12;79\n";

    private static StubLlmEndpoint llm;

    private int status;

    @BeforeClass
    public static void aLanguageModel() throws Exception {
        llm = new StubLlmEndpoint(StubLlmEndpoint.TEST_PORT);
    }

    @AfterClass
    public static void noMoreLanguageModel() {
        llm.close();
    }

    @Before
    public void noRequestYet() {
        llm.reset();
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(AiImportSessionStore.COLLECTION);
    }

    @Test
    public void aCsvFileOpensAConversationOfOneSheet() throws Exception {
        JsonNode session = open("plan.csv", PLAN);

        assertEquals("custom", session.get("profile_id").asText());
        assertEquals(1, session.get("files").size());
        JsonNode file = session.get("files").get(0);
        assertEquals("plan.csv", file.get("name").asText());
        assertEquals("plan", file.get("sheets").get(0).asText());
        assertEquals(0, llm.getRequestCount());
    }

    @Test
    public void anAddedFileBringsItsSheetsToTheDescriptionAndTheReport() throws Exception {
        String id = open("plan.csv", PLAN).get("session_id").asText();

        JsonNode files = add(id, "mesures.csv", MEASURES);

        assertEquals(files.toString(), 200, status);
        assertEquals(2, files.size());
        assertEquals("the new file is last", "mesures.csv", files.get(1).get("name").asText());
        JsonNode sheets = get("sessions/" + id + "/" + FileDescriptionAPI.PATH).get("sheets");
        assertEquals(2, sheets.size());
        assertEquals("plan", sheets.get(0).get("sheet").asText());
        assertEquals("mesures", sheets.get(1).get("sheet").asText());
        assertEquals("DATE", sheets.get(1).get("roles").get("date").asText());
        assertEquals("the variable of the second file is asked of the instance", 1,
                get("sessions/" + id + "/report").get("variables").size());
        assertEquals(files.toString(), get("sessions/" + id + "/" + SessionFileAPI.PATH).toString());
    }

    @Test
    public void aFileOfTheSameNameIsRefused() throws Exception {
        String id = open("plan.csv", PLAN).get("session_id").asText();

        add(id, "PLAN.csv", MEASURES);

        assertEquals(400, status);
    }

    @Test
    public void aFileOfAnotherKindIsRefused() throws Exception {
        String id = open("plan.csv", PLAN).get("session_id").asText();

        add(id, "notes.txt", "just text");

        assertEquals(400, status);
    }

    @Test
    public void eachFileNamesItsSheetAfterItself() throws Exception {
        String id = open("plan.csv", PLAN).get("session_id").asText();

        JsonNode files = add(id, "copy.csv", "plot_id;x\nP01;1\n");

        assertEquals("copy", files.get(1).get("sheets").get(0).asText());
    }

    @Test
    public void aRemovedFileTakesItsSheetsAway() throws Exception {
        String id = open("plan.csv", PLAN).get("session_id").asText();
        JsonNode files = add(id, "mesures.csv", MEASURES);

        JsonNode remaining = delete(id, files.get(1).get("id").asText());

        assertEquals(200, status);
        assertEquals(1, remaining.size());
        assertEquals(1, get("sessions/" + id + "/" + FileDescriptionAPI.PATH).get("sheets").size());
        assertEquals(0, get("sessions/" + id + "/report").get("variables").size());
    }

    @Test
    public void theOnlyFileIsNotRemovedAndAnUnknownOneIsNotFound() throws Exception {
        JsonNode session = open("plan.csv", PLAN);
        String id = session.get("session_id").asText();

        delete(id, session.get("files").get(0).get("id").asText());
        assertEquals(400, status);

        delete(id, "nobody");
        assertEquals(404, status);
    }

    @Test
    public void aConversationATemplateReadsHoldsOneFile() throws Exception {
        llm.queue(StubLlmEndpoint.assistantReply("Bonjour"));
        String id;
        try (MultiPart multiPart = new FormDataMultiPart()
                .bodyPart(new FileDataBodyPart("file", WorkbookFixture.vitisFile()))) {
            Response response = getJsonPostResponseMultipart(target(BASE + "sessions"), multiPart);
            assertEquals(201, response.getStatus());
            id = response.readEntity(JsonNode.class).get("result").get("session_id").asText();
        }

        add(id, "plan.csv", PLAN);

        assertEquals(400, status);
    }

    @Test
    public void anUnknownConversationIsNotFound() throws Exception {
        get("sessions/nobody/" + SessionFileAPI.PATH);
        assertEquals(404, status);
        add("nobody", "plan.csv", PLAN);
        assertEquals(404, status);
        delete("nobody", "x");
        assertEquals(404, status);
    }

    //#region helpers

    private JsonNode open(String name, String content) throws Exception {
        return upload(target(BASE + "sessions").queryParam("profile", "custom"), name, content, 201);
    }

    private JsonNode add(String sessionId, String name, String content) throws Exception {
        return upload(target(BASE + "sessions/" + sessionId + "/" + SessionFileAPI.PATH), name, content, -1);
    }

    private JsonNode upload(WebTarget target, String name, String content, int expected) throws Exception {
        Path directory = Files.createTempDirectory("upload-");
        File file = directory.resolve(name).toFile();
        Files.write(file.toPath(), content.getBytes(Charset.forName("UTF-8")));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            Response response = getJsonPostResponseMultipart(target, multiPart);
            status = response.getStatus();
            if (expected > 0) {
                assertEquals(expected, status);
            }
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        } finally {
            Files.deleteIfExists(file.toPath());
            Files.deleteIfExists(directory);
        }
    }

    private JsonNode get(String path) throws Exception {
        return read(getJsonGetResponseAsAdmin(target(BASE + path)));
    }

    private JsonNode delete(String sessionId, String fileId) throws Exception {
        return read(appendAdminToken(target(BASE + "sessions/" + sessionId + "/" + SessionFileAPI.PATH + "/" + fileId))
                .delete());
    }

    private JsonNode read(Response response) {
        status = response.getStatus();
        JsonNode node = response.readEntity(JsonNode.class);
        return node != null && node.has("result") ? node.get("result") : node;
    }

    //#endregion
}
