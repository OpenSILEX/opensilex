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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.api.dto.ImportProvenanceDTO;
import org.opensilex.graphimport.api.dto.SheetDecisionsDTO;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.integration.test.ServiceDescription;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A sheet of data read row by row: one datum per row and column of measures, the variable of a column
 * suggested by its header, a value not available counted apart, problems said on their cells.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportDataAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String DATA_SHEET = "mesures";
    private static final String DATA = "date;placette;PM_LEAF_PC;hauteur\n"
            + "2026-05-03;P1;12,5;NA\n"
            + "2026-05-03;P2;abc;3\n"
            + "2026-05-04;P9;1;\n";
    private static final String PLOTS = "nom;remarque\nP1;a\nP2;b\n";
    private static final String VARIABLE_GRAPH = "http://test.opensilex.org/graph-import/variables";
    private static final URI LEAF = URI.create("http://test.opensilex.org/id/variable/pm_leaf_pc");
    private static final URI HEIGHT = URI.create("http://test.opensilex.org/id/variable/hauteur");
    private static final URI LABEL = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
    private static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");

    private static final ServiceDescription CREATE;
    private static final ServiceDescription ADD_FILE;
    private static final ServiceDescription DECIDE;
    private static final ServiceDescription VALIDATE;
    private static final ServiceDescription PROVENANCE;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            ADD_FILE = new ServiceDescription(GraphImportFileAPI.class.getMethod("addFile", String.class, File.class,
                    FormDataContentDisposition.class), SESSION_PATH + "/{sessionId}/" + GraphImportFileAPI.PATH);
            DECIDE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("decideSheet",
                    SheetDecisionsDTO.class), GraphImportAPI.PATH + "/sheet_decisions");
            VALIDATE = new ServiceDescription(GraphImportValidationAPI.class.getMethod("validate", String.class),
                    SESSION_PATH + "/{sessionId}/validation");
            PROVENANCE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("chooseProvenance",
                    ImportProvenanceDTO.class), GraphImportAPI.PATH + "/import_provenances");
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

    /** Two variables, written as the platform reads them: a name and a datatype. */
    @Before
    public void writeVariables() throws Exception {
        getSparqlService().executeUpdateQuery("INSERT DATA { GRAPH <" + VARIABLE_GRAPH + "> { "
                + variable(LEAF, "PM_LEAF_PC", "decimal") + variable(HEIGHT, "hauteur", "integer") + "} }");
    }

    @After
    public void dropVariables() throws Exception {
        getSparqlService().clearGraph(URI.create(VARIABLE_GRAPH));
    }

    @Test
    public void theVariableOfAColumnIsSuggestedByItsHeader() throws Exception {
        String id = prepared();

        JsonNode interpretation = call(new UserCallBuilder(DECIDE).setBody(dataSheet(id)));
        JsonNode sheet = sheetOf(interpretation, DATA_SHEET);

        assertEquals(LEAF.toString(), sheet.get("columns").get(2).get("variable").asText());
        assertEquals("SUGGESTED", sheet.get("columns").get(2).get("variable_status").asText());
        assertEquals(HEIGHT.toString(), sheet.get("columns").get(3).get("variable").asText());
        assertEquals("the measures give the variable and value of each datum", 0,
                sheet.get("unfilled_required").size());
    }

    @Test
    public void eachCellOfMeasuresIsADatumOrSaysWhyNot() throws Exception {
        String id = prepared();
        call(new UserCallBuilder(DECIDE).setBody(dataSheet(id)));

        JsonNode validation = call(new UserCallBuilder(VALIDATE).addPathTemplateParam("sessionId", id));
        JsonNode counted = validation.get("data_sheets").get(0);

        assertEquals(DATA_SHEET, counted.get("sheet").asText());
        assertEquals("P1 and P2 are rows of the file still to create", 2, counted.get("waiting").asInt());
        assertEquals(0, counted.get("ready").asInt());
        assertEquals(1, counted.get("not_available").asInt());
        assertEquals("abc is no decimal, P9 is nothing known", 2, counted.get("refused").asInt());
        List<String> keys = new ArrayList<>();
        validation.get("problems").forEach(problem -> keys.add(problem.get("message").get("key").asText()));
        assertTrue(keys.toString(), keys.contains("measure.targetUnfound"));
        assertTrue("the data sheet's date column can be read in a zone", validation.get("date_columns").toString()
                .contains(DATA_SHEET));
    }

    @Test
    public void anUnknownProvenanceCannotBeChosen() throws Exception {
        String id = prepared();
        ImportProvenanceDTO dto = new ImportProvenanceDTO();
        dto.setSessionId(id);
        dto.setProvenance(URI.create("http://test.opensilex.org/id/provenance/absent"));

        call(new UserCallBuilder(PROVENANCE).setBody(dto));

        assertEquals(404, status);
    }

    /** The plots, of the root class, and the data sheet added to the import. */
    private String prepared() throws Exception {
        String id = upload(CREATE, "placettes", PLOTS, null);
        upload(ADD_FILE, DATA_SHEET, DATA, id);
        SheetDecisionsDTO plots = new SheetDecisionsDTO();
        plots.setSessionId(id);
        plots.setSheet("placettes");
        plots.setClassUri(URI.create(Oeso.ScientificObject.getURI()));
        plots.setIdentifierColumn(0);
        plots.setColumns(List.of(column(0, LABEL), column(1, COMMENT)));
        call(new UserCallBuilder(DECIDE).setBody(plots));
        assertEquals(200, status);
        return id;
    }

    private SheetDecisionsDTO dataSheet(String id) {
        SheetDecisionsDTO data = new SheetDecisionsDTO();
        data.setSessionId(id);
        data.setSheet(DATA_SHEET);
        data.setClassUri(URI.create(Oeso.NS + "Data"));
        data.setColumns(List.of(column(0, URI.create(Oeso.NS + "hasDate")), column(1, URI.create(Oeso.NS + "hasTarget"))));
        return data;
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

    private static String variable(URI uri, String name, String datatype) {
        return "<" + uri + "> a <" + Oeso.Variable.getURI() + "> ; <" + LABEL + "> \"" + name + "\" ; <"
                + Oeso.hasDataType.getURI() + "> <http://www.w3.org/2001/XMLSchema#" + datatype + "> . ";
    }

    private static SheetDecisionsDTO.ColumnDecisionDTO column(int index, URI property) {
        SheetDecisionsDTO.ColumnDecisionDTO column = new SheetDecisionsDTO.ColumnDecisionDTO();
        column.setColumn(index);
        column.setProperty(property);
        return column;
    }

    private static JsonNode sheetOf(JsonNode interpretation, String name) {
        for (JsonNode sheet : interpretation.get("sheets")) {
            if (name.equals(sheet.get("name").asText())) {
                return sheet;
            }
        }
        throw new AssertionError("No sheet " + name + " in " + interpretation);
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }
}
