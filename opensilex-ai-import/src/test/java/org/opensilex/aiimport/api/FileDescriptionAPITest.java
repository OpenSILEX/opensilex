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
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.custom.CustomWorkbooks;
import org.opensilex.aiimport.service.StubLlmEndpoint;
import org.opensilex.aiimport.service.store.AiImportSessionStore;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Describing a file nobody has a template for, end to end: the upload opens a conversation without
 * asking the assistant anything, the description is read and changed, the report follows, and every
 * change that cannot hold is refused as a whole.
 *
 * @author Arnaud Charleroy
 */
public class FileDescriptionAPITest extends AbstractMongoIntegrationTest {

    private static final String BASE = AiImportAPI.PATH + "/";
    private static final String SHEET = "essai";

    private static StubLlmEndpoint llm;

    private ExperimentModel experiment;
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
    public void seedAnExperiment() throws Exception {
        llm.reset();
        experiment = new ExperimentModel();
        experiment.setName("Described trial");
        experiment.setObjective("described");
        experiment.setStartDate(LocalDate.of(2026, 1, 1));
        getSparqlService().create(experiment);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(AiImportSessionStore.COLLECTION);
    }

    //#region reading

    @Test
    public void aFileOpenedAsDescribedDoesNotAskTheAssistantAnything() throws Exception {
        JsonNode session = open("custom");

        assertEquals("custom", session.get("profile_id").asText());
        assertEquals("no request was made to the language model", 0, llm.getRequestCount());
    }

    @Test
    public void theDescriptionShowsWhatEachSheetAndColumnIsTakenToBe() throws Exception {
        String id = open("custom").get("session_id").asText();

        JsonNode description = get(path(id));

        assertEquals(200, status);
        JsonNode sheet = description.get("sheets").get(0);
        assertEquals(SHEET, sheet.get("sheet").asText());
        assertEquals("OBJECTS_AND_DATA", sheet.get("kind").asText());
        assertFalse(sheet.get("kind_chosen").asBoolean());
        assertEquals("OBJECT", sheet.get("roles").get("plot_id").asText());
        assertEquals("DATE", sheet.get("roles").get("date").asText());
        assertEquals("VARIABLE", sheet.get("roles").get("hauteur_cm").asText());
        assertEquals(4, sheet.get("row_count").asInt());
        assertTrue("objects and observations belong to an experiment", sheet.get("needs_experiment").asBoolean());
        assertFalse("and no column names it", sheet.get("names_experiment").asBoolean());
        assertFalse("kinds and roles to choose from are served, not written in the page",
                description.get("available_kinds").isEmpty() || description.get("available_roles").isEmpty());
    }

    @Test
    public void anUnknownConversationIsNotFound() throws Exception {
        get(path("nobody"));
        assertEquals(404, status);
        describe("nobody", Map.of("sheets", List.of()));
        assertEquals(404, status);
    }

    //#endregion

    //#region describing

    @Test
    public void aRoleChangedByTheUserChangesTheReport() throws Exception {
        String id = open("custom").get("session_id").asText();
        int before = get("sessions/" + id + "/report").get("variables").size();

        JsonNode description = describe(id, Map.of("sheets", List.of(
                Map.of("sheet", SHEET, "roles", Map.of("nb_grappes", "UNREAD")))));

        assertEquals(description.toString(), 200, status);
        JsonNode sheet = description.get("sheets").get(0);
        assertEquals("UNREAD", sheet.get("roles").get("nb_grappes").asText());
        assertEquals("nb_grappes", sheet.get("roles_chosen").get(0).asText());
        assertEquals("one variable fewer is asked of the instance", before - 1,
                get("sessions/" + id + "/report").get("variables").size());
        assertEquals("the choice is the user's, so it is kept in the next read", "UNREAD",
                get(path(id)).get("sheets").get(0).get("roles").get("nb_grappes").asText());
    }

