//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.vocabulary.XSD;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.graphimport.api.dto.CreationRequestDTO;
import org.opensilex.graphimport.api.dto.ImportExperimentDTO;
import org.opensilex.graphimport.api.dto.ImportProvenanceDTO;
import org.opensilex.graphimport.api.dto.SheetDecisionsDTO;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.graphimport.write.DataWriter;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The step of data: data measured on objects still to create wait for them, are created through the
 * platform's data logic as its data API would create them, and are never created twice.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportDataCreationAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String PLOTS = "placettes";
    private static final String DATA_SHEET = "mesures";
    private static final String DATA = "date;placette;PM_LEAF_PC\n"
            + "2026-05-03;P1;12,5\n"
            + "2026-05-03;P2;NA\n"
            + "2026-05-04;P2;3\n";
    private static final URI LABEL = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
    private static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");

    private static final ServiceDescription CREATE;
    private static final ServiceDescription ADD_FILE;
    private static final ServiceDescription DECIDE;
    private static final ServiceDescription EXPERIMENT;
    private static final ServiceDescription PROVENANCE;
    private static final ServiceDescription PLAN;
    private static final ServiceDescription CHECK;
    private static final ServiceDescription RUN;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            ADD_FILE = new ServiceDescription(GraphImportFileAPI.class.getMethod("addFile", String.class, File.class,
                    FormDataContentDisposition.class), SESSION_PATH + "/{sessionId}/" + GraphImportFileAPI.PATH);
            DECIDE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("decideSheet",
                    SheetDecisionsDTO.class), GraphImportAPI.PATH + "/sheet_decisions");
            EXPERIMENT = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("chooseExperiment",
                    ImportExperimentDTO.class), GraphImportAPI.PATH + "/import_experiments");
            PROVENANCE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("chooseProvenance",
                    ImportProvenanceDTO.class), GraphImportAPI.PATH + "/import_provenances");
            PLAN = new ServiceDescription(GraphImportCreationAPI.class.getMethod("getCreationPlan", String.class,
                    boolean.class), SESSION_PATH + "/{sessionId}/creation");
            CHECK = new ServiceDescription(GraphImportCreationAPI.class.getMethod("checkCreation",
                    CreationRequestDTO.class), GraphImportAPI.PATH + "/creation_checks");
            RUN = new ServiceDescription(GraphImportCreationAPI.class.getMethod("createStep",
                    CreationRequestDTO.class), GraphImportAPI.PATH + "/creations");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private int status;
    private URI experiment;
    private URI variable;
    private URI provenance;

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(GraphImportAccess.COLLECTION, LearnedAliasStore.COLLECTION, DataDAO.DATA_COLLECTION_NAME,
                ProvenanceDaoV2.PROVENANCE_COLLECTION_NAME);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, ScientificObjectModel.class, VariableModel.class, EntityModel.class,
                CharacteristicModel.class, MethodModel.class, UnitModel.class);
    }

    /** An experiment, a decimal variable named as the column of measures, and a provenance. */
    @Before
    public void createFixture() throws Exception {
        ExperimentModel created = ExperimentAPITest.getCreationDTO().newModel();
        new ExperimentDAO(getSparqlService(), getMongoDBService(), getFs()).create(created);
        experiment = created.getUri();

        VariableCreationDTO variableDto = new VariableApiTest().getCreationDto();
        variableDto.setName("PM_LEAF_PC");
        variableDto.setDataType(URI.create(XSD.decimal.getURI()));
        variable = new VariableDAO(getSparqlService(), getMongoDBService(), null, null).create(variableDto.newModel())
                .getUri();

        ProvenanceModel model = new ProvenanceModel();
        model.setName("graph-import test provenance");
        new ProvenanceDaoV2(getMongoDBService().getServiceV2()).create(model);
        provenance = model.getUri();
    }

    @Test
    public void dataWaitForTheirObjectsThenAreCreatedOnce() throws Exception {
        String id = prepared();
        choose(id, provenance);

        JsonNode before = step(call(new UserCallBuilder(PLAN).addPathTemplateParam("sessionId", id)), DataWriter.STEP);
        assertEquals(before.toString(), "BLOCKED", before.get("status").asText());
        assertEquals("P1 and P2 are rows of the file still to create", 2, before.get("data").get("waiting").asInt());
        assertEquals(1, before.get("data").get("not_available").asInt());
        assertEquals(0, before.get("count").asInt());

        JsonNode objects = call(new UserCallBuilder(RUN).setBody(request(id, "scientific-objects", 2)));
        JsonNode ready = step(objects.get("plan"), DataWriter.STEP);
        assertEquals(ready.toString(), "READY", ready.get("status").asText());
        assertEquals("once created, the objects are the targets", 2, ready.get("count").asInt());

        JsonNode checked = call(new UserCallBuilder(CHECK).setBody(request(id, DataWriter.STEP, 2)));
        assertEquals(checked.toString(), 0, checked.get("problems").size());
        assertTrue("a check writes nothing", stored().isEmpty());

        JsonNode run = call(new UserCallBuilder(RUN).setBody(request(id, DataWriter.STEP, 2)));
        assertEquals(run.toString(), 2, run.get("created_count").asInt());
        assertEquals(2, stored().size());
        JsonNode done = step(run.get("plan"), DataWriter.STEP);
        assertEquals("DONE", done.get("status").asText());
        assertEquals(2, done.get("done").asInt());

        JsonNode again = call(new UserCallBuilder(RUN).setBody(request(id, DataWriter.STEP, 0)));
        assertEquals(0, again.get("created_count").asInt());
        assertEquals("nothing is created twice", 2, stored().size());
    }

    @Test
    public void aDatumIsStoredAsTheDataApiStoresIt() throws Exception {
        String id = prepared();
        choose(id, provenance);
        call(new UserCallBuilder(RUN).setBody(request(id, "scientific-objects", 2)));
        call(new UserCallBuilder(RUN).setBody(request(id, DataWriter.STEP, 2)));
        URI plot = objectNamed("P1").getUri();
        DataModel ours = stored().stream().filter(datum -> datum.getValue() instanceof Number number
                && number.doubleValue() == 12.5).findFirst().orElseThrow();

        // The URIs as the data API's JSON reader gives them.
        DataCreationDTO dto = new DataCreationDTO();
        dto.setTarget(URIDeserializer.formatURI(plot));
        dto.setVariable(URIDeserializer.formatURI(variable));
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(URIDeserializer.formatURI(provenance));
        dataProvenance.setExperiments(List.of(URIDeserializer.formatURI(experiment)));
        dto.setProvenance(dataProvenance);
        dto.setDate("2026-06-01");
        dto.setValue(12.5);
        AccountModel admin = new AccountDAO(getSparqlService()).getByEmail(new InternetAddress(
                SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL));
        DataModel posted = dto.newModel();
        new DataLogic(getSparqlService(), getMongoDBService(), getFs(), admin).createMany(List.of(posted));
        DataModel reference = stored().stream().filter(datum -> !datum.getUri().equals(ours.getUri())
                && datum.getDate().equals(posted.getDate())).findFirst().orElseThrow();

        assertTrue(SPARQLDeserializers.compareURIs(reference.getTarget(), ours.getTarget()));
        assertTrue(SPARQLDeserializers.compareURIs(reference.getVariable(), ours.getVariable()));
        assertTrue(SPARQLDeserializers.compareURIs(reference.getProvenance().getUri(), ours.getProvenance().getUri()));
        assertEquals(reference.getProvenance().getExperiments().size(), ours.getProvenance().getExperiments().size());
        assertTrue(SPARQLDeserializers.compareURIs(reference.getProvenance().getExperiments().get(0),
                ours.getProvenance().getExperiments().get(0)));
        assertEquals(reference.getValue().getClass(), ours.getValue().getClass());
        assertEquals(reference.getIsDateTime(), ours.getIsDateTime());
        assertEquals(reference.getOffset(), ours.getOffset());
        assertNotNull(ours.getPublisher());
        assertEquals(reference.getPublisher(), ours.getPublisher());
    }

    @Test
    public void dataWithoutProvenanceAreStopped() throws Exception {
        ScientificObjectModel plot = new ScientificObjectModel();
        plot.setName("P1");
        getSparqlService().create(SPARQLDeserializers.nodeURI(experiment), plot);
        String id = prepared();

        JsonNode plan = step(call(new UserCallBuilder(PLAN).addPathTemplateParam("sessionId", id)),
                DataWriter.STEP);

        assertEquals(plan.toString(), "BLOCKED", plan.get("status").asText());
        assertEquals(0, plan.get("count").asInt());
        assertEquals("creation.noProvenance", plan.get("blocked").get(0).get("reason").get("key").asText());
        call(new UserCallBuilder(RUN).setBody(request(id, DataWriter.STEP, 1)));
        assertEquals("what the user saw is no longer what the step would do", 409, status);
    }

    /** The plots of the experiment and the sheet of data measured on them, decided. */
    private String prepared() throws Exception {
        String id = upload(CREATE, PLOTS, "nom;remarque\nP1;a\nP2;b\n", null);
        upload(ADD_FILE, DATA_SHEET, DATA, id);
        ImportExperimentDTO chosen = new ImportExperimentDTO();
        chosen.setSessionId(id);
        chosen.setExperiment(experiment);
        call(new UserCallBuilder(EXPERIMENT).setBody(chosen));
        assertEquals(200, status);

        SheetDecisionsDTO plots = new SheetDecisionsDTO();
        plots.setSessionId(id);
        plots.setSheet(PLOTS);
        plots.setClassUri(URI.create(Oeso.ScientificObject.getURI()));
        plots.setIdentifierColumn(0);
        plots.setColumns(List.of(column(0, LABEL), column(1, COMMENT)));
        call(new UserCallBuilder(DECIDE).setBody(plots));
        assertEquals(200, status);

        SheetDecisionsDTO data = new SheetDecisionsDTO();
        data.setSessionId(id);
        data.setSheet(DATA_SHEET);
        data.setClassUri(URI.create(Oeso.NS + "Data"));
        data.setColumns(List.of(column(0, URI.create(Oeso.NS + "hasDate")),
                column(1, URI.create(Oeso.NS + "hasTarget"))));
        call(new UserCallBuilder(DECIDE).setBody(data));
        assertEquals(200, status);
        return id;
    }

    private void choose(String id, URI chosen) throws Exception {
        ImportProvenanceDTO dto = new ImportProvenanceDTO();
        dto.setSessionId(id);
        dto.setProvenance(chosen);
        call(new UserCallBuilder(PROVENANCE).setBody(dto));
        assertEquals(200, status);
    }

    private List<DataModel> stored() throws Exception {
        return new DataDAO(getMongoDBService(), getSparqlService(), getFs()).search(null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private ScientificObjectModel objectNamed(String name) throws Exception {
        return getSparqlService().search(SPARQLDeserializers.nodeURI(experiment), ScientificObjectModel.class, null,
                null).stream().filter(object -> name.equals(object.getName())).findFirst().orElseThrow();
    }

    private String upload(ServiceDescription service, String name, String content, String id) throws Exception {
        File file = new File(folder.newFolder(), name + ".csv");
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            UserCallBuilder builder = new UserCallBuilder(service).setMultipartBody(multiPart);
            if (id != null) {
                builder.addPathTemplateParam("sessionId", id);
            }
            JsonNode created = call(builder);
            return id != null ? id : created.get("session_id").asText();
        }
    }

    private static SheetDecisionsDTO.ColumnDecisionDTO column(int index, URI property) {
        SheetDecisionsDTO.ColumnDecisionDTO column = new SheetDecisionsDTO.ColumnDecisionDTO();
        column.setColumn(index);
        column.setProperty(property);
        return column;
    }

    private static CreationRequestDTO request(String id, String step, int expected) {
        CreationRequestDTO dto = new CreationRequestDTO();
        dto.setSessionId(id);
        dto.setStep(step);
        dto.setExpected(expected);
        return dto;
    }

    private static JsonNode step(JsonNode plan, String name) {
        for (JsonNode step : plan.get("steps")) {
            if (name.equals(step.get("step").asText())) {
                return step;
            }
        }
        throw new AssertionError("No step " + name + " in " + plan);
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }
}
