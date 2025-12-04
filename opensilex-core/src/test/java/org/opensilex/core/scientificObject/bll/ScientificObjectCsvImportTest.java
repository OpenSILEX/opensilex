package org.opensilex.core.scientificObject.bll;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.riot.Lang;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GENETIC_RESOURCE_RESTRICTION_ONTOLOGY_GRAPH;
import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GENETIC_RESOURCE_RESTRICTION_ONTOLOGY_PATH;

public class ScientificObjectCsvImportTest extends AbstractMongoIntegrationTest {

    private ExperimentModel experiment;
    private ExperimentModel experiment2;
    private static AccountModel user;

    private static final Path CSV_FILES_DIR = Paths.get("src","test","resources","scientificObject","csv");

    @BeforeClass
    public static void setup() throws Exception {
        SPARQLService sparql = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        // create user
        user = new AccountModel();
        user.setUri(URI.create("test:user_test"));
        user.setLanguage("en");
        user.setAdmin(true);

        //Add a custom relation to some OS type for type change testing during bulk update
        addCustomProperty(sparql);
    }

    @Before
    public void beforeEachTest() throws Exception {

        experiment = initExperimentModel("test_os_csv_import");
        experiment2 = initExperimentModel("test_os_csv_import2");
        getSparqlService().create(experiment);
        getSparqlService().create(experiment2);

        // load ontology extension used for OS <-> geneticResource relation handling
        // indeed this relation is not declared inside opensilex-core package.
        getSparqlService().loadOntology(new URI(GENETIC_RESOURCE_RESTRICTION_ONTOLOGY_GRAPH),
                OpenSilex.getResourceAsStream(GENETIC_RESOURCE_RESTRICTION_ONTOLOGY_PATH.toString()), Lang.RDFXML);
    }

    private static ExperimentModel initExperimentModel(String xpName) {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName(xpName);
        experiment.setObjective(experiment.getName());
        experiment.setStartDate(LocalDate.now());
        return experiment;
    }

    private CSVValidationModel testImport(String csvFileName, URI experiment, AccountModel user) throws Exception {

        ScientificObjectCsvImporterLogic importer = new ScientificObjectCsvImporterLogic(getSparqlService(),getMongoDBService(),experiment,user);
        File csvFile = CSV_FILES_DIR.resolve(csvFileName).toFile();
        return importer.importCSV(csvFile,false);
    }

