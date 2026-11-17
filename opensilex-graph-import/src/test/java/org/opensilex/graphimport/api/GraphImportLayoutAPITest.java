//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.graphimport.api.dto.SheetLayoutDTO;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.integration.test.ServiceDescription;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Choosing the header row and the first row of values of a sheet, end to end.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportLayoutAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String SHEET_PATH = SESSION_PATH + "/{sessionId}/sheets/{sheet}";

    private static final ServiceDescription CREATE;
    private static final ServiceDescription GET;
    private static final ServiceDescription ROWS;
    private static final ServiceDescription LAYOUT;
    private static final ServiceDescription RESET;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            GET = new ServiceDescription(GraphImportAPI.class.getMethod("getSession", String.class),
                    SESSION_PATH + "/{sessionId}");
            ROWS = new ServiceDescription(GraphImportLayoutAPI.class.getMethod("getSheetRows", String.class,
                    String.class), SHEET_PATH + "/rows");
            LAYOUT = new ServiceDescription(GraphImportLayoutAPI.class.getMethod("setSheetLayout",
                    SheetLayoutDTO.class), GraphImportAPI.PATH + "/sheet_layouts");
            RESET = new ServiceDescription(GraphImportLayoutAPI.class.getMethod("resetSheetLayout", String.class,
                    String.class), SHEET_PATH + "/layout");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private int status;

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(GraphImportAccess.COLLECTION);
    }

    @Test
    public void theRowsShowTheTitleAndTheLayoutTheReaderFound() throws Exception {
        String id = open();

        JsonNode rows = rows(id, "mesures");

        assertEquals(200, status);
        assertEquals("Essai 2026", rows.get("rows").get(0).get(0).asText());
        assertEquals(0, rows.get("rows").get(1).size());
        assertEquals(3, rows.get("detected_layout").get("header_row").asInt());
        assertEquals(4, rows.get("layout").get("first_data_row").asInt());
        assertEquals(6, rows.get("total_rows").asInt());
        assertEquals("plan.xlsx", rows.get("file_name").asText());
    }

    @Test
    public void aChosenLayoutIsAppliedStoredAndResumed() throws Exception {
        String id = open();

        JsonNode session = layout(id, "mesures", 3, 5);

        assertEquals(200, status);
        assertEquals(2, sheet(session, "mesures").get("data_row_count").asInt());
        new GraphImportSessionCache(null).remove(id);
        JsonNode resumed = call(new UserCallBuilder(GET).addPathTemplateParam("sessionId", id));
        assertEquals(2, sheet(resumed, "mesures").get("data_row_count").asInt());
        assertEquals(5, rows(id, "mesures").get("layout").get("first_data_row").asInt());
    }

    @Test
    public void theDetectedLayoutStaysWhateverTheUserChoseAndCanBeRestored() throws Exception {
        String id = open();

        layout(id, "mesures", 1, 3);
        assertEquals(200, status);
        new GraphImportSessionCache(null).remove(id);
        JsonNode rows = rows(id, "mesures");
        assertEquals(1, rows.get("layout").get("header_row").asInt());
        assertEquals(3, rows.get("detected_layout").get("header_row").asInt());

        JsonNode restored = call(new UserCallBuilder(RESET).addPathTemplateParam("sessionId", id)
                .addPathTemplateParam("sheet", "mesures"));
        assertEquals(200, status);
        assertEquals(3, sheet(restored, "mesures").get("data_row_count").asInt());
        new GraphImportSessionCache(null).remove(id);
        assertEquals(3, rows(id, "mesures").get("layout").get("header_row").asInt());
    }

    @Test
    public void aLayoutOutsideTheSheetOrAnUnknownSheetIsRefused() throws Exception {
        String id = open();

        layout(id, "mesures", 3, 3);
        assertEquals(400, status);
        layout(id, "mesures", 3, 40);
        assertEquals(400, status);
        layout(id, "nowhere", 1, 2);
        assertEquals(404, status);
        rows(id, "nowhere");
        assertEquals(404, status);
        rows("nobody", "mesures");
        assertEquals(404, status);
    }

    private String open() throws Exception {
        File file = new File(folder.getRoot(), "plan.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet("mesures");
            row(sheet, 0, "Essai 2026");
            row(sheet, 2, "nom", "hauteur");
            row(sheet, 3, "-", "cm");
            row(sheet, 4, "P1", "12");
            row(sheet, 5, "P2", "14");
            workbook.write(out);
        }
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart)).get("session_id").asText();
        }
    }

    private static void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private JsonNode rows(String sessionId, String sheet) throws Exception {
        return call(new UserCallBuilder(ROWS).addPathTemplateParam("sessionId", sessionId)
                .addPathTemplateParam("sheet", sheet));
    }

    private JsonNode layout(String sessionId, String sheet, int headerRow, int firstDataRow) throws Exception {
        SheetLayoutDTO body = new SheetLayoutDTO();
        body.setSessionId(sessionId);
        body.setSheet(sheet);
        body.setHeaderRow(headerRow);
        body.setFirstDataRow(firstDataRow);
        return call(new UserCallBuilder(LAYOUT).setBody(body));
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }

    private static JsonNode sheet(JsonNode session, String name) {
        for (JsonNode sheet : session.get("sheets")) {
            if (name.equals(sheet.get("name").asText())) {
                return sheet;
            }
        }
        throw new AssertionError("No sheet " + name);
    }
}
