package org.opensilex.core.scientificObject.bll;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.riot.Lang;
import org.geojson.Feature;
import org.geojson.Point;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.MoveSearchFilter;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.api.BaseGermplasmAPITest;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.position.api.PositionGetDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectAPITest;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ListWithPagination;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH;
import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GERMPLASM_RESTRICTION_ONTOLOGY_PATH;

public class ScientificObjectCsvImportTest extends AbstractMongoIntegrationTest {

    private ExperimentModel experiment;
    private ExperimentModel experiment2;
    private ExperimentModel experimentWithFacility;
    private FacilityModel facility1;
    private FacilityModel facility2;
    private FacilityModel facilityInXP;
    private static AccountModel user;

    private static final Path CSV_FILES_DIR = Paths.get("src","test","resources","scientificObject","csv");

    private static FileStorageService fs;

    private static final URI osUriWithLocationWithCoordinates1 = URI.create("test:id/os_import_location_coords_1");
    private static final URI osUriWithLocationWithCoordinates2 = URI.create("test:id/os_import_location_coords_2");
    private static final URI osUriWithLocationXYZ1 = URI.create("test:id/os_import_location_xyz1");
    private static final URI osUriWithLocationXYZ2 = URI.create("test:id/os_import_location_xyz_2");
    private static final URI osUriWithLocationFromToo1 = URI.create("test:id/os_import_location_fromTo1");
    private static final URI osUriWithLocationFromToo2 = URI.create("test:id/os_import_location_fromTo2");
    private static final URI osUriWithNoLocation1 = URI.create("test:id/os_import_no_location1");
    private static final URI osUriWithNoLocation2 = URI.create("test:id/os_import_no_location2");
    private static final URI osUriWithNoLocation3 = URI.create("test:id/os_import_no_location3");
    private static final String facility1Name = "facility1";
    private static final String facility2Name = "facility2";
    private static final String facilityInXPName = "facilityInXP";
    private static final String osWithLocationsImportCsvFilename1 = "os_import_location_create.csv";
    private static final String osWithLocationsImportCsvFilename2 = "os_import_location_new_experiment.csv";

    private static final String TEMPLATE_URI_PLACEHOLDER = "{{uri}}";
    private static final String TEMPLATE_FACILITY_URI_PLACEHOLDER = "{{facility_uri}}";

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() throws Exception {
        SPARQLService sparql = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        fs = getFs();
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

        //Create facilities if they haven't already
        facility1 = new FacilityModel();
        facility1.setName(facility1Name);
        facility2 = new FacilityModel();
        facility2.setName(facility2Name);
        facilityInXP = new FacilityModel();
        facilityInXP.setName(facilityInXPName);
        facility1.setUri(URI.create("test:id/organization/facility.facility1"));
        facility2.setUri(URI.create("test:id/organization/facility.facility2"));
        facilityInXP.setUri(URI.create("test:id/organization/facility.facilityInXP"));
        List<FacilityModel> facilities = Arrays.asList(facility1, facility2, facilityInXP);
        getSparqlService().create(FacilityModel.class,facilities);

        experimentWithFacility = initExperimentModel("experiment_with_facility");
        experimentWithFacility.setFacilities(List.of(facilityInXP));
        getSparqlService().create(experimentWithFacility);

        // load ontology extension used for OS <-> germplasm relation handling
        // indeed this relation is not declared inside opensilex-core package.
        getSparqlService().loadOntology(new URI(GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH),
                OpenSilex.getResourceAsStream(GERMPLASM_RESTRICTION_ONTOLOGY_PATH.toString()), Lang.RDFXML);
    }

    private static ExperimentModel initExperimentModel(String xpName) {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName(xpName);
        experiment.setObjective(experiment.getName());
        experiment.setStartDate(LocalDate.now());
        return experiment;
    }

    private void createGermplasm(String name) throws Exception {
        var germplasmDto = new GermplasmCreationDTO();
        germplasmDto.setName(name);
        germplasmDto.setRdfType(URI.create(Oeso.Species.getURI()));

        new UserCallBuilder(BaseGermplasmAPITest.create)
                .setBody(germplasmDto)
                .buildAdmin()
                .executeCallAndReturnURI();
    }

