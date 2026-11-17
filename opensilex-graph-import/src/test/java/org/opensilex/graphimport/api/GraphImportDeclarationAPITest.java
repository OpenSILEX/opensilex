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
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.api.dto.DeclaredResourceDTO;
import org.opensilex.graphimport.api.dto.PropertyValueDTO;
import org.opensilex.graphimport.api.dto.ResourceDeclarationDTO;
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
 * A sheet of factor levels names no factor: the user gives the factor for all rows and declares it in the
 * assistant, and the levels then point to it. Kept with the import, refused when it cannot be.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportDeclarationAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String SHEET = "modalites";
    private static final String LEVELS = "nom;description\nT1;Témoin\nT2;Irrigué\n";
    private static final URI FACTOR = URI.create(Oeso.Factor.getURI());
    private static final URI FACTOR_LEVEL = URI.create(Oeso.FactorLevel.getURI());
    private static final URI HAS_FACTOR = URI.create(Oeso.hasFactor.getURI());
    private static final URI STUDIED_IN = URI.create(Oeso.studiedEffectIn.getURI());
    private static final URI LABEL = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
    private static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");

    private static final ServiceDescription CREATE;
    private static final ServiceDescription INTERPRET;
    private static final ServiceDescription DECIDE;
    private static final ServiceDescription DECLARE;
    private static final ServiceDescription FORGET;
    private static final ServiceDescription VALIDATE;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            INTERPRET = new ServiceDescription(GraphImportInterpretationAPI.class.getMethod("getInterpretation",
                    String.class), SESSION_PATH + "/{sessionId}/interpretation");
            DECIDE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("decideSheet",
                    SheetDecisionsDTO.class), GraphImportAPI.PATH + "/sheet_decisions");
            DECLARE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("declareResource",
                    ResourceDeclarationDTO.class), GraphImportAPI.PATH + "/declared_resources");
            FORGET = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("forgetDeclaredResource",
                    String.class, String.class), SESSION_PATH + "/{sessionId}/declared_resources/{resourceId}");
            VALIDATE = new ServiceDescription(GraphImportValidationAPI.class.getMethod("validate", String.class),
                    SESSION_PATH + "/{sessionId}/validation");
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
    public void aSheetOfLevelsSaysItsFactorIsMissing() throws Exception {
        String id = open();

        JsonNode sheet = sheetOf(call(new UserCallBuilder(DECIDE).setBody(levels(id, null))), SHEET);

        assertEquals(200, status);
        assertTrue(sheet.toString(), texts(sheet.get("unfilled_required")).contains(HAS_FACTOR.toString()));
    }

    @Test
    public void theFactorGivenForAllRowsIsAColumnThatPointsToNothingYet() throws Exception {
        String id = open();

        JsonNode sheet = sheetOf(call(new UserCallBuilder(DECIDE).setBody(levels(id, "Irrigation"))), SHEET);
        JsonNode given = sheet.get("columns").get(2);

        assertTrue(given.get("given").asBoolean());
        assertEquals(HAS_FACTOR.toString(), given.get("property").get("chosen_uri").asText());
        assertFalse(texts(sheet.get("unfilled_required")).contains(HAS_FACTOR.toString()));
        assertEquals(List.of("2 UNRESOLVED - 2"), links(validate(id)));
    }

    @Test
    public void onceTheFactorIsDeclaredTheLevelsPointToIt() throws Exception {
        String id = open();
        URI experiment = experiment();
        call(new UserCallBuilder(DECIDE).setBody(levels(id, "Irrigation")));

        JsonNode interpretation = call(new UserCallBuilder(DECLARE).setBody(declaration(id, null, "Irrigation",
                experiment)));
        JsonNode added = sheetOf(interpretation, "[Factor]");

        assertEquals(200, status);
        assertTrue(added.get("declared").asBoolean());
        assertEquals(1, added.get("declared_ids").size());
        assertEquals(1, interpretation.get("declared").size());
        List<String> links = links(validate(id));
        assertTrue(links.toString(), links.contains("2 ROW [Factor] 2"));
        assertTrue("the factor is studied in the experiment: " + links, links.stream()
                .anyMatch(link -> link.startsWith("1 INSTANCE")));
    }

    @Test
    public void whatWasAddedIsKeptWithTheImport() throws Exception {
        String id = open();
        call(new UserCallBuilder(DECIDE).setBody(levels(id, "Irrigation")));
        call(new UserCallBuilder(DECLARE).setBody(declaration(id, "f1", "Irrigation", null)));

        new GraphImportSessionCache(null).remove(id);
        JsonNode interpretation = call(new UserCallBuilder(INTERPRET).addPathTemplateParam("sessionId", id));

        assertEquals("f1", interpretation.get("declared").get(0).get("id").asText());
        assertEquals("Irrigation", sheetOf(interpretation, SHEET).get("given").get(0).get("value").asText());
    }

    @Test
    public void aDeclaredResourceCanBeChangedAndForgotten() throws Exception {
        String id = open();
        call(new UserCallBuilder(DECIDE).setBody(levels(id, "Irrigation")));
        call(new UserCallBuilder(DECLARE).setBody(declaration(id, "f1", "Irrigation", null)));

        call(new UserCallBuilder(DECLARE).setBody(declaration(id, "f1", "Arrosage", null)));
        assertEquals(List.of("2 UNRESOLVED - 2"), links(validate(id)));

        JsonNode forgotten = call(new UserCallBuilder(FORGET).addPathTemplateParam("sessionId", id)
                .addPathTemplateParam("resourceId", "f1"));
        assertEquals(200, status);
        assertEquals(0, forgotten.get("declared").size());

        call(new UserCallBuilder(FORGET).addPathTemplateParam("sessionId", id).addPathTemplateParam("resourceId", "f1"));
        assertEquals(404, status);
    }

    @Test
    public void aResourceWithoutNameOrOfAnUnknownClassIsRefused() throws Exception {
        String id = open();
        ResourceDeclarationDTO nameless = declaration(id, null, null, null);
        nameless.getResource().getValues().add(new PropertyValueDTO(COMMENT, "sans nom"));

        call(new UserCallBuilder(DECLARE).setBody(nameless));
        assertEquals(400, status);

        ResourceDeclarationDTO unknown = declaration(id, null, "X", null);
        unknown.getResource().setClassUri(URI.create("http://test.opensilex.org/Nothing"));
        call(new UserCallBuilder(DECLARE).setBody(unknown));
        assertEquals(400, status);
    }

    @Test
    public void aValueForAPropertyTheClassDoesNotHaveIsRefused() throws Exception {
        String id = open();
        SheetDecisionsDTO decision = levels(id, null);
        decision.getGiven().add(new PropertyValueDTO(URI.create(Oeso.NS + "isPartOf"), "Bloc A"));

        call(new UserCallBuilder(DECIDE).setBody(decision));

        assertEquals(400, status);
    }

    private String open() throws Exception {
        File file = new File(folder.newFolder(), SHEET + ".csv");
        Files.write(file.toPath(), LEVELS.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart)).get("session_id").asText();
        }
    }

    /** The sheet of levels: its class, its name column, its description, and the factor of all rows. */
    private static SheetDecisionsDTO levels(String id, String factor) {
        SheetDecisionsDTO dto = new SheetDecisionsDTO();
        dto.setSessionId(id);
        dto.setSheet(SHEET);
        dto.setClassUri(FACTOR_LEVEL);
        dto.setIdentifierColumn(0);
        List<SheetDecisionsDTO.ColumnDecisionDTO> columns = new ArrayList<>();
        columns.add(column(0, LABEL));
        columns.add(column(1, COMMENT));
        dto.setColumns(columns);
        if (factor != null) {
            dto.getGiven().add(new PropertyValueDTO(HAS_FACTOR, factor));
        }
        return dto;
    }

    private static SheetDecisionsDTO.ColumnDecisionDTO column(int index, URI property) {
        SheetDecisionsDTO.ColumnDecisionDTO column = new SheetDecisionsDTO.ColumnDecisionDTO();
        column.setColumn(index);
        column.setProperty(property);
        return column;
    }

    private static ResourceDeclarationDTO declaration(String id, String resourceId, String name, URI experiment) {
        DeclaredResourceDTO resource = new DeclaredResourceDTO();
        resource.setId(resourceId);
        resource.setClassUri(FACTOR);
        List<PropertyValueDTO> values = new ArrayList<>();
        if (name != null) {
            values.add(new PropertyValueDTO(LABEL, name));
        }
        if (experiment != null) {
            values.add(new PropertyValueDTO(STUDIED_IN, experiment.toString()));
        }
        resource.setValues(values);
        ResourceDeclarationDTO dto = new ResourceDeclarationDTO();
        dto.setSessionId(id);
        dto.setResource(resource);
        return dto;
    }

    private URI experiment() throws Exception {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName("Essai déclaration");
        experiment.setObjective("Tester les ressources déclarées");
        experiment.setStartDate(LocalDate.parse("2026-04-01"));
        experiment.setIsPublic(true);
        getSparqlService().create(ExperimentModel.class, List.of(experiment));
        return experiment.getUri();
    }

    private JsonNode validate(String id) throws Exception {
        return call(new UserCallBuilder(VALIDATE).addPathTemplateParam("sessionId", id));
    }

    /** Where the links land, as "column kind sheet count", the kind's target written - when it has none. */
    private static List<String> links(JsonNode validation) {
        List<String> links = new ArrayList<>();
        validation.get("column_links").forEach(link -> links.add(link.get("column").asInt() + " "
                + link.get("kind").asText() + " "
                + (link.hasNonNull("to_sheet") ? link.get("to_sheet").asText() : "-") + " "
                + link.get("count").asInt()));
        return links;
    }

    private static JsonNode sheetOf(JsonNode interpretation, String name) {
        for (JsonNode sheet : interpretation.get("sheets")) {
            if (name.equals(sheet.get("name").asText())) {
                return sheet;
            }
        }
        throw new AssertionError("No sheet " + name + " in " + interpretation);
    }

    private static List<String> texts(JsonNode array) {
        List<String> texts = new ArrayList<>();
        array.forEach(node -> texts.add(node.asText()));
        return texts;
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }
}
