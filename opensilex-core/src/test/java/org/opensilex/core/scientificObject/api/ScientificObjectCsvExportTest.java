package org.opensilex.core.scientificObject.api;

import com.univocity.parsers.csv.CsvParser;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDFS;
import org.junit.*;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.scientificObject.dal.*;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.export.CsvExporter;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH;
import static org.opensilex.core.scientificObject.api.ScientificObjectAPITest.GERMPLASM_RESTRICTION_ONTOLOGY_PATH;

/**
 * @author rcolin
 * The main control/tests to perform on CSV export are  <br><br>
 * <b>CSV file</b>
 * <ul>
 *     <li>File integrity (not empty or null)</li>
 * </ul>
 *
 * <b>Header</b>
 * <ul>
 *     <li>Header size</ul>
 *     <li>Expected columns/property set</ul>
 * </ul>
 *
 * <b>Body</b>
 *  <ul>
 *      <li>Row size</ul>
 *      <li>Ensure that uri and type are OK</ul>
 *      <li>Ensure that each cell contains the expected value</ul>
 *      <li>Multi valued relation are well encoded (grouped inside a cell/with column repetition)</li>
 *  </ul>
 */
public class ScientificObjectCsvExportTest extends AbstractMongoIntegrationTest {

    private static final Path CSV_FILES_DIR = Paths.get("src", "test", "resources", "scientificObject", "csv", "export");

    public static final String EXPORT_ONTOLOGY_GRAPH = "http://www.opensilex.org/vocabulary/test-os-export#";
    public static final Path EXPORT_ONTOLOGY_PATH = Paths.get("scientificObject", "csv", "export", "export_ontology.owl");


    private static AccountModel user;
    private static ExperimentModel experiment;
    private static SPARQLService sparql;
    private static MongoDBService mongodb;

    @BeforeClass
    public static void beforeTest() throws Exception {

        sparql = newSparqlService();
        mongodb = getMongoDBService();

        experiment = new ExperimentModel();
        experiment.setName("test_os_csv_export");
        experiment.setObjective(experiment.getName());
        experiment.setStartDate(LocalDate.now());
        sparql.create(experiment);

        user = sparql.search(AccountModel.class, null).get(0);
        Objects.requireNonNull(user);

        // create factor and associated levels
        FactorLevelModel factorLevel1 = new FactorLevelModel();
        factorLevel1.setName("level1");
        factorLevel1.setUri(URI.create("test:id/factor/test_os_csv_import.factor.level1"));
        sparql.create(factorLevel1);

        FactorModel factor = new FactorModel();
        factor.setExperiment(experiment);
        factor.setAssociatedExperiments(Collections.singletonList(experiment));
        factor.setName("factor");
        factor.setFactorLevels(Collections.singletonList(factorLevel1));
        sparql.create(factor);

        // load ontology extension used for OS <-> germplasm relation handling
        // indeed this relation is not declared inside opensilex-core package.
        sparql.loadOntology(new URI(GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH),
                OpenSilex.getResourceAsStream(GERMPLASM_RESTRICTION_ONTOLOGY_PATH.toString()), Lang.RDFXML);

        // create germplasms
        GermplasmModel germplasm1 = new GermplasmModel();
        germplasm1.setName("test_os_csv_export");
        germplasm1.setType(URI.create(Oeso.Germplasm.getURI()));
        germplasm1.setUri(URI.create("test:id/germplasm/germplasm.test_os_csv_export"));

        GermplasmModel germplasm2 = new GermplasmModel();
        germplasm2.setName("test_os_csv_export2");
        germplasm2.setType(URI.create(Oeso.Germplasm.getURI()));
        germplasm2.setUri(URI.create("test:id/germplasm/germplasm.test_os_csv_export-2"));
        sparql.create(GermplasmModel.class, Arrays.asList(germplasm1, germplasm2));

        // create a device, in order to ensure that the custom property "customObjectPropExport" from the test ontology is well exported
        DeviceModel device = new DeviceModel();
        device.setName("device_test1");
        device.setUri(URI.create("test:id/device/device_test1"));
        sparql.create(device);

        // load custom ontology extension used for OS export testing
        // this ontology declares the two following properties :
        //      - vocabulary:customDataPropExport
        //      - vocabulary:customObjectPropExport
        sparql.loadOntology(URI.create(EXPORT_ONTOLOGY_GRAPH),
                OpenSilex.getResourceAsStream(EXPORT_ONTOLOGY_PATH.toString()), Lang.RDFXML);

        // load object with CSV import for testing purpose
        ScientificObjectCsvImporter importer = new ScientificObjectCsvImporter(sparql, getMongoDBService(), experiment.getUri(), user);
        File csvFile = CSV_FILES_DIR.resolve("os_export_test_file.csv").toFile();

        CSVValidationModel validation = importer.importCSV(csvFile, false);
        Assert.assertFalse(validation.hasErrors());
        Assert.assertEquals(10, validation.getNbObjectImported());
        Assert.assertEquals(10, sparql.count(ScientificObjectModel.class));
    }


    private void parseCSV(List<ScientificObjectModel> models,
                          List<String> expectedColumns,
                          Consumer<String[]> rowAssertion,
                          Map<String, Consumer<String>> assertByProperty
    ) throws IOException, SPARQLException {

        CsvExporter<ScientificObjectModel> exporter = new ScientificObjectCsvExporter(
                sparql,
                models,
                experiment.getUri(),
                user.getLanguage(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
        byte[] csvRawData = exporter.exportCSV();

        CsvParser csvParser = new CsvParser(ClassUtils.getCSVParserDefaultSettings());
        List<String[]> rows = csvParser.parseAll(new ByteArrayInputStream(csvRawData));

        String[] header = rows.get(0);

        Set<String> headerSet = new HashSet<>(Arrays.asList(header));
        Assert.assertTrue(headerSet.containsAll(expectedColumns));

        List<String[]> body = rows.subList(0, rows.size());
        for (String[] row : body) {
            assertRow(row, header, rowAssertion, assertByProperty);
        }
    }

    private void assertRow(String[] row, String[] header, Consumer<String[]> rowAssertion, Map<String, Consumer<String>> assertByProperty) {

        rowAssertion.accept(row);

        for (int i = 0; i < row.length; i++) {
            String cell = row[i];
            String column = header[i];
            Consumer<String> relationChecker = assertByProperty.get(column);

            Assert.assertNotNull(relationChecker);
            relationChecker.accept(cell);
        }
    }

    @Test
    public void testExportGlobal() throws Exception {

        // export objets from global graph

        // ensure that only name,type and geometry are exported


        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, mongodb);

        List<ScientificObjectModel> models = dao.search(
                new ScientificObjectSearchFilter().setExperiment(experiment.getUri()),
                Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD)
        ).getList();

        Consumer<String[]> rowSizeAssertion = (row -> Assert.assertEquals(4, row.length));

        Map<String, Consumer<String>> assertByProperty = new HashMap<>();
        assertByProperty.put(RDFS.label.getURI(), name -> Assert.assertTrue(name.startsWith("dffd")));

    }

    @Test
    public void testExportCommonProperties() throws Exception {

    }

    @Test
    public void testExportCustomProperties() throws Exception {

    }

    @Test
    public void testExportMultiValuedProperties() throws Exception {

    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(
                ExperimentModel.class,
                FactorLevelModel.class,
                FactorModel.class,
                GermplasmModel.class,
                FacilityModel.class
        );
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Collections.singletonList(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME);
    }

}
