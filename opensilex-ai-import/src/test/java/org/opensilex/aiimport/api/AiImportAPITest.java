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
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.service.StubLlmEndpoint;
import org.opensilex.aiimport.service.store.AiImportSessionStore;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The assistant's REST surface, end to end: a real upload of the VitisExplorer workbook, a real
 * resolution against an instance in memory, and a language model stood in for by
 * {@link StubLlmEndpoint}, which replies with the tool calls each test scripts.
 * <p>
 * The module's test configuration ({@code config/test/opensilex.yml}) turns the assistant on and
 * points it at the stub's port.
 *
 * @author Arnaud Charleroy
 */
public class AiImportAPITest extends AbstractMongoIntegrationTest {

    private static final String BASE = AiImportAPI.PATH + "/";

    private static StubLlmEndpoint llm;
    private static String experimentInFile;

    private ExperimentModel nearExperiment;
    private int status;

    @BeforeClass
    public static void aLanguageModel() throws Exception {
        llm = new StubLlmEndpoint(StubLlmEndpoint.TEST_PORT);
        experimentInFile = new VitisExplorerProfile().extract(WorkbookFixture.vitis())
                .getExperimentNames().get(0);
    }

    @AfterClass
    public static void noMoreLanguageModel() {
        llm.close();
    }

    /**
     * An experiment whose name is one letter away from the file's, so the report suggests it.
     */
    @Before
    public void seedANearExperiment() throws Exception {
        llm.reset();
        nearExperiment = new ExperimentModel();
        nearExperiment.setName(experimentInFile.substring(0, experimentInFile.length() - 1));
        nearExperiment.setObjective("near match");
        nearExperiment.setStartDate(LocalDate.of(2020, 1, 1));
        getSparqlService().create(nearExperiment);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, ProjectModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(AiImportSessionStore.COLLECTION);
    }

    //#region sessions

    @Test
    public void theProfilesAreListed() throws Exception {
        JsonNode profiles = get("profiles");

        assertEquals(200, status);
        assertTrue(profiles.toString(), profiles.toString().contains("vitis-explorer"));
    }

    @Test
    public void theAssistantIsReportedConnectedWhenItsEndpointAnswers() throws Exception {
        JsonNode assistant = get(AiImportAPI.ASSISTANT_PATH);

        assertEquals(200, status);
        assertTrue(assistant.get("configured").asBoolean());
        assertTrue(assistant.get("reachable").asBoolean());
    }

    @Test
    public void theAssistantIsReportedDisconnectedWhenItsEndpointRefuses() throws Exception {
        llm.respondToModelsWithStatus(503);

        JsonNode assistant = get(AiImportAPI.ASSISTANT_PATH);

        assertTrue(assistant.get("configured").asBoolean());
        assertFalse("the page folds the conversation away on this answer",
                assistant.get("reachable").asBoolean());
    }

    @Test
    public void aWorkbookOpensAConversationWithItsAnalysis() throws Exception {
        JsonNode session = openSession();

        assertEquals(WorkbookFixture.VITIS_FILE_NAME, session.get("file_name").asText());
        assertEquals("vitis-explorer", session.get("profile_id").asText());
        assertFalse(session.get("report").get("experiments").isEmpty());
        assertFalse(session.get("structure").get("sheets").isEmpty());
        assertFalse(session.get("mapping").isEmpty());
        assertEquals("the opening analysis", "Bonjour",
                last(session.get("messages")).get("content").asText());

        String id = session.get("session_id").asText();
        assertEquals(id, get("sessions/" + id).get("session_id").asText());
        assertFalse(get("sessions/" + id + "/report").get("experiments").isEmpty());
        assertFalse(get("sessions/" + id + "/mapping").isEmpty());
        assertFalse(post("sessions/" + id + "/revalidate", "").get("experiments").isEmpty());
        assertEquals(200, status);
    }

    @Test
    public void theRequirementsOfEachTargetAreAnswered() throws Exception {
        String id = openSession().get("session_id").asText();

        JsonNode project = get("sessions/" + id + "/creation-requirements?target=PROJECT");
        assertEquals(200, status);
        assertTrue(project.get("fields").toString().contains("start_date"));

        JsonNode data = get("sessions/" + id + "/creation-requirements?target=DATA");
        assertFalse("the file's variables are not in the instance", data.get("is_available").asBoolean());

        get("sessions/" + id + "/creation-requirements?target=NOTHING");
        assertEquals(400, status);
    }

