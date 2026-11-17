//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template.csvw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateFixtures;
import org.opensilex.aiimport.template.TemplateSheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A template leaves as CSVW and comes back as the same template; a document that is not one of this
 * module's is refused with the reason.
 *
 * @author Arnaud Charleroy
 */
public class CsvwTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CsvwWriter writer = new CsvwWriter(MAPPER);
    private final CsvwReader reader = new CsvwReader(MAPPER);

    private void refused(String document, String reason) {
        try {
            reader.read(document);
            fail("should have been refused: " + reason);
        } catch (CsvwException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(reason));
        }
    }

    @Test
    public void aTemplateIsWrittenWithTheColumnsOfEachSheetAndWhatTheyMean() {
        JsonNode document = writer.write(TemplateFixtures.planAndMeasures());

        assertEquals("TableGroup", document.get("@type").asText());
        JsonNode plan = document.get("tables").get(0);
        assertEquals("plan.csv", plan.get("url").asText());
        assertEquals("plot_id", plan.get("tableSchema").get("primaryKey").asText());
        JsonNode germplasm = plan.get("tableSchema").get("columns").get(1);
        assertEquals("variete", germplasm.get("titles").asText());
        assertEquals("http://www.opensilex.org/vocabulary/oeso#hasGermplasm", germplasm.get("propertyUrl").asText());
        JsonNode property = plan.get("tableSchema").get("columns").get(3);
        assertEquals("the property the user chose is what the column means", TemplateFixtures.ALTITUDE,
                property.get("propertyUrl").asText());
    }

    @Test
    public void aTemplateComesBackAsItWas() throws Exception {
        FileTemplate original = TemplateFixtures.planAndMeasures();

        FileTemplate back = reader.read(MAPPER.writeValueAsString(writer.write(original)));

        assertEquals(original.getName(), back.getName());
        assertEquals(2, back.getSheets().size());
        for (int i = 0; i < 2; i++) {
            TemplateSheet before = original.getSheets().get(i);
            TemplateSheet after = back.getSheets().get(i);
            assertEquals(before.getName(), after.getName());
            assertEquals(before.getHeaders(), after.getHeaders());
            assertEquals(before.kind(), after.kind());
            assertEquals(before.roles(), after.roles());
            assertEquals(before.getType(), after.getType());
            assertEquals(before.getProperties(), after.getProperties());
        }
        assertEquals(SheetKind.DATA, back.getSheets().get(1).kind().orElseThrow());
        assertEquals(DescribedRole.VARIABLE, back.getSheets().get(1).roles().get("hauteur_cm"));
    }

    @Test
    public void aDocumentThatIsNotOneOfThisModulesIsRefusedWithTheReason() {
        refused("not json at all", "not valid JSON");
        refused("[]", "not a CSVW group");
        refused("{\"@type\": \"Table\"}", "not a CSVW group");
        refused("{\"@type\": \"TableGroup\", \"tables\": []}", "no table");
        refused("{\"@type\": \"TableGroup\", \"tables\": [{\"tableSchema\": {}}]}", "no url");
        refused("{\"@type\": \"TableGroup\", \"tables\": [{\"url\": \"a.csv\", \"tableSchema\": {\"columns\": []}}]}",
                "does not say what its rows are");
        refused("{\"@type\": \"TableGroup\", \"tables\": [{\"url\": \"a.csv\", \"" + CsvwVocabulary.KIND
                + "\": \"OBJECTS\"}]}", "no columns");
        refused("{\"@type\": \"TableGroup\", \"tables\": [{\"url\": \"a.csv\", \"" + CsvwVocabulary.KIND
                + "\": \"OBJECTS\", \"tableSchema\": {\"columns\": [{\"name\": \"\"}]}}]}", "neither a title");
    }

    @Test
    public void annotationsTheModuleDoesNotKnowAreIgnored() throws Exception {
        String document = "{\"@type\": \"TableGroup\", \"http://example.org/other\": 1, \"tables\": [{\"url\": \"a.csv\", \""
                + CsvwVocabulary.KIND + "\": \"OBJECTS\", \"http://example.org/note\": \"x\", \"tableSchema\": "
                + "{\"columns\": [{\"name\": \"a\", \"titles\": \"A\", \"http://example.org/c\": 2}]}}]}";

        FileTemplate template = reader.read(document);

        assertEquals("A", template.getSheets().get(0).getHeaders().get(0));
    }

    @Test
    public void aRoleThisVersionDoesNotKnowIsDroppedNotRefused() throws Exception {
        String document = "{\"@type\": \"TableGroup\", \"tables\": [{\"url\": \"a.csv\", \"" + CsvwVocabulary.KIND
                + "\": \"OBJECTS\", \"tableSchema\": {\"columns\": [{\"name\": \"a\", \"titles\": \"a\", \""
                + CsvwVocabulary.ROLE + "\": \"FUTURE_ROLE\"}]}}]}";

        assertTrue(reader.read(document).getSheets().get(0).roles().isEmpty());
    }

    @Test
    public void aTypeThatIsNotAUriIsRefused() {
        refused("{\"@type\": \"TableGroup\", \"tables\": [{\"url\": \"a.csv\", \"" + CsvwVocabulary.KIND
                + "\": \"OBJECTS\", \"" + CsvwVocabulary.TYPE + "\": \"not a uri\", \"tableSchema\": "
                + "{\"columns\": [{\"name\": \"a\"}]}}]}", "not a URI");
    }
}
