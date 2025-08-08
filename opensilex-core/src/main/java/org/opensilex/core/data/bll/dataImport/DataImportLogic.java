package org.opensilex.core.data.bll.dataImport;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.bulk.BulkWriteError;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.OA;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.api.DataCSVValidationDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryDao;
import org.opensilex.core.data.factory.DAOFactory;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.exception.*;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ImportDataIndex;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author MKourdi
 */
public class DataImportLogic {
    protected final static Logger LOGGER = LoggerFactory.getLogger(DataImportLogic.class);
    public static final String DEVICE_ID = "DEVICE_ID";
    public static final String DEVICE_AMBIGUITY_ID = "DEVICE_AMBIGUITY_ID";
    public static final String OBJECT_ID = "OBJECT_ID";
    public static final String OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT = "OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT";
    public static final String TARGET_ID = "TARGET_ID";
    public static final String EXPERIMENT_ID = "EXPERIMENT_ID";
    public static final String UNDERSCORE = "_";
    public static final String DATE_FORMAT = "yyyyMMddHHmmss";
    public static final String EMPTY_CSV_FILE_ERROR_MSG = "No data imported: The CSV file contains no rows to process";
    public static final String ZIP_EXTENSION = ".zip";
    public static final String CSV_EXTENSION = ".csv";
    public static final String ZIP = "zip";
    public static final String VOCABULARY_OESO_IMPORTED_DATASET = "http://www.opensilex.org/vocabulary/oeso#ImportedDataset";
    public static final int BUFFER_SIZE = 8192;
    public static final String TEMP_EXTENSION = ".tmp";
    public static final String TEMP_FILE_PREFIX = "uploaded_csv_";
    public static final int NB_THREADS = 5;


    //Private stored data
    private Map<URI, URI> rootDeviceTypes = null;
    private final String EXPERIMENT_HEADER = "experiment";
    private final String TARGET_HEADER = "target";
    private final String DATE_HEADER = "date";
    private final String DEVICE_HEADER = "device";
    private final String RAW_DATA_HEADER = "raw_data";
    private final String SCIENTIFIC_OBJ_HEADER = "scientific_object";
    private final String ANNOTATION_HEADER = "object_annotation";
    private final MongoDBService nosql;
    private final SPARQLService sparql;
    private final FileStorageService fs;
    private final DAOFactory daoFactory;

    private final AccountModel user;
    private final DataLogic dataLogic;
    private final BatchHistoryDao batchHistoryDao;
    private static final Cache<String, DataCSVValidationModel> csvValidationModelCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    /**
     * Default document name if batch history model has no uri (happens during mocked tests)
     */
    private static final String DEFAULT_DOCUMENT_NAME = "imported_data";

    public DataImportLogic(MongoDBService nosql, SPARQLService sparql, FileStorageService fs, AccountModel user) {
        this.nosql = nosql;
        this.sparql = sparql;
        this.fs = fs;
        this.user = user;
        this.dataLogic = new DataLogic(sparql, nosql, fs, user);
        this.daoFactory = new DAOFactory(sparql, nosql, fs, user);
        this.batchHistoryDao = new BatchHistoryDao(nosql.getServiceV2());


    }

    /**
     * For test purposes only, allows injection of mocked dependencies
     */
    public DataImportLogic(MongoDBService nosql, SPARQLService sparql, FileStorageService fs, AccountModel user, DataLogic dataLogic, DAOFactory daoFactory, BatchHistoryDao batchHistoryDao) {
        this.nosql = nosql;
        this.sparql = sparql;
        this.fs = fs;
        this.user = user;
        this.dataLogic = dataLogic;
        this.daoFactory = daoFactory;
        this.batchHistoryDao = batchHistoryDao;
    }

    /**
     * Imports a CSV file for the given provenanceURI and experimentURI.
     * This function is called after the validation step.
     * It checks if the csv file is valid and if the user has access to the experiment.
     * If the csv file is valid, it inserts the data in the database.
     *
     * @param provenance    the provenance URI
     * @param experiment    the experiment URI
     * @param file          the CSV file to import
     * @param fileName      the name of the CSV file
     * @param validationKey the validation key of the csv file
     * @return a DataCSVValidationDTO containing the status of the import
     * @throws Exception if an error occurs during the import
     */
    public DataCSVValidationDTO importCSVData(URI provenance, URI experiment, InputStream file, String fileName, String validationKey) throws Exception {
        try{
            return new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session -> {
                //Set DataLogic's session so that it knows to not execute more transactions
                dataLogic.setClientSession(session);

                DataCSVValidationModel validation = getValidationDataInCacheBy(validationKey);
                // Create temp file from input stream to reuse it in the validation step and save it after the insertion step in the document system
                File tempFile = createTempFile(file);
                try (FileInputStream tempFileInputStream = new FileInputStream(tempFile)) {
                    validation = importCsvValidationStep(provenance, experiment, tempFileInputStream, fileName, validation);
                    if (validation.isValidCSV()) {
                        importCsvInsertionStep(validationKey, validation, dataLogic);
                        processAndSaveDocument(tempFile, validation);
                    }
                } finally {
                    deleteTempFile(tempFile);
                }
                return buildDataCSVValidationDTO(validation);
            });
        }catch(Exception e){
            throw e;
        }
    }

    /**
     * Processes and saves a document by creating a zip file from the provided temporary file,
     * saving the document with the file, and updating the batch history with the saved document.
     *
     * @param tempFile   the temporary file to be zipped and saved
     * @param validation the validation model containing metadata for processing
     * @throws Exception if an error occurs during file processing or saving
     */
    private void processAndSaveDocument(File tempFile, DataCSVValidationModel validation) throws Exception {
        // create zip file
        File zipFile = createZipFile(tempFile, validation);

        // Save the document file
        DocumentModel savedDocument = saveDocumentWithFile(zipFile, validation);

        // Update batch history
        updateBatchHistoryWithDocument(validation, savedDocument);

        // Delete the temporary zip file
        deleteTempFile(zipFile);
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            if (!tempFile.delete()) {
                LOGGER.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
            } else {
                LOGGER.info("Temporary file deleted: {}", tempFile.getAbsolutePath());
            }
        }
    }

    private File createTempFile(InputStream file) throws IOException {
        File tempFile = File.createTempFile(TEMP_FILE_PREFIX + user.getName(), TEMP_EXTENSION);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            file.transferTo(fos);
        }
        return tempFile;
    }

    private File createZipFile(File fileContent, DataCSVValidationModel validationModel) throws Exception {
        if (fileContent == null) {
            LOGGER.error("File content cannot be null");
            return null;
        }

        String fileName = (validationModel.getBatchHistoryUri() == null ? DEFAULT_DOCUMENT_NAME : validationModel.getBatchHistoryUri().toString());
        File tempZipFile = File.createTempFile(fileName, ZIP_EXTENSION);

        try (FileInputStream fis = new FileInputStream(fileContent);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(tempZipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zipOut = new ZipOutputStream(bos)) {

            // Set compression level for the smallest file size
            zipOut.setLevel(Deflater.BEST_COMPRESSION);

            // Create ZIP entry with original filename
            zipOut.putNextEntry(new ZipEntry(fileName + CSV_EXTENSION));

            // Copy the input stream to the ZIP file
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                zipOut.write(buffer, 0, bytesRead);
            }
            zipOut.closeEntry();
            LOGGER.info("CSV successfully zipped: {}", fileName);
        } catch (IOException e) {
            LOGGER.error("Failed to save file: {}", e.getMessage());
            return null;
        }

        return tempZipFile;
    }

    private DocumentModel saveDocumentWithFile(File tempZipFile, DataCSVValidationModel validationModel) throws Exception {
        if (Objects.isNull(tempZipFile)) {
            LOGGER.error("[saveDocumentWithFile] File cannot be null.");
            return null;
        }
        if (Objects.isNull(validationModel.getBatchHistoryUri())) {
            LOGGER.error("validationModel.getBatchHistoryUri is null. Abandoning document creation.");
            return null;
        }
        DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
        DocumentModel documentModel = new DocumentModel();
        documentModel.setTitle(DEFAULT_DOCUMENT_NAME + "_" + validationModel.getBatchHistoryUri().getSchemeSpecificPart());
        documentModel.setFormat(ZIP);
        documentModel.setPublisher(user.getUri());
        documentModel.setDeprecated("false");
        documentModel.setType(new URI(VOCABULARY_OESO_IMPORTED_DATASET));
        documentModel.setTargets(Collections.singletonList(validationModel.getBatchHistoryUri()));
        documentModel.setDate(new Date().toString());

        DocumentModel savedDocument = documentDAO.createWithFile(documentModel, tempZipFile, false);
        LOGGER.info("Document {} successfully saved.", savedDocument.getTitle());
        return savedDocument;
    }

    private void updateBatchHistoryWithDocument(DataCSVValidationModel validationModel, DocumentModel savedDocument) throws NoSQLInvalidURIException {
        if (Objects.nonNull(savedDocument) && Objects.nonNull(savedDocument.getUri())) {
            BatchHistoryModel batchHistoryModel = batchHistoryDao.get(validationModel.getBatchHistoryUri());
            if (Objects.nonNull(batchHistoryModel)) {
                batchHistoryModel.setDocumentUri(savedDocument.getUri());
                batchHistoryDao.update(batchHistoryModel);
                LOGGER.info("[updateBatchHistoryWithDocument] Batch history {} successfully updated.", validationModel.getBatchHistoryUri());
            } else {
                LOGGER.error("[updateBatchHistoryWithDocument] Batch history {} not found.", validationModel.getBatchHistoryUri());
            }
        } else {
            LOGGER.error("[updateBatchHistoryWithDocument] Document or document URI not found.");
        }
    }

    /**
     * private final DAOFactory daoFactory;
     * <p>
     * Validates the csv for data import after verifying provenance and experiment
     */
    public DataCSVValidationModel validateWholeCsv(URI provenance, URI experiment, InputStream file, String fileName) throws Exception {
        // retrieve provenance model
        ProvenanceModel provenanceModel = getProvenanceModel(provenance);

        // check user access to experiment if not null
        if (experiment != null) {
            new ExperimentDAO(sparql, nosql).validateExperimentAccess(experiment, user);
        }

        //Validate csv file
        DataCSVValidationModel validation = getCsvValidation(experiment, file, provenanceModel, fileName);

        if (!validation.hasErrors()) {
            // save validation data in cache
            putValidationDataInCache(validation);
        }

        return validation;
    }

    /**
     * This function performs the insertion step of the csv import.
     * It takes a DataCSVValidationModel as argument and inserts the data in the database if the model is valid.
     *
     * @param validationKey the validation key of the csv file
     * @param validation    the DataCSVValidationModel to insert
     * @throws Exception if an error occurs during the insertion
     */
    private void importCsvInsertionStep(
            String validationKey,
            DataCSVValidationModel validation,
            DataLogic dataLogicWithSession
    ) throws Exception {
        handleDataInsertion(validation, dataLogicWithSession);
        removeValidationDataInCacheBy(validationKey);
    }

    private void removeValidationDataInCacheBy(String key) {
        if (StringUtils.isNotBlank(key)) {
            csvValidationModelCache.invalidate(key);
        }
    }

    private DataCSVValidationModel importCsvValidationStep(URI provenance, URI experiment, InputStream file, String fileName, DataCSVValidationModel validation) throws Exception {
        if (Objects.isNull(validation)) {
            LOGGER.debug("[importCsvValidationStep] Start validation step.");
            validation = validateWholeCsv(provenance, experiment, file, fileName);
            validation.setValidationStep(true);
            LOGGER.debug("[importCsvValidationStep] End validation step with status valid {}", validation.isValidCSV());
        }
        return validation;
    }

    private DataCSVValidationModel getValidationDataInCacheBy(String key) {
        return StringUtils.isNotBlank(key) ? csvValidationModelCache.getIfPresent(key) : null;
    }

    private DataCSVValidationModel getCsvValidation(URI experiment, InputStream file, ProvenanceModel provenanceModel, String fileName) throws Exception {
        DataCSVValidationModel validation;
        // Validate the whole csv file containing data
        validation = validateWholeCSVInnerCodeV2(provenanceModel, experiment, file, fileName);

        // Set DataCSVValidationModel attributes
        validation.setValidCSV(!validation.hasErrors());
        validation.setValidationStep(true);

        return validation;
    }

    private void putValidationDataInCache(DataCSVValidationModel validation) {
        // Set generate and set validationKey for whole csv into the cache
        String validationKey = generateValidationKey();
        validation.setValidationKey(validationKey);
        csvValidationModelCache.put(validationKey, validation);
    }

    public ProvenanceModel getProvenanceModel(URI provenance) {
        ProvenanceModel provenanceModel;
        try {
            provenanceModel = new ProvenanceDaoV2(nosql.getServiceV2()).get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        }
        return provenanceModel;
    }

    /**
     * Does the actual validating once we have loaded our provenance
     */
    private DataCSVValidationModel validateWholeCSVInnerCodeV2(ProvenanceModel provenance, URI experiment, InputStream file, String fileName) throws Exception {
        // create dao for each context to access data from database
        DeviceDAO deviceDAO = daoFactory.createDeviceDAO();
        VariableDAO variableDAO = daoFactory.createVariableDAO();
        ExperimentDAO experimentDAO = daoFactory.createExperimentDAO();
        OntologyDAO ontologyDAO = daoFactory.createOntologyDAO();
        ScientificObjectDAO scientificObjectDAO = daoFactory.createScientificObjectDAO();

        // create CSVValidationModel object to return data after validation
        DataCSVValidationModel csvValidation = new DataCSVValidationModel();
        csvValidation.setFileName(fileName);
        Map<Integer, String> headerByIndex = new HashMap<>();

        List<String[]> allRows = new ArrayList<>();

        // prepare each context with own attributes
        ExperimentContext experimentContext = ExperimentContext.buildExperimentContext(experiment);
        DeviceContext deviceContext = new DeviceContext();
        TargetContext targetContext = new TargetContext();
        DAOContext daoContext = DAOContext.buildDaoContext(deviceDAO, ontologyDAO, experimentDAO, scientificObjectDAO, variableDAO);

        // checking device type for agent
        boolean sensingDeviceFoundFromProvenance = isSensingDeviceFoundFromProvenance(provenance, deviceDAO);

        // Start csv processing
        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8)) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);

            LOGGER.debug("[validateWholeCSVInnerCodeV2] Import data - CSV format => \n '{}'", csvReader.getDetectedFormat());

            // Process headers
            String[] ids = csvReader.parseNext();
            validateAndProcessHeaders(ids, sensingDeviceFoundFromProvenance, csvValidation, headerByIndex, deviceContext, variableDAO);

            // Check for errors after headers validation
            if (csvValidation.hasErrors()) {
                return csvValidation;
            }

            // Retrieve headers labels
            setHeadersLabels(csvValidation, ids, csvReader);

            // SKip line 3
            csvReader.parseNext();

            // Line 4 start rows validation
            String[] values;
            while ((values = csvReader.parseNext()) != null) {
                allRows.add(values);
            }

            // check csv file contains values to process
            if (allRows.isEmpty()) {
                csvValidation.setValidCSV(false);
                csvValidation.setNbLinesToImport(0);
                csvValidation.setErrorMessage(EMPTY_CSV_FILE_ERROR_MSG);
                return csvValidation;
            }

            // Check for too large dataset
            if (allRows.size() > DataAPI.SIZE_MAX) {
                csvValidation.setValidCSV(false);
                csvValidation.setTooLargeDataset(true);
                csvValidation.setNbLinesToImport(allRows.size());
                return csvValidation;
            }
        }

        // start rows processing in parallel
        validateCSVRowsInParallel(provenance, allRows, sensingDeviceFoundFromProvenance,
                headerByIndex, experimentContext, targetContext, deviceContext, daoContext, csvValidation);

        // Set nb lines to import
        csvValidation.setNbLinesToImport(csvValidation.getData().size());

        return csvValidation;
    }

    private void setHeadersLabels(DataCSVValidationModel csvValidation, String[] ids, CsvParser csvReader) {
        csvValidation.setHeadersFromArray(ids);

        // Process headers labels
        csvValidation.setHeadersLabelsFromArray(csvReader.parseNext());
    }

    private void validateAndProcessHeaders(String[] ids, boolean sensingDeviceFoundFromProvenance, DataCSVValidationModel csvValidation, Map<Integer, String> headerByIndex, DeviceContext deviceContext, VariableDAO variableDAO) {
        Set<String> headers = getHeadersFrom(ids);

        // Validate headers
        validateHeaders(headers, sensingDeviceFoundFromProvenance, csvValidation);

        // Process headers values
        processHeadersValue(ids, headerByIndex, deviceContext, csvValidation, variableDAO);
    }

    private boolean isSensingDeviceFoundFromProvenance(ProvenanceModel provenance, DeviceDAO deviceDAO) {
        return provenance.getAgents() != null
                && provenance.getAgents().stream()
                .anyMatch(agent -> {
                    try {
                        return (agent.getRdfType() != null) && deviceDAO.isDeviceType(agent.getRdfType());
                    } catch (SPARQLException e) {
                        LOGGER.error("Error while checking device type for agent: {}", agent, e);
                        return false;
                    }
                });
    }


    private Set<String> getHeadersFrom(String[] ids) {
        return Arrays.stream(ids).filter(Objects::nonNull)
                .map(id -> id.toLowerCase(Locale.ENGLISH))
                .collect(Collectors.toSet());
    }


    /**
     * Validates rows of a CSV file in parallel using a multithreaded approach.
     * This method divides the rows into batches, processes each batch concurrently,
     * and merges the results into a shared {@code DataCSVValidationModel} instance.
     *
     * <p>This method is designed for efficiency and scalability, particularly when working with large datasets.
     * It uses a fixed thread pool executor to manage concurrent tasks and ensures proper merging of validation
     * results across threads.</p>
     *
     * @param provenance                       the {@code ProvenanceModel} containing metadata related to the provenance of the data.
     * @param allRows                          a list of all rows from the CSV file, where each row is represented as a {@code String[]} array.
     * @param sensingDeviceFoundFromProvenance a boolean indicating if the sensing device is derived from the provenance data.
     * @param headerByIndex                    a map of column indexes to their corresponding header names.
     * @param experimentContext                the context for validating experiment-related data.
     * @param targetContext                    the context for validating target-related data.
     * @param deviceContext                    the context for validating device-related data.
     * @param daoContext                       the DAO context for database-related operations.
     * @param csvValidation                    the {@code DataCSVValidationModel} to which validation results will be merged.
     * @throws InterruptedException if thread execution is interrupted while awaiting termination.
     */
    private void validateCSVRowsInParallel(ProvenanceModel provenance, List<String[]> allRows,
                                           boolean sensingDeviceFoundFromProvenance, Map<Integer, String> headerByIndex,
                                           ExperimentContext experimentContext, TargetContext targetContext, DeviceContext deviceContext,
                                           DAOContext daoContext, DataCSVValidationModel csvValidation) throws InterruptedException {

        List<Future<DataCSVValidationModel>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        int totalRows = allRows.size();
        int batchSize = determineBatchSize(totalRows);
        int numberOfBatches = (int) Math.ceil((double) totalRows / batchSize);
        AtomicInteger nbError = new AtomicInteger();
        AtomicBoolean stopProcessing = new AtomicBoolean(false);


        try {
            // Process each batch in a separate thread
            for (int i = 0; i < numberOfBatches && !stopProcessing.get(); i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, totalRows);
                List<String[]> batch = allRows.subList(start, end);

                futures.add(executor.submit(() ->
                        processBatch(provenance, sensingDeviceFoundFromProvenance, headerByIndex,
                                experimentContext, targetContext, deviceContext, daoContext, start, batch, stopProcessing, nbError, csvValidation)));
            }

            // Collect results from all threads
            for (Future<DataCSVValidationModel> future : futures) {
                try {
                    // Merge results into the final model
                    csvValidation.merge(future.get());
                } catch (InterruptedException e) {
                    LOGGER.error("Thread execution failed: ", e);
                } catch (ExecutionException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Thread interrupted: ", e);
                }
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)
            ) {
                executor.shutdownNow();
            }
        }
    }

    private int determineBatchSize(int totalRows) {
        return Math.max(200, totalRows / NB_THREADS);
    }


    private DataCSVValidationModel processBatch(
            ProvenanceModel provenance,
            boolean sensingDeviceFoundFromProvenance,
            Map<Integer, String> headerByIndex,
            ExperimentContext experimentContext,
            TargetContext targetContext,
            DeviceContext deviceContext,
            DAOContext daoContext,
            int start,
            List<String[]> batch,
            AtomicBoolean stopProcessing,
            AtomicInteger nbError,
            DataCSVValidationModel csvValidation
    ) throws Exception {
        // foreach thread we create a localValidation to collect (data, errors)
        DataCSVValidationModel localValidation = new DataCSVValidationModel();
        ValidationContext validationContext = null;
        int localRowIndex = start;

        // Start row validation
        for (String[] row : batch) {
            if (stopProcessing.get()) {
                // Early termination of threads when the nbError limit is reached by one of the threads
                break;
            }
            // Initialize the validation context for each row validation
            validationContext = new ValidationContext(
                    provenance,
                    row,
                    localRowIndex,
                    headerByIndex,
                    experimentContext,
                    targetContext,
                    deviceContext,
                    daoContext,
                    localValidation,
                    csvValidation,
                    sensingDeviceFoundFromProvenance
            );
            try {
                boolean isValid = validateCSVRowV2(validationContext);
                if (!isValid) {
                    nbError.getAndIncrement();
                }

                if (nbError.get() >= ExperimentAPI.CSV_NB_ERRORS_MAX) {
                    // Trigger the Stop validation process
                    stopProcessing.set(true);
                    break;
                }
            } catch (CSVDataTypeException e) {
                validationContext.getLocalCsvValidation().addInvalidDataTypeError(e.getCsvCell());
            }
            localRowIndex++;
        }

        return validationContext != null ? validationContext.getLocalCsvValidation() : localValidation;
    }


    private void validateHeaders(Set<String> headers, boolean sensingDeviceFound, DataCSVValidationModel csvValidation) {
        if (!headers.contains(DEVICE_HEADER) && !headers.contains(TARGET_HEADER)
                && !headers.contains(SCIENTIFIC_OBJ_HEADER) && !sensingDeviceFound) {
            csvValidation.addMissingHeaders(List.of(DEVICE_HEADER + " or " + TARGET_HEADER + " or " + SCIENTIFIC_OBJ_HEADER));
        }
        if (headers.contains(ANNOTATION_HEADER) && !headers.contains(TARGET_HEADER) && !headers.contains(SCIENTIFIC_OBJ_HEADER)) {
            csvValidation.addMissingHeaders(List.of(TARGET_HEADER + " or " + SCIENTIFIC_OBJ_HEADER));
        }
    }

    private void processHeadersValue(String[] ids, Map<Integer, String> headerByIndex, DeviceContext deviceContext,
                                     DataCSVValidationModel csvValidation, VariableDAO variableDAO) {
        for (int i = 0; i < ids.length; i++) {
            String header = ids[i];
            if (header == null) {
                csvValidation.addEmptyHeader(i + 1);
                continue;
            }
            if (isFixedHeader(header)) {
                headerByIndex.put(i, header);
            } else {
                try {
                    URI uri = URI.create(header);
                    if (!URIDeserializer.validateURI(header)) {
                        csvValidation.addInvalidHeaderURI(i, header);
                    } else {
                        VariableModel var = variableDAO.get(uri);
                        if (var == null) {
                            csvValidation.addInvalidHeaderURI(i, header);
                        } else {
                            deviceContext.getMapVariableUriDataType().put(var.getUri(), var.getDataType());
                            headerByIndex.put(i, header);
                        }
                    }
                } catch (Exception e) {
                    csvValidation.addInvalidHeaderURI(i, ids[i]);
                }
            }
        }
    }

    private boolean isFixedHeader(String header) {
        return Arrays.asList(EXPERIMENT_HEADER, TARGET_HEADER, DATE_HEADER, DEVICE_HEADER, SCIENTIFIC_OBJ_HEADER, RAW_DATA_HEADER, ANNOTATION_HEADER)
                .contains(header.toLowerCase(Locale.ENGLISH));
    }

    private boolean validateCSVRowV2(ValidationContext validationContext) throws Exception {
        LOGGER.debug("[validateCSVRowV2] Validating row {}", validationContext.getRowIndex());
        ExperimentContext experimentContext = validationContext.getExperimentContext();
        // Validate row step by step
        if (experimentContext.getExperiment() != null) {
            validationContext.getExperiments().add(experimentContext.getExperiment());
        }

        // Validate each column of the row
        validateColumnsRow(validationContext);

        //Do the variable value columns now that we know the target or device is loaded if the user correctly filled it
        processVariableAndDataModelColumns(validationContext);

        // If an AnnotationModel was created on this row as well as a target, we need to set the Annotation's target
        processAnnotations(validationContext);

        //the device or the target is mandatory if there is no device in the provenance
        handleMissingTargetOrDevice(validationContext);

        return validationContext.isValidRow();
    }

    private void validateColumnsRow(ValidationContext context) throws Exception {
        for (int colIndex = 0; colIndex < context.getValues().length; colIndex++) {
            String headerName = context.getHeaderByIndex().get(colIndex);
            LOGGER.debug("[validateColumnsRow] start validating column : {} , value : {}", headerName, context.getValues()[colIndex]);
            if (headerName.equalsIgnoreCase(EXPERIMENT_HEADER)) {
                validateExperimentColumn(context, colIndex);
            } else if (headerName.equalsIgnoreCase(TARGET_HEADER)) {
                validateTargetColumn(context, colIndex);
            } else if (headerName.equalsIgnoreCase(SCIENTIFIC_OBJ_HEADER)) {
                validateScientificObjectColumn(context, colIndex);
            } else if (headerName.equalsIgnoreCase(DATE_HEADER)) {
                validateDateColumn(context, colIndex);
            } else if (headerName.equalsIgnoreCase(DEVICE_HEADER)) {
                validateDeviceColumn(context, colIndex);
            } else if (headerName.equalsIgnoreCase(ANNOTATION_HEADER)) {
                processAnnotationColumn(context, colIndex);
            } else if (!headerName.equalsIgnoreCase(RAW_DATA_HEADER)) {
                handleVariableColumn(context, colIndex);
            }
        }
        LOGGER.debug("[validateColumnsRow] end validating columns");
    }

    private void handleMissingTargetOrDevice(ValidationContext validationContext) {
        DataCSVValidationModel localCsvValidation = validationContext.getLocalCsvValidation();
        if (validationContext.isMissingTargetOrDevice()) {
            //the device or the target is mandatory if there is no device in the provenance
            CSVCell cell1 = new CSVCell(validationContext.getRowIndex(), validationContext.getDeviceColIndex(), null, DEVICE_HEADER);
            CSVCell cell2 = new CSVCell(validationContext.getRowIndex(), validationContext.getTargetColIndex(), null, TARGET_HEADER);
            localCsvValidation.addMissingRequiredValue(cell1);
            localCsvValidation.addMissingRequiredValue(cell2);
        }
    }

    private void processAnnotations(ValidationContext validationContext) {
        AnnotationModel annotationFromAnnotationColumn = validationContext.getAnnotationFromAnnotationColumn();
        DataCSVValidationModel localCsvValidation = validationContext.getLocalCsvValidation();
        SPARQLNamedResourceModel target = validationContext.getTarget();
        SPARQLNamedResourceModel object = validationContext.getObject();

        if (validationContext.getAnnotationFromAnnotationColumn() != null) {
            if (target == null && object == null) {
                CSVCell annotationCell = new CSVCell(validationContext.getRowIndex(), validationContext.getAnnotationIndex(), annotationFromAnnotationColumn.getDescription(), ANNOTATION_HEADER);
                localCsvValidation.addInvalidAnnotationError(annotationCell);
                validationContext.setValidRow(false);
            } else {
                if (validationContext.isValidRow()) {
                    annotationFromAnnotationColumn.setTargets(Collections.singletonList(target == null ? object.getUri() : target.getUri()));
                    localCsvValidation.addToAnnotationsOnObjects(annotationFromAnnotationColumn);
                }
            }
        }
    }


    private void processVariableAndDataModelColumns(ValidationContext context) throws Exception {
        for (Integer colIndex : context.getColsToDoAtEnd()) {
            if (!context.isValidRow()) {
                break;
            }
            processVariableColumn(context, colIndex);
            processDataModelColumn(context, colIndex);
        }
    }

    private void processDataModelColumn(ValidationContext context, Integer colIndex) throws Exception {
        if (context.isValidRow()) {
            String variable = context.getHeaderByIndex().get(colIndex);
            URI varURI = URI.create(variable);
            DataModel dataModel = createDataModel(context, colIndex, varURI);
            checkAndAddData(dataModel, context, colIndex, varURI);
        }
    }

    private void checkAndAddData(DataModel dataModel, ValidationContext validationContext, int colIndex, URI varURI) {
        SPARQLNamedResourceModel target = validationContext.getTarget();
        SPARQLNamedResourceModel object = validationContext.getObject();
        DataCSVValidationModel localCsvValidation = validationContext.getLocalCsvValidation();
        DeviceContext deviceContext = validationContext.getDeviceContext();
        URI targetUri = null;
        URI deviceUri = null;
        if (target != null) {
            targetUri = target.getUri();
        }
        if (object != null) {
            targetUri = object.getUri();
        }
        if (validationContext.getDeviceFromDeviceColumn() != null) {
            deviceUri = validationContext.getDeviceFromDeviceColumn().getUri();
        }
        ImportDataIndex importDataIndex = new ImportDataIndex(validationContext.getParsedDateTimeMongo().getInstant(), varURI, validationContext.getProvenance().getUri(), targetUri, deviceUri);
        if (!deviceContext.getDuplicateDataByIndex().contains(importDataIndex)) {
            deviceContext.getDuplicateDataByIndex().add(importDataIndex);
        } else {
            String variableName = validationContext.getCsvValidation().getHeadersLabels().get(colIndex) + '(' + validationContext.getCsvValidation().getHeaders().get(colIndex) + ')';
            CSVCell duplicateCell = new CSVCell(validationContext.getRowIndex(), colIndex, validationContext.getValues()[colIndex].trim(), variableName);
            localCsvValidation.addDuplicatedDataError(duplicateCell);
        }
        localCsvValidation.addData(dataModel, validationContext.getRowIndex());
    }

    private DataModel createDataModel(ValidationContext validationContext, Integer colIndex, URI varURI) throws Exception {
        DeviceContext deviceContext = validationContext.getDeviceContext();
        List<URI> experiments = validationContext.getExperiments();
        DeviceModel deviceFromDeviceColumn = validationContext.getDeviceFromDeviceColumn();
        String[] values = validationContext.getValues();
        DataModel dataModel = new DataModel();
        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(validationContext.getProvenance().getUri());
        String variable = validationContext.getHeaderByIndex().get(colIndex);

        if (!experiments.isEmpty()) {
            provenanceModel.setExperiments(experiments);
        }

        if (deviceFromDeviceColumn != null) {
            ProvEntityModel agent = createProvEntityModel(validationContext.getDaoContext(), rootDeviceTypes, deviceFromDeviceColumn);
            provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));
        } else if (validationContext.isSensingDeviceFoundFromProvenance()) {
            DeviceModel checkedDevice = validationContext.getDeviceContext().getVariableCheckedProvDevice().get(variable);
            ProvEntityModel agent = createProvEntityModel(validationContext.getDaoContext(), rootDeviceTypes, checkedDevice);
            provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));
        }

        // Get parsed date
        ParsedDateTimeMongo parsedDateTimeMongo = validationContext.getParsedDateTimeMongo();
        dataModel.setDate(parsedDateTimeMongo.getInstant());
        dataModel.setOffset(parsedDateTimeMongo.getOffset());
        dataModel.setIsDateTime(parsedDateTimeMongo.getIsDateTime());

        if (validationContext.getObject() != null) {
            dataModel.setTarget(validationContext.getObject().getUri());
        }
        if (validationContext.getTarget() != null) {
            dataModel.setTarget(validationContext.getTarget().getUri());
        }
        dataModel.setProvenance(provenanceModel);
        dataModel.setVariable(varURI);
        DataValidateUtils.checkAndConvertValue(dataModel, varURI, values[colIndex].trim(), deviceContext.getMapVariableUriDataType().get(varURI), validationContext.getRowIndex(), colIndex, validationContext.getCsvValidation());

        if (colIndex + 1 < values.length) {
            if (validationContext.getHeaderByIndex().get(colIndex + 1).equalsIgnoreCase(RAW_DATA_HEADER) && values[colIndex + 1] != null) {
                dataModel.setRawData(DataValidateUtils.returnValidRawData(varURI, values[colIndex + 1].trim(), deviceContext.getMapVariableUriDataType().get(varURI), validationContext.getRowIndex(), colIndex + 1, validationContext.getCsvValidation()));
            }
        }

        return dataModel;
    }

    private ProvEntityModel createProvEntityModel(DAOContext daoContext, Map<URI, URI> rootDeviceTypes, DeviceModel device) throws Exception {
        ProvEntityModel agent = new ProvEntityModel();
        if (rootDeviceTypes == null) {
            rootDeviceTypes = daoContext.getDeviceDAO().getRootDeviceTypes(user);
        }
        URI rootType = rootDeviceTypes.get(device.getType());
        agent.setType(rootType);
        agent.setUri(device.getUri());
        return agent;
    }

    private void processVariableColumn(ValidationContext context, Integer colIndex) throws Exception {
        String variable = context.getHeaderByIndex().get(colIndex);
        URI varURI = URI.create(variable);

        // Check if target or device is present
        if (context.getDeviceFromDeviceColumn() == null &&
                context.getTarget() == null &&
                context.getObject() == null) {
            context.setMissingTargetOrDevice(true);
            context.setValidRow(false);
            return;
        }
        // Validate variable association with device
        validateVariableDeviceAssociation(context, colIndex, variable, varURI);
    }

    private void validateVariableDeviceAssociation(ValidationContext context, Integer colIndex, String variable, URI varURI) throws Exception {
        DeviceContext deviceContext = context.getDeviceContext();
        DeviceModel deviceFromDeviceColumn = context.getDeviceFromDeviceColumn();
        DAOContext daoContext = context.getDaoContext();
        DataCSVValidationModel localCsvValidation = context.getLocalCsvValidation();

        if (deviceFromDeviceColumn != null) {
            boolean variableIsChecked = deviceContext.getVariableCheckedDevice().containsKey(variable) &&
                    deviceContext.getVariableCheckedDevice().get(variable) == deviceFromDeviceColumn;
            if (!variableIsChecked) {
                if (!daoContext.getVariableDAO().variableIsAssociatedToDevice(deviceFromDeviceColumn, varURI)) {
                    localCsvValidation.addVariableToDevice(deviceFromDeviceColumn, varURI);
                }
                deviceContext.getVariableCheckedDevice().put(variable, deviceFromDeviceColumn);
            }
        } else if ((context.isSensingDeviceFoundFromProvenance())) {
            handleProvenanceDeviceAssociation(context, colIndex, variable, varURI);
        }
    }

    private void handleProvenanceDeviceAssociation(ValidationContext context, int colIndex, String variable, URI varURI) throws Exception {
        DeviceContext deviceContext = context.getDeviceContext();
        DAOContext daoContext = context.getDaoContext();
        ProvenanceModel provenance = context.getProvenance();

        if (!deviceContext.getCheckedVariables().contains(variable)) {
            List<DeviceModel> devices = new ArrayList<>();
            List<DeviceModel> linkedDevice = new ArrayList<>();

            for (AgentModel agent : provenance.getAgents()) {
                if (agent.getRdfType() != null && daoContext.getDeviceDAO().isDeviceType(agent.getRdfType())) {
                    DeviceModel dev = daoContext.getDeviceDAO().getDeviceByURI(agent.getUri(), user);
                    if (dev != null) {
                        if (daoContext.getVariableDAO().variableIsAssociatedToDevice(dev, varURI)) {
                            linkedDevice.add(dev);
                        }
                        devices.add(dev);
                    }
                }
            }

            handleDeviceLinkedToVariable(context, colIndex, variable, varURI, linkedDevice, devices);
            deviceContext.getCheckedVariables().add(variable);
        } else {
            handleExistingVariableCheck(context, variable, colIndex);
        }
    }

    private void handleExistingVariableCheck(ValidationContext context, String variable, int colIndex) {
        if (!context.getDeviceContext().getVariableCheckedProvDevice().containsKey(variable)) {
            handleDeviceChoiceAmbiguityError(context, context.getLocalCsvValidation(), colIndex);
        }
    }

    private void handleDeviceLinkedToVariable(ValidationContext validationContext, int colIndex, String variable, URI varURI, List<DeviceModel> linkedDevice, List<DeviceModel> devices) {
        DataCSVValidationModel localCsvValidation = validationContext.getLocalCsvValidation();
        DeviceContext deviceContext = validationContext.getDeviceContext();

        if (linkedDevice.isEmpty()) {
            handleNoLinkedDevice(devices, validationContext, localCsvValidation, varURI, variable, colIndex);
        } else if (linkedDevice.size() == 1) {
            deviceContext.getVariableCheckedProvDevice().put(variable, linkedDevice.get(0));
        } else {
            handleMultipleLinkedDevices(validationContext, localCsvValidation, colIndex);
        }
    }

    private void handleMultipleLinkedDevices(ValidationContext validationContext, DataCSVValidationModel localCsvValidation, int colIndex) {
        // which device to choose ?
        handleDeviceChoiceAmbiguityError(validationContext, localCsvValidation, colIndex);
    }

    private void handleDeviceChoiceAmbiguityError(ValidationContext validationContext, DataCSVValidationModel localCsvValidation, int colIndex) {
        CSVCell cell = new CSVCell(validationContext.getRowIndex(), colIndex, validationContext.getProvenance().getUri().toString(), DEVICE_AMBIGUITY_ID); // add specific exception
        localCsvValidation.addDeviceChoiceAmbiguityError(cell);
        validationContext.setValidRow(false);
    }

    private void handleNoLinkedDevice(List<DeviceModel> devices, ValidationContext validationContext, DataCSVValidationModel localCsvValidation, URI varURI, String variable, int colIndex) {
        if (devices.size() > 1) {
            // which device to choose ?
            handleMultipleLinkedDevices(validationContext, localCsvValidation, colIndex);
        } else {
            if (!devices.isEmpty()) {
                localCsvValidation.addVariableToDevice(devices.get(0), varURI);
                validationContext.getDeviceContext().getVariableCheckedProvDevice().put(variable, devices.get(0));
            } else {
                if (validationContext.getTarget() == null) {
                    validationContext.setMissingTargetOrDevice(true);
                    validationContext.setValidRow(false);
                }
            }
        }
    }

    private void handleVariableColumn(ValidationContext context, int colIndex) {
        if (context.getHeaderByIndex().containsKey(colIndex)) {
            // If value is not blank and null
            if (!StringUtils.isEmpty(context.getValues()[colIndex])) {
                context.getColsToDoAtEnd().add(colIndex);
            }
        }
    }

    private void processAnnotationColumn(ValidationContext context, int colIndex) {
        String annotation = context.getValues()[colIndex];
        context.setAnnotationIndex(colIndex);
        if (!StringUtils.isEmpty(annotation)) {
            AnnotationModel annotationFromAnnotationColumn = new AnnotationModel();
            annotationFromAnnotationColumn.setDescription(annotation.trim());
            annotationFromAnnotationColumn.setPublisher(user.getUri());
            MotivationModel motivationModel = new MotivationModel();
            motivationModel.setUri(URI.create(OA.commenting.getURI()));
            annotationFromAnnotationColumn.setMotivation(motivationModel);

            context.setAnnotationFromAnnotationColumn(annotationFromAnnotationColumn);
        }
    }

    private void validateDeviceColumn(ValidationContext context, int colIndex) throws Exception {
        // check device column
        String deviceNameOrUri = context.getValues()[colIndex];
        context.setDeviceColIndex(colIndex);
        DeviceContext deviceContext = context.getDeviceContext();
        DAOContext daoContext = context.getDaoContext();
        DataCSVValidationModel localCsvValidation = context.getLocalCsvValidation();
        DeviceModel deviceFromDeviceColumn = null;

        // test in uri list
        if (!StringUtils.isEmpty(deviceNameOrUri)) {
            if (deviceContext.getNameURIDevices().containsKey(deviceNameOrUri)) {
                deviceFromDeviceColumn = deviceContext.getNameURIDevices().get(deviceNameOrUri);
            } else {
                // test not in uri list
                if (deviceContext.getDuplicatedDevices().contains(deviceNameOrUri)) {
                    CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, deviceNameOrUri, DEVICE_ID);
                    localCsvValidation.addDuplicateDeviceError(cell);
                    context.setValidRow(false);
                } else if (!deviceContext.getNotExistingDevices().contains(deviceNameOrUri)) {
                    try {
                        deviceFromDeviceColumn = daoContext.getDeviceDAO().getDeviceByNameOrURI(user, deviceNameOrUri);
                        if (deviceFromDeviceColumn == null) {
                            if (!deviceContext.getNotExistingDevices().contains(deviceNameOrUri)) {
                                deviceContext.getNotExistingDevices().add(deviceNameOrUri);
                            }
                            CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, deviceNameOrUri, DEVICE_ID);
                            localCsvValidation.addInvalidDeviceError(cell);
                            context.setValidRow(false);
                        } else {
                            deviceContext.getNameURIDevices().put(deviceNameOrUri, deviceFromDeviceColumn);
                        }
                    } catch (DuplicateNameException e) {
                        CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, deviceNameOrUri, DEVICE_ID);
                        localCsvValidation.addDuplicateDeviceError(cell);
                        deviceContext.getDuplicatedDevices().add(deviceNameOrUri);
                        context.setValidRow(false);
                    }
                } else {
                    CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, deviceNameOrUri, DEVICE_ID);
                    localCsvValidation.addInvalidDeviceError(cell);
                    context.setValidRow(false);
                }

            }
        }
        context.setDeviceFromDeviceColumn(deviceFromDeviceColumn);
    }

    private void validateDateColumn(ValidationContext context, int colIndex) throws TimezoneException, TimezoneAmbiguityException {
        // check date
        String dateValue = context.getValues()[colIndex];
        // TODO : Validate timezone ambiguity
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(dateValue, null);
        if (parsedDateTimeMongo == null) {
            CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, dateValue, "DATE");
            context.getLocalCsvValidation().addInvalidDateError(cell);
            context.setValidRow(false);
            return;
        }
        context.setParsedDateTimeMongo(parsedDateTimeMongo);
    }

    private void validateScientificObjectColumn(ValidationContext context, int colIndex) throws Exception {
        TargetContext targetContext = context.getTargetContext();
        DAOContext daoContext = context.getDaoContext();
        DataCSVValidationModel localCsvValidation = context.getLocalCsvValidation();
        DataCSVValidationModel csvValidation = context.getCsvValidation();
        ExperimentContext experimentContext = context.getExperimentContext();
        String objectNameOrUri = context.getValues()[colIndex];
        SPARQLNamedResourceModel object = null;
        // check if the object name/uri has been previously referenced -> if so, no need to re-perform a check with the Dao
        if (!StringUtils.isEmpty(objectNameOrUri) && targetContext.getNameURIScientificObjects().containsKey(objectNameOrUri)) {
            object = targetContext.getNameURIScientificObjects().get(objectNameOrUri);
        } else {

            SPARQLNamedResourceModel existingOs = null;
            Node experimentNode = experimentContext.getExperiment() == null ? null : SPARQLDeserializers.nodeURI(experimentContext.getExperiment());

            // check if the object has been previously referenced as unknown, if not, then performs a check with Dao
            if (!StringUtils.isEmpty(objectNameOrUri) && !targetContext.getScientificObjectsNotInXp().contains(objectNameOrUri)) {
                existingOs = testNameOrURI(daoContext.getScientificObjectDAO(), localCsvValidation, context.getRowIndex(), colIndex, experimentNode, objectNameOrUri);
            }

            if (existingOs == null) {
                context.setValidRow(false);
                targetContext.getScientificObjectsNotInXp().add(objectNameOrUri);
                CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, objectNameOrUri, OBJECT_ID);
                csvValidation.addInvalidObjectError(cell);
            } else {
                object = existingOs;
                // object exist, put it into name/URI cache
                targetContext.getNameURIScientificObjects().put(objectNameOrUri, existingOs);
            }
        }
        context.setObject(object);
    }

    private void validateTargetColumn(ValidationContext context, int colIndex) {
        TargetContext targetContext = context.getTargetContext();
        DAOContext daoContext = context.getDaoContext();
        DataCSVValidationModel localCsvValidation = context.getLocalCsvValidation();
        SPARQLNamedResourceModel target = null;

        //check target column
        String targetNameOrUri = context.getValues()[colIndex];
        context.setTargetColIndex(colIndex);

        if (!StringUtils.isEmpty(targetNameOrUri)) {
            if (targetContext.getNameURITargets().containsKey(targetNameOrUri)) {
                target = targetContext.getNameURITargets().get(targetNameOrUri);
            } else {
                // test not in uri list
                if (targetContext.getDuplicatedTargets().contains(targetNameOrUri)) {
                    CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, targetNameOrUri, TARGET_ID);
                    localCsvValidation.addDuplicateTargetError(cell);
                    context.setValidRow(false);
                } else if (!targetContext.getNotExistingTargets().contains(targetNameOrUri)) {
                    try {
                        target = daoContext.getOntologyDAO().getTargetByNameOrURI(targetNameOrUri);
                        if (target == null) {
                            targetContext.getNotExistingTargets().add(targetNameOrUri);
                            CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, targetNameOrUri, TARGET_ID);
                            localCsvValidation.addInvalidTargetError(cell);
                            context.setValidRow(false);
                        } else {
                            targetContext.getNameURITargets().put(targetNameOrUri, target);
                        }
                    } catch (Exception e) {
                        CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, targetNameOrUri, TARGET_ID);
                        localCsvValidation.addDuplicateTargetError(cell);
                        targetContext.getDuplicatedTargets().add(targetNameOrUri);
                        context.setValidRow(false);
                    }

                } else {
                    CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, targetNameOrUri, TARGET_ID);
                    localCsvValidation.addInvalidTargetError(cell);
                    context.setValidRow(false);
                }

            }
        }
        context.setTarget(target);
    }

    private void validateExperimentColumn(ValidationContext context, int colIndex) throws Exception {
        ExperimentModel exp = null;
        ExperimentContext experimentContext = context.getExperimentContext();
        DAOContext daoContext = context.getDaoContext();
        DataCSVValidationModel localCsvValidation = context.getLocalCsvValidation();
        //check experiment column
        String expNameOrUri = context.getValues()[colIndex];
        // test in uri list
        if (!StringUtils.isEmpty(expNameOrUri)) {
            if (experimentContext.getNameURIExperiments().containsKey(expNameOrUri)) {
                exp = experimentContext.getNameURIExperiments().get(expNameOrUri);
            } else {
                // test not in uri list
                if (experimentContext.getDuplicatedExperiments().contains(expNameOrUri)) {
                    CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, expNameOrUri, EXPERIMENT_ID);
                    localCsvValidation.addDuplicateExperimentError(cell);
                    context.setValidRow(false);
                } else if (!experimentContext.getNotExistingExperiments().contains(expNameOrUri)) {
                    try {
                        exp = daoContext.getExperimentDAO().getExperimentByNameOrURI(expNameOrUri, user);
                        if (exp == null) {
                            experimentContext.getNotExistingExperiments().add(expNameOrUri);
                            CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, expNameOrUri, EXPERIMENT_ID);
                            localCsvValidation.addInvalidExperimentError(cell);
                            context.setValidRow(false);
                        } else {
                            experimentContext.getNameURIExperiments().put(expNameOrUri, exp);
                        }
                    } catch (DuplicateNameException e) {
                        CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, expNameOrUri, EXPERIMENT_ID);
                        localCsvValidation.addDuplicateExperimentError(cell);
                        experimentContext.getDuplicatedExperiments().add(expNameOrUri);
                        context.setValidRow(false);
                    }
                } else {
                    CSVCell cell = new CSVCell(context.getRowIndex(), colIndex, expNameOrUri, EXPERIMENT_ID);
                    localCsvValidation.addInvalidExperimentError(cell);
                    context.setValidRow(false);

                }
            }
        }
        if (exp != null) {
            context.getExperiments().add(exp.getUri());
        }
    }

    private SPARQLNamedResourceModel testNameOrURI(ScientificObjectDAO scientificObjectDAO, CSVValidationModel validation, int rowIndex, int colIndex, Node experiment, String nameOrUri) throws Exception {

        // check if object exist by URI inside experiment
        if (URIDeserializer.validateURI(nameOrUri)) {
            URI objectUri = URI.create(nameOrUri);

            SPARQLNamedResourceModel existingObject = sparql.getByURI(experiment, ScientificObjectModel.class, objectUri, null);
            if (existingObject == null) {
                validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, OBJECT_ID));
                return null;
            }
            return existingObject;

            // check if object exist by name inside experiment
        } else if (experiment != null) {
            SPARQLNamedResourceModel existingObject = scientificObjectDAO.getUriByNameAndGraph(experiment, nameOrUri);
            if (existingObject == null) {
                validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, OBJECT_ID));
                return null;
            }
            return existingObject;
        } else {
            // ambiguity about name inside global OS graph, indeed, several OS can have the same name inside the global graph,
            // so there are no guarantee that a unique OS URI will be found with this name
            validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT));
            return null;
        }
    }

    private void handleDataInsertion(DataCSVValidationModel validation, DataLogic dataLogicWithSession) throws Exception {
        LOGGER.debug("[importCsvInsertionStep] Start insertion step of {} row(s) ", validation.getNbLinesToImport());
        Instant startTime = Instant.now();
        // Create batch history model to track data insertion
        BatchHistoryModel batchHistoryModel = createBatchHistoryModel(startTime);
        List<DataModel> data = new ArrayList<>(validation.getData().keySet());

        try {
            //Insert batch history
            batchHistoryDao.create(batchHistoryModel);
            // Set batchUri and publicationDate for the data
            setBatchUriAndPublicationDateToData(batchHistoryModel.getUri(), startTime, data);
            dataLogicWithSession.createManyFromImport(data, validation);
            validation.setBatchHistoryUri(batchHistoryModel.getUri());
            validation.setInsertionStep(true);
            validation.setValidCSV(!validation.hasErrors());
        } catch (NoSQLTooLargeSetException ex) {
            validation.setTooLargeDataset(true);
        } catch (MongoBulkWriteException duplicateError) {
            handleBulkWriteErrors(duplicateError, validation, data);
        } catch (MongoCommandException e) {
            addUnknownDuplicateError(validation);
        } catch (DataTypeException e) {
            handleDataTypeError(e, validation);
        }
        LOGGER.debug("[importCsvInsertionStep] Completed insertion in {} milliseconds", Duration.between(startTime, Instant.now()).toMillis());
    }

    private BatchHistoryModel createBatchHistoryModel(Instant startTime) {
        BatchHistoryModel batchHistoryModel = new BatchHistoryModel();
        batchHistoryModel.setPublicationDate(startTime);
        batchHistoryModel.setPublisher(user.getUri());
        return batchHistoryModel;
    }

    private void setBatchUriAndPublicationDateToData(URI batchUri, Instant startTime, List<DataModel> data) {
        data.forEach(elm -> {
            elm.setBatchUri(batchUri);
            elm.setPublicationDate(startTime);
        });
    }

    private void handleBulkWriteErrors(MongoBulkWriteException duplicateError, DataCSVValidationModel validation, List<DataModel> data) {
        for (BulkWriteError bulkError : duplicateError.getWriteErrors()) {
            int index = bulkError.getIndex();
            DataModel dataModel = data.get(index);
            int variableIndex = validation.getHeaders().indexOf(dataModel.getVariable().toString());
            String variableName = buildVariableName(validation, variableIndex);

            CSVCell csvCell = new CSVCell(
                    validation.getData().get(data.get(index)),
                    variableIndex,
                    dataModel.getValue().toString(),
                    variableName
            );
            validation.addDuplicatedDataError(csvCell);
        }
    }

    private void addUnknownDuplicateError(DataCSVValidationModel validation) {
        CSVCell csvCell = new CSVCell(-1, -1, "Unknown value", "Unknown variable");
        validation.addDuplicatedDataError(csvCell);
    }

    private void handleDataTypeError(DataTypeException e, DataCSVValidationModel validation) {
        int variableIndex = validation.getHeaders().indexOf(e.getVariable().toString());
        String variableName = buildVariableName(validation, variableIndex);

        CSVCell csvCell = new CSVCell(
                e.getDataIndex(),
                variableIndex,
                e.getValue().toString(),
                variableName
        );
        validation.addInvalidDataTypeError(csvCell);
    }

    private String buildVariableName(DataCSVValidationModel validation, int variableIndex) {
        return validation.getHeadersLabels().get(variableIndex) + '(' + validation.getHeaders().get(variableIndex) + ')';
    }

    public DataCSVValidationDTO buildDataCSVValidationDTO(DataCSVValidationModel validation) {
        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();
        csvValidation.setDataErrors(validation);
        return csvValidation;
    }

    /**
     * Generates a unique validation ID for a user.
     *
     * @return A string representing the unique validation ID
     */
    private String generateValidationKey() {
        String currentDate = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        // UUID unique
        String randomUUID = UUID.randomUUID().toString().substring(0, 8);
        return user.getName() + UNDERSCORE + currentDate + UNDERSCORE + randomUUID;
    }
}
