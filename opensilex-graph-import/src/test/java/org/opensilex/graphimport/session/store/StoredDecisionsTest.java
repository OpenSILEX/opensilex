//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opensilex.graphimport.binding.ImportDecisions;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The decisions of a sheet as the store keeps them, the columns left out included, and read back from an
 * import stored before they existed.
 *
 * @author Arnaud Charleroy
 */
public class StoredDecisionsTest {

    private static final URI PLOT = URI.create("http://www.opensilex.org/vocabulary/oeso#Plot");
    private static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");

    @Test
    public void theColumnsLeftOutAreStoredAndReadBack() throws Exception {
        ImportDecisions.SheetDecisions decided = new ImportDecisions.SheetDecisions(PLOT, 0, Map.of(1, COMMENT),
                Map.of(), Set.of(3, 2));
        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(GraphImportSnapshot.StoredDecisions.of("Parcelles", decided));
        ImportDecisions.SheetDecisions read = mapper.readValue(json, GraphImportSnapshot.StoredDecisions.class).toModel();

        assertTrue(json, json.contains("\"left_out\":[2,3]"));
        assertEquals(decided, read);
    }

    @Test
    public void anImportStoredBeforeHasNoColumnLeftOut() throws Exception {
        String stored = "{\"sheet\":\"Parcelles\",\"class_uri\":\"" + PLOT + "\",\"identifier_column\":0,"
                + "\"properties\":{\"1\":\"" + COMMENT + "\"}}";

        ImportDecisions.SheetDecisions read = new ObjectMapper()
                .readValue(stored, GraphImportSnapshot.StoredDecisions.class).toModel();

        assertTrue(read.leftOut().isEmpty());
        assertEquals(COMMENT, read.properties().get(1));
    }

    @Test
    public void valuesGivenForAllRowsAreStoredAndReadBack() throws Exception {
        URI isPartOf = URI.create("http://www.opensilex.org/vocabulary/oeso#isPartOf");
        ImportDecisions.SheetDecisions decided = new ImportDecisions.SheetDecisions(PLOT, 0, Map.of(), Map.of(),
                Set.of(), Map.of(isPartOf, "Bloc A"));
        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(GraphImportSnapshot.StoredDecisions.of("Parcelles", decided));

        assertEquals(decided, mapper.readValue(json, GraphImportSnapshot.StoredDecisions.class).toModel());
    }

    @Test
    public void declaredResourcesAreStoredAndReadBack() throws Exception {
        URI label = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
        ImportDecisions.DeclaredResource resource = new ImportDecisions.DeclaredResource("f1", PLOT,
                Map.of(label, List.of("Bloc A"), COMMENT, List.of("nord", "sec")));
        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(GraphImportSnapshot.StoredDeclared.of(resource));

        assertEquals(resource, mapper.readValue(json, GraphImportSnapshot.StoredDeclared.class).toModel());
    }

    @Test
    public void anImportStoredBeforeHasNoGivenValue() throws Exception {
        String stored = "{\"sheet\":\"Parcelles\",\"class_uri\":\"" + PLOT + "\"}";

        assertTrue(new ObjectMapper().readValue(stored, GraphImportSnapshot.StoredDecisions.class).toModel()
                .given().isEmpty());
    }
}