    private CSVValidationModel testImport(String csvFileName, URI experiment, AccountModel user) throws Exception {
        return testImport(csvFileName, experiment, user, Collections.emptyMap());
    }

    public CSVValidationModel testImport(String csvFileName, URI experiment, AccountModel user, @NotNull Map<String, Object> templatePlaceholderValues) throws Exception {
        ScientificObjectCsvImporterLogic importer = new ScientificObjectCsvImporterLogic(getSparqlService(), getMongoDBService(), experiment, user, fs, null);

        // Create a new temporary file to store and replace the placeholder in the file
        File placeholderCsvFile = CSV_FILES_DIR.resolve(csvFileName).toFile();
        var csvContent = IOUtils.toString(new FileInputStream(placeholderCsvFile), StandardCharsets.UTF_8);
        for (var entry : templatePlaceholderValues.entrySet()) {
            csvContent = csvContent.replace(entry.getKey(), entry.getValue().toString());
        }

        File csvFile = tmpFolder.newFile();
        FileUtils.writeStringToFile(csvFile, csvContent, StandardCharsets.UTF_8);

        return importer.importCSV(csvFile, false);
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
                GermplasmModel.class,
                FacilityModel.class,
                ScientificObjectModel.class,
                MoveModel.class
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
        ScientificObjectDAO dao = new ScientificObjectDAO(getSparqlService());
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
        ScientificObjectDAO dao = new ScientificObjectDAO(getSparqlService());
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

    //Tests an import of OS's with Locations in an Experiment, with OS's that were already created in this Experiment
    // Verifies each update case: delete location, update location, create location
    @Test
    public void testImportUpdateWithLocationsSuccess() throws Exception{

        //Import the OS's with their Locations
        performBasicImportOfOSWithLocations(facility1, facility2, experiment, osWithLocationsImportCsvFilename1);

        //Now import the csv that should update, delete and create at least 1 location of each type,
        //(wkt model, custom coordinates or from/too facility
        CSVValidationModel validation = testImport("os_import_location_update.csv", experiment.getUri(), user);
        //Verify size and no errors
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(9, validation.getNbObjectImported());

        //Fetch moves for verification of values
        ListWithPagination<MoveModel> fetchedMoves = fetchAllLocationTestsMoves();

        //Here if it was equal to 9 then we would know the deletes did not work
        Assert.assertEquals(6, fetchedMoves.getTotal());

        //Verify that the creates of the update operation have correct values
        //verify location content for one of each type (wkt, XYZ and from-too)
        testEachLocationTypeHasCorrectInformation(
                osUriWithNoLocation1,
                osUriWithNoLocation2,
                osUriWithNoLocation3,
                facility1,
                facility2,
                fetchedMoves,
                false,
                false
        );

        //Verify that the updates of the update operation have correct values
        //verify location content for one of each type (wkt, XYZ and from-too)
        testEachLocationTypeHasCorrectInformation(
                osUriWithLocationWithCoordinates1,
                osUriWithLocationXYZ2,
                osUriWithLocationFromToo2,
                facility1,
                facility2,
                fetchedMoves,
                true,
                false
        );
    }

    //Test some case when a Move has been created in between, these Os imports have to be done globally and not in an experiment as
    //The global move we create would be ignored when checking if any existing Moves match
    @Test
    public void testUpdateLocationWhenMultipleMovesExist() throws Exception {
        //Perform first import
        performBasicImportOfOSWithLocations(facility1, facility2, null, osWithLocationsImportCsvFilename1);
        //Create some extra move on one of the imported OSs with some dates
        MoveLogic moveLogic = new MoveLogic(getSparqlService(), getMongoDBService(), user, getFs());
        MoveModel extraMove = new MoveModel();
        extraMove.setIsInstant(Boolean.TRUE);
        InstantModel movesInstantModel = new InstantModel();
        movesInstantModel.setDateTimeStamp(OffsetDateTime.now());
        extraMove.setEnd(movesInstantModel);
        extraMove.setTargets(Collections.singletonList(URI.create("test:id/os_import_location_coords_1")));
        moveLogic.createNoTransaction(extraMove, null);

        //Case No matching dates, should fail as system doesn't know which move to modify
        CSVValidationModel validation = testImport("os_update_to_fail_because_no_dates_match.csv", null, user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertEquals(0, validation.getNbObjectImported());

        //Case 1 move exists with matching dates, we know which move to update so it should pass
        validation = testImport("os_import_location_update.csv", null, user);
        //Verify size and no errors
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(9, validation.getNbObjectImported());
    }


    //Tests importing some OS's with locations in a new Experiment,
    // that were already imported with locations in some other Experiment.
    // Tests creation, the update cases and the error expected cases.
    @Test
    public void testImportWithLocationWithExistingOs() throws Exception{

        //Import OS's in experiment 1
        performBasicImportOfOSWithLocations(facility1, facility2, experiment, osWithLocationsImportCsvFilename1);

        //Import OS's in experiment 2
        performBasicImportOfOSWithLocations(facility1, facility2, experiment2, osWithLocationsImportCsvFilename2);

        //Fetch the supposedly created moves to verify some things
        ListWithPagination<MoveModel> fetchedMoves = fetchAllLocationTestsMoves();

        //Should be 2 times 6 moves
        Assert.assertEquals(12, fetchedMoves.getTotal());
        //verify location content for one of each type (wkt, XYZ and from-too)
        testEachLocationTypeHasCorrectInformation(
                osUriWithLocationWithCoordinates1,
                osUriWithLocationXYZ2,
                osUriWithLocationFromToo2,
                facility1,
                facility2,
                fetchedMoves,
                false,
                true
        );


        //Now import the csv that should update, delete and create at least 1 location of each type,
        //(wkt model, custom coordinates or from/too facility),
        //In the new Experiment
        CSVValidationModel newValidation = testImport("os_import_location_new_experiment_update.csv", experiment2.getUri(), user);

        //Verify size and no errors
        Assert.assertFalse(newValidation.hasErrors());
        Assert.assertEquals(9, newValidation.getNbObjectImported());

        //Refetched moves for verification of values
        fetchedMoves = fetchAllLocationTestsMoves();

        Assert.assertEquals(12, fetchedMoves.getTotal());

        //Verify that the creates of the update operation have correct values
        //verify location content for one of each type (wkt, XYZ and from-too)
        testEachLocationTypeHasCorrectInformation(
                osUriWithNoLocation1,
                osUriWithNoLocation2,
                osUriWithNoLocation3,
                facility1,
                facility2,
                fetchedMoves,
                false,
                true
        );

        //Verify that the updates of the update operation have correct values
        //verify location content for one of each type (wkt, XYZ and from-too)
        testEachLocationTypeHasCorrectInformation(
                osUriWithLocationWithCoordinates1,
                osUriWithLocationXYZ2,
                osUriWithLocationFromToo2,
                facility1,
                facility2,
                fetchedMoves,
                true,
                true
        );
    }

    //Tests that an import of OS's within an experiment, with locations
    // Verifies that the location has correct information after import
    //Also verifies that a single OS with no location thrown in the mix gets created with no location.
    @Test
    public void testImportWithLocationsSuccess() throws Exception{

        performBasicImportOfOSWithLocations(facility1, facility2, experiment, osWithLocationsImportCsvFilename1);

        //Fetch the supposedly created moves to verify some things
        ListWithPagination<MoveModel> fetchedMoves = fetchAllLocationTestsMoves();

        //Verify correct quantity, more than 6 would mean that some OS with no location got given an empty location.
        Assert.assertEquals(6, fetchedMoves.getTotal());
        //verify location content for one of each type (wkt, XYZ and from-too)
        testEachLocationTypeHasCorrectInformation(
                osUriWithLocationWithCoordinates1,
                osUriWithLocationXYZ2,
                osUriWithLocationFromToo2,
                facility1,
                facility2,
                fetchedMoves,
                false,
                false
        );
    }

    /**
     * Performs the same every time import of some Os's with a Location,
     * verifies no errors in csv and correct number of imported items.
     */
    private void performBasicImportOfOSWithLocations(
            FacilityModel facility1,
            FacilityModel facility2,
            ExperimentModel experiment,
            String csvFileName
    ) throws Exception {
        if(experiment != null){
            List<FacilityModel> facilities = Arrays.asList(facility1,facility2);

            experiment.setFacilities(facilities);
            getSparqlService().update(experiment);
        }

        //Perform import and verify right size and no errors
        CSVValidationModel validation = testImport(csvFileName, (experiment != null ?experiment.getUri() : null), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(9, validation.getNbObjectImported());
    }

    private ListWithPagination<MoveModel> fetchAllLocationTestsMoves() throws Exception{
        MoveLogic moveLogic = new MoveLogic(getSparqlService(), getMongoDBService(), user, getFs());
        List<URI> importedOSsURIList = Arrays.asList(
                osUriWithLocationWithCoordinates1,
                osUriWithLocationWithCoordinates2,
                osUriWithLocationXYZ1,
                osUriWithLocationXYZ2,
                osUriWithLocationFromToo1,
                osUriWithLocationFromToo2,
                osUriWithNoLocation1,
                osUriWithNoLocation2,
                osUriWithNoLocation3
        );

        MoveSearchFilter moveSearchFilter = new MoveSearchFilter();
        moveSearchFilter.setTargets(importedOSsURIList);
        moveSearchFilter.setType(URI.create(Oeev.Move.getURI()));
        moveSearchFilter.setPage(0);
        moveSearchFilter.setPageSize(20);
        return moveLogic.searchMovesWithLocationObservation(moveSearchFilter);
    }

    /**
     * Tests presence of an expected one of each type (wkt, custom xyz, or facility from/too
     * With the corresponding expected uris for this test
     *
     * @param osWithWkt uri of OS that has the wkt expected values
     * @param osWithCustomXYZ uri of OS that has the XYZ expected values
     * @param osWithFacilityFromToo uri of OS that has the expected facility values
     * @param facility1 facility1 model used in some Locations
     * @param facility2 facility2 model used in some Locations
     * @param fetchedMoves the fetched Moves that are expected to contain ones that correspond to the URIs
     * @param forUpdate if not for update then expected numeric values are 50, else it's 150. And the facility from/too values are reversed
     */
    private void testEachLocationTypeHasCorrectInformation(
            URI osWithWkt,
            URI osWithCustomXYZ,
            URI osWithFacilityFromToo,
            FacilityModel facility1,
            FacilityModel facility2,
            ListWithPagination<MoveModel> fetchedMoves,
            boolean forUpdate,
            boolean inSecondExperiment
    ){
        //For wkt coordinates I'm just verifying that the geometry object is not null as I haven't understood how to read inner value simply
        Assert.assertTrue(
                fetchedMoves.getList().stream().anyMatch(moveModel -> {
                    //No match if this isn't the uri we're looking for
                    if(!SPARQLDeserializers.compareURIs(moveModel.getTargets().get(0), osWithWkt)){
                        return false;
                    }
                    //no match if any of the location stuff is null or if the expected present geometry is null
                    if(moveModel.getLocationObservation() == null ||
                            moveModel.getLocationObservation().getLocation() == null ||
                            moveModel.getLocationObservation().getLocation().getGeometry() == null){
                        return false;
                    }
                    return true;
                })
        );

        //Verify a custom coordinates one, this one also has textual position
        Assert.assertTrue(
                fetchedMoves.getList().stream().anyMatch(moveModel -> {
                    //No match if this isn't the uri we're looking for
                    if(!SPARQLDeserializers.compareURIs(moveModel.getTargets().get(0), osWithCustomXYZ)){
                        return false;
                    }
                    //no match if any of the location stuff is null or if an expected present coordinate is null
                    if(moveModel.getLocationObservation() == null ||
                            moveModel.getLocationObservation().getLocation() == null ||
                            moveModel.getLocationObservation().getLocation().getX() == null){
                        return false;
                    }
                    //Now return true only if the values match
                    LocationModel foundLocationModel = moveModel.getLocationObservation().getLocation();
                    if(!forUpdate){
                        return foundLocationModel.getX().equals(inSecondExperiment? "30" : "50")
                                && foundLocationModel.getY().equals(inSecondExperiment? "30" : "50")
                                && foundLocationModel.getZ().equals(inSecondExperiment? "30" : "50")
                                && foundLocationModel.getTextualPosition().equals("Brian is in kitchen");
                    }
                    return foundLocationModel.getX().equals(inSecondExperiment ? "130" : "150")
                            && foundLocationModel.getY().equals(inSecondExperiment ? "130" : "150")
                            && foundLocationModel.getZ().equals(inSecondExperiment ? "130" : "150");


                })
        );

        //Verify one with facility information
        Assert.assertTrue(
                fetchedMoves.getList().stream().anyMatch(moveModel -> {
                    //No match if this isn't the uri we're looking for
                    if(!SPARQLDeserializers.compareURIs(moveModel.getTargets().get(0), osWithFacilityFromToo)){
                        return false;
                    }
                    //no match if any of the location stuff is null or if an expected present facility value is null
                    if(moveModel.getLocationObservation() == null ||
                            moveModel.getLocationObservation().getLocation() == null ||
                            moveModel.getLocationObservation().getLocation().getFrom() == null ||
                            moveModel.getLocationObservation().getLocation().getTo() == null){
                        return false;
                    }
                    //Now return true only if the values match
                    LocationModel foundLocationModel = moveModel.getLocationObservation().getLocation();
                    if(!forUpdate && !inSecondExperiment || forUpdate && inSecondExperiment){
                        return
                                SPARQLDeserializers.compareURIs(foundLocationModel.getFrom(), facility1.getUri()) &&
                                        SPARQLDeserializers.compareURIs(foundLocationModel.getTo(), facility2.getUri());
                    }
                    return
                            SPARQLDeserializers.compareURIs(foundLocationModel.getFrom(), facility2.getUri()) &&
                                    SPARQLDeserializers.compareURIs(foundLocationModel.getTo(), facility1.getUri());

                })
        );
    }

    @Test
    public void testHasGermplasm() throws Exception {
        createGermplasm("test_os_csv_import");
        createGermplasm("test_os_csv_import2");

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
    public void testInvalidGeometry() throws Exception {

        // test with invalid geometry
        CSVValidationModel validation = testImport("os_import_location_invalid_geometry.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertFalse(validation.getInvalidValueErrors().isEmpty());
        Assert.assertEquals(2,validation.getInvalidValueErrors().size());

    }

    //#region Compatibility tests - hasGeometry and isHosted columns
    @Test
    public void testCompatibilityHasGeometry() throws Exception {
        CSVValidationModel validation = testImport("compatibility/os_import_compat_has_geometry.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(1, validation.getNbObjectImported());

        var soList = new UserCallBuilder(ScientificObjectAPITest.searchWithGeometry)
                .addParam("experiment", experiment.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {})
                .getDeserializedResponse()
                .getResult();
        Assert.assertEquals(1, soList.size());
        var so = soList.get(0);
        Assert.assertEquals(
                new Point(49, 3),
                ((Feature) so.getLocation().getGeojson()).getGeometry()
        );
    }

    @Test
    public void testCompatibilityIsHosted() throws Exception {
        CSVValidationModel validation = testImport("compatibility/os_import_compat_is_hosted.csv", experimentWithFacility.getUri(), user, Map.of(
                TEMPLATE_FACILITY_URI_PLACEHOLDER, facilityInXP.getUri()
        ));
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(1, validation.getNbObjectImported());

        var soList = new UserCallBuilder(ScientificObjectAPITest.search)
                .addParam("experiment", experimentWithFacility.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {})
                .getDeserializedResponse()
                .getResult();
        Assert.assertEquals(1, soList.size());
        var so = soList.get(0);

        var position = new UserCallBuilder(ScientificObjectAPITest.getPosition)
                .setUriInPath(so.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<PositionGetDTO>>() {})
                .getDeserializedResponse()
                .getResult();
        Assert.assertTrue(SPARQLDeserializers.compareURIs(facilityInXP.getUri(), position.getLocation().getTo()));
    }

    @Test
    public void testCompatibilityHasGeometryWithMoveShouldFail() throws Exception {
        CSVValidationModel validation = testImport("compatibility/os_import_compat_has_geometry_with_move_should_fail.csv", experiment.getUri(), user);
        Assert.assertTrue(validation.hasErrors());
        Assert.assertTrue(validation.getInvalidValueErrors().containsKey(3)); // 3 is the index of hasGeometry column
    }

    @Test
    public void testCompatibilityIsHostedWithMoveShouldFail() throws Exception {
        CSVValidationModel validation = testImport("compatibility/os_import_compat_is_hosted_with_move_should_fail.csv", experimentWithFacility.getUri(), user, Map.of(
                TEMPLATE_FACILITY_URI_PLACEHOLDER, facilityInXP.getUri()
        ));
        Assert.assertTrue(validation.hasErrors());
        Assert.assertTrue(validation.getInvalidValueErrors().containsKey(3)); // 3 is the index of isHosted column
    }

    @Test
    public void testCompatibilityReimportHasGeometryShouldFail() throws Exception {
        CSVValidationModel validation = testImport("compatibility/os_import_compat_has_geometry.csv", experiment.getUri(), user);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(1, validation.getNbObjectImported());


        var soList = new UserCallBuilder(ScientificObjectAPITest.searchWithGeometry)
                .addParam("experiment", experiment.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {})
                .getDeserializedResponse()
                .getResult();
        Assert.assertEquals(1, soList.size());
        var so = soList.get(0);

        CSVValidationModel reimportValidation = testImport("compatibility/os_reimport_compat_has_geometry_should_fail.csv", experiment.getUri(), user, Map.of(
                TEMPLATE_URI_PLACEHOLDER, so.getUri()
        ));
        Assert.assertTrue(reimportValidation.hasErrors());
        Assert.assertTrue(reimportValidation.getInvalidValueErrors().containsKey(3)); // 3 is the index of hasGeometry column
    }

    @Test
    public void testCompatibilityReimportIsHostedShouldFail() throws Exception {
        CSVValidationModel validation = testImport("compatibility/os_import_compat_is_hosted.csv", experimentWithFacility.getUri(), user, Map.of(
                TEMPLATE_FACILITY_URI_PLACEHOLDER, facilityInXP.getUri()
        ));
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(1, validation.getNbObjectImported());

        var soList = new UserCallBuilder(ScientificObjectAPITest.search)
                .addParam("experiment", experimentWithFacility.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {})
                .getDeserializedResponse()
                .getResult();
        Assert.assertEquals(1, soList.size());
        var so = soList.get(0);

        var position = new UserCallBuilder(ScientificObjectAPITest.getPosition)
                .setUriInPath(so.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<PositionGetDTO>>() {})
                .getDeserializedResponse()
                .getResult();
        Assert.assertTrue(SPARQLDeserializers.compareURIs(facilityInXP.getUri(), position.getLocation().getTo()));

        CSVValidationModel reimportValidation = testImport("compatibility/os_reimport_compat_is_hosted_should_fail.csv", experiment.getUri(), user, Map.of(
                TEMPLATE_URI_PLACEHOLDER, so.getUri(),
                TEMPLATE_FACILITY_URI_PLACEHOLDER, facilityInXP.getUri()
        ));
        Assert.assertTrue(reimportValidation.hasErrors());
        Assert.assertTrue(reimportValidation.getInvalidValueErrors().containsKey(3)); // 3 is the index of isHosted column
    }
    //#endregion

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(LocationObservationDAO.LOCATION_COLLECTION_NAME, GeospatialDAO.GEOSPATIAL_COLLECTION_NAME);
    }
}
