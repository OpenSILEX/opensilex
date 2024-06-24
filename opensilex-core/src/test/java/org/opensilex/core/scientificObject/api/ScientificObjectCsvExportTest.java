package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.univocity.parsers.csv.CsvParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDFS;
import org.junit.*;
import org.locationtech.jts.io.ParseException;
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

import static org.opensilex.sparql.csv.AbstractCsvImporter.CSV_TYPE_KEY;
import static org.opensilex.sparql.csv.AbstractCsvImporter.CSV_URI_KEY;

import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.export.CsvExporter;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
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
import java.util.stream.Collectors;

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
    private static ScientificObjectDAO dao;
    private static GeospatialDAO geospatialDAO;


    public static final String RDFS_LABEL = URIDeserializer.formatURIAsStr(RDFS.label.getURI());
    public static final String GEOMETRY = URIDeserializer.formatURIAsStr(Oeso.hasGeometry.getURI());

    @BeforeClass
    public static void beforeTest() throws Exception {

        sparql = newSparqlService();
        mongodb = getMongoDBService();

        dao = new ScientificObjectDAO(sparql, mongodb);
        geospatialDAO = new GeospatialDAO(mongodb);

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


    private void assertCSV(List<ScientificObjectModel> models,
                           URI graph,
                           Map<String, Geometry> geometryMap,
                           List<String> expectedColumns,
                           Consumer<String[]> rowAssertion,
                           Map<String, Consumer<String>> assertByProperty
    ) throws IOException, SPARQLException {

        // Export CSV according the provided objects
        CsvExporter<ScientificObjectModel> exporter = new ScientificObjectCsvExporter(
                sparql,
                models,
                graph,
                user.getLanguage(),
                Collections.emptyMap(),
                geometryMap
        );
        byte[] csvRawData = exporter.exportCSV();

        // parse exported CSV
        CsvParser csvParser = new CsvParser(ClassUtils.getCSVParserDefaultSettings());
        List<String[]> rows = csvParser.parseAll(new ByteArrayInputStream(csvRawData));

        // check header
        String[] header = rows.get(0);
        Set<String> headerSet = new HashSet<>(Arrays.asList(header));

        List<String> formattedExpectedColumns = expectedColumns.stream()
                .map(SPARQLDeserializers::formatURI)
                .collect(Collectors.toList());

        Assert.assertTrue(headerSet.containsAll(formattedExpectedColumns));

        Map<String, Consumer<String>> assertByFormattedProperty = assertByProperty.entrySet().stream().collect(
                Collectors.toMap(entry -> SPARQLDeserializers.formatURI(entry.getKey()),Map.Entry::getValue)
        );

        // check body
        List<String[]> body = rows.subList(2, rows.size());
        for (String[] row : body) {
            assertRow(row, header, rowAssertion, assertByFormattedProperty);
        }
    }

    private void assertRow(String[] row, String[] header, Consumer<String[]> rowAssertion, Map<String, Consumer<String>> assertByProperty) {

        rowAssertion.accept(row);

        for (int i = 0; i < row.length; i++) {
            String cell = row[i];
            String column = header[i];
            Consumer<String> relationChecker = assertByProperty.get(column);

            if(relationChecker != null){
                relationChecker.accept(cell);
            }
        }
    }

    private Map<String, Consumer<String>> getDefaultAssertByProperty(List<ScientificObjectModel> models) {

        // check that type, name and geometry are well exported
        Map<String, Consumer<String>> assertByProperty = new HashMap<>();

        // collect models URIs
        Set<String> uriSet = models.stream()
                .map(model -> URIDeserializer.formatURIAsStr(model.getUri().toString()))
                .collect(Collectors.toSet());

        // check that the good OS are exported
        assertByProperty.put(CSV_URI_KEY, uri -> Assert.assertTrue(uriSet.contains(uri)));

        // check that type is OK
        assertByProperty.put(CSV_TYPE_KEY, type -> Assert.assertTrue(SPARQLDeserializers.compareURIs(type, "vocabulary:os_type_export_test")));

        // check that name is OK
        assertByProperty.put(RDFS_LABEL, name -> Assert.assertTrue(name.startsWith("os_export_test")));

        // check that geometry is OK
        assertByProperty.put(GEOMETRY, geometry -> {
            try {
                Geometry parsedGeometry = GeospatialDAO.wktToGeometry(geometry);
                Assert.assertTrue(parsedGeometry instanceof Point);
            } catch (ParseException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        return assertByProperty;
    }

    @Test
    public void testExportGlobal() throws Exception {

        // search objects from global graph
        List<ScientificObjectModel> models = dao.search(new ScientificObjectSearchFilter(), Collections.emptyList()).getList();
        List<URI> uris = models.stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        Map<String, Geometry> geospatialMap = geospatialDAO.getGeometryByUris(null, uris);

        // ensure that only name,type and geometry are exported
        List<String> expectedColumns = Arrays.asList(
                CSV_URI_KEY, CSV_TYPE_KEY, RDFS_LABEL, GEOMETRY
        );
        Consumer<String[]> rowAssertion = (row -> Assert.assertEquals(4, row.length));

        Map<String, Consumer<String>> assertByProperty = getDefaultAssertByProperty(models);

        // export objets from global graph and evaluate assertions
        assertCSV(models, null, geospatialMap, expectedColumns, rowAssertion, assertByProperty);
    }

    @Test
    public void testExportExperiment() throws Exception {

        // search objects from experiment
        List<ScientificObjectModel> models = dao.search(
                new ScientificObjectSearchFilter().setExperiment(experiment.getUri()),
                Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD)
        ).getList();

        List<URI> uris = models.stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        Map<String, Geometry> geospatialMap = geospatialDAO.getGeometryByUris(experiment.getUri(), uris);

        // ensure that only name,type and geometry are exported
        List<String> expectedColumns = Arrays.asList(
                CSV_URI_KEY, CSV_TYPE_KEY,
                RDFS.label.getURI(),
                RDFS.comment.getURI(),
                Oeso.hasCreationDate.getURI(),
                Oeso.hasDestructionDate.getURI(),
                Oeso.isPartOf.getURI(),
                Oeso.hasGermplasm.getURI(),
                Oeso.hasFactorLevel.getURI(),
                "vocabulary:customDataPropExport",
                "vocabulary:customObjectPropExport"
        );
        Consumer<String[]> rowAssertion = (row -> Assert.assertEquals(11, row.length));

        Map<String, Consumer<String>> assertByProperty = getDefaultAssertByProperty(models);

        // check common properties

        // check rdfs:comment
        assertByProperty.put(RDFS.comment.getURI(), comment -> Assert.assertTrue(comment.startsWith("os_export_comment")));

        // check factorLevel
        assertByProperty.put(Oeso.hasFactorLevel.getURI(), factorLevel ->
                Assert.assertEquals("test:id/factor/test_os_csv_import.factor.level1", factorLevel)
        );

        // check multivalued properties
        assertByProperty.put(Oeso.hasGermplasm.getURI(), germplasm -> {
            Set<String> germplasms = Arrays.stream(germplasm.split(" ")).map(
                    SPARQLDeserializers::formatURI
            ).collect(Collectors.toSet());

            Assert.assertTrue(germplasms.contains("test:id/germplasm/germplasm.test_os_csv_export"));
            Assert.assertTrue(germplasms.contains("test:id/germplasm/germplasm.test_os_csv_export-2"));
        });

        // check custom properties
        assertByProperty.put("vocabulary:customDataPropExport ", relation -> Assert.assertEquals("dataProp-1",relation));
        assertByProperty.put("vocabulary:customObjectPropExport ", relation -> Assert.assertEquals("test:id/device/device_test1",relation));

        // export objets from global graph and evaluate assertions
        assertCSV(models, experiment.getUri(), geospatialMap, expectedColumns, rowAssertion, assertByProperty);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        sparql.clearGraph(ScientificObjectModel.class);
        sparql.clearGraph(ExperimentModel.class);
        sparql.clearGraph(GermplasmModel.class);
        sparql.clearGraph(FactorModel.class);
        sparql.clearGraph(FacilityModel.class);

        sparql.clearGraph(experiment.getUri());

        MongoCollection<?> collection = mongodb.getDatabase().getCollection(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME);
        collection.drop();
    }

}
