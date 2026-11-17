//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.apache.jena.vocabulary.RDFS;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.graphimport.api.dto.ImportExperimentDTO;
import org.opensilex.graphimport.api.dto.SheetDecisionsDTO;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The preview of an import through the API, on the platform's own ontology: figures, a sheet a page at
 * a time, one resource, the Turtle export.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportPreviewAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String PREVIEW_PATH = SESSION_PATH + "/{sessionId}/preview";
    private static final String SHEET = "parcelles";
    private static final String PLOTS = "nom;Date de création;parent\n"
            + "P1;2026-04-03;\n"
            + "P2;2026-05-04;P1\n"
            + "P3;2026-06-05;P404\n";
    private static final URI SO = URI.create(Oeso.ScientificObject.getURI());
    private static final URI CREATION_DATE = URI.create(Oeso.NS + "hasCreationDate");
    private static final URI IS_PART_OF = URI.create(Oeso.NS + "isPartOf");

    private static final ServiceDescription CREATE;
    private static final ServiceDescription DECIDE;
    private static final ServiceDescription PREVIEW;
    private static final ServiceDescription ROWS;
    private static final ServiceDescription NODE;
    private static final ServiceDescription TURTLE;
    private static final ServiceDescription CHOOSE_EXPERIMENT;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            DECIDE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("decideSheet",
                    SheetDecisionsDTO.class), GraphImportAPI.PATH + "/sheet_decisions");
            PREVIEW = new ServiceDescription(GraphImportPreviewAPI.class.getMethod("preview", String.class,
                    boolean.class), PREVIEW_PATH);
            ROWS = new ServiceDescription(GraphImportPreviewAPI.class.getMethod("previewRows", String.class,
                    String.class, String.class, boolean.class, int.class, int.class), PREVIEW_PATH + "/sheets/{sheet}/rows");
            NODE = new ServiceDescription(GraphImportPreviewAPI.class.getMethod("previewNode", String.class, int.class),
                    PREVIEW_PATH + "/nodes/{node}");
            TURTLE = new ServiceDescription(GraphImportPreviewAPI.class.getMethod("exportTurtle", String.class),
                    PREVIEW_PATH + "/turtle");
            CHOOSE_EXPERIMENT = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("chooseExperiment",
                    ImportExperimentDTO.class), GraphImportAPI.PATH + "/import_experiments");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private int status;

    /** The experiment graph a test filled, cleared after it. */
    private URI experimentGraph;

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(GraphImportAccess.COLLECTION, LearnedAliasStore.COLLECTION);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, ScientificObjectModel.class);
    }

    @After
    public void dropExperimentGraph() throws Exception {
        if (experimentGraph != null) {
            getSparqlService().clearGraph(experimentGraph);
        }
    }

    @Test
    public void anObjectOfTheChosenExperimentIsCompletedWhateverTheCaseOfItsName() throws Exception {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName("Essai aperçu");
        experiment.setObjective("Preview");
        experiment.setStartDate(LocalDate.parse("2026-04-01"));
        experiment.setIsPublic(true);
        getSparqlService().create(ExperimentModel.class, List.of(experiment));
        experimentGraph = experiment.getUri();
        ScientificObjectModel plot = new ScientificObjectModel();
        plot.setName("Placette 1");
        plot.setType(SO);
        getSparqlService().create(SPARQLDeserializers.nodeURI(experimentGraph), plot);

        String id = open("nom;Date de création\nplacette 1;2026-04-03\nPlacette 9;2026-04-04\n");
        decide(id, Map.of(0, URI.create(RDFS.label.getURI()), 1, CREATION_DATE));
        ImportExperimentDTO chosen = new ImportExperimentDTO();
        chosen.setSessionId(id);
        chosen.setExperiment(experimentGraph);
        call(new UserCallBuilder(CHOOSE_EXPERIMENT).setBody(chosen));
        assertEquals(200, status);

        JsonNode preview = call(new UserCallBuilder(PREVIEW).addPathTemplateParam("sessionId", id)
                .addParam("afresh", true));
        assertEquals(1, preview.get("counts").get("completed").asInt());
        assertEquals(1, preview.get("counts").get("created").asInt());

        JsonNode node = call(new UserCallBuilder(NODE).addPathTemplateParam("sessionId", id)
                .addPathTemplateParam("node", 0));
        assertEquals("COMPLETED", node.get("state").asText());
        assertEquals(plot.getUri().toString(), URI.create(node.get("existing").asText()).toString());
        for (JsonNode value : node.get("values")) {
            String expected = value.get("property").asText().equals(RDFS.label.getURI()) ? "PRESENT" : "NEW";
            assertEquals(value.toString(), expected, value.get("state").asText());
        }
    }

    @Test
    public void thePreviewGivesFiguresPagesAndResources() throws Exception {
        String id = open();
        decide(id);

        JsonNode preview = call(new UserCallBuilder(PREVIEW).addPathTemplateParam("sessionId", id)
                .addParam("afresh", true));
        assertEquals(200, status);
        assertEquals("no experiment chosen: no scientific object can exist", 3,
                preview.get("counts").get("created").asInt());
        assertEquals(1, preview.get("counts").get("to_fix").asInt());
        assertEquals(SHEET, preview.get("sheets").get(0).get("name").asText());
        assertEquals("ROW", preview.get("links").get(0).get("kind").asText());

        JsonNode page = call(rows(id).addParam("page_size", 2));
        assertEquals(200, status);
        assertEquals(3, page.get("total_count").asInt());
        assertEquals(2, page.get("rows").size());
        assertEquals("IDENTIFIER", page.get("columns").get(0).get("role").asText());

        JsonNode toFix = call(rows(id).addParam("to_fix", true));
        assertEquals(1, toFix.get("total_count").asInt());
        JsonNode p3 = toFix.get("rows").get(0);
        assertEquals("P3", p3.get("node_name").asText());

        JsonNode node = call(new UserCallBuilder(NODE).addPathTemplateParam("sessionId", id)
                .addPathTemplateParam("node", p3.get("node").asInt()));
        assertEquals(200, status);
        assertEquals("CREATED", node.get("state").asText());
        List<String> states = new ArrayList<>();
        node.get("values").forEach(value -> states.add(value.get("state").asText()));
        assertTrue(states.toString(), states.contains("UNUSABLE"));
        assertTrue(node.get("to_fix").asBoolean());
    }

    @Test
    public void theTurtleExportIsAFile() throws Exception {
        String id = open();
        decide(id);

        try (Response response = new UserCallBuilder(TURTLE).addPathTemplateParam("sessionId", id)
                .setResponseMediaTypes(List.of(MediaType.valueOf(GraphImportPreviewAPI.TURTLE_MEDIA_TYPE)))
                .buildAdmin().executeCall()) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getHeaderString("Content-Disposition").contains("parcelles-preview.ttl"));
            String turtle = response.readEntity(String.class);
            assertTrue(turtle, turtle.contains("urn:graph-import:resource:p1"));
        }
    }

    @Test
    public void anUnknownSheetStateOrResourceIsRefused() throws Exception {
        String id = open();
        decide(id);

        call(new UserCallBuilder(ROWS).addPathTemplateParam("sessionId", id).addPathTemplateParam("sheet", "absent"));
        assertEquals(404, status);
        call(rows(id).addParam("state", "DONE"));
        assertEquals(400, status);
        call(rows(id).addParam("page_size", 500));
        assertEquals("a page holds at most 100 rows", 400, status);
        call(new UserCallBuilder(NODE).addPathTemplateParam("sessionId", id).addPathTemplateParam("node", 99));
        assertEquals(404, status);
        call(new UserCallBuilder(PREVIEW).addPathTemplateParam("sessionId", "nothing"));
        assertEquals(404, status);
    }

    @Test
    public void aFileNameIsMadeSafeForAHeader() {
        assertEquals("plan_d_essai-preview.ttl", GraphImportPreviewAPI.fileName("plan d'essai.xlsx"));
        assertEquals("import-preview.ttl", GraphImportPreviewAPI.fileName(""));
        assertEquals("a__b_-preview.ttl", GraphImportPreviewAPI.fileName("a\"\nb\r.csv"));
    }

    private UserCallBuilder rows(String id) {
        return new UserCallBuilder(ROWS).addPathTemplateParam("sessionId", id).addPathTemplateParam("sheet", SHEET);
    }

    private String open() throws Exception {
        return open(PLOTS);
    }

    private String open(String content) throws Exception {
        File file = new File(folder.newFolder(), SHEET + ".csv");
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart)).get("session_id").asText();
        }
    }

    private void decide(String id) throws Exception {
        decide(id, Map.of(1, CREATION_DATE, 2, IS_PART_OF));
    }

    private void decide(String id, Map<Integer, URI> properties) throws Exception {
        SheetDecisionsDTO dto = new SheetDecisionsDTO();
        dto.setSessionId(id);
        dto.setSheet(SHEET);
        dto.setClassUri(SO);
        dto.setIdentifierColumn(0);
        List<SheetDecisionsDTO.ColumnDecisionDTO> columns = new ArrayList<>();
        for (Map.Entry<Integer, URI> column : properties.entrySet()) {
            SheetDecisionsDTO.ColumnDecisionDTO decision = new SheetDecisionsDTO.ColumnDecisionDTO();
            decision.setColumn(column.getKey());
            decision.setProperty(column.getValue());
            columns.add(decision);
        }
        dto.setColumns(columns);
        call(new UserCallBuilder(DECIDE).setBody(dto));
        assertEquals(200, status);
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }
}
