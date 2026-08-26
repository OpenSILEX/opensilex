package org.opensilex.core.ontology.dal;

import org.junit.Test;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link SPARQLRelationFetcher} updateMonoValued and updateMultiValued methods.
 * Uses reflection to bypass the constructor which requires ontology loading.
 *
 * @author OpenSilex
 */
public class SPARQLRelationFetcherTest {

    // -----------------------------------------------------------------------
    // Helper: mock SPARQLResult implementation
    // -----------------------------------------------------------------------

    private static class MockSPARQLResult implements SPARQLResult {

        private final Map<String, String> values = new HashMap<>();

        MockSPARQLResult(String... keyValuePairs) {
            for (int i = 0; i < keyValuePairs.length; i += 2) {
                values.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }

        @Override
        public String getStringValue(String key) {
            return values.get(key);
        }

        @Override
        public void forEach(java.util.function.BiConsumer<? super String, ? super String> action) {
            values.forEach(action);
        }
    }

    // -----------------------------------------------------------------------
    // Helper: create fetcher using reflection to bypass constructor
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <T extends SPARQLResourceModel> SPARQLRelationFetcher<T> createFetcherEmpty(SPARQLService sparql, Class<T> modelClass)
            throws Exception {
        Constructor<?> constructor = SPARQLRelationFetcher.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (SPARQLRelationFetcher<T>) constructor.newInstance();
    }

    // -----------------------------------------------------------------------
    // Helper: reflection utilities to set internal maps
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void setMonoValuedProperties(
            SPARQLRelationFetcher<?> fetcher, URI type, URI propertyUri, String varName)
            throws Exception {
        Field monoByType = SPARQLRelationFetcher.class.getDeclaredField("monoValuedPropertiesByType");
        monoByType.setAccessible(true);
        Map<URI, List<URI>> map = (Map<URI, List<URI>>) monoByType.get(fetcher);
        map.put(type, Collections.singletonList(propertyUri));

        Field monoByTypeVar = SPARQLRelationFetcher.class.getDeclaredField("monoValuedPropertiesByTypeVarNames");
        monoByTypeVar.setAccessible(true);
        Map<URI, List<String>> varMap = (Map<URI, List<String>>) monoByTypeVar.get(fetcher);
        varMap.put(type, Collections.singletonList(varName));
    }

    @SuppressWarnings("unchecked")
    private static void setMultiValuedProperties(
            SPARQLRelationFetcher<?> fetcher, URI type, URI propertyUri, String varName)
            throws Exception {
        Field multiByType = SPARQLRelationFetcher.class.getDeclaredField("multiValuedPropertiesByType");
        multiByType.setAccessible(true);
        Map<URI, List<URI>> map = (Map<URI, List<URI>>) multiByType.get(fetcher);
        map.put(type, Collections.singletonList(propertyUri));

        Field multiByTypeVar = SPARQLRelationFetcher.class.getDeclaredField("multiValuedPropertiesByTypeVarNames");
        multiByTypeVar.setAccessible(true);
        Map<URI, List<String>> varMap = (Map<URI, List<String>>) multiByTypeVar.get(fetcher);
        varMap.put(type, Collections.singletonList(varName));
    }

    // -----------------------------------------------------------------------
    // C1 — updateMonoValued met à jour les relations mono-valuées
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateMonoValued_updatesRelations() throws Exception {
        SPARQLService sparql = mock(SPARQLService.class);

        // Create a mock model
        SPARQLResourceModel model = new SPARQLResourceModel() {
            @Override
            public URI getType() {
                return URI.create("http://test.opensilex.org/TestType");
            }
        };
        model.setUri(URI.create("test:model1"));

        URI testType = URI.create("http://test.opensilex.org/TestType");
        URI testProperty = URI.create("http://test.opensilex.org/hasMonoValue");
        String propertyVarName = "hasMonoValue";

        SPARQLResult result = new MockSPARQLResult(propertyVarName, "testStringValue");

        SPARQLRelationFetcher<SPARQLResourceModel> fetcher = createFetcherEmpty(sparql, SPARQLResourceModel.class);
        setMonoValuedProperties(fetcher, testType, testProperty, propertyVarName);

        fetcher.updateMonoValued(result, model);

        // Then: relations must contain the added relation
        List<SPARQLModelRelation> relations = model.getRelations();
        assertNotNull(relations);
        assertFalse(relations.isEmpty());

        SPARQLModelRelation relation = relations.get(0);
        assertNotNull(relation);
        assertEquals("testStringValue", relation.getValue());
    }

