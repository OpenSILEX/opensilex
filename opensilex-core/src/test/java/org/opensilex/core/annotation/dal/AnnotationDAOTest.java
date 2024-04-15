package org.opensilex.core.annotation.dal;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class AnnotationDAOTest extends AbstractMongoIntegrationTest {

    // TODO : there are multiple CRUD tests are missing.

    private static OpenSilexTestEnvironment openSilexTestEnv;
    private static AccountModel user;
    private static AnnotationModel a1, a2, a3;
    private static AnnotationDAO dao;


    @BeforeClass
    public static void beforeClass() throws Exception {

        openSilexTestEnv = OpenSilexTestEnvironment.getInstance();
        SPARQLService sparql = openSilexTestEnv.getSparql();

        // create annotations
        a1 = getAnnotationModel(1);
        a2 = getAnnotationModel(2);
        a3 = getAnnotationModel(3);
        sparql.create(AnnotationModel.class, Arrays.asList(a1, a2, a3));

        // create user
        user = new AccountModel();
        user.setUri(URI.create("test:user_test"));
        user.setLanguage("en");
        user.setAdmin(true);

        dao = new AnnotationDAO(sparql);
    }

    private static AnnotationModel getAnnotationModel(int i) {
        AnnotationModel annotation = new AnnotationModel();
        annotation.setUri(URI.create("test:annotation" + i));
        annotation.setDescription("test create annotation " + i);
        annotation.setPublicationDate(OffsetDateTime.now().minusDays(3));
        annotation.setTargets(new ArrayList<URI>());

        MotivationModel motivation = new MotivationModel();
        motivation.setUri(URI.create("http://www.w3.org/ns/oa#describing"));
        annotation.setMotivation(motivation);

        return annotation;
    }

    @Test
    public void testCreate() throws Exception {
        AnnotationModel annotation = getAnnotationModel(0);

        dao.create(annotation);

        assertModelEquals(annotation, dao.get(annotation.getUri(), user));
    }

    @Test
    public void testCreateList() throws Exception {
        AnnotationModel annotation4 = getAnnotationModel(4);
        AnnotationModel annotation5 = getAnnotationModel(5);
        AnnotationModel annotation6 = getAnnotationModel(6);

        dao.create(Arrays.asList(annotation4, annotation5, annotation6));

        assertModelEquals(annotation4, dao.get(annotation4.getUri(), user));
        assertModelEquals(annotation5, dao.get(annotation5.getUri(), user));
        assertModelEquals(annotation6, dao.get(annotation6.getUri(), user));
    }

    public void testUpdate() {
    }

    public void testDelete() {
    }

    @Test
    public void testGetByUri() throws Exception {
        URI uri = a2.getUri();

        AnnotationModel annotation = dao.get(uri, user);

        assertModelEquals(a2, annotation);
    }

    @Test
    public void testSearchAll() throws Exception {
        List<AnnotationModel> objects =
                dao.search(
                    null,
                    null,
                    null,
                    null,
                    user.getLanguage(),
                    new ArrayList<OrderBy>(),
                    0,
                    20).getList();

        assertNotNull(objects);
        assertEquals(3, objects.size());

        assertModelEquals(a1, objects.get(0));
        assertModelEquals(a2, objects.get(1));
        assertModelEquals(a3, objects.get(2));
    }

    public void testCountAnnotations() {
    }

    public void testSearchMotivations() {
    }

    private void assertModelEquals(AnnotationModel expected, AnnotationModel actual) {

        assertTrue(SPARQLDeserializers.compareURIs(expected.getUri(), actual.getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(expected.getMotivation().getUri(), actual.getMotivation().getUri()));
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getTargets(), actual.getTargets());
    }
}