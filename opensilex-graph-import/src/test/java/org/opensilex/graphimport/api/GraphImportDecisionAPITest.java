//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.api.dto.ImportExperimentDTO;
import org.opensilex.graphimport.api.dto.SheetDecisionsDTO;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The user's decisions about an import: kept, applied to the interpretation, remembered for the next
 * imports, refused when the sheet cannot take them.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportDecisionAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String SHEET = "scientific_objects";
    private static final String OBJECTS = "nom;remarque libre;parent\nP1;a;\nP2;b;P1\n";
    private static final URI SO = URI.create(Oeso.ScientificObject.getURI());
    private static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");

    private static final ServiceDescription CREATE;
    private static final ServiceDescription INTERPRET;
    private static final ServiceDescription DECIDE;
    private static final ServiceDescription FORGET;
    private static final ServiceDescription EXPERIMENT;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            INTERPRET = new ServiceDescription(GraphImportInterpretationAPI.class.getMethod("getInterpretation",
                    String.class), SESSION_PATH + "/{sessionId}/interpretation");
            DECIDE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("decideSheet",
                    SheetDecisionsDTO.class), GraphImportAPI.PATH + "/sheet_decisions");
            FORGET = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("forgetSheetDecisions",
                    String.class, String.class), SESSION_PATH + "/{sessionId}/sheets/{sheet}/decisions");
            EXPERIMENT = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("chooseExperiment",
                    ImportExperimentDTO.class), GraphImportAPI.PATH + "/import_experiments");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private int status;

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(GraphImportAccess.COLLECTION, LearnedAliasStore.COLLECTION);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class);
    }

    @Test
    public void aDecisionIsAppliedKeptAndLearned() throws Exception {
        String id = open();

        JsonNode interpretation = decide(id, SO, 0, 1, COMMENT);

        assertEquals(200, status);
        JsonNode sheet = interpretation.get("sheets").get(0);
        assertEquals("CHOSEN", sheet.get("sheet_class").get("status").asText());
        assertEquals("CHOSEN", sheet.get("identifier").get("status").asText());
        JsonNode column = sheet.get("columns").get(1).get("property");
        assertEquals("CHOSEN", column.get("status").asText());
        assertEquals(COMMENT.toString(), column.get("chosen_uri").asText());
        assertFalse(sheet.get("properties").isEmpty());
        assertFalse(interpretation.get("classes").isEmpty());

        new GraphImportSessionCache(null).remove(id);
        JsonNode resumed = interpret(id).get("sheets").get(0);
        assertEquals("CHOSEN", resumed.get("columns").get(1).get("property").get("status").asText());

        Document learned = getMongoDBService().getDatabase().getCollection(LearnedAliasStore.COLLECTION)
                .find(Filters.eq("header", "remarquelibre")).first();
        assertEquals(COMMENT.toString(), learned.getString("property"));
        assertEquals(SO.toString(), learned.getString("class"));

        decide(id, SO, 0, 1, COMMENT);
        Document again = getMongoDBService().getDatabase().getCollection(LearnedAliasStore.COLLECTION)
                .find(Filters.eq("header", "remarquelibre")).first();
        assertEquals("the same decision sent again teaches nothing more", 1, again.getInteger("count").intValue());
    }

    @Test
    public void prefixedUrisAreUnderstoodAsTheirFullForm() throws Exception {
        String id = open();

        JsonNode sheet = decide(id, URI.create("vocabulary:ScientificObject"), 0, 1, URI.create("rdfs:comment"))
                .get("sheets").get(0);

        assertEquals(200, status);
        assertEquals(SO.toString(), sheet.get("sheet_class").get("chosen_uri").asText());
        assertEquals("CHOSEN", sheet.get("columns").get(1).get("property").get("status").asText());
        assertEquals(COMMENT.toString(), sheet.get("columns").get(1).get("property").get("chosen_uri").asText());
    }

    @Test
    public void aChoiceIsSuggestedInTheNextImportWithTheSameHeader() throws Exception {
        decide(open(), SO, 0, 1, COMMENT);

        JsonNode column = interpret(open()).get("sheets").get(0).get("columns").get(1).get("property");

        assertEquals("SUGGESTED", column.get("status").asText());
        assertEquals(COMMENT.toString(), column.get("chosen_uri").asText());
        List<String> sources = new ArrayList<>();
        column.get("candidates").get(0).get("evidence").forEach(evidence -> sources.add(evidence.get("source").asText()));
        assertTrue(sources.toString(), sources.contains("alias"));
    }

    @Test
    public void aDeletedExperimentIsDroppedRatherThanBlockingTheImport() throws Exception {
        String id = open();
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName("Essai éphémère");
        experiment.setObjective("Objective");
        experiment.setStartDate(LocalDate.parse("2026-04-01"));
        getSparqlService().create(ExperimentModel.class, List.of(experiment));
        chooseExperiment(id, experiment.getUri());

        getSparqlService().delete(ExperimentModel.class, experiment.getUri());
        JsonNode interpretation = interpret(id);

        assertEquals(200, status);
        assertFalse(interpretation.has("experiment"));
    }

    @Test
    public void aForgottenDecisionGoesBackToTheSuggestions() throws Exception {
        String id = open();
        decide(id, SO, 0, 1, COMMENT);

        JsonNode interpretation = call(new UserCallBuilder(FORGET).addPathTemplateParam("sessionId", id)
                .addPathTemplateParam("sheet", SHEET));

        assertEquals(200, status);
        assertFalse("CHOSEN".equals(interpretation.get("sheets").get(0).get("sheet_class").get("status").asText()));
    }

    @Test
    public void aColumnLeftOutStaysOutUntilADecisionNoLongerNamesIt() throws Exception {
        String id = open();

        JsonNode left = leaveOut(id, 1);
        assertEquals(200, status);
        assertEquals("LEFT_OUT", left.get("sheets").get(0).get("columns").get(1).get("property").get("status").asText());

        new GraphImportSessionCache(null).remove(id);
        assertEquals("kept when the import is resumed", "LEFT_OUT",
                interpret(id).get("sheets").get(0).get("columns").get(1).get("property").get("status").asText());

        JsonNode back = decide(id, SO, 0, null, null);
        assertFalse("LEFT_OUT".equals(back.get("sheets").get(0).get("columns").get(1).get("property").get("status").asText()));
    }

    @Test
    public void aLinkPropertySaysWhichSheetsItMayPointTo() throws Exception {
        String id = open();

        JsonNode interpretation = decide(id, SO, 0, null, null);

        JsonNode isPartOf = null;
        for (JsonNode property : interpretation.get("sheets").get(0).get("properties")) {
            if (property.get("uri").asText().endsWith("isPartOf")) {
                isPartOf = property;
            }
            if ("DATA".equals(property.get("kind").asText())) {
                assertFalse(property.has("fitting_sheets"));
            }
        }
        assertEquals("[\"" + SHEET + "\"]", isPartOf.get("fitting_sheets").toString());
    }

    @Test
    public void aColumnCannotBeLeftOutAndGivenAPropertyAtOnce() throws Exception {
        String id = open();
        SheetDecisionsDTO dto = decision(id, SO, 0, 1, COMMENT);
        dto.setLeftOut(List.of(1));

        call(new UserCallBuilder(DECIDE).setBody(dto));
        assertEquals(400, status);
        leaveOut(id, 9);
        assertEquals(400, status);
    }

    @Test
    public void aDecisionTheSheetCannotTakeIsRefusedWhole() throws Exception {
        String id = open();

        decide(id, SO, 0, 1, URI.create("http://test.opensilex.org/nothing"));
        assertEquals(400, status);
        decide(id, URI.create("http://test.opensilex.org/Nothing"), 0, null, null);
        assertEquals(400, status);
        decide(id, SO, 7, null, null);
        assertEquals(400, status);
        decide(id, null, null, 1, COMMENT);
        assertEquals(400, status);

        SheetDecisionsDTO elsewhere = new SheetDecisionsDTO();
        elsewhere.setSessionId(id);
        elsewhere.setSheet("nowhere");
        call(new UserCallBuilder(DECIDE).setBody(elsewhere));
        assertEquals(404, status);
        assertFalse("CHOSEN".equals(interpret(id).get("sheets").get(0).get("sheet_class").get("status").asText()));
    }

    @Test
    public void anExperimentIsChosenOnlyWhenItExists() throws Exception {
        String id = open();

        chooseExperiment(id, URI.create("http://test.opensilex.org/id/experiment/none"));
        assertEquals(404, status);

        ExperimentModel experiment = new ExperimentModel();
        experiment.setName("Essai décisions");
        experiment.setObjective("Objective");
        experiment.setStartDate(LocalDate.parse("2026-04-01"));
        getSparqlService().create(ExperimentModel.class, List.of(experiment));

        JsonNode interpretation = chooseExperiment(id, experiment.getUri());
        assertEquals(200, status);
        assertTrue(interpretation.has("experiment"));
        assertTrue(interpretation.get("notes").isEmpty());

        new GraphImportSessionCache(null).remove(id);
        assertTrue(interpret(id).has("experiment"));
    }

    private String open() throws Exception {
        File file = new File(folder.newFolder(), SHEET + ".csv");
        Files.write(file.toPath(), OBJECTS.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart)).get("session_id").asText();
        }
    }

    private JsonNode decide(String id, URI classUri, Integer identifier, Integer column, URI property) throws Exception {
        return call(new UserCallBuilder(DECIDE).setBody(decision(id, classUri, identifier, column, property)));
    }

    private JsonNode leaveOut(String id, int column) throws Exception {
        SheetDecisionsDTO dto = decision(id, SO, 0, null, null);
        dto.setLeftOut(List.of(column));
        return call(new UserCallBuilder(DECIDE).setBody(dto));
    }

    private static SheetDecisionsDTO decision(String id, URI classUri, Integer identifier, Integer column,
                                              URI property) {
        SheetDecisionsDTO dto = new SheetDecisionsDTO();
        dto.setSessionId(id);
        dto.setSheet(SHEET);
        dto.setClassUri(classUri);
        dto.setIdentifierColumn(identifier);
        if (column != null) {
            SheetDecisionsDTO.ColumnDecisionDTO decision = new SheetDecisionsDTO.ColumnDecisionDTO();
            decision.setColumn(column);
            decision.setProperty(property);
            dto.setColumns(List.of(decision));
        }
        return dto;
    }

    private JsonNode chooseExperiment(String id, URI experiment) throws Exception {
        ImportExperimentDTO dto = new ImportExperimentDTO();
        dto.setSessionId(id);
        dto.setExperiment(experiment);
        return call(new UserCallBuilder(EXPERIMENT).setBody(dto));
    }

    private JsonNode interpret(String id) throws Exception {
        return call(new UserCallBuilder(INTERPRET).addPathTemplateParam("sessionId", id));
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }
}
