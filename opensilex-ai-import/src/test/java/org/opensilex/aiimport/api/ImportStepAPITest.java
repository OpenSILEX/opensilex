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
import org.opensilex.aiimport.create.steps.ImportStep;
import org.opensilex.aiimport.profile.custom.CustomWorkbooks;
import org.opensilex.aiimport.service.StubLlmEndpoint;
import org.opensilex.aiimport.service.store.AiImportSessionStore;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The order an import is declared in, as a conversation reads it: an experiment before the factors, a
 * factor before its levels, the objects after both — and a creation that comes too early is refused,
 * with the step to do first.
 *
 * @author Arnaud Charleroy
 */
public class ImportStepAPITest extends AbstractMongoIntegrationTest {

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

    @Test
    public void withoutAnExperimentTheObjectsAndTheFactorsWait() throws Exception {
        String id = open("custom").get("session_id").asText();

        JsonNode steps = get(path(id));

        assertEquals(200, status);
        assertEquals("every step, in the order they are declared in",
                Arrays.stream(ImportStep.values()).map(Enum::name).collect(Collectors.toList()), names(steps));
        assertEquals("POSSIBLE", step(steps, "EXPERIMENT").get("status").asText());
        assertTrue("the order a diagram draws", step(steps, "OBJECTS").get("depends_on").toString()
                .contains("EXPERIMENT") && step(steps, "EXPERIMENT").get("depends_on").isEmpty());
        assertEquals("BLOCKED", step(steps, "FACTORS").get("status").asText());
        assertEquals("EXPERIMENT", step(steps, "FACTORS").get("blocked_by").get(0).asText());
        assertEquals("FACTORS", step(steps, "FACTORS").get("target").asText());

        JsonNode requirements = get("sessions/" + id + "/creation-requirements?target=SCIENTIFIC_OBJECTS");
        assertTrue(requirements.toString(), requirements.get("blockers").toString()
                .contains("AiImport.proposal.block.after.EXPERIMENT"));
        assertFalse(requirements.get("is_available").asBoolean());
    }

    @Test
    public void onceTheExperimentIsChosenItsFactorsCanBeDeclared() throws Exception {
        String id = open("custom").get("session_id").asText();

        post("sheet_descriptions", Map.of("session_id", id, "experiment", experiment.getUri().toString(),
                "sheets", List.of()));
        assertEquals(200, status);
        JsonNode steps = get(path(id));

        assertEquals("DONE", step(steps, "EXPERIMENT").get("status").asText());
        assertEquals("POSSIBLE", step(steps, "FACTORS").get("status").asText());
        assertEquals("FACTORS", step(steps, "FACTOR_LEVELS").get("blocked_by").get(0).asText());
        assertEquals("NOT_APPLICABLE", step(steps, "PROJECTS").get("status").asText());
        assertTrue(step(steps, "FACTORS").get("total").asInt() >= 1);
    }

    @Test
    public void anUnknownConversationIsNotFound() throws Exception {
        get(path("nobody"));

        assertEquals(404, status);
    }

    //#region helpers

    private static String path(String sessionId) {
        return "sessions/" + sessionId + "/" + ImportStepAPI.PATH;
    }

    private static JsonNode step(JsonNode steps, String name) {
        for (JsonNode step : steps) {
            if (name.equals(step.get("step").asText())) {
                return step;
            }
        }
        throw new AssertionError("no step " + name + " in " + steps);
    }

    private static List<String> names(JsonNode steps) {
        List<String> names = new ArrayList<>();
        steps.forEach(step -> names.add(step.get("step").asText()));
        return names;
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