    // -----------------------------------------------------------------------
    // C2 — updateMonoValued retourne silencieusement si type non mappé
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateMonoValued_returnsEarlyWhenTypeNotMapped() throws Exception {
        SPARQLService sparql = mock(SPARQLService.class);

        SPARQLResourceModel model = new SPARQLResourceModel() {
            @Override
            public URI getType() {
                return URI.create("http://test.opensilex.org/UnknownType");
            }
        };
        model.setUri(URI.create("test:model2"));

        SPARQLResult result = new MockSPARQLResult("hasString", "someValue");

        SPARQLRelationFetcher<SPARQLResourceModel> fetcher = createFetcherEmpty(sparql, SPARQLResourceModel.class);
        // Do NOT call setMonoValuedProperties — monoValuedPropertiesByType.get(type) will be null

        // When + Then: no exception, no relations added
        fetcher.updateMonoValued(result, model);

        assertTrue(model.getRelations().isEmpty());
    }

    // -----------------------------------------------------------------------
    // C3 — updateMultiValued met à jour les relations multi-valuées
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateMultiValued_updatesRelationsWithSplitValues() throws Exception {
        SPARQLService sparql = mock(SPARQLService.class);

        SPARQLResourceModel model = new SPARQLResourceModel() {
            @Override
            public URI getType() {
                return URI.create("http://test.opensilex.org/TestType");
            }
        };
        model.setUri(URI.create("test:model3"));

        URI testType = URI.create("http://test.opensilex.org/TestType");
        URI testProperty = URI.create("http://test.opensilex.org/hasMultiValue");
        String propertyVarName = "hasMultiValue";

        String concatenatedValues = "val1,val2,val3";
        String concatVarName = SPARQLQueryHelper.getConcatVarName(propertyVarName);
        SPARQLResult result = new MockSPARQLResult(concatVarName, concatenatedValues);

        SPARQLRelationFetcher<SPARQLResourceModel> fetcher = createFetcherEmpty(sparql, SPARQLResourceModel.class);
        setMultiValuedProperties(fetcher, testType, testProperty, propertyVarName);

        fetcher.updateMultiValued(result, model);

        // Then: 3 relations must have been added
        List<SPARQLModelRelation> relations = model.getRelations();
        assertNotNull(relations);
        assertEquals(3, relations.size());

        Set<String> values = new HashSet<>();
        for (SPARQLModelRelation r : relations) {
            assertNotNull(r);
            values.add(r.getValue());
        }

        assertTrue(values.contains("val1"));
        assertTrue(values.contains("val2"));
        assertTrue(values.contains("val3"));
    }

    // -----------------------------------------------------------------------
    // C4 — updateMultiValued retourne silencieusement si type non mappé
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateMultiValued_returnsEarlyWhenTypeNotMapped() throws Exception {
        SPARQLService sparql = mock(SPARQLService.class);

        SPARQLResourceModel model = new SPARQLResourceModel() {
            @Override
            public URI getType() {
                return URI.create("http://test.opensilex.org/UnknownType");
            }
        };
        model.setUri(URI.create("test:model4"));

        String concatVarName = SPARQLQueryHelper.getConcatVarName("hasMultiValue");
        SPARQLResult result = new MockSPARQLResult(concatVarName, "val1,val2");

        SPARQLRelationFetcher<SPARQLResourceModel> fetcher = createFetcherEmpty(sparql, SPARQLResourceModel.class);
        // Do NOT call setMultiValuedProperties — multiValuedPropertiesByType.get(type) will be null

        // When + Then: no exception, no relations added
        fetcher.updateMultiValued(result, model);

        assertTrue(model.getRelations().isEmpty());
    }
}