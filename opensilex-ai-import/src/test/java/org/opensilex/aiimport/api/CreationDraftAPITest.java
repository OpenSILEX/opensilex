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
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Creating without the assistant: the draft it would have proposed is made from the files, each factor
 * is planned against the experiment it is declared in, and the facilities a sheet describes come with
 * their type and their properties. Nothing is asked of the language model, and nothing is written.
 *
 * @author Arnaud Charleroy
 */
public class CreationDraftAPITest extends AbstractMongoIntegrationTest {

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
        return List.of(ExperimentModel.class, FactorModel.class, FactorLevelModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(AiImportSessionStore.COLLECTION);
    }

    @Test
    public void aDraftIsMadeFromTheFilesWithoutTheAssistant() throws Exception {
        String id = open("custom").get("session_id").asText();
        choose(id);

        JsonNode proposal = post("drafts", Map.of("session_id", id, "target", "SCIENTIFIC_OBJECTS"));

        assertEquals(proposal.toString(), 200, status);
        assertEquals("SCIENTIFIC_OBJECTS", proposal.get("target").asText());
        assertEquals("the draft was filled from the experiment of the files", "FILE", sourceOf(proposal, "experiment"));
        assertEquals("the pending draft of the conversation", proposal.get("id").asText(),
                get("sessions/" + id).get("pending_proposal").get("id").asText());
        assertEquals("nothing was asked of the language model", 0, llm.getRequestCount());
    }

    @Test
    public void aDraftThatComesTooEarlyCarriesTheStepToDoFirst() throws Exception {
        String id = open("custom").get("session_id").asText();

        JsonNode proposal = post("drafts", Map.of("session_id", id, "target", "FACTORS"));

        assertEquals(200, status);
        assertTrue(proposal.get("blockers").toString().contains("AiImport.proposal.block.after.EXPERIMENT"));
    }

    @Test
    public void whatIsNotACreationIsRefused() throws Exception {
        String id = open("custom").get("session_id").asText();

        post("drafts", Map.of("session_id", id, "target", "NOTHING"));
        assertEquals(400, status);

        post("drafts", Map.of("session_id", "nobody", "target", "DATA"));
        assertEquals(404, status);
    }

    @Test
    public void eachFactorOfTheFilesIsPlannedAgainstTheExperiment() throws Exception {
        String id = open("custom").get("session_id").asText();
        choose(id);

        JsonNode plans = get("sessions/" + id + "/drafts/factors");

        assertEquals(plans.toString(), 200, status);
        JsonNode plan = plans.get(0);
        assertEquals("the header of a level column names its factor", "traitement", plan.get("name").asText());
        assertEquals("NEW", plan.get("status").asText());
        assertEquals(List.of("T1", "T2"), List.of(plan.get("levels").get(0).get("name").asText(),
                plan.get("levels").get(1).get("name").asText()));
        assertFalse(plan.get("levels").get(0).get("exists").asBoolean());
    }

    @Test
    public void aFactorThatLacksLevelsIsPlannedAsAnEditWithAllItsLevels() throws Exception {
        String id = open("custom").get("session_id").asText();
        choose(id);
        FactorModel existing = new FactorModel();
        existing.setName("Traitement");
        existing.setExperiment(experiment);
        existing.setAssociatedExperiments(List.of(experiment));
        FactorLevelModel level = new FactorLevelModel();
        level.setName("T1");
        level.setFactor(existing);
        existing.setFactorLevels(List.of(level));
        new FactorDAO(getSparqlService()).create(existing);

        JsonNode plan = get("sessions/" + id + "/drafts/factors").get(0);

        assertEquals("INCOMPLETE", plan.get("status").asText());
        String uri = existing.getUri().toString();
        assertTrue("the factor on the instance",
                plan.get("uri").asText().endsWith(uri.substring(uri.lastIndexOf('/'))));
        assertTrue("the level it has comes first, with its URI", plan.get("levels").get(0).get("exists").asBoolean());
        assertFalse(plan.get("levels").get(0).get("uri").isNull());
        assertEquals("T2", plan.get("levels").get(1).get("name").asText());
        assertFalse(plan.get("levels").get(1).get("exists").asBoolean());
    }

    @Test
    public void aCompleteFactorNeedsNothing() throws Exception {
        String id = open("custom").get("session_id").asText();
        choose(id);
        FactorModel existing = new FactorModel();
        existing.setName("Traitement");
        existing.setExperiment(experiment);
        existing.setAssociatedExperiments(List.of(experiment));
        List<FactorLevelModel> levels = new ArrayList<>();
        for (String name : List.of("T1", "T2")) {
            FactorLevelModel level = new FactorLevelModel();
            level.setName(name);
            level.setFactor(existing);
            levels.add(level);
        }
        existing.setFactorLevels(levels);
        new FactorDAO(getSparqlService()).create(existing);

        assertEquals("COMPLETE", get("sessions/" + id + "/drafts/factors").get(0).get("status").asText());
    }

    @Test
    public void theFactorFormOfASheetOfLevelsIsFilledWithWhatTheSheetSays() throws Exception {
        File file = WorkbookFiles.write("levels-", CustomWorkbooks.sheet("modalite",
                List.of("xp_trt_code", "xp_trt_name", "xp_trt_desc", "protocole"),
                CustomWorkbooks.row("TNT", "Témoin non traité", "", "Essai cuivre"),
                CustomWorkbooks.row("1", "Cuivre tardif", "cuivre 2 semaines après symptômes", "")));
        String id;
        try {
            id = openFile(file, "custom").get("session_id").asText();
        } finally {
            Files.deleteIfExists(file.toPath());
        }
        choose(id);
        describe(id, Map.of("sheets", List.of(Map.of("sheet", "modalite", "kind", "FACTORS", "factor", "Traitement",
                "roles", Map.of("xp_trt_code", "FACTOR_LEVEL", "xp_trt_name", "FACTOR_LEVEL_NAME",
                        "xp_trt_desc", "FACTOR_LEVEL_DESCRIPTION", "protocole", "FACTOR_DESCRIPTION")))));
        assertEquals(200, status);

        JsonNode plan = get("sessions/" + id + "/drafts/factors").get(0);

        assertEquals("Traitement", plan.get("name").asText());
        assertEquals("NEW", plan.get("status").asText());
        assertEquals("Essai cuivre", plan.get("description").asText());
        assertEquals("TNT", plan.get("levels").get(0).get("name").asText());
        assertEquals("Témoin non traité", plan.get("levels").get(0).get("description").asText());
        assertEquals("Cuivre tardif — cuivre 2 semaines après symptômes",
                plan.get("levels").get(1).get("description").asText());
    }

    @Test
    public void factorsAreNotPlannedWithoutAnExperiment() throws Exception {
        String id = open("custom").get("session_id").asText();

        get("sessions/" + id + "/drafts/factors");
        assertEquals(400, status);

        get("sessions/nobody/drafts/factors");
        assertEquals(404, status);
    }

    @Test
    public void aSheetOfFacilitiesGivesTheFormItsTypeAndItsProperties() throws Exception {
        File file = WorkbookFiles.write("facilities-", CustomWorkbooks.sheet("sites",
                List.of("nom", "altitude"), CustomWorkbooks.row("Parcelle Nord", "120"),
                CustomWorkbooks.row("Parcelle Sud", ""), CustomWorkbooks.row("", "10")));
        String id;
        try {
            id = openFile(file, "custom").get("session_id").asText();
        } finally {
            Files.deleteIfExists(file.toPath());
        }
        String property = "http://example.org/vocabulary#altitude";
        describe(id, Map.of("sheets", List.of(Map.of("sheet", "sites", "kind", "FACILITIES",
                "roles", Map.of("nom", "LOCATION", "altitude", "OBJECT_PROPERTY"),
                "properties", Map.of("altitude", property)))));

        JsonNode drafts = get("sessions/" + id + "/drafts/facilities");

        assertEquals(drafts.toString(), 200, status);
        assertEquals("the row without a name is not a facility", 2, drafts.size());
        assertEquals("Parcelle Nord", drafts.get(0).get("name").asText());
        assertEquals("120", drafts.get(0).get("relations").get(0).get("value").asText());
        assertEquals(property, drafts.get(0).get("relations").get(0).get("property").asText());
        assertTrue("an empty cell gives no property", drafts.get(1).get("relations").isEmpty());
    }

    @Test
    public void aFileWithoutFacilitiesGivesNoDraft() throws Exception {
        String id = open("custom").get("session_id").asText();

        assertTrue(get("sessions/" + id + "/drafts/facilities").isEmpty());
    }

    private static String sourceOf(JsonNode proposal, String field) {
        for (JsonNode candidate : proposal.get("fields")) {
            if (field.equals(candidate.get("name").asText())) {
                return candidate.get("source").asText();
            }
        }
        throw new AssertionError("no field " + field + " in " + proposal.get("fields"));
    }

    /**
     * Says which experiment the files belong to, as the user does in the description.
     */
    private void choose(String sessionId) throws Exception {
        post("sheet_descriptions", Map.of("session_id", sessionId, "experiment", experiment.getUri().toString(),
                "sheets", List.of()));
        assertEquals(200, status);
    }

    private JsonNode describe(String sessionId, Map<String, ?> body) throws Exception {
        Map<String, Object> full = new LinkedHashMap<>(body);
        full.put("session_id", sessionId);
        return post("sheet_descriptions", full);
    }

    //#region helpers

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