    @Test
    public void anEmptyRoleGivesTheColumnBackItsDefault() throws Exception {
        String id = open("custom").get("session_id").asText();
        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "roles", Map.of("nb_grappes", "UNREAD")))));

        JsonNode description = describe(id, Map.of("sheets", List.of(
                Map.of("sheet", SHEET, "roles", Map.of("nb_grappes", "")))));

        JsonNode sheet = description.get("sheets").get(0);
        assertEquals("VARIABLE", sheet.get("roles").get("nb_grappes").asText());
        assertTrue(sheet.get("roles_chosen").isEmpty());
    }

    @Test
    public void theFactorOfASheetOfLevelsIsKeptAndCleared() throws Exception {
        String id = open("custom").get("session_id").asText();

        JsonNode sheet = describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "FACTORS",
                "factor", "Traitement")))).get("sheets").get(0);

        assertEquals(200, status);
        assertEquals("Traitement", sheet.get("factor").asText());
        assertEquals("kept with the conversation", "Traitement",
                get(path(id)).get("sheets").get(0).get("factor").asText());

        JsonNode cleared = describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "factor", "")))).get("sheets")
                .get(0);
        assertTrue(cleared.get("factor").isNull());
    }

    @Test
    public void aKindChangedByTheUserIsRead() throws Exception {
        String id = open("custom").get("session_id").asText();

        JsonNode description = describe(id, Map.of("sheets", List.of(
                Map.of("sheet", SHEET, "kind", "DATA"))));

        JsonNode sheet = description.get("sheets").get(0);
        assertEquals("DATA", sheet.get("kind").asText());
        assertTrue(sheet.get("kind_chosen").asBoolean());
    }

    @Test
    public void anExperimentTheUserCanSeeIsConfirmedForTheFile() throws Exception {
        String id = open("custom").get("session_id").asText();

        JsonNode description = describe(id, Map.of("experiment", experiment.getUri().toString(),
                "sheets", List.of()));

        assertEquals(description.toString(), 200, status);
        assertEquals(experiment.getUri().toString(), description.get("experiment").asText());
        assertEquals("Described trial", description.get("experiment_name").asText());
        assertEquals("the report names the experiment", "Described trial",
                get("sessions/" + id + "/report").get("experiments").get(0).get("source_value").asText());
    }

    @Test
    public void aFileNoTemplateReadsIsReadAsDescribedFromTheFirstDescription() throws Exception {
        String id = open(null).get("session_id").asText();
        assertEquals("generic", get("sessions/" + id).get("profile_id").asText());

        describe(id, Map.of("sheets", List.of(Map.of("sheet", SHEET, "kind", "OBJECTS_AND_DATA"))));

        assertEquals(200, status);
        assertEquals("custom", get("sessions/" + id).get("profile_id").asText());
    }

    //#endregion

    //#region refusing

    @Test
    public void aFileReadByATemplateIsNotDescribed() throws Exception {
        String id = openFile(WorkbookFixture.vitisFile(), null).get("session_id").asText();
        llm.queue(StubLlmEndpoint.assistantReply("unused"));

        describe(id, Map.of("sheets", List.of(Map.of("sheet", "Cartouche_Fixe", "kind", "DATA"))));

        assertEquals(400, status);
    }

    @Test
    public void whatCannotHoldIsRefusedAndNothingIsChanged() throws Exception {
        String id = open("custom").get("session_id").asText();

        refused(id, Map.of("sheet", "nowhere", "kind", "DATA"));
        refused(id, Map.of("sheet", SHEET, "kind", "NOT_A_KIND"));
        refused(id, Map.of("sheet", SHEET, "roles", Map.of("no_such_column", "DATE")));
        refused(id, Map.of("sheet", SHEET, "roles", Map.of("date", "NOT_A_ROLE")));
        refused(id, Map.of("sheet", SHEET, "properties", Map.of("no_such_column", "http://example.org/p")));
        refused(id, Map.of("sheet", SHEET, "properties", Map.of("remarque", "not a uri")));
        refused(id, Map.of("sheet", SHEET, "rdf_type", Oeso.Facility.getURI().toString()));

        // A change fully valid but for its last part leaves the first part undone.
        describe(id, Map.of("sheets", List.of(
                Map.of("sheet", SHEET, "roles", Map.of("nb_grappes", "UNREAD")),
                Map.of("sheet", "nowhere", "kind", "DATA"))));
        assertEquals(400, status);
        assertEquals("VARIABLE",
                get(path(id)).get("sheets").get(0).get("roles").get("nb_grappes").asText());

        describe(id, Map.of("experiment", "http://example.org/not-an-experiment", "sheets", List.of()));
        assertEquals(400, status);
    }

    //#endregion

    //#region properties of a type

    @Test
    public void thePropertiesOfATypeAreReadFromTheOntologyForTheKindOfSheet() throws Exception {
        String id = open("custom").get("session_id").asText();

        JsonNode objects = get(path(id) + "/type_properties?kind=OBJECTS&type=" + Oeso.ScientificObject.getURI());
        assertEquals(objects.toString(), 200, status);
        assertTrue(objects.size() > 0);

        JsonNode facilities = get(path(id) + "/type_properties?kind=FACILITIES&type=" + Oeso.Facility.getURI());
        assertEquals(facilities.toString(), 200, status);

        get(path(id) + "/type_properties?kind=DATA&type=" + Oeso.ScientificObject.getURI());
        assertEquals("observations have no type", 400, status);

        get(path(id) + "/type_properties?kind=OBJECTS&type=" + Oeso.Facility.getURI());
        assertEquals("a facility is not an object", 400, status);

        get(path(id) + "/type_properties?kind=NOT_A_KIND&type=" + Oeso.Facility.getURI());
        assertEquals(400, status);
    }

    //#endregion

    //#region helpers

    private static String path(String sessionId) {
        return "sessions/" + sessionId + "/" + FileDescriptionAPI.PATH;
    }

    private void refused(String sessionId, Map<String, Object> sheet) throws Exception {
        describe(sessionId, Map.of("sheets", List.of(sheet)));
        assertEquals(sheet.toString(), 400, status);
    }

    private JsonNode open(String profile) throws Exception {
        File file = WorkbookFiles.write("described-", CustomWorkbooks.flatSheet());
        try {
            return openFile(file, profile);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private JsonNode openFile(File workbook, String profile) throws Exception {
        if (!"custom".equals(profile)) {
            llm.queue(StubLlmEndpoint.assistantReply("Bonjour"));
        }
        WebTarget target = target(BASE + "sessions");
        if (profile != null) {
            target = target.queryParam("profile", profile);
        }
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", workbook))) {
            Response response = getJsonPostResponseMultipart(target, multiPart);
            assertEquals(201, response.getStatus());
            return response.readEntity(JsonNode.class).get("result");
        }
    }

    private JsonNode get(String path) throws Exception {
        String[] parts = path.split("\\?", 2);
        WebTarget target = target(BASE + parts[0]);
        if (parts.length > 1) {
            for (String pair : parts[1].split("&")) {
                String[] query = pair.split("=", 2);
                target = target.queryParam(query[0], query[1]);
            }
        }
        return read(getJsonGetResponseAsAdmin(target));
    }

    private JsonNode describe(String sessionId, Map<String, ?> body) throws Exception {
        Map<String, Object> full = new LinkedHashMap<>(body);
        full.put("session_id", sessionId);
        return post(FileDescriptionAPI.PATH, full);
    }

    private JsonNode post(String path, Object body) throws Exception {
        return read(getJsonPostResponseAsAdmin(target(BASE + path), body));
    }

    private JsonNode read(Response response) {
        status = response.getStatus();
        JsonNode node = response.readEntity(JsonNode.class);
        return node != null && node.has("result") ? node.get("result") : node;
    }

    //#endregion
}
