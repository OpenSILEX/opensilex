package org.opensilex.core.scientificObject.api;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.riot.Lang;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectCsvImporter;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH;
import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GERMPLASM_RESTRICTION_ONTOLOGY_PATH;

public class ScientificObjectCsvImportTest extends AbstractMongoIntegrationTest {

    private ExperimentModel experiment;
    private AccountModel user;

    private static final Path CSV_FILES_DIR = Paths.get("src","test","resources","scientificObject","csv");

    @Before
    public void beforeTest() throws Exception {
        experiment = new ExperimentModel();
        experiment.setName("test_os_csv_import");
        experiment.setObjective(experiment.getName());
        experiment.setStartDate(LocalDate.now());
        getSparqlService().create(experiment);

        user = getSparqlService().search(AccountModel.class,null).get(0);
        Objects.requireNonNull(user);

        // load ontology extension used for OS <-> germplasm relation handling
        // indeed this relation is not declared inside opensilex-core package.
        getSparqlService().loadOntology(new URI(GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH),
                OpenSilex.getResourceAsStream(GERMPLASM_RESTRICTION_ONTOLOGY_PATH.toString()), Lang.RDFXML);
    }

    private CSVValidationModel testImport(String csvFileName, URI experiment, AccountModel user) throws Exception {

        ScientificObjectCsvImporter importer = new ScientificObjectCsvImporter(getSparqlService(),getMongoDBService(),experiment,user);
        File csvFile = CSV_FILES_DIR.resolve(csvFileName).toFile();
        return importer.importCSV(csvFile,false);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(
                FactorLevelModel.class,
                GermplasmModel.class,
                FacilityModel.class
        );
    }

