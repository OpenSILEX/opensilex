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
import org.opensilex.aiimport.WorkbookFiles;
import org.opensilex.aiimport.profile.custom.CustomWorkbooks;
import org.opensilex.aiimport.service.StubLlmEndpoint;
import org.opensilex.aiimport.service.store.AiImportSessionStore;
import org.opensilex.aiimport.service.store.FileTemplateStore;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Templates, end to end: what a user says of a file is kept and proposed for the next of the same
 * shape, and a template can be renamed, shared, downloaded as CSVW or RML, deleted and uploaded
 * again.
 *
 * @author Arnaud Charleroy
 */
public class FileTemplateAPITest extends AbstractMongoIntegrationTest {

    private static final String BASE = AiImportAPI.PATH + "/";
    private static final String SHEET = "essai";

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
        return List.of(AiImportSessionStore.COLLECTION, FileTemplateStore.COLLECTION);
    }

    //#region keeping and proposing

    @Test
    public void whatTheUserSaysIsKeptAsATemplateAndProposedForTheNextFileOfTheSameShape() throws Exception {
        String first = open().get("session_id").asText();
        describe(first, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "OBJECTS_AND_DATA",
                "roles", Map.of("remarque", "COMMENT")))), null);
        assertEquals(200, status);
        assertEquals("one template, the user's own", 1, get("templates").size());

        String second = open().get("session_id").asText();
        JsonNode suggestion = get("sessions/" + second + "/" + FileDescriptionAPI.PATH).get("sheets").get(0)
                .get("suggestion");

        assertFalse("a sheet of the same shape is proposed the template", suggestion.isNull());
        assertEquals("OBJECTS_AND_DATA", suggestion.get("kind").asText());
        assertEquals("COMMENT", suggestion.get("roles").get("remarque").asText());
        assertEquals(8, suggestion.get("recognised").asInt());
        assertEquals(8, suggestion.get("total").asInt());
        assertEquals(1.0, suggestion.get("score").asDouble(), 0.0001);
        assertEquals("nothing was applied", false,
                get("sessions/" + second + "/" + FileDescriptionAPI.PATH).get("sheets").get(0).get("kind_chosen")
                        .asBoolean());
    }

    @Test
    public void theTemplateOfAConversationIsNotProposedBackToIt() throws Exception {
        String id = open().get("session_id").asText();
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "DATA"))), null);

        assertTrue(get("sessions/" + id + "/" + FileDescriptionAPI.PATH).get("sheets").get(0).get("suggestion")
                .isNull());
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "OBJECTS"))), null);
        assertEquals("the template follows the last word: still one", 1, get("templates").size());
    }

    @Test
    public void nothingIsKeptWhenTheUserSaysNotTo() throws Exception {
        String id = open().get("session_id").asText();

        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "DATA"))), false);

        assertEquals(200, status);
        assertTrue(get("templates").isEmpty());
    }

    @Test
    public void choosingTheExperimentAloneKeepsNoTemplate() throws Exception {
        String id = open().get("session_id").asText();

        describe(id, Map.of("sheets", List.of()), null);

        assertTrue(get("templates").isEmpty());
    }

    //#endregion

    //#region managing

    @Test
    public void aTemplateIsRenamedSharedAndDeletedByItsAuthor() throws Exception {
        String id = open().get("session_id").asText();
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "DATA"))), null);
        String template = get("templates").get(0).get("id").asText();

        JsonNode updated = put("templates", Map.of("id", template, "name", "mon gabarit", "shared", true));

        assertEquals(200, status);
        assertEquals("mon gabarit", updated.get("name").asText());
        assertTrue(updated.get("shared").asBoolean());
        assertTrue(updated.get("is_mine").asBoolean());

        put("templates", Map.of("id", "nobody", "name", "x"));
        assertEquals(404, status);

        assertEquals(200, delete("templates/" + template));
        assertTrue(get("templates").isEmpty());
        assertEquals(404, delete("templates/" + template));
    }

    @Test
    public void aTemplateIsDownloadedAsCsvwAndAsRml() throws Exception {
        String id = open().get("session_id").asText();
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "OBJECTS_AND_DATA"))), null);
        String template = get("templates").get(0).get("id").asText();

        Response csvw = appendAdminToken(target(BASE + "templates/" + template + "/csvw")).get();
        assertEquals(200, csvw.getStatus());
        assertEquals("application/csvm+json", csvw.getMediaType().toString());
        assertTrue(csvw.getHeaderString("Content-Disposition").contains(".csvm.json"));
        String document = csvw.readEntity(String.class);
        assertTrue(document, document.contains("\"TableGroup\"") && document.contains("plot_id"));

        Response rml = appendAdminToken(target(BASE + "templates/" + template + "/rml")).get();
        assertEquals(200, rml.getStatus());
        assertTrue(rml.getMediaType().toString().startsWith("text/turtle"));
        assertTrue(rml.readEntity(String.class).contains("TriplesMap"));

        assertEquals(404, appendAdminToken(target(BASE + "templates/nobody/csvw")).get().getStatus());
        assertEquals(404, appendAdminToken(target(BASE + "templates/nobody/rml")).get().getStatus());
    }

    @Test
    public void theDescriptionIsDownloadedAsCsvwWithoutKeepingATemplate() throws Exception {
        String id = open().get("session_id").asText();

        Response csvw = appendAdminToken(target(BASE + "sessions/" + id + "/" + FileDescriptionAPI.PATH + "/csvw"))
                .get();

        assertEquals(200, csvw.getStatus());
        assertTrue(csvw.readEntity(String.class).contains("\"tables\""));
        assertTrue(get("templates").isEmpty());
    }

    @Test
    public void aDownloadedTemplateComesBackWhenUploadedAgain() throws Exception {
        String id = open().get("session_id").asText();
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "OBJECTS_AND_DATA"))), null);
        String template = get("templates").get(0).get("id").asText();
        String document = appendAdminToken(target(BASE + "templates/" + template + "/csvw")).get()
                .readEntity(String.class);
        delete("templates/" + template);

        JsonNode imported = upload("templates/import", "gabarit.csvm.json", document);

        assertEquals(imported.toString(), 201, status);
        assertEquals(List.of(SHEET), List.of(imported.get("sheets").get(0).asText()));
        assertTrue(imported.get("is_mine").asBoolean());
        assertEquals(1, get("templates").size());
    }

    @Test
    public void aDocumentThatIsNotATemplateIsRefusedWithTheReason() throws Exception {
        JsonNode refused = upload("templates/import", "x.json", "{\"hello\": 1}");

        assertEquals(400, status);
        assertTrue(refused.toString(), refused.toString().contains("CSVW"));
        assertTrue(get("templates").isEmpty());
    }

    //#endregion

    //#region exporting

    @Test
    public void anExperimentIsExportedInTheShapeOfATemplate() throws Exception {
        String id = open().get("session_id").asText();
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "OBJECTS_AND_DATA"))), null);
        String template = get("templates").get(0).get("id").asText();
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName("Exported trial");
        experiment.setObjective("export");
        experiment.setStartDate(LocalDate.of(2026, 1, 1));
        getSparqlService().create(experiment);

        Response response = appendAdminToken(target(BASE + "templates/" + template + "/export")
                .queryParam("experiment", experiment.getUri().toString())).get();

        assertEquals(200, response.getStatus());
        assertEquals(StarExportAPI.XLSX_MEDIA_TYPE, response.getMediaType().toString());
        assertTrue(response.getHeaderString("Content-Disposition").contains(".xlsx"));
        try (org.apache.poi.ss.usermodel.Workbook book = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                response.readEntity(java.io.InputStream.class))) {
            assertEquals(List.of(SHEET, "readme"), List.of(book.getSheetName(0), book.getSheetName(1)));
            assertTrue(book.getSheet("readme").getRow(0).getCell(0).toString().contains("Exported trial"));
        }
    }

    @Test
    public void aTemplateOrAnExperimentThatIsNotThereIsNotFound() throws Exception {
        assertEquals(404, appendAdminToken(target(BASE + "templates/nobody/export")
                .queryParam("experiment", "http://opensilex.test/id/experiment/nowhere")).get().getStatus());

        String id = open().get("session_id").asText();
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "DATA"))), null);
        String template = get("templates").get(0).get("id").asText();
        assertEquals(404, appendAdminToken(target(BASE + "templates/" + template + "/export")
                .queryParam("experiment", "http://opensilex.test/id/experiment/nowhere")).get().getStatus());
    }

    //#endregion

    //#region helpers

    private JsonNode open() throws Exception {
        File file = WorkbookFiles.write("shape-", CustomWorkbooks.flatSheet());
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            Response response = getJsonPostResponseMultipart(target(BASE + "sessions").queryParam("profile", "custom"),
                    multiPart);
            assertEquals(201, response.getStatus());
            return response.readEntity(JsonNode.class).get("result");
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private JsonNode describe(String sessionId, Map<String, ?> body, Boolean remember) throws Exception {
        Map<String, Object> full = new LinkedHashMap<>(body);
        full.put("session_id", sessionId);
        if (remember != null) {
            full.put("remember", remember);
        }
        return post("sheet_descriptions", full);
    }

    private JsonNode upload(String path, String name, String content) throws Exception {
        Path directory = Files.createTempDirectory("upload-");
        File file = directory.resolve(name).toFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return read(getJsonPostResponseMultipart(target(BASE + path), multiPart));
        } finally {
            Files.deleteIfExists(file.toPath());
            Files.deleteIfExists(directory);
        }
    }

    private JsonNode get(String path) throws Exception {
        WebTarget target = target(BASE + path);
        return read(getJsonGetResponseAsAdmin(target));
    }

    private JsonNode post(String path, Object body) throws Exception {
        return read(getJsonPostResponseAsAdmin(target(BASE + path), body));
    }

    private JsonNode put(String path, Object body) throws Exception {
        return read(getJsonPutResponse(target(BASE + path), body));
    }

    private int delete(String path) throws Exception {
        return appendAdminToken(target(BASE + path)).delete().getStatus();
    }

    private JsonNode read(Response response) {
        status = response.getStatus();
        JsonNode node = response.readEntity(JsonNode.class);
        return node != null && node.has("result") ? node.get("result") : node;
    }

    //#endregion
}
