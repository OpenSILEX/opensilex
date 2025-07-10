package org.opensilex.core.data.bll;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.ClientSession;
import org.apache.jena.graph.Node;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensilex.OpenSilex;
import org.opensilex.core.data.api.DataCSVValidationDTO;
import org.opensilex.core.data.bll.dataImport.DataImportLogic;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryDao;
import org.opensilex.core.data.factory.DAOFactory;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryModel;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingFunction;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link DataImportLogic} class.
 * <p>
 * This class validates the functionality of importing CSV data using mocked dependencies.
 * It includes tests for both valid and invalid CSV inputs.
 * </p>
 *
 * <p>
 * Mocked services include:
 * <ul>
 *     <li>MongoDBService for NoSQL database operations</li>
 *     <li>SPARQLService for SPARQL-based triple store interactions</li>
 *     <li>FileStorageService for file handling</li>
 *     <li>DataLogic for business logic related to data operations</li>
 *     <li>DAOFactory for creating Data Access Objects</li>
 * </ul>
 * </p>
 *
 * @author Marouan KOURDI
 * @see DataImportLogic
 * @see DataLogic
 */

public class DataImportLogicTest {

    public static final String INVALID_DATASET_TO_IMPORT_CSV = "data/invalidDatasetToImport.csv";
    public static final String VALID_DATASET_TO_IMPORT_CSV = "data/validDatasetToImport.csv";
    public static final String STANDARD_PROVENANCE = "dev:provenance/standard_provenance";
    public static final String DEV_ID_EXPERIMENT_TEST_EXP = "dev:id/experiment/test_exp";
    public static final String ID_SCIENTIFIC_OBJECT_TEST_SO = "id/scientific-object/so_obj_sc_v103";
    public static final String ID_VARIABLE_TEST_HEIGHT_CM = "http://opensilex.dev/id/variable/plant_height_manual_centimeter";
    @Mock
    private MongoDBService nosql;

    @Mock
    private SPARQLService sparql;

    @Mock
    private FileStorageService fs;

    @Mock
    private AccountModel user;

    @Mock
    private DataLogic dataLogicMock;

    @Mock
    private BatchHistoryDao batchHistoryDao;

    @Mock
    private ProvenanceDaoV2 provenanceDaoV2;

    @Mock
    DeviceDAO deviceDAO;

    @Mock
    VariableDAO variableDAO;

    @Mock
    ExperimentDAO experimentDAO;

    @Mock
    OntologyDAO ontologyDAO;

    @Mock
    ScientificObjectDAO scientificObjectDAO;

    @Mock
    DAOFactory daoFactory;

    @Mock
    MongoDBServiceV2 nosqlV2;

    @Mock
    MongoDatabase mockDatabase;

    private DataImportLogic dataImportLogic;
    URI provenance;
    URI experiment;


    /**
     * Sets up the test environment by initializing mocked dependencies and the DataImportLogic instance.
     *
     * @throws URISyntaxException if an error occurs while creating URIs for testing
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(daoFactory.createDeviceDAO()).thenReturn(deviceDAO);
        when(daoFactory.createVariableDAO()).thenReturn(variableDAO);
        when(daoFactory.createOntologyDAO()).thenReturn(ontologyDAO);
        when(daoFactory.createExperimentDAO()).thenReturn(experimentDAO);
        when(daoFactory.createScientificObjectDAO()).thenReturn(scientificObjectDAO);

        //Mock mongoDBv2
        when(nosql.getServiceV2()).thenReturn(nosqlV2);

        when(nosqlV2.getDatabase()).thenReturn(mockDatabase);

        // Stub startTransaction and commit/rollback
        doNothing().when(sparql).startTransaction();
        when(sparql.hasActiveTransaction()).thenReturn(true);
        doNothing().when(sparql).commitTransaction();
        doNothing().when(sparql).rollbackTransaction(any());

        // Stub computeThrowingTransaction to directly call the lambda
        when(nosqlV2.computeThrowingTransaction(any()))
                .thenAnswer(invocation -> {
                    ThrowingFunction<ClientSession, ?, ?> lambda = invocation.getArgument(0);
                    return lambda.apply(mock(ClientSession.class));
                });

        when(nosqlV2.getGenerationPrefixURI()).thenReturn(URI.create(""));

        dataImportLogic = spy(new DataImportLogic(nosql, sparql, fs, user, dataLogicMock, daoFactory, batchHistoryDao));
        provenance = new URI(STANDARD_PROVENANCE);
        experiment = new URI(DEV_ID_EXPERIMENT_TEST_EXP);
    }

    @Test
    public void testImportCSVDataV2_ValidCSV() throws Exception {
        InputStream file = OpenSilex.getResourceAsStream(VALID_DATASET_TO_IMPORT_CSV);

        // Mock ProvenanceDaoV2 and ProvenanceModel
        ProvenanceModel mockProvenanceModel = getProvenanceModel();
        when(provenanceDaoV2.get(provenance)).thenReturn(mockProvenanceModel);
        doReturn(mockProvenanceModel).when(dataImportLogic).getProvenanceModel(provenance);

        // Mock Variable
        when(variableDAO.get(any(URI.class))).thenReturn(getVariableModel());

        // Mock Scientific object
        when(sparql.getByURI(any(Node.class), eq(ScientificObjectModel.class), any(URI.class), any())).thenReturn(getScientificObjectModel());

        // Mock data logic
        doNothing().when(dataLogicMock).createManyFromImport(anyList(), any(DataCSVValidationModel.class));

        // Mock batch history dao
        doReturn(getMockInsertOneResult()).when(batchHistoryDao).create(any(BatchHistoryModel.class));

        // Methode to test
        DataCSVValidationDTO result = dataImportLogic.importCSVData(provenance, experiment, file, "fileName", null);

        // Assert
        assertNotNull("CSV file input stream is null", file);
        Assert.assertNotNull(result);
        assertTrue(result.getDataErrors().isValidCSV());
        assertTrue(result.getDataErrors().isValidationStep());
        assertTrue(result.getDataErrors().isInsertionStep());
        verify(dataLogicMock, times(1)).createManyFromImport(anyList(), any(DataCSVValidationModel.class));
        verify(batchHistoryDao, times(1)).create(any(BatchHistoryModel.class));
        assertEquals("Number of lines to import", Integer.valueOf(9975), result.getDataErrors().getNbLinesToImport());
    }

    @NotNull
    private InsertOneResult getMockInsertOneResult() {
        return InsertOneResult.acknowledged(new BsonObjectId(new ObjectId("676139f882532f4fcd2867ba")));
    }


    @Test
    public void testImportCSVDataV2_InValidCSV() throws Exception {
        String validationKey = UUID.randomUUID().toString();
        InputStream file = OpenSilex.getResourceAsStream(INVALID_DATASET_TO_IMPORT_CSV);

        // Mock ProvenanceDaoV2 and ProvenanceModel
        ProvenanceModel mockProvenanceModel = getProvenanceModel();
        when(provenanceDaoV2.get(provenance)).thenReturn(mockProvenanceModel);
        doReturn(mockProvenanceModel).when(dataImportLogic).getProvenanceModel(provenance);

        // Mock Variable
        when(variableDAO.get(any(URI.class))).thenReturn(getVariableModel());

        // Mock Scientific object
        when(sparql.getByURI(any(Node.class), eq(ScientificObjectModel.class), any(URI.class), any())).thenReturn(getScientificObjectModel());

        // Mock data logic
        doNothing().when(dataLogicMock).createManyFromImport(anyList(), any(DataCSVValidationModel.class));

        // Methode to test
        DataCSVValidationDTO result = dataImportLogic.importCSVData(provenance, experiment, file, "fileName", validationKey);

        // Assert
        assertNotNull("CSV file input stream is null", file);
        Assert.assertNotNull(result);
        assertFalse(result.getDataErrors().isValidCSV());
        assertTrue(result.getDataErrors().isValidationStep());
        assertEquals("Invalid Date with 1 error", 1, result.getDataErrors().getInvalidDateErrors().size());
    }

    private ScientificObjectModel getScientificObjectModel() {
        ScientificObjectModel scientificObjResourceModel = new ScientificObjectModel();
        URI scientificObjUri = URI.create(ID_SCIENTIFIC_OBJECT_TEST_SO);
        scientificObjResourceModel.setUri(scientificObjUri);
        return scientificObjResourceModel;
    }

    private VariableModel getVariableModel() {
        VariableModel variableModel = new VariableModel();
        URI variableUri = URI.create(ID_VARIABLE_TEST_HEIGHT_CM);
        variableModel.setUri(variableUri);
        return variableModel;
    }

    private ProvenanceModel getProvenanceModel() {
        ProvenanceModel mockProvenanceModel = new ProvenanceModel();
        mockProvenanceModel.setUri(provenance);
        return mockProvenanceModel;
    }
}
