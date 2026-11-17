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
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.integration.test.ServiceDescription;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The imports of the experimental assistant, end to end: opened with a file, completed with others,
 * left, resumed from the store and deleted.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String ONE_SESSION_PATH = SESSION_PATH + "/{sessionId}";
    private static final String FILES_PATH = ONE_SESSION_PATH + "/" + GraphImportFileAPI.PATH;
    private static final String PLAN = "plot_id;variete;bloc\nP01;Syrah;B1\nP02;Grenache;B2\n";
    private static final String MEASURES = "plot_id;date;hauteur_cm\nP01;2026-06-12;84\n";

    private static final ServiceDescription CREATE;
    private static final ServiceDescription LIST;
    private static final ServiceDescription GET;
    private static final ServiceDescription DELETE;
    private static final ServiceDescription ADD_FILE;
    private static final ServiceDescription REMOVE_FILE;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            LIST = new ServiceDescription(GraphImportAPI.class.getMethod("listSessions"), SESSION_PATH);
            GET = new ServiceDescription(GraphImportAPI.class.getMethod("getSession", String.class), ONE_SESSION_PATH);
            DELETE = new ServiceDescription(GraphImportAPI.class.getMethod("deleteSession", String.class),
                    ONE_SESSION_PATH);
            ADD_FILE = new ServiceDescription(GraphImportFileAPI.class.getMethod("addFile", String.class, File.class,
                    FormDataContentDisposition.class), FILES_PATH);
            REMOVE_FILE = new ServiceDescription(GraphImportFileAPI.class.getMethod("removeFile", String.class,
                    String.class), FILES_PATH + "/{fileId}");
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
    public void aWorkbookOpensAnImportWithEachOfItsSheets() throws Exception {
        JsonNode session = open(aWorkbook());

        assertEquals(201, status);
        assertEquals("plan.xlsx", session.get("title").asText());
        JsonNode sheets = session.get("sheets");
        assertEquals(2, sheets.size());
        assertEquals("parcelles", sheets.get(0).get("name").asText());
        assertEquals(List.of("plot_id", "variete"), texts(sheets.get(0).get("headers")));
        assertEquals(2, sheets.get(0).get("data_row_count").asInt());
        assertEquals("varietes", sheets.get(1).get("name").asText());
    }

    @Test
    public void aFileOfAnotherKindOrUnreadableIsRefused() throws Exception {
        open(aFile("notes.txt", "just text"));
        assertEquals(400, status);

        open(aFile("broken.xlsx", "not a workbook"));
        assertEquals(400, status);
    }

    @Test
    public void anImportLeftInTheStoreIsResumedWithItsFiles() throws Exception {
        String id = open(aFile("plan.csv", PLAN)).get("session_id").asText();
        add(id, aFile("mesures.csv", MEASURES));
        new GraphImportSessionCache(null).remove(id);

        JsonNode resumed = get(id);

        assertEquals(200, status);
        assertEquals(List.of("plan", "mesures"), names(resumed.get("sheets")));
        assertEquals(2, resumed.get("files").size());
        List<String> titles = names(call(new UserCallBuilder(LIST)), "title");
        assertTrue(titles.toString(), titles.contains("plan.csv"));
    }

    @Test
    public void filesComeAndGoButTheLastOneStays() throws Exception {
        JsonNode session = open(aFile("plan.csv", PLAN));
        String id = session.get("session_id").asText();
        String planId = session.get("files").get(0).get("id").asText();

        remove(id, planId);
        assertEquals("the only file stays", 400, status);

        JsonNode files = add(id, aFile("mesures.csv", MEASURES));
        assertEquals(200, status);
        assertEquals("mesures", files.get(1).get("sheets").get(0).asText());

        add(id, aFile("PLAN.csv", MEASURES));
        assertEquals("a file of the same name", 400, status);
        add(id, aFile("notes.txt", "text"));
        assertEquals(400, status);

        JsonNode remaining = remove(id, files.get(1).get("id").asText());
        assertEquals(200, status);
        assertEquals(1, remaining.size());
        remove(id, "nobody");
        assertEquals(404, status);
    }

    @Test
    public void aDeletedImportIsGone() throws Exception {
        String id = open(aFile("plan.csv", PLAN)).get("session_id").asText();

        call(new UserCallBuilder(DELETE).addPathTemplateParam("sessionId", id));
        assertEquals(200, status);

        get(id);
        assertEquals(404, status);
        call(new UserCallBuilder(DELETE).addPathTemplateParam("sessionId", id));
        assertEquals(404, status);
        add(id, aFile("mesures.csv", MEASURES));
        assertEquals(404, status);
        remove(id, "x");
        assertEquals(404, status);
    }

    //#region helpers

    private File aWorkbook() throws Exception {
        File file = new File(folder.getRoot(), "plan.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = new FileOutputStream(file)) {
            Sheet plots = workbook.createSheet("parcelles");
            row(plots, 0, "plot_id", "variete");
            row(plots, 1, "P01", "Syrah");
            row(plots, 2, "P02", "Grenache");
            Sheet varieties = workbook.createSheet("varietes");
            row(varieties, 0, "variete", "couleur");
            row(varieties, 1, "Syrah", "noir");
            workbook.write(out);
        }
        return file;
    }

    private static void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private File aFile(String name, String content) throws Exception {
        File file = new File(folder.newFolder(), name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private JsonNode open(File file) throws Exception {
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart));
        }
    }

    private JsonNode add(String sessionId, File file) throws Exception {
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(ADD_FILE).setMultipartBody(multiPart)
                    .addPathTemplateParam("sessionId", sessionId));
        }
    }

    private JsonNode get(String sessionId) throws Exception {
        return call(new UserCallBuilder(GET).addPathTemplateParam("sessionId", sessionId));
    }

    private JsonNode remove(String sessionId, String fileId) throws Exception {
        return call(new UserCallBuilder(REMOVE_FILE)
                .addPathTemplateParam("sessionId", sessionId)
                .addPathTemplateParam("fileId", fileId));
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }

    private static List<String> texts(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }

    private static List<String> names(JsonNode array) {
        return names(array, "name");
    }

    private static List<String> names(JsonNode array, String field) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(node -> node.get(field).asText()).toList();
    }

    //#endregion
}
