package org.opensilex.core.species.dal;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


/**
 * @author Brice MAUSSANG
 */
public class SpeciesDAOTest extends AbstractMongoIntegrationTest {

    private static OpenSilexTestEnvironment openSilexTestEnv;
    private static ExperimentModel expA, expB;
    private static SpeciesModel s1, s2, s3;
    private static SpeciesDAO dao;

    public static SpeciesModel getSpeciesModel(int i) {
        SpeciesModel species = new SpeciesModel();
        species.setUri(URI.create("test:species" + i));
        species.setLabel(new SPARQLLabel("species" + i, "en"));

        return species;
    }

    public static ExperimentModel getExperimentModel(int i) {
        LocalDate currentDate = LocalDate.now();
        ExperimentModel exp = new ExperimentModel();
        exp.setUri(URI.create("test:exp" + i));
        exp.setObjective("");
        exp.setStartDate(currentDate.minusDays(3));

        return exp;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {

        openSilexTestEnv = OpenSilexTestEnvironment.getInstance();
        SPARQLService sparql = openSilexTestEnv.getSparql();

        // create species
        s1 = getSpeciesModel(1);
        s2 = getSpeciesModel(2);
        s3 = getSpeciesModel(3);
        sparql.create(SpeciesModel.class, Arrays.asList(s1, s2, s3));

        // create experiments
        expA = getExperimentModel(1);
        expA.setSpecies(Arrays.asList(s1, s2));
        expB = getExperimentModel(2);
        expB.setSpecies(Arrays.asList(s3));
        sparql.create(ExperimentModel.class, Arrays.asList(expA, expB));

        dao = new SpeciesDAO(sparql);
    }

    @Test
    public void testGetAll() throws Exception {
        List<SpeciesModel> objects = dao.getAll("en");

        assertNotNull(objects);
        assertEquals(3, objects.size());

        assertModelEquals(objects.get(0), s1);
        assertModelEquals(objects.get(1), s2);
        assertModelEquals(objects.get(2), s3);
    }

    @Test
    public void testGetByExperiment() throws Exception {
        List<SpeciesModel> objects = dao.getByExperiment(expA.getUri(), "en");

        assertNotNull(objects);
        assertEquals(2, objects.size());

        assertModelEquals(objects.get(0), s1);
        assertModelEquals(objects.get(1), s2);

        objects = dao.getByExperiment(expB.getUri(), "en");

        assertNotNull(objects);
        assertEquals(1, objects.size());

        assertModelEquals(objects.get(0), s3);
    }

    private void assertModelEquals(SpeciesModel expected, SpeciesModel actual){

        assertTrue(SPARQLDeserializers.compareURIs(expected.getUri(), actual.getUri()));
        assertEquals(expected.getName(), actual.getName());
    }
}