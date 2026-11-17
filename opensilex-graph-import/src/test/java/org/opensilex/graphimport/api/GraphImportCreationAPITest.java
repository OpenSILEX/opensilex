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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.graphimport.api.dto.ColumnReadingDTO;
import org.opensilex.graphimport.api.dto.CreationRequestDTO;
import org.opensilex.graphimport.api.dto.DeclaredResourceDTO;
import org.opensilex.graphimport.api.dto.PropertyValueDTO;
import org.opensilex.graphimport.api.dto.ResourceDeclarationDTO;
import org.opensilex.graphimport.api.dto.SheetDecisionsDTO;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.mail.internet.InternetAddress;
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
 * The creation step on an import whose experiment and factor are declared in the assistant and whose
 * levels come from a file: the experiment first, then the factor with its levels, each the way the
 * platform's own API would write it, and nothing twice.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportCreationAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String SHEET = "modalites";
    private static final String LEVELS = "nom;description\nT1;Témoin\nT2;Irrigué\n";
    private static final String EXPERIMENT_NAME = "Essai créé par l'assistant";
    private static final URI LABEL = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
    private static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");
    private static int accounts;

    private static final ServiceDescription CREATE;
    private static final ServiceDescription DECIDE;
    private static final ServiceDescription DECLARE;
    private static final ServiceDescription PLAN;
    private static final ServiceDescription CHECK;
    private static final ServiceDescription RUN;
    private static final ServiceDescription ADD_FILE;
    private static final ServiceDescription READ_COLUMN;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            DECIDE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("decideSheet",
                    SheetDecisionsDTO.class), GraphImportAPI.PATH + "/sheet_decisions");
            DECLARE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("declareResource",
                    ResourceDeclarationDTO.class), GraphImportAPI.PATH + "/declared_resources");
            PLAN = new ServiceDescription(GraphImportCreationAPI.class.getMethod("getCreationPlan", String.class,
                    boolean.class), SESSION_PATH + "/{sessionId}/creation");
            CHECK = new ServiceDescription(GraphImportCreationAPI.class.getMethod("checkCreation",
                    CreationRequestDTO.class), GraphImportAPI.PATH + "/creation_checks");
            RUN = new ServiceDescription(GraphImportCreationAPI.class.getMethod("createStep", CreationRequestDTO.class),
                    GraphImportAPI.PATH + "/creations");
            READ_COLUMN = new ServiceDescription(GraphImportValidationAPI.class.getMethod("readColumn",
                    ColumnReadingDTO.class), GraphImportAPI.PATH + "/column_readings");
            ADD_FILE = new ServiceDescription(GraphImportFileAPI.class.getMethod("addFile", String.class, File.class,
                    FormDataContentDisposition.class), SESSION_PATH + "/{sessionId}/" + GraphImportFileAPI.PATH);
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
        return List.of(ExperimentModel.class, FactorModel.class, FactorLevelModel.class, ScientificObjectModel.class,
                EventModel.class);
    }

    @Test
    public void theFactorWaitsForItsExperiment() throws Exception {
        String id = prepared(null);

        JsonNode plan = call(new UserCallBuilder(PLAN).addPathTemplateParam("sessionId", id), null);

        assertEquals("READY", step(plan, "experiments").get("status").asText());
        assertEquals(1, step(plan, "experiments").get("ready").size());
        assertEquals("BLOCKED", step(plan, "factors").get("status").asText());
        assertEquals(3, step(plan, "factors").get("blocked").size());
    }

    @Test
    public void aFactorWaitsForItsLevels() throws Exception {
        String id = prepared(null);
        call(new UserCallBuilder(RUN).setBody(request(id, "experiments", 1)), null);
        SheetDecisionsDTO nameless = new SheetDecisionsDTO();
        nameless.setSessionId(id);
        nameless.setSheet(SHEET);
        nameless.setClassUri(URI.create(Oeso.FactorLevel.getURI()));
        nameless.setColumns(List.of(column(1, COMMENT)));
        nameless.setLeftOut(List.of(0));
        nameless.getGiven().add(new PropertyValueDTO(URI.create(Oeso.hasFactor.getURI()), "Irrigation"));
        call(new UserCallBuilder(DECIDE).setBody(nameless), null);

        JsonNode plan = call(new UserCallBuilder(PLAN).addPathTemplateParam("sessionId", id), null);

        assertEquals("the levels have no name: the factor waits for them", 0,
                step(plan, "factors").get("ready").size());
        assertEquals(3, step(plan, "factors").get("blocked").size());
    }

    @Test
    public void aCheckWritesNothing() throws Exception {
        String id = prepared(null);

        JsonNode outcome = call(new UserCallBuilder(CHECK).setBody(request(id, "experiments", 1)), null);

        assertEquals(200, status);
        assertEquals(0, outcome.get("problems").size());
        assertEquals(0, outcome.get("created").size());
        assertTrue(experimentsNamed(EXPERIMENT_NAME).isEmpty());
    }

    @Test
    public void theExperimentThenTheFactorAreCreatedAsThePlatformWouldOnce() throws Exception {
        String id = prepared(null);

        JsonNode experiment = call(new UserCallBuilder(RUN).setBody(request(id, "experiments", 1)), null);
        assertEquals(200, status);
        assertEquals(1, experiment.get("created").size());
        assertEquals("DONE", step(experiment.get("plan"), "experiments").get("status").asText());
        assertEquals(3, step(experiment.get("plan"), "factors").get("ready").size());

        JsonNode factors = call(new UserCallBuilder(RUN).setBody(request(id, "factors", 3)), null);
        assertEquals(200, status);
        assertEquals(factors.toString(), 3, factors.get("created").size());
        assertEquals("DONE", step(factors.get("plan"), "factors").get("status").asText());

        ExperimentModel created = experimentsNamed(EXPERIMENT_NAME).get(0);
        assertEquals("Tester l'assistant", created.getObjective());
        assertEquals(LocalDate.parse("2026-04-01"), created.getStartDate());
        List<FactorModel> studied = new FactorDAO(getSparqlService()).getByExperiment(created.getUri(), null);
        assertEquals(1, studied.size());
        assertEquals("Irrigation", studied.get(0).getName());
        assertEquals(List.of("T1", "T2"), studied.get(0).getFactorLevels().stream().map(FactorLevelModel::getName)
                .sorted().toList());

        JsonNode again = call(new UserCallBuilder(RUN).setBody(request(id, "factors", 0)), null);
        assertEquals(200, status);
        assertEquals("nothing is created twice", 0, again.get("created").size());
        assertEquals(1, new FactorDAO(getSparqlService()).getByExperiment(created.getUri(), null).size());
        assertEquals(2, again.get("plan").get("history").size());
    }

    @Test
    public void scientificObjectsAreCreatedInTheExperimentParentsFirstWithTheirLevels() throws Exception {
        String id = prepared(null);
        addPlots(id);
        JsonNode before = call(new UserCallBuilder(PLAN).addPathTemplateParam("sessionId", id), null);
        assertEquals("objects wait for the experiment", "BLOCKED", step(before, "scientific-objects").get("status").asText());

        call(new UserCallBuilder(RUN).setBody(request(id, "experiments", 1)), null);
        call(new UserCallBuilder(RUN).setBody(request(id, "factors", 3)), null);
        JsonNode objects = call(new UserCallBuilder(RUN).setBody(request(id, "scientific-objects", 3)), null);

        assertEquals(objects.toString(), 3, objects.get("created").size());
        assertEquals("DONE", step(objects.get("plan"), "scientific-objects").get("status").asText());
        URI experiment = experimentsNamed(EXPERIMENT_NAME).get(0).getUri();
        ScientificObjectModel plot = objectNamed(experiment, "P1");
        assertEquals(objectNamed(experiment, "B1").getUri(), plot.getParent().getUri());
        assertEquals(1, plot.getFactorLevels().size());

        JsonNode again = call(new UserCallBuilder(RUN).setBody(request(id, "scientific-objects", 0)), null);
        assertEquals("nothing is created twice", 0, again.get("created").size());
    }

    @Test
    public void eventsAreCreatedOnceTheirObjectsExistAndNeverTwice() throws Exception {
        String id = prepared(null);
        addPlots(id);
        addEvents(id);
        JsonNode before = call(new UserCallBuilder(PLAN).addPathTemplateParam("sessionId", id), null);
        assertEquals("events wait for their objects", "BLOCKED", step(before, "events").get("status").asText());

        call(new UserCallBuilder(RUN).setBody(request(id, "experiments", 1)), null);
        call(new UserCallBuilder(RUN).setBody(request(id, "factors", 3)), null);
        call(new UserCallBuilder(RUN).setBody(request(id, "scientific-objects", 3)), null);
        JsonNode events = call(new UserCallBuilder(RUN).setBody(request(id, "events", 2)), null);

        assertEquals(events.toString(), 2, events.get("created").size());
        JsonNode done = step(events.get("plan"), "events");
        assertEquals("an event has no name: the writer finds it by its type, objects and date", "DONE",
                done.get("status").asText());
        assertEquals(2, done.get("done").asInt());
        JsonNode again = call(new UserCallBuilder(RUN).setBody(request(id, "events", 0)), null);
        assertEquals(0, again.get("created").size());
    }

    @Test
    public void aStepThatChangedSinceTheUserSawItIsNotRun() throws Exception {
        String id = prepared(null);

        call(new UserCallBuilder(RUN).setBody(request(id, "experiments", 2)), null);

        assertEquals(409, status);
        assertTrue(experimentsNamed(EXPERIMENT_NAME).isEmpty());
    }

    @Test
    public void aUserWithoutTheCredentialCannotCreate() throws Exception {
        accounts++;
        String email = "gi-creation" + accounts + "@test.test";
        new AccountDAO(getSparqlService()).create(null, new InternetAddress(email), false,
                getAuthenticationService().getPasswordHash("pwd"), "en");
        String id = prepared(email);

        call(new UserCallBuilder(RUN).setBody(request(id, "experiments", 1)), email);

        assertEquals(403, status);
        assertTrue(experimentsNamed(EXPERIMENT_NAME).isEmpty());
    }

    /**
     * The levels file, its factor given for all rows, the factor declared in the experiment declared too.
     *
     * @param email the user preparing the import; the administrator when null
     */
    private String prepared(String email) throws Exception {
        String id = open(email);
        SheetDecisionsDTO levels = new SheetDecisionsDTO();
        levels.setSessionId(id);
        levels.setSheet(SHEET);
        levels.setClassUri(URI.create(Oeso.FactorLevel.getURI()));
        levels.setIdentifierColumn(0);
        levels.setColumns(List.of(column(0, LABEL), column(1, COMMENT)));
        levels.getGiven().add(new PropertyValueDTO(URI.create(Oeso.hasFactor.getURI()), "Irrigation"));
        call(new UserCallBuilder(DECIDE).setBody(levels), email);
        assertEquals(200, status);

        call(new UserCallBuilder(DECLARE).setBody(declaration(id, Oeso.Experiment.getURI(), List.of(
                new PropertyValueDTO(LABEL, EXPERIMENT_NAME),
                new PropertyValueDTO(URI.create(Oeso.hasObjective.getURI()), "Tester l'assistant"),
                new PropertyValueDTO(URI.create(Oeso.startDate.getURI()), "2026-04-01")))), email);
        assertEquals(200, status);
        call(new UserCallBuilder(DECLARE).setBody(declaration(id, Oeso.Factor.getURI(), List.of(
                new PropertyValueDTO(LABEL, "Irrigation"),
                new PropertyValueDTO(URI.create(Oeso.studiedEffectIn.getURI()), EXPERIMENT_NAME)))), email);
        assertEquals(200, status);
        return id;
    }

    /** A sheet of plots: a block, and two plots in it, each in a level of the factor. */
    private void addPlots(String id) throws Exception {
        File file = new File(folder.newFolder(), "placettes.csv");
        Files.write(file.toPath(), "nom;parent;modalite\nB1;;\nP1;B1;T1\nP2;B1;T2\n".getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            call(new UserCallBuilder(ADD_FILE).setMultipartBody(multiPart).addPathTemplateParam("sessionId", id), null);
        }
        SheetDecisionsDTO plots = new SheetDecisionsDTO();
        plots.setSessionId(id);
        plots.setSheet("placettes");
        plots.setClassUri(URI.create(Oeso.ScientificObject.getURI()));
        plots.setIdentifierColumn(0);
        plots.setColumns(List.of(column(0, LABEL), column(1, URI.create(Oeso.isPartOf.getURI())),
                column(2, URI.create(Oeso.hasFactorLevel.getURI()))));
        JsonNode decided = call(new UserCallBuilder(DECIDE).setBody(plots), null);
        assertEquals(String.valueOf(decided), 200, status);
    }

    /** A sheet of events: a date alone, the plot it concerns, a remark; its dates in a zone. */
    private void addEvents(String id) throws Exception {
        File file = new File(folder.newFolder(), "evenements.csv");
        Files.write(file.toPath(), "date;cible;remarque\n2026-05-03;P1;taille\n2026-05-04;P2;récolte\n"
                .getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            call(new UserCallBuilder(ADD_FILE).setMultipartBody(multiPart).addPathTemplateParam("sessionId", id), null);
        }
        SheetDecisionsDTO events = new SheetDecisionsDTO();
        events.setSessionId(id);
        events.setSheet("evenements");
        events.setClassUri(URI.create("http://www.opensilex.org/vocabulary/oeev#Event"));
        events.setColumns(List.of(column(0, URI.create("http://www.w3.org/2006/time#hasBeginning")),
                column(1, URI.create("http://www.opensilex.org/vocabulary/oeev#concerns")), column(2, COMMENT)));
        JsonNode decided = call(new UserCallBuilder(DECIDE).setBody(events), null);
        assertEquals(String.valueOf(decided), 200, status);
        ColumnReadingDTO zone = new ColumnReadingDTO();
        zone.setSessionId(id);
        zone.setSheet("evenements");
        zone.setColumn(0);
        zone.setZone("+02:00");
        JsonNode read = call(new UserCallBuilder(READ_COLUMN).setBody(zone), null);
        assertEquals(String.valueOf(read), 200, status);
    }

    private ScientificObjectModel objectNamed(URI experiment, String name) throws Exception {
        return getSparqlService().search(SPARQLDeserializers.nodeURI(experiment), ScientificObjectModel.class, null, null)
                .stream().filter(object -> name.equals(object.getName())).findFirst().orElseThrow();
    }

    private String open(String email) throws Exception {
        File file = new File(folder.newFolder(), SHEET + ".csv");
        Files.write(file.toPath(), LEVELS.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart), email).get("session_id").asText();
        }
    }

    private static SheetDecisionsDTO.ColumnDecisionDTO column(int index, URI property) {
        SheetDecisionsDTO.ColumnDecisionDTO column = new SheetDecisionsDTO.ColumnDecisionDTO();
        column.setColumn(index);
        column.setProperty(property);
        return column;
    }

    private static ResourceDeclarationDTO declaration(String id, String classUri, List<PropertyValueDTO> values) {
        DeclaredResourceDTO resource = new DeclaredResourceDTO();
        resource.setClassUri(URI.create(classUri));
        resource.setValues(new ArrayList<>(values));
        ResourceDeclarationDTO dto = new ResourceDeclarationDTO();
        dto.setSessionId(id);
        dto.setResource(resource);
        return dto;
    }

    private static CreationRequestDTO request(String id, String step, int expected) {
        CreationRequestDTO dto = new CreationRequestDTO();
        dto.setSessionId(id);
        dto.setStep(step);
        dto.setExpected(expected);
        return dto;
    }

    private List<ExperimentModel> experimentsNamed(String name) throws Exception {
        return getSparqlService().search(ExperimentModel.class, null).stream()
                .filter(experiment -> name.equals(experiment.getName())).toList();
    }

    private static JsonNode step(JsonNode plan, String name) {
        for (JsonNode step : plan.get("steps")) {
            if (name.equals(step.get("step").asText())) {
                return step;
            }
        }
        throw new AssertionError("No step " + name + " in " + plan);
    }

    private JsonNode call(UserCallBuilder builder, String email) throws Exception {
        try (Response response = (email == null ? builder.buildAdmin()
                : builder.setUserEmail(email).setUserPassword("pwd").build()).executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }
}