    /**
     * Function to add a custom property to an OS type, used for the testing of bulk update with type changes.
     * For now, it will just add the hardcoded property, add some attributes to make this function dynamic if need be
     */
    private static void addCustomProperty(SPARQLService sparql) throws Exception {
        //Hard coded params
        URI domain = URI.create("vocabulary:Population");
        URI propertyUri = URI.create("vocabulary:someTestProperty");
        String propertyEnglishLabel = "someTestProp";
        URI range = URI.create("xsd:decimal");

        //First stage create a property
        OntologyDAO dao = new OntologyDAO(sparql);
        DatatypePropertyModel propertyModel = new DatatypePropertyModel();
        propertyModel.setUri(propertyUri);
        Map<String, String> propertyLabels = new HashMap<>();
        propertyLabels.put("en", propertyEnglishLabel);
        propertyModel.setLabel(SPARQLLabel.fromMap(propertyLabels));
        ClassModel domainModel = new ClassModel();
        domainModel.setUri(domain);
        propertyModel.setDomain(domainModel);
        propertyModel.setRange(range);
        propertyModel.setPublisher(user.getUri());
        dao.createDataProperty(propertyModel);
        SPARQLModule.getOntologyStoreInstance().reload();

        //Second stage create restriction so the property will be recognized during import
        OwlRestrictionModel restriction = new OwlRestrictionModel();
        restriction.setDomain(domainModel);
        restriction.setOnProperty(propertyUri);
        restriction.setOnDataRange(range);
        //Min amount 0, as property won't be obligatory
        restriction.setMinQualifiedCardinality(0);
        //Max amount 1 as it's not a multivalued property
        restriction.setMaxQualifiedCardinality(1);
        dao.addClassPropertyRestriction(domain, restriction, user.getLanguage());
        SPARQLModule.getOntologyStoreInstance().reload();
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(
                FactorLevelModel.class,
                GeneticResourceModel.class,
                FacilityModel.class,
                ScientificObjectModel.class
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

        validation = testImport("os_reimport.csv", null, user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());
    }

    //To verify that it fails when we try to update type of OS that is present in at least one experiment
    @Test
    public void testUpdateSOTypeInBulkGlobalShouldFail() throws Exception {

        // test by importing a CSV to create a few Scientific Objects in an experiment
        CSVValidationModel validation = testImport("os_import_create.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        //Now validate that it will fail when we try to update some types globally
        validation = testImport("os_reimport_typechange_with_custom_property.csv", null, user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());
        Assert.assertEquals("type", validation.getInvalidValueErrors().values().stream().findFirst().stream().findFirst().get().get(0).getHeader());
    }

    //To verify that it fails when we try to update type of OS that is present in at least one other experiment
    @Test
    public void testUpdateSOTypeInExperimentShouldFail() throws Exception {
        // Start by adding same OS URIs to two different experiments
        //XP 1:
        CSVValidationModel validation = testImport("os_import_create.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());
        //XP 2:
        validation = testImport("os_import_create.csv", experiment2.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        //Now validate that it will fail when we try to update some types in an experiment
        validation = testImport("os_reimport_typechange_with_custom_property.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());
        Assert.assertEquals("type", validation.getInvalidValueErrors().values().stream().findFirst().stream().findFirst().get().get(0).getHeader());
    }

    /**
     * Function to validate that new type and custom relation were taken into account
     * HARD linked to the uri we know we effected test:id/csv_import_os_sample3
     * If we are testing globally after an indirect update because we updated in an XP, then this also verifies
     * that the new rdfs:comment and rdfs:label were not copied.
     *
     * @param contextUri to look for OS in.
     * @param forGlobalIndirect if true then we are testing a global OS that was just indirectly updated because the
     *                          type of OS was updated in an experiment.
     */
    private void validateOSAfterTypeChange(URI contextUri, boolean forGlobalIndirect) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(getSparqlService(), getMongoDBService());
        ScientificObjectModel updatedOS = dao.getObjectByURI(
                URI.create("test:id/csv_import_os_sample3"),
                contextUri,
                "en"
        );
        Assert.assertNotNull(updatedOS);
        Assert.assertTrue(SPARQLDeserializers.compareURIs(updatedOS.getType(), URI.create("vocabulary:Population")));
        Assert.assertTrue(
                updatedOS.getRelations().stream().anyMatch(
                        e -> SPARQLDeserializers.compareURIs(
                                e.getProperty().getURI(),
                                "vocabulary:someTestProperty"
                        )
                )
        );

        //Verify that name and comment were not copied  if for global indirect update, otherwise confirm that they were updated
        if(forGlobalIndirect){
            Assert.assertFalse(
                    updatedOS.getRelations().stream().anyMatch(
                    e->SPARQLDeserializers.compareURIs(e.getProperty().getURI(), URI.create("rdfs:comment"))
                    )
            );
            Assert.assertEquals("Sample3", updatedOS.getName());
        }else{
            Assert.assertTrue(
                    updatedOS.getRelations().stream().anyMatch(
                            e->SPARQLDeserializers.compareURIs(e.getProperty().getURI(), URI.create("rdfs:comment"))
                    )
            );
            Assert.assertEquals("Sample3Renamed", updatedOS.getName());
        }
    }

    /**
     * Similar to the validateOSAfterTypeChange function, now we want to test the opposite
     *
     * @param contextUri
     * @throws Exception
     */
    private void verifyRelationRemovedAfterTypeReChange(URI contextUri) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(getSparqlService(), getMongoDBService());
        ScientificObjectModel updatedOS = dao.getObjectByURI(
                URI.create("test:id/csv_import_os_sample3"),
                contextUri,
                "en"
        );
        Assert.assertNotNull(updatedOS);
        Assert.assertTrue(SPARQLDeserializers.compareURIs(updatedOS.getType(), URI.create("vocabulary:Sample")));
        Assert.assertFalse(
                updatedOS.getRelations().stream().anyMatch(
                        e -> SPARQLDeserializers.compareURIs(
                                e.getProperty().getURI(),
                                "vocabulary:someTestProperty"
                        )
                )
        );
    }

    //Verify that it succeeds when the OS is in 0 experiments
    @Test
    public void testUpdateSOTypeWithCustomRelationsInBulkGlobal() throws Exception {
        // test by importing a CSV to create a few Scientific Objects globally
        CSVValidationModel validation = testImport("os_import_create.csv", null, user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        validation = testImport("os_reimport_typechange_with_custom_property.csv", null, user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        //Validate that new type and relation value are there in one of the updated objects
        validateOSAfterTypeChange(getSparqlService().getDefaultGraphURI(ScientificObjectModel.class), false);
    }

    //Verify that it succeeds when the OS is in only this experiment
    //Tests updating in both directions, so adding a custom relation, then removing it
    @Test
    public void testUpdateSOTypeWithCustomRelationsInExperiment() throws Exception {
        // test by importing a CSV to create a few Scientific Objects globally
        CSVValidationModel validation = testImport("os_import_create.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        validation = testImport("os_reimport_typechange_with_custom_property.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());

        //Validate that new type and relation value are there in one of the updated objects in BOTH global context and in our XP
        validateOSAfterTypeChange(experiment.getUri(), false);
        validateOSAfterTypeChange(getSparqlService().getDefaultGraphURI(ScientificObjectModel.class), true);

        //Finally if we reupdate the type back to what it was before, verify that the custom relation was removed, in Both contexts.
        validation = testImport("os_import_create.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(4, validation.getNbObjectImported());
        verifyRelationRemovedAfterTypeReChange(experiment.getUri());
        verifyRelationRemovedAfterTypeReChange(getSparqlService().getDefaultGraphURI(ScientificObjectModel.class));
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
    public void testHasGeneticResource() throws Exception {

        // create geneticResources
        GeneticResourceModel geneticResource1 = new GeneticResourceModel();
        geneticResource1.setName("test_os_csv_import");
        geneticResource1.setType(URI.create(Oeso.GeneticResource.getURI()));

        GeneticResourceModel geneticResource2 = new GeneticResourceModel();
        geneticResource2.setName("test_os_csv_import2");
        geneticResource2.setType(URI.create(Oeso.GeneticResource.getURI()));

        getSparqlService().create(GeneticResourceModel.class,Arrays.asList(geneticResource1, geneticResource2));

        // test with valid geneticResource
        CSVValidationModel validation = testImport("os_import_geneticResource.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test multiple geneticResource
        validation = testImport("os_import_geneticResource_multiple.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(2, validation.getNbObjectImported());

        // test with unknown geneticResource
        validation = testImport("os_import_geneticResource_unknown.csv", experiment.getUri(), user);
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
