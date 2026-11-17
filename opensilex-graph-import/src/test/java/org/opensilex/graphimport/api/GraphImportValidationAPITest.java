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
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.api.dto.ColumnReadingDTO;
import org.opensilex.graphimport.api.dto.SheetDecisionsDTO;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.graphimport.ontology.literal.DateOrder;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.integration.test.ServiceDescription;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The graph of an import, checked through the API on the platform's own ontology, and how the dates
 * of a column are read, kept apart from the other decisions.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportValidationAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String SHEET = "parcelles";
    private static final String PLOTS = "nom;Date de création;parent\n"
            + "P1;03/04/2026;\n"
            + "P2;04/05/2026;P1\n"
            + "P3;05/06/2026;P404\n";
    private static final URI SO = URI.create(Oeso.ScientificObject.getURI());
    private static final URI CREATION_DATE = URI.create(Oeso.NS + "hasCreationDate");
    private static final URI IS_PART_OF = URI.create(Oeso.NS + "isPartOf");

    private static final ServiceDescription CREATE;
    private static final ServiceDescription DECIDE;
    private static final ServiceDescription VALIDATE;
    private static final ServiceDescription READ_COLUMN;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            DECIDE = new ServiceDescription(GraphImportDecisionAPI.class.getMethod("decideSheet",
                    SheetDecisionsDTO.class), GraphImportAPI.PATH + "/sheet_decisions");
            VALIDATE = new ServiceDescription(GraphImportValidationAPI.class.getMethod("validate", String.class),
                    SESSION_PATH + "/{sessionId}/validation");
            READ_COLUMN = new ServiceDescription(GraphImportValidationAPI.class.getMethod("readColumn",
                    ColumnReadingDTO.class), GraphImportAPI.PATH + "/column_readings");
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

    @Test
    public void theGraphIsCheckedCellByCell() throws Exception {
        String id = open();
        decide(id);

        JsonNode validation = validate(id);

        assertEquals(200, status);
        assertEquals(3, validation.get("node_count").asInt());
        assertEquals(2, validation.get("link_count").asInt());
        List<String> keys = keys(validation);
        assertTrue(keys.toString(), keys.contains("literal.ambiguousDate"));
        assertTrue(keys.toString(), keys.contains("problem.linkUnfound"));
        JsonNode dates = validation.get("date_columns").get(0);
        assertEquals("UNDECIDED", dates.get("detected_order").asText());
        assertFalse(dates.has("date_order"));
        assertTrue("a problem whose cells lie in two places counts in both",
                sum(validation.get("counts")) >= validation.get("total").asInt());

        List<String> links = new ArrayList<>();
        validation.get("column_links").forEach(link -> links.add(link.get("column").asInt() + " "
                + link.get("kind").asText() + " " + link.path("to_sheet").asText("-") + " " + link.get("count").asInt()));
        assertEquals("P2's parent is the row P1; P404 names nothing", List.of("2 ROW parcelles 1", "2 UNRESOLVED - 1"),
                links);
        assertTrue("one sheet shares resources with no other", validation.get("shared_resources").isEmpty());
    }

    @Test
    public void theOrderOfAColumnSurvivesTheOtherDecisionsAndAResume() throws Exception {
        String id = open();
        decide(id);

        JsonNode read = readColumn(id, 1, DateOrder.DAY_FIRST, "+02:00");
        assertEquals(200, status);
        assertFalse(keys(read).contains("literal.ambiguousDate"));
        assertEquals("DAY_FIRST", read.get("date_columns").get(0).get("date_order").asText());
        assertEquals("+02:00", read.get("date_columns").get(0).get("zone").asText());

        decide(id);
        assertEquals("DAY_FIRST", validate(id).get("date_columns").get(0).get("date_order").asText());

        new GraphImportSessionCache(null).remove(id);
        assertEquals("DAY_FIRST", validate(id).get("date_columns").get(0).get("date_order").asText());
    }

    @Test
    public void aReadingTheSheetCannotTakeIsRefused() throws Exception {
        String id = open();

        readColumn(id, 9, DateOrder.DAY_FIRST, null);
        assertEquals(400, status);
        readColumn(id, 1, null, "+25:00");
        assertEquals(400, status);
        call(new UserCallBuilder(VALIDATE).addPathTemplateParam("sessionId", "nothing"));
        assertEquals(404, status);
    }

    private String open() throws Exception {
        File file = new File(folder.newFolder(), SHEET + ".csv");
        Files.write(file.toPath(), PLOTS.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart)).get("session_id").asText();
        }
    }

    private void decide(String id) throws Exception {
        SheetDecisionsDTO dto = new SheetDecisionsDTO();
        dto.setSessionId(id);
        dto.setSheet(SHEET);
        dto.setClassUri(SO);
        dto.setIdentifierColumn(0);
        List<SheetDecisionsDTO.ColumnDecisionDTO> columns = new ArrayList<>();
        for (Map.Entry<Integer, URI> column : Map.of(1, CREATION_DATE, 2, IS_PART_OF).entrySet()) {
            SheetDecisionsDTO.ColumnDecisionDTO decision = new SheetDecisionsDTO.ColumnDecisionDTO();
            decision.setColumn(column.getKey());
            decision.setProperty(column.getValue());
            columns.add(decision);
        }
        dto.setColumns(columns);
        call(new UserCallBuilder(DECIDE).setBody(dto));
        assertEquals(200, status);
    }

    private JsonNode readColumn(String id, int column, DateOrder order, String zone) throws Exception {
        ColumnReadingDTO dto = new ColumnReadingDTO();
        dto.setSessionId(id);
        dto.setSheet(SHEET);
        dto.setColumn(column);
        dto.setDateOrder(order);
        dto.setZone(zone);
        return call(new UserCallBuilder(READ_COLUMN).setBody(dto));
    }

    private JsonNode validate(String id) throws Exception {
        return call(new UserCallBuilder(VALIDATE).addPathTemplateParam("sessionId", id));
    }

    private static List<String> keys(JsonNode validation) {
        List<String> keys = new ArrayList<>();
        validation.get("problems").forEach(problem -> keys.add(problem.get("message").get("key").asText()));
        return keys;
    }

    private static int sum(JsonNode counts) {
        int sum = 0;
        for (JsonNode count : counts) {
            sum += count.get("count").asInt();
        }
        return sum;
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }
}
