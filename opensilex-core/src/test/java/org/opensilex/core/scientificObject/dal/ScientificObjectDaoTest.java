package org.opensilex.core.scientificObject.dal;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests on {@link ScientificObjectModel} at {@link ScientificObjectDAO} levels
 * @author rcolin
 */
public class ScientificObjectDaoTest {

    private static OpenSilexTestEnvironment openSilexTestEnv;
    private static ScientificObjectModel s1, s2,s3;
    private static List<FactorLevelModel> factorLevels;
    private static ScientificObjectDAO dao;

    private static ScientificObjectModel getModel(int i){
        ScientificObjectModel model = new ScientificObjectModel();
        model.setName("dao_test_"+i);

        return model;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {

        openSilexTestEnv = OpenSilexTestEnvironment.getInstance();
        SPARQLService sparql = openSilexTestEnv.getSparql();

        // create factor levels
        FactorLevelModel f1 = new FactorLevelModel();
        f1.setUri(URI.create("test:f1"));
        f1.setName("f1");

        FactorLevelModel f2 = new FactorLevelModel();
        f2.setUri(URI.create("test:f2"));
        f2.setName("f2");

        FactorLevelModel f3 = new FactorLevelModel();
        f3.setUri(URI.create("test:f3"));
        f3.setName("f3");
        sparql.create(FactorLevelModel.class, Arrays.asList(f1, f2, f3));

        // create scientific objects
        s1 = getModel(1);
        s1.setFactorLevels(Arrays.asList(f1, f2));
        s2 = getModel(2);
        s2.setFactorLevels(Arrays.asList(f2, f3));
        s3 = getModel(3);
        s3.setFactorLevels(Arrays.asList(f1, f2, f3));
        sparql.create(ScientificObjectModel.class, Arrays.asList(s1, s2, s3));

        MongoDBService mongo = openSilexTestEnv.getOpenSilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE,MongoDBService.class);
        dao = new ScientificObjectDAO(sparql,mongo);
    }

    @Test
    public void testSearchAll() throws Exception {
        ScientificObjectSearchFilter filter = new ScientificObjectSearchFilter();
        filter.setLang("en");
        List<ScientificObjectModel> objects = dao.search(filter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD)).getList();

        assertNotNull(objects);
        assertEquals(3,objects.size());

        assertModelEquals(objects.get(0),s1);
        assertModelEquals(objects.get(1),s2);
        assertModelEquals(objects.get(2),s3);
    }

    @Test
    public void testSearchSomeUris() throws Exception{

        ScientificObjectSearchFilter filter = new ScientificObjectSearchFilter();
        filter.setLang("en");
        filter.setUris(Arrays.asList(s1.getUri(),s3.getUri()));
        List<ScientificObjectModel> objects = dao.search(filter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD)).getList();

        assertNotNull(objects);
        assertEquals(2,objects.size());

        assertModelEquals(objects.get(0),s1);
        assertModelEquals(objects.get(1),s3);
    }

    private void assertModelEquals(ScientificObjectModel expected, ScientificObjectModel actual){

        assertTrue(SPARQLDeserializers.compareURIs(expected.getUri(),actual.getUri()));

        // assert factor levels equality
        Set<URI> expectedFactorUris = expected.getFactorLevels().stream()
                .map(factor -> SPARQLDeserializers.formatURI(factor.getUri())).collect(Collectors.toSet());

        Set<URI> actualFactorUris = actual.getFactorLevels().stream()
                .map(factor -> SPARQLDeserializers.formatURI(factor.getUri())).collect(Collectors.toSet());
        assertEquals(expectedFactorUris,actualFactorUris);
    }
}
