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
import org.opensilex.integration.test.ServiceDescription;

import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The interpretation of an import, end to end, on the OpenSILEX vocabulary.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportInterpretationAPITest extends AbstractMongoIntegrationTest {

    private static final String SESSION_PATH = GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH;
    private static final String OBJECTS = "nom;Date de création;parent\nP1;2026-04-03;\nP2;2026-04-04;P1\n"
            + "P3;2026-04-05;P1\n";

    private static final ServiceDescription CREATE;
    private static final ServiceDescription INTERPRET;

    static {
        try {
            CREATE = new ServiceDescription(GraphImportAPI.class.getMethod("createSession", File.class,
                    FormDataContentDisposition.class), SESSION_PATH);
            INTERPRET = new ServiceDescription(GraphImportInterpretationAPI.class.getMethod("getInterpretation",
                    String.class), SESSION_PATH + "/{sessionId}/interpretation");
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
    public void aSheetOfScientificObjectsIsInterpretedWithItsEvidence() throws Exception {
        String id = open("scientific_objects.csv", OBJECTS);

        JsonNode interpretation = interpret(id);

        assertEquals(200, status);
        JsonNode sheet = interpretation.get("sheets").get(0);
        assertEquals(Oeso.ScientificObject.getURI(), sheet.get("sheet_class").get("chosen_uri").asText());
        assertEquals("SUGGESTED", sheet.get("sheet_class").get("status").asText());
        assertEquals(0, sheet.get("identifier").get("chosen_column").asInt());
        assertEquals(3, sheet.get("row_count").asInt());

        JsonNode columns = sheet.get("columns");
        assertEquals("http://www.w3.org/2000/01/rdf-schema#label", chosen(columns.get(0)));
        assertTrue(columns.get(0).get("identifier").asBoolean());
        assertEquals(Oeso.NS + "hasCreationDate", chosen(columns.get(1)));
        assertEquals(Oeso.NS + "isPartOf", chosen(columns.get(2)));

        JsonNode best = columns.get(2).get("property").get("candidates").get(0);
        assertEquals("OBJECT", best.get("kind").asText());
        assertEquals(1, best.get("max_values").asInt());
        List<String> keys = StreamSupport.stream(best.get("evidence").spliterator(), false)
                .map(evidence -> evidence.get("reason").get("key").asText()).toList();
        assertTrue(keys.toString(), keys.contains("evidence.sheetIdentifiers"));
        assertEquals("note.noExperiment", interpretation.get("notes").get(0).get("key").asText());
    }

    @Test
    public void anUnknownImportIsRefused() throws Exception {
        interpret("nobody");
        assertEquals(404, status);
    }

    private String open(String name, String content) throws Exception {
        File file = new File(folder.newFolder(), name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        try (MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("file", file))) {
            return call(new UserCallBuilder(CREATE).setMultipartBody(multiPart)).get("session_id").asText();
        }
    }

    private JsonNode interpret(String sessionId) throws Exception {
        return call(new UserCallBuilder(INTERPRET).addPathTemplateParam("sessionId", sessionId));
    }

    private JsonNode call(UserCallBuilder builder) throws Exception {
        try (Response response = builder.buildAdmin().executeCall()) {
            status = response.getStatus();
            JsonNode node = response.readEntity(JsonNode.class);
            return node != null && node.has("result") ? node.get("result") : node;
        }
    }

    private static String chosen(JsonNode column) {
        return column.get("property").get("chosen_uri").asText();
    }
}