    @Test
    public void aFileThatIsNotASpreadsheetIsRefused() throws Exception {
        File text = File.createTempFile("not-a-workbook", ".txt");
        Files.writeString(text.toPath(), "just text");
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", text))) {
            Response response = getJsonPostResponseMultipart(target(BASE + "sessions"), multiPart);
            assertEquals(400, response.getStatus());
        } finally {
            Files.deleteIfExists(text.toPath());
        }
    }

    @Test
    public void anUnknownConversationIsNotFound() throws Exception {
        get("sessions/nobody");
        assertEquals(404, status);
        get("sessions/nobody/report");
        assertEquals(404, status);
        post("messages", Map.of("session_id", "nobody", "content", "hello"));
        assertEquals(404, status);
    }

    @Test
    public void storedConversationsAreListedAndDeleted() throws Exception {
        String id = openSession().get("session_id").asText();

        JsonNode saved = get("sessions");
        assertEquals(200, status);
        assertTrue(saved.toString(), saved.toString().contains(id));
        assertTrue(saved.get(0).has("expires_at"));

        assertEquals(200, appendAdminToken(target(BASE + "sessions/" + id)).delete().getStatus());
        get("sessions/" + id);
        assertEquals(404, status);
        assertEquals(404, appendAdminToken(target(BASE + "sessions/" + id)).delete().getStatus());
    }

    //#endregion

    //#region conversation

    /**
     * The model looks things up before answering; every lookup runs against the instance, and the
     * reply lists them.
     */
    @Test
    public void aQuestionIsAnsweredWithTheLookupsMade() throws Exception {
        String id = openSession().get("session_id").asText();
        llm.queue(StubLlmEndpoint.toolCallReply("c1", "get_report", "{\"category\":\"experiments\"}"))
                .queue(StubLlmEndpoint.toolCallReply("c2", "get_sheet_preview", "{\"sheet\":\"Cartouche_Fixe\"}"))
                .queue(StubLlmEndpoint.toolCallReply("c3", "get_mapping", "{}"))
                .queue(StubLlmEndpoint.toolCallReply("c4", "get_creation_fields", "{\"target\":\"PROJECT\"}"))
                .queue(StubLlmEndpoint.assistantReply("Voilà ce que j'ai trouvé."));

        JsonNode reply = post("messages", Map.of("session_id", id, "content", "Que manque-t-il ?"));

        assertEquals(reply.toString(), 200, status);
        assertEquals("Voilà ce que j'ai trouvé.", reply.get("content").asText());
        assertTrue(reply.get("lookups").toString(), reply.get("lookups").size() >= 4);

        // The searches, in a second turn: a turn allows a bounded number of lookups.
        llm.queue(StubLlmEndpoint.toolCallReply("c5", "search_experiments", "{\"name\":\"CEP\"}"))
                .queue(StubLlmEndpoint.toolCallReply("c6", "search_projects", "{\"name\":\"Vitis\"}"))
                .queue(StubLlmEndpoint.toolCallReply("c7", "search_germplasm", "{\"name\":\"Chardonnay\"}"))
                .queue(StubLlmEndpoint.toolCallReply("c8", "search_variables", "{\"name\":\"height\"}"))
                .queue(StubLlmEndpoint.assistantReply("Rien de plus."));
        reply = post("messages", Map.of("session_id", id, "content", "Et dans l'instance ?"));
        assertEquals("Rien de plus.", reply.get("content").asText());
    }

    /**
     * A model that keeps calling tools is stopped, and the user is told why rather than left
     * waiting.
     */
    @Test
    public void aModelThatNeverConcludesIsStopped() throws Exception {
        String id = openSession().get("session_id").asText();
        for (int i = 0; i < 8; i++) {
            llm.queue(StubLlmEndpoint.toolCallReply("l" + i, "get_report", "{}"));
        }

        JsonNode reply = post("messages", Map.of("session_id", id, "content", "Cherche encore."));

        assertEquals(200, status);
        assertEquals("AiImport.chat.tooManyLookups", reply.get("content_key").asText());
    }

    /**
     * A draft made by the model, confirmed by the user: the project is created, the draft cannot be
     * confirmed twice, and the conversation remembers it as applied.
     */
    @Test
    public void aDraftIsConfirmedIntoAProjectOnce() throws Exception {
        String id = openSession().get("session_id").asText();
        String proposal = propose(id, "{\"target\":\"PROJECT\",\"fields\":{\"name\":\"Vitis Adaptation Test\","
                + "\"start_date\":\"2024-01-01\"},\"rationale\":\"from the cartouche\"}");

        JsonNode created = post("create", Map.of("session_id", id, "proposal_id", proposal,
                "values", Map.of()));
        assertEquals(created.toString(), 201, status);
        assertNotNull(created.get("uri").asText());
        assertTrue(created.has("report"));

        post("create", Map.of("session_id", id, "proposal_id", proposal, "values", Map.of()));
        assertEquals("a draft is applied once", 400, status);

        assertTrue(get("sessions/" + id).get("proposals").toString().contains("APPLIED"));
    }

    @Test
    public void aDraftIsConfirmedIntoAnExperiment() throws Exception {
        String id = openSession().get("session_id").asText();
        String proposal = propose(id, "{\"target\":\"EXPERIMENT\",\"fields\":{\"name\":\"Essai ai-import\","
                + "\"objective\":\"Tester l'assistant\",\"start_date\":\"2020-03-01\","
                + "\"end_date\":\"2020-10-31\"}}");

        JsonNode created = post("create", Map.of("session_id", id, "proposal_id", proposal,
                "values", Map.of("description", "Créée par le test")));

        assertEquals(created.toString(), 201, status);
        assertEquals("EXPERIMENT", created.get("target").asText());
        assertNotNull(created.get("uri").asText());
    }

    @Test
    public void aDraftMissingARequiredFieldIsRefused() throws Exception {
        String id = openSession().get("session_id").asText();
        String proposal = propose(id, "{\"target\":\"PROJECT\",\"fields\":{\"name\":\"No date\"}}");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("session_id", id);
        request.put("proposal_id", proposal);
        request.put("values", Map.of("start_date", ""));
        post("create", request);

        assertEquals(400, status);
    }

    @Test
    public void aDraftCanBeCancelled() throws Exception {
        String id = openSession().get("session_id").asText();
        String proposal = propose(id, "{\"target\":\"EXPERIMENT\",\"fields\":{\"name\":\"Essai\"}}");

        post("cancel-proposal", Map.of("session_id", id, "proposal_id", proposal));
        assertEquals(200, status);
        post("create", Map.of("session_id", id, "proposal_id", proposal, "values", Map.of()));
        assertEquals("a cancelled draft is not confirmed", 400, status);
    }

    /**
     * The data cannot be inserted while the file's variables are missing: the draft is refused with
     * what stands in the way, before anything is written.
     */
    @Test
    public void dataIsBlockedWhileItsVariablesAreMissing() throws Exception {
        String id = openSession().get("session_id").asText();
        String proposal = propose(id, "{\"target\":\"DATA\",\"fields\":{\"provenance_name\":\"test\"}}");

        post("create", Map.of("session_id", id, "proposal_id", proposal, "values", Map.of()));

        assertEquals(400, status);
    }

    /**
     * What a model may get wrong — a tool that does not exist, arguments that are not JSON — is
     * told back to it as a result, and the conversation goes on.
     */
    @Test
    public void aMisbehavingModelIsAnsweredNotObeyed() throws Exception {
        String id = openSession().get("session_id").asText();
        llm.queue(StubLlmEndpoint.toolCallReply("m1", "delete_everything", "{}"))
                .queue(StubLlmEndpoint.toolCallReply("m2", "get_report", "{not json"))
                .queue(StubLlmEndpoint.assistantReply("Désolé."));

        JsonNode reply = post("messages", Map.of("session_id", id, "content", "Fais n'importe quoi."));

        assertEquals(200, status);
        assertEquals("Désolé.", reply.get("content").asText());
        assertTrue(llm.getRequestBodies().get(llm.getRequestCount() - 1).contains("There is no tool named"));
    }

    @Test
    public void anUnreachableModelIsSaidPlainly() throws Exception {
        String id = openSession().get("session_id").asText();
        llm.respondWithStatus(500);

        JsonNode reply = post("messages", Map.of("session_id", id, "content", "Tu es là ?"));

        assertEquals(200, status);
        assertEquals("AiImport.chat.assistantUnreachable", reply.get("content_key").asText());
    }

    /**
     * Old lookups are shortened as the conversation grows, so the prompt does not.
     */
    @Test
    public void aLongConversationKeepsGoing() throws Exception {
        String id = openSession().get("session_id").asText();
        llm.queue(StubLlmEndpoint.toolCallReply("r1", "get_report", "{\"category\":\"variables\"}"))
                .queue(StubLlmEndpoint.assistantReply("Première réponse."));
        post("messages", Map.of("session_id", id, "content", "Les variables ?"));
        for (int turn = 2; turn <= 4; turn++) {
            llm.queue(StubLlmEndpoint.assistantReply("Réponse " + turn));
            JsonNode reply = post("messages", Map.of("session_id", id, "content", "Et encore ?"));
            assertEquals("Réponse " + turn, reply.get("content").asText());
        }
        assertTrue("the old lookup was shortened in what the model is sent",
                llm.getRequestBodies().get(llm.getRequestCount() - 1).contains("shortened"));
    }

    @Test
    public void aFileFamilyCanBeForced() throws Exception {
        llm.queue(StubLlmEndpoint.assistantReply("Bonjour"));
        try (MultiPart multiPart = new FormDataMultiPart()
                .bodyPart(new FileDataBodyPart("file", WorkbookFixture.vitisFile()))) {
            Response response = getJsonPostResponseMultipart(
                    target(BASE + "sessions").queryParam("profile", "generic"), multiPart);
            assertEquals(201, response.getStatus());
            assertEquals("generic", response.readEntity(JsonNode.class).get("result").get("profile_id").asText());
        }
    }

    /**
     * Someone else's conversation, or one that never existed, is not found — from every endpoint.
     */
    @Test
    public void everyEndpointSaysNotFoundForAnUnknownConversation() throws Exception {
        Map<String, Object> onNobody = Map.of("session_id", "nobody", "proposal_id", "p",
                "category", "experiments", "value", "x", "uri", "http://opensilex.test/id/x",
                "values", Map.of());
        for (String path : List.of("create", "cancel-proposal", "matches", "matches/forget",
                "matches/created", "corrections", "corrections/forget")) {
            post(path, onNobody);
            assertEquals(path, 404, status);
        }
        for (String path : List.of("sessions/nobody/mapping", "sessions/nobody/creation-requirements?target=DATA")) {
            get(path);
            assertEquals(path, 404, status);
        }
        post("sessions/nobody/revalidate", "");
        assertEquals(404, status);

        String id = openSession().get("session_id").asText();
        post("create", Map.of("session_id", id, "proposal_id", "no-such-draft", "values", Map.of()));
        assertEquals("an unknown draft", 404, status);
        post("cancel-proposal", Map.of("session_id", id, "proposal_id", "no-such-draft"));
        assertEquals(404, status);
        post("corrections", Map.of("session_id", id, "category", "scientific_objects", "value", "x",
                "uri", "http://opensilex.test/id/x"));
        assertEquals("a plot code is never taught", 400, status);
        post("corrections/forget", Map.of("session_id", id, "category", "planets", "value", "x",
                "uri", "http://opensilex.test/id/x"));
        assertEquals(400, status);
    }

    //#endregion

    //#region near matches

    /**
     * The whole life of a misspelling: suggested, confirmed for the session, taught to the instance,
     * forgotten again.
     */
    @Test
    public void aMisspeltExperimentIsConfirmedTaughtAndForgotten() throws Exception {
        JsonNode session = openSession();
        String id = session.get("session_id").asText();
        JsonNode item = experimentItem(session.get("report"));
        assertFalse("the near experiment is suggested: " + item, item.get("suggestions").isEmpty());
        String uri = item.get("suggestions").get(0).get("uri").asText();

        Map<String, Object> match = Map.of("session_id", id, "category", "experiments",
                "value", experimentInFile, "uri", uri);
        JsonNode confirmed = post("matches", match);
        assertEquals(confirmed.toString(), 200, status);
        assertEquals("FOUND", experimentItem(confirmed).get("status").asText());

        post("corrections", match);
        assertEquals(200, status);
        post("corrections/forget", match);
        assertEquals(200, status);
        post("corrections/forget", match);
        assertEquals("nothing left to forget", 404, status);

        post("matches/forget", match);
        assertEquals(200, status);
    }

    @Test
    public void onlyASuggestionCanBeConfirmed() throws Exception {
        String id = openSession().get("session_id").asText();

        post("matches", Map.of("session_id", id, "category", "experiments", "value", experimentInFile,
                "uri", "http://opensilex.test/id/experiment/elsewhere"));
        assertEquals(400, status);
        post("matches", Map.of("session_id", id, "category", "nothing", "value", "x",
                "uri", "http://opensilex.test/id/x"));
        assertEquals(400, status);
    }

    /**
     * A resource created from the report's Create button is bound to its row by URI, read back under
     * the user's rights; a URI nobody can see is refused.
     */
    @Test
    public void aCreatedResourceIsBoundToItsRow() throws Exception {
        String id = openSession().get("session_id").asText();

        JsonNode report = post("matches/created", Map.of("session_id", id, "category", "experiments",
                "value", experimentInFile, "uri", nearExperiment.getUri().toString()));
        assertEquals(report.toString(), 200, status);
        assertEquals("FOUND", experimentItem(report).get("status").asText());

        post("matches/created", Map.of("session_id", id, "category", "experiments",
                "value", experimentInFile, "uri", "http://opensilex.test/id/experiment/nowhere"));
        assertEquals(400, status);
    }

    //#endregion

    //#region object sheets

    private static final String STAR_FILE = "STAR_standard.xlsx";
    private static final String PLOTS = "ed_placette";

    private JsonNode sheetNamed(JsonNode sheets, String name) {
        for (JsonNode sheet : sheets) {
            if (name.equals(sheet.get("sheet").asText())) {
                return sheet;
            }
        }
        throw new AssertionError("no sheet " + name + " in " + sheets);
    }

    /**
     * The plot sheet of a STAR file, as the panel draws it: the name column locked, the columns
     * STAR names already mapped, no type until someone chooses one.
     */
    @Test
    public void theObjectSheetsOfAStarFileAreListed() throws Exception {
        String id = openSession(WorkbookFixture.file(STAR_FILE)).get("session_id").asText();

        JsonNode plots = sheetNamed(get("sessions/" + id + "/object-sheets"), PLOTS);

        assertEquals(200, status);
        assertEquals("plot_id", plots.get("name_column").asText());
        assertEquals(44, plots.get("row_count").asInt());
        assertTrue("the type is asked, never guessed", plots.get("type").isNull());
        assertEquals(ObjectTargets.FACTOR_LEVEL, plots.get("mapping").get("xp_trt_code").asText());
        assertEquals("", plots.get("mapping").get("plot_n").asText());
    }

    /**
     * A type and a column chosen are kept, and what the type contradicts is said — two columns on a
     * property that holds one value — until it is put right.
     */
    @Test
    public void aSheetsTypeAndColumnsAreChosenAndChecked() throws Exception {
        String id = openSession(WorkbookFixture.file(STAR_FILE)).get("session_id").asText();
        String objectType = Oeso.ScientificObject.getURI();

        JsonNode chosen = post("object-sheets", Map.of("session_id", id, "sheet", PLOTS,
                "rdf_type", objectType, "mapping", Map.of("plot_n", ObjectTargets.COMMENT)));
        assertEquals(chosen.toString(), 200, status);
        assertEquals(objectType, chosen.get("type").asText());
        assertTrue(chosen.get("problems").toString(), chosen.get("problems").toString().contains("singleValued"));

        JsonNode corrected = post("object-sheets", Map.of("session_id", id, "sheet", PLOTS,
                "mapping", Map.of("plot_n", "")));
        assertEquals("the type chosen stays", objectType, corrected.get("type").asText());
        assertTrue(corrected.get("problems").isEmpty());
        assertEquals(objectType, sheetNamed(get("sessions/" + id + "/object-sheets"), PLOTS).get("type").asText());

        post("object-sheets", Map.of("session_id", id, "sheet", PLOTS, "rdf_type", "http://opensilex.test/NotAType"));
        assertEquals(400, status);
        post("object-sheets", Map.of("session_id", id, "sheet", "nowhere"));
        assertEquals(404, status);
        post("object-sheets", Map.of("session_id", id, "sheet", PLOTS, "mapping", Map.of("no_such_column", "")));
        assertEquals(400, status);
        post("object-sheets", Map.of("session_id", id, "sheet", PLOTS, "mapping", Map.of("plot_n", "not a uri")));
        assertEquals(400, status);
    }

    @Test
    public void theDropDownOffersWhatTheTypeAccepts() throws Exception {
        JsonNode properties = get("object-types/properties?type=" + Oeso.ScientificObject.getURI());

        assertEquals(200, status);
        assertTrue(properties.toString(), properties.toString().contains(ObjectTargets.COMMENT));

        get("object-types/properties?type=http://opensilex.test/NotAType");
        assertEquals(400, status);
    }

    /**
     * Checking writes nothing, and says on the workbook's rows what would stop the creation: here,
     * the field the plots stand in is not in the instance.
     */
    @Test
    public void theObjectSheetsAreCheckedWithoutWriting() throws Exception {
        String id = openSession(WorkbookFixture.file(STAR_FILE)).get("session_id").asText();
        post("object-sheets", Map.of("session_id", id, "sheet", PLOTS, "rdf_type", Oeso.ScientificObject.getURI()));

        JsonNode validation = post("object-sheets/validate", Map.of("session_id", id,
                "experiment", nearExperiment.getUri().toString()));

        assertEquals(validation.toString(), 200, status);
        assertEquals("SCIENTIFIC_OBJECTS", validation.get("target").asText());
        assertEquals(44, validation.get("rows_checked").asInt());
        assertTrue(validation.get("error_count").asInt() > 0);

        post("object-sheets/validate", Map.of("session_id", "nowhere", "experiment", nearExperiment.getUri().toString()));
        assertEquals(404, status);
    }

    //#endregion

    //#region helpers

    private JsonNode openSession() throws Exception {
        return openSession(WorkbookFixture.vitisFile());
    }

    private JsonNode openSession(File workbook) throws Exception {
        llm.queue(StubLlmEndpoint.assistantReply("Bonjour"));
        try (MultiPart multiPart = new FormDataMultiPart()
                .bodyPart(new FileDataBodyPart("file", workbook))) {
            Response response = getJsonPostResponseMultipart(target(BASE + "sessions"), multiPart);
            assertEquals(201, response.getStatus());
            return response.readEntity(JsonNode.class).get("result");
        }
    }

    /**
     * Has the model draft a creation, and returns the draft's identifier.
     */
    private String propose(String sessionId, String arguments) throws Exception {
        llm.queue(StubLlmEndpoint.toolCallReply("p1", "propose_creation", arguments))
                .queue(StubLlmEndpoint.assistantReply("Voici le brouillon."));
        JsonNode reply = post("messages", Map.of("session_id", sessionId, "content", "Propose-le."));
        assertEquals(reply.toString(), 200, status);
        JsonNode proposals = get("sessions/" + sessionId).get("proposals");
        assertTrue("a draft was made: " + proposals, proposals.size() > 0);
        return last(proposals).get("id").asText();
    }

    private JsonNode experimentItem(JsonNode report) {
        for (JsonNode item : report.get("experiments")) {
            if (experimentInFile.equals(item.get("source_value").asText())) {
                return item;
            }
        }
        throw new AssertionError("no experiment " + experimentInFile + " in " + report.get("experiments"));
    }

    private JsonNode get(String path) throws Exception {
        String[] parts = path.split("\\?", 2);
        WebTarget target = target(BASE + parts[0]);
        if (parts.length > 1) {
            String[] query = parts[1].split("=", 2);
            target = target.queryParam(query[0], query[1]);
        }
        return read(getJsonGetResponseAsAdmin(target));
    }

    private JsonNode post(String path, Object body) throws Exception {
        return read(getJsonPostResponseAsAdmin(target(BASE + path), body));
    }

    private JsonNode read(Response response) {
        status = response.getStatus();
        JsonNode node = response.readEntity(JsonNode.class);
        return node != null && node.has("result") ? node.get("result") : node;
    }

    private static JsonNode last(JsonNode array) {
        return array.get(array.size() - 1);
    }

    //#endregion
}