    @Test
    public void testBasicCsv() throws Exception {

        // test with generated uri and type
        CSVValidationModel validation = testImport("os_import_basic.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test with fixed uri and type
        validation = testImport("os_import_basic_with_fixed_uri_and_type.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test with a bad type
        validation = testImport("os_import_basic_fail_with_bad_type.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());

        // test with already existing uri -> reuse previously imported file to update OS in bulk
        validation = testImport("os_import_basic_with_fixed_uri_and_type.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());
    }

    @Test
    public void testBasicCsvEmptyName() throws Exception {
        // test with fixed uri and empty name
        CSVValidationModel validation = testImport("os_import_basic_empty_name_with_uri.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());

        // test with empty uri and empty name
        validation = testImport("os_import_basic_empty_name_empty_uri.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getMissingRequiredValueErrors().isEmpty());
    }

    @Test
    public void testBasicCsvIncorrectURI() throws Exception {
        // test with an incorrect uri
        CSVValidationModel validation = testImport("os_import_basic_with_incorrect_uri.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidURIErrors().isEmpty());
    }

    @Test
    public void testBasicCsvDuplicates() throws Exception {
        // test with duplicate URIs
        CSVValidationModel validation = testImport("os_import_duplicate_uris.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());

        // test with duplicate names
        validation = testImport("os_import_duplicate_names.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());

        // test with duplicate URIs and duplicate names
        validation = testImport("os_import_dup_uris_dup_names.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());
    }

    @Test
    public void testUpdateSOInBulkInExp() throws Exception {
        // test by importing a CSV to create a few Scientific Objects
        CSVValidationModel validation = testImport("os_import_create.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        // in an Experiment
        // test by importing a CSV to create a few Scientific Objects and to update a few existing Scientific Objects
        validation = testImport("os_reimport.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());
    }

    @Test
    public void testUpdateSOInBulkGlobal() throws Exception {
        // test by importing a CSV to create a few Scientific Objects
        CSVValidationModel validation = testImport("os_import_create.csv", null, user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        // globally - not inside an experiment
        // test by importing a CSV to create a few Scientific Objects and to update a few Scientific Objects
        // it shouldn't allow updating existing Scientific objects when we import globally
        validation = testImport("os_reimport.csv", null, user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getAlreadyExistingURIErrors().isEmpty());
    }


    @Test
    public void testSoXpRelation() throws Exception {

        File csvFile = CSV_FILES_DIR.resolve("os_import_so_xp-relation.csv").toFile();
        testImport("os_import_so_xp-relation.csv", experiment.getUri(), user);

        ScientificObjectModel so1 = new ScientificObjectModel();
        ScientificObjectModel so2 = new ScientificObjectModel();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            // Skip first two lines (header)
            br.readLine(); // Skip line 1
            br.readLine(); // Skip line 2
            String[] values = null;

            // Split the line based on the delimiter , and assign to the ScientificObjectModel
            values = br.readLine().split(",");
            so1.setUri(new URI(values[0]));

            values = br.readLine().split(",");
            so2.setUri(new URI(values[0]));

        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean doesSO1ParticipatesInXP = getSparqlService().executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(so1.getUri()), Oeso.participatesIn, SPARQLDeserializers.nodeURI(experiment.getUri())));
        Assert.assertTrue(doesSO1ParticipatesInXP);

        boolean doesSO2ParticipatesInXP = getSparqlService().executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(so2.getUri()), Oeso.participatesIn, SPARQLDeserializers.nodeURI(experiment.getUri())));
        Assert.assertTrue(doesSO2ParticipatesInXP);
    }

    @Test
    public void testHasFactorLevel() throws Exception {

        // create factor and associated levels
        FactorModel factor = new FactorModel();
        factor.setExperiment(experiment);
        factor.setAssociatedExperiments(Collections.singletonList(experiment));
        factor.setName("factor");

        FactorLevelModel level1 = new FactorLevelModel();
        level1.setFactor(factor);
        level1.setName("level1");

        FactorLevelModel level2 = new FactorLevelModel();
        level2.setFactor(factor);
        level2.setName("level2");

        factor.setFactorLevels(Arrays.asList(level1,level2));
        getSparqlService().create(factor);

        experiment.setFactors(Collections.singletonList(factor));

        // test with valid factor level
        CSVValidationModel validation = testImport("os_import_factor_level.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test with factor level outside an experiment
        validation = testImport("os_import_factor_level.csv", null, user);
        Assert.assertEquals(0,validation.getNbObjectImported());
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());

        // test multiple factor level
        validation = testImport("os_import_factor_level_multiple.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test with unknown factor level from experiment
        validation = testImport("os_import_factor_level_unknown.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());

    }


    @Test
    public void testIsHostedInsideExperimentFacilities() throws Exception {

        // create facilities associated to the experiment
        FacilityModel facility1 = new FacilityModel();
        facility1.setName("facility1");

        FacilityModel facility2 = new FacilityModel();
        facility2.setName("facility2");

        List<FacilityModel> facilities = Arrays.asList(facility1,facility2);
        getSparqlService().create(FacilityModel.class,facilities);

        experiment.setFacilities(facilities);
        getSparqlService().update(experiment);

        // test with valid facility
        CSVValidationModel validation = testImport("os_import_facility.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test with facility outside an experiment
        validation = testImport("os_import_facility.csv", null, user);
        Assert.assertEquals(0,validation.getNbObjectImported());
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());

        // test multiple facility -> error
        validation = testImport("os_import_facility_multiple.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());

        // test with unknown facility from experiment
        validation = testImport("os_import_facility_unknown.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());
    }

    @Test
    public void testHasGermplasm() throws Exception {

        // create germplasms
        GermplasmModel germplasm1 = new GermplasmModel();
        germplasm1.setName("test_os_csv_import");
        germplasm1.setType(URI.create(Oeso.Germplasm.getURI()));

        GermplasmModel germplasm2 = new GermplasmModel();
        germplasm2.setName("test_os_csv_import2");
        germplasm2.setType(URI.create(Oeso.Germplasm.getURI()));

        getSparqlService().create(GermplasmModel.class,Arrays.asList(germplasm1, germplasm2));

        // test with valid germplasm
        CSVValidationModel validation = testImport("os_import_germplasm.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test multiple germplasm
        validation = testImport("os_import_germplasm_multiple.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test with unknown germplasm
        validation = testImport("os_import_germplasm_unknown.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());
    }

    @Test
    public void testGeometry() throws Exception {

        // test with invalid geometry
        CSVValidationModel validation = testImport("os_import_invalid_geometry.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());
        Assert.assertEquals(2,validation.getInvalidValueErrors().size());

        // test with valid geometry
        validation = testImport("os_import_geometry.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Collections.singletonList(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME);
    }
}
