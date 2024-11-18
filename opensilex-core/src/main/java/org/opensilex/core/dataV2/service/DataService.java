package org.opensilex.core.dataV2.service;

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
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.dataV2.model.DAOContext;
import org.opensilex.core.dataV2.model.DeviceContext;
import org.opensilex.core.dataV2.model.ExperimentContext;
import org.opensilex.core.dataV2.model.TargetContext;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.CSVDataTypeException;
import org.opensilex.core.exception.DataTypeException;
import org.opensilex.core.exception.DuplicateNameException;
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DataService {
    protected final static Logger LOGGER = LoggerFactory.getLogger(DataService.class);
    public static final String DEVICE_ID = "DEVICE_ID";
    public static final String DEVICE_AMBIGUITY_ID = "DEVICE_AMBIGUITY_ID";
    public static final String OBJECT_ID = "OBJECT_ID";
    public static final String OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT = "OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT";
    public static final String TARGET_ID = "TARGET_ID";
    public static final String EXPERIMENT_ID = "EXPERIMENT_ID";

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
    AccountModel user;
    private static final Cache<String, DataCSVValidationModel> csvValidationModelCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public DataService(MongoDBService nosql, SPARQLService sparql, FileStorageService fs, AccountModel user) {
        this.nosql = nosql;
        this.sparql = sparql;
        this.fs = fs;
        this.user = user;
    }

    public DataCSVValidationDTO importCSVDataV2(URI provenance, URI experiment, InputStream file, String validationId) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
        DataCSVValidationModel validation = csvValidationModelCache.getIfPresent(validationId);
        if (validation == null) {
            validation = validateWholeCsvV2(provenance, experiment, file);
        }

        if (validation.isValidCSV()) {
            handleDataInsertion(dataLogic, validation);
            validation.setValidCSV(!validation.hasErrors());
            csvValidationModelCache.invalidate(validationId);
        }

        return buildDataCSVValidationDTO(validation);
    }

    /**
     * Validates the csv for data import after verifying provenance and experiment
     */
    public DataCSVValidationModel validateWholeCsvV2(URI provenance, URI experiment, InputStream file) throws Exception {
        // test prov
        ProvenanceModel provenanceModel;
        try {
            provenanceModel = new ProvenanceDaoV2(nosql.getServiceV2()).get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        }

        // test exp
        if (experiment != null) {
            new ExperimentDAO(sparql, nosql).validateExperimentAccess(experiment, user);
        }

        //Validate csv
        DataCSVValidationModel validation;
        validation = validateWholeCSVInnerCodeV2(provenanceModel, experiment, file);

        //Set DataCSVValidationModel attributes
        validation.setValidCSV(!validation.hasErrors());

        validation.setNbLinesToImport(validation.getData().size());

        // Set generate and set validationID for whole csv into the cache
        String validationId = "mko"; //generateValidationId();
        validation.setValidationId(validationId);
        csvValidationModelCache.put(validationId, validation);

        return validation;
    }

    /**
     * Does the actual validating once we have loaded our provenance
     */
    private DataCSVValidationModel validateWholeCSVInnerCodeV2(ProvenanceModel provenance, URI experiment, InputStream file) throws Exception {
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);

        DataCSVValidationModel csvValidation = new DataCSVValidationModel();

        Map<String, SPARQLNamedResourceModel> nameURITargets = new HashMap<>();
        Map<String, ExperimentModel> nameURIExperiments = new HashMap<>();
        Map<String, SPARQLNamedResourceModel> nameURIScientificObjectsInXp = new HashMap<>();
        Map<String, DeviceModel> nameURIDevices = new HashMap<>();
        Map<String, DeviceModel> variableCheckedProvDevice = new HashMap<>();
        Map<String, DeviceModel> variableCheckedDevice = new HashMap<>();
        HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
        Map<Integer, String> headerByIndex = new HashMap<>();

        List<String> notExistingTargets = new ArrayList<>();
        List<String> duplicatedTargets = new ArrayList<>();
        List<String> notExistingExperiments = new ArrayList<>();
        List<String> duplicatedExperiments = new ArrayList<>();
        List<String> scientificObjectsNotInXp = new ArrayList<>();
        List<String> notExistingDevices = new ArrayList<>();
        List<String> duplicatedDevices = new ArrayList<>();
        List<String> checkedVariables = new ArrayList<>();
        List<ImportDataIndex> duplicateDataByIndex = new ArrayList<>();

        boolean sensingDeviceFoundFromProvenance = provenance.getAgents() != null
                && provenance.getAgents().stream()
                .anyMatch(agent -> {
                    try {
                        return (agent.getRdfType() != null) && deviceDAO.isDeviceType(agent.getRdfType());
                    } catch (SPARQLException e) {
                        LOGGER.error("Error while checking device type for agent: {}", agent, e);
                        return false;
                    }
                });

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8)) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);

            LOGGER.debug("Import data - CSV format => \n '{}'", csvReader.getDetectedFormat());

            // Process headers
            String[] ids = csvReader.parseNext();
            Set<String> headers = Arrays.stream(ids).filter(Objects::nonNull)
                    .map(id -> id.toLowerCase(Locale.ENGLISH))
                    .collect(Collectors.toSet());

            if (!validateHeaders(headers, sensingDeviceFoundFromProvenance, csvValidation)) {
                return csvValidation;
            }

            // Process and validate each header URI if applicable
            if (!processHeaders(ids, headerByIndex, mapVariableUriDataType, csvValidation, variableDAO)) {
                return csvValidation;
            }

            csvValidation.setHeadersFromArray(ids);

            // Process rows
            csvValidation.setHeadersLabelsFromArray(csvReader.parseNext());

            // SKip line 3
            csvReader.parseNext();

            // Line 4
            int rowIndex = 0;
            int nbError = 0;
            String[] values;

            ExperimentContext experimentContext = ExperimentContext.buildExperimentContext(experiment, duplicatedExperiments, nameURIExperiments, notExistingExperiments);
            DeviceContext deviceContext = DeviceContext.buildDeviceContext(duplicatedDevices, duplicateDataByIndex, checkedVariables, nameURIDevices, notExistingDevices, variableCheckedDevice, variableCheckedProvDevice, mapVariableUriDataType);
            TargetContext targetContext = TargetContext.buildTargetContext(duplicatedTargets, nameURITargets, notExistingTargets, nameURIScientificObjectsInXp, scientificObjectsNotInXp);
            DAOContext daoContext = DAOContext.buildDaoContext(deviceDAO, ontologyDAO, experimentDAO, scientificObjectDAO);

            while ((values = csvReader.parseNext()) != null) {
                try {
                    boolean validateCSVRow = validateCSVRow(
                            provenance,
                            sensingDeviceFoundFromProvenance,
                            values,
                            rowIndex,
                            csvValidation,
                            headerByIndex,
                            experimentContext,
                            targetContext,
                            deviceContext,
                            daoContext
                    );
                    if (!validateCSVRow) {
                        nbError++;
                    }
                    if (nbError >= ExperimentAPI.CSV_NB_ERRORS_MAX) {
                        break;
                    }
                } catch (CSVDataTypeException e) {
                    csvValidation.addInvalidDataTypeError(e.getCsvCell());
                }
                rowIndex++;
            }
        }

        if (csvValidation.getData().keySet().size() > DataAPI.SIZE_MAX) {
            csvValidation.setTooLargeDataset(true);
        }

        return csvValidation;
    }

    private boolean validateHeaders(Set<String> headers, boolean sensingDeviceFound, DataCSVValidationModel csvValidation) {
        if (!headers.contains(DEVICE_HEADER) && !headers.contains(TARGET_HEADER)
                && !headers.contains(SCIENTIFIC_OBJ_HEADER) && !sensingDeviceFound) {
            csvValidation.addMissingHeaders(List.of(DEVICE_HEADER + " or " + TARGET_HEADER + " or " + SCIENTIFIC_OBJ_HEADER));
        }
        if (headers.contains(ANNOTATION_HEADER) && !headers.contains(TARGET_HEADER) && !headers.contains(SCIENTIFIC_OBJ_HEADER)) {
            csvValidation.addMissingHeaders(List.of(TARGET_HEADER + " or " + SCIENTIFIC_OBJ_HEADER));
        }
        return !csvValidation.hasErrors();
    }

    private boolean processHeaders(String[] ids, Map<Integer, String> headerByIndex, Map<URI, URI> mapVariableUriDataType,
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
                            mapVariableUriDataType.put(var.getUri(), var.getDataType());
                            headerByIndex.put(i, header);
                        }
                    }
                } catch (Exception e) {
                    csvValidation.addInvalidHeaderURI(i, ids[i]);
                }
            }
        }
        return !csvValidation.hasErrors();
    }

    private boolean isFixedHeader(String header) {
        return Arrays.asList(EXPERIMENT_HEADER, TARGET_HEADER, DATE_HEADER, DEVICE_HEADER, SCIENTIFIC_OBJ_HEADER, RAW_DATA_HEADER, ANNOTATION_HEADER)
                .contains(header.toLowerCase(Locale.ENGLISH));
    }

    //TODO sort this mess out
    private boolean validateCSVRow(
            ProvenanceModel provenance,
            boolean sensingDeviceFoundFromProvenance,
            String[] values,
            int rowIndex,
            DataCSVValidationModel csvValidation,
            Map<Integer, String> headerByIndex,
            ExperimentContext experimentContext,
            TargetContext targetContext,
            DeviceContext deviceContext,
            DAOContext daoContext)
            throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);

        boolean validRow = true;


        ParsedDateTimeMongo parsedDateTimeMongo = null;

        List<URI> experiments = new ArrayList<>();
        SPARQLNamedResourceModel target = null;

        boolean missingTargetOrDevice = false;
        int targetColIndex = 0;
        int deviceColIndex = 0;

        AnnotationModel annotationFromAnnotationColumn = null;
        int annotationIndex = 0;

        DeviceModel deviceFromDeviceColumn = null;
        SPARQLNamedResourceModel object = null;
        if (experimentContext.getExperiment() != null) {
            experiments.add(experimentContext.getExperiment());
        }

        //Set to remember which columns to do at end of row iteration (in case required columns like target are at the end).
        Set<Integer> colsToDoAtEnd = new HashSet<>();

        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (headerByIndex.get(colIndex).equalsIgnoreCase(EXPERIMENT_HEADER)) {
                //check experiment column
                ExperimentModel exp = null;
                String expNameOrUri = values[colIndex];
                // test in uri list
                if (!StringUtils.isEmpty(expNameOrUri)) {
                    if (experimentContext.getNameURIExperiments().containsKey(expNameOrUri)) {
                        exp = experimentContext.getNameURIExperiments().get(expNameOrUri);
                    } else {
                        // test not in uri list
                        if (experimentContext.getDuplicatedExperiments().contains(expNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, EXPERIMENT_ID);
                            csvValidation.addDuplicateExperimentError(cell);
                            validRow = false;
                        } else if (!experimentContext.getNotExistingExperiments().contains(expNameOrUri)) {
                            try {
                                exp = daoContext.getExperimentDAO().getExperimentByNameOrURI(expNameOrUri, user);
                                if (exp == null) {
                                    if (!experimentContext.getNotExistingExperiments().contains(expNameOrUri)) {
                                        experimentContext.getNotExistingExperiments().add(expNameOrUri);
                                    }

                                    CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, EXPERIMENT_ID);
                                    csvValidation.addInvalidExperimentError(cell);
                                    validRow = false;
                                } else {
                                    experimentContext.getNameURIExperiments().put(expNameOrUri, exp);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, EXPERIMENT_ID);
                                csvValidation.addDuplicateExperimentError(cell);
                                experimentContext.getDuplicatedExperiments().add(expNameOrUri);
                                validRow = false;
                            }
                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, EXPERIMENT_ID);
                            csvValidation.addInvalidExperimentError(cell);
                            validRow = false;

                        }
                    }
                }
                if (exp != null) {
                    experiments.add(exp.getUri());
                }


            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(TARGET_HEADER)) {
                //check target column
                String targetNameOrUri = values[colIndex];
                targetColIndex = colIndex;

                if (!StringUtils.isEmpty(targetNameOrUri)) {
                    if (targetContext.getNameURITargets().containsKey(targetNameOrUri)) {
                        target = targetContext.getNameURITargets().get(targetNameOrUri);
                    } else {
                        // test not in uri list
                        if (targetContext.getDuplicatedTargets().contains(targetNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, TARGET_ID);
                            csvValidation.addDuplicateTargetError(cell);
                            validRow = false;
                        } else if (!targetContext.getNotExistingTargets().contains(targetNameOrUri)) {
                            try {
                                target = daoContext.getOntologyDAO().getTargetByNameOrURI(targetNameOrUri);
                                if (target == null) {
                                    if (!targetContext.getNotExistingTargets().contains(targetNameOrUri)) {
                                        targetContext.getNotExistingTargets().add(targetNameOrUri);
                                    }

                                    CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, TARGET_ID);
                                    csvValidation.addInvalidTargetError(cell);
                                    validRow = false;
                                } else {
                                    targetContext.getNameURITargets().put(targetNameOrUri, target);
                                }
                            } catch (Exception e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, TARGET_ID);
                                csvValidation.addDuplicateTargetError(cell);
                                targetContext.getDuplicatedTargets().add(targetNameOrUri);
                                validRow = false;
                            }

                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, TARGET_ID);
                            csvValidation.addInvalidTargetError(cell);
                            validRow = false;
                        }

                    }
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(SCIENTIFIC_OBJ_HEADER)) {

                String objectNameOrUri = values[colIndex];
                // check if the object name/uri has been previously referenced -> if so, no need to re-perform a check with the Dao
                if (!StringUtils.isEmpty(objectNameOrUri) && targetContext.getNameURIScientificObjects().containsKey(objectNameOrUri)) {
                    object = targetContext.getNameURIScientificObjects().get(objectNameOrUri);
                } else {

                    SPARQLNamedResourceModel existingOs = null;
                    Node experimentNode = experimentContext.getExperiment() == null ? null : SPARQLDeserializers.nodeURI(experimentContext.getExperiment());

                    // check if the object has been previously referenced as unknown, if not, then performs a check with Dao
                    if (!StringUtils.isEmpty(objectNameOrUri) && !targetContext.getScientificObjectsNotInXp().contains(objectNameOrUri)) {
                        existingOs = testNameOrURI(daoContext.getScientificObjectDAO(), csvValidation, rowIndex, colIndex, experimentNode, objectNameOrUri);
                    }

                    if (existingOs == null) {
                        validRow = false;
                        targetContext.getScientificObjectsNotInXp().add(objectNameOrUri);
                    } else {
                        object = existingOs;
                        // object exist, put it into name/URI cache
                        targetContext.getNameURIScientificObjects().put(objectNameOrUri, existingOs);
                    }
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(DATE_HEADER)) {
                // check date
                // TODO : Validate timezone ambiguity
                parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(values[colIndex], null);
                if (parsedDateTimeMongo == null) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, values[colIndex], "DATE");
                    csvValidation.addInvalidDateError(cell);
                    validRow = false;
                    break;
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(DEVICE_HEADER)) {
                // check device column
                String deviceNameOrUri = values[colIndex];
                deviceColIndex = colIndex;

                // test in uri list
                if (!StringUtils.isEmpty(deviceNameOrUri)) {
                    if (deviceContext.getNameURIDevices().containsKey(deviceNameOrUri)) {
                        deviceFromDeviceColumn = deviceContext.getNameURIDevices().get(deviceNameOrUri);
                    } else {
                        // test not in uri list
                        if (deviceContext.getDuplicatedDevices().contains(deviceNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, DEVICE_ID);
                            csvValidation.addDuplicateDeviceError(cell);
                            validRow = false;
                        } else if (!deviceContext.getNotExistingDevices().contains(deviceNameOrUri)) {
                            try {
                                deviceFromDeviceColumn = daoContext.getDeviceDAO().getDeviceByNameOrURI(user, deviceNameOrUri);
                                if (deviceFromDeviceColumn == null) {
                                    if (!deviceContext.getNotExistingDevices().contains(deviceNameOrUri)) {
                                        deviceContext.getNotExistingDevices().add(deviceNameOrUri);
                                    }
                                    CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, DEVICE_ID);
                                    csvValidation.addInvalidDeviceError(cell);
                                    validRow = false;
                                } else {
                                    deviceContext.getNameURIDevices().put(deviceNameOrUri, deviceFromDeviceColumn);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, DEVICE_ID);
                                csvValidation.addDuplicateDeviceError(cell);
                                deviceContext.getDuplicatedDevices().add(deviceNameOrUri);
                                validRow = false;
                            }
                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, DEVICE_ID);
                            csvValidation.addInvalidDeviceError(cell);
                            validRow = false;
                        }

                    }
                }

            }
            //If we are at the annotation column, and the cell isn't empty, create a new Annotation Model.
            //Set the motivation to commenting, and leave the target for now until we're sure that the target column has already been imported
            else if (headerByIndex.get(colIndex).equalsIgnoreCase(ANNOTATION_HEADER)) {
                String annotation = values[colIndex];
                annotationIndex = colIndex;
                if (!StringUtils.isEmpty(annotation)) {
                    annotationFromAnnotationColumn = new AnnotationModel();
                    annotationFromAnnotationColumn.setDescription(annotation.trim());
                    annotationFromAnnotationColumn.setPublisher(user.getUri());
                    MotivationModel motivationModel = new MotivationModel();
                    motivationModel.setUri(URI.create(OA.commenting.getURI()));
                    annotationFromAnnotationColumn.setMotivation(motivationModel);
                }
            } else if (!headerByIndex.get(colIndex).equalsIgnoreCase(RAW_DATA_HEADER)) { // Variable/Value column
                if (headerByIndex.containsKey(colIndex)) {
                    // If value is not blank and null
                    if (!StringUtils.isEmpty(values[colIndex])) {
                        colsToDoAtEnd.add(colIndex);
                    }
                }
            }
        }
        //Do the variable value columns now that we know the target or device is loaded if the user correctly filled it
        for (Integer colIndex : colsToDoAtEnd) {
            if (validRow) {
                String variable = headerByIndex.get(colIndex);
                URI varURI = URI.create(variable);
                if (deviceFromDeviceColumn == null && target == null && object == null) {
                    missingTargetOrDevice = true;
                    validRow = false;
                    break;
                }
                if (deviceFromDeviceColumn != null) {
                    boolean variableIsChecked = deviceContext.getVariableCheckedDevice().containsKey(variable) && deviceContext.getVariableCheckedDevice().get(variable) == deviceFromDeviceColumn;
                    if (!variableIsChecked) {
                        if (!variableDAO.variableIsAssociatedToDevice(deviceFromDeviceColumn, varURI)) {
                            csvValidation.addVariableToDevice(deviceFromDeviceColumn, varURI);
                        }
                        deviceContext.getVariableCheckedDevice().put(variable, deviceFromDeviceColumn);
                    }

                } else if (sensingDeviceFoundFromProvenance) {
                    if (!deviceContext.getCheckedVariables().contains(variable)) { // do it one time but write the error on each row if there is one
                        List<DeviceModel> devices = new ArrayList<>();
                        List<DeviceModel> linkedDevice = new ArrayList<>();
                        DeviceModel dev;
                        for (AgentModel agent : provenance.getAgents()) {
                            if (agent.getRdfType() != null && daoContext.getDeviceDAO().isDeviceType(agent.getRdfType())) {
                                dev = daoContext.getDeviceDAO().getDeviceByURI(agent.getUri(), user);
                                if (dev != null) {

                                    if (variableDAO.variableIsAssociatedToDevice(dev, varURI)) {
                                        linkedDevice.add(dev);
                                    }
                                    devices.add(dev);
                                }
                            }
                        }
                        switch (linkedDevice.size()) {
                            case 0:
                                if (devices.size() > 1) {
                                    //which device to choose ?
                                    CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), DEVICE_AMBIGUITY_ID);  // add specific exception
                                    csvValidation.addDeviceChoiceAmbiguityError(cell);
                                    validRow = false;
                                    break;
                                } else {
                                    if (!devices.isEmpty()) {
                                        csvValidation.addVariableToDevice(devices.get(0), varURI);
                                        deviceContext.getVariableCheckedProvDevice().put(variable, devices.get(0));
                                    } else {
                                        if (target == null) {
                                            missingTargetOrDevice = true;
                                            validRow = false;
                                            break;
                                        }
                                    }
                                }
                                break;
                            case 1:

                                deviceContext.getVariableCheckedProvDevice().put(variable, linkedDevice.get(0));
                                break;

                            default:
                                //which device to choose ?
                                CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), DEVICE_AMBIGUITY_ID); // add specific exception
                                csvValidation.addDeviceChoiceAmbiguityError(cell);
                                validRow = false;
                                break;
                        }
                        deviceContext.getCheckedVariables().add(variable);
                    } else {
                        if (!deviceContext.getVariableCheckedProvDevice().containsKey(variable)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), DEVICE_AMBIGUITY_ID);  // add specific exception
                            csvValidation.addDeviceChoiceAmbiguityError(cell);
                            break;
                        }
                    }
                }
                if (validRow) {
                    DataModel dataModel = new DataModel();
                    DataProvenanceModel provenanceModel = new DataProvenanceModel();
                    provenanceModel.setUri(provenance.getUri());

                    if (!experiments.isEmpty()) {
                        provenanceModel.setExperiments(experiments);
                    }

                    if (deviceFromDeviceColumn != null) {
                        ProvEntityModel agent = new ProvEntityModel();
                        if (rootDeviceTypes == null) {
                            rootDeviceTypes = daoContext.getDeviceDAO().getRootDeviceTypes(user);
                        }
                        URI rootType = rootDeviceTypes.get(deviceFromDeviceColumn.getType());
                        agent.setType(rootType);
                        agent.setUri(deviceFromDeviceColumn.getUri());
                        provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));

                    } else if (sensingDeviceFoundFromProvenance) {

                        DeviceModel checkedDevice = deviceContext.getVariableCheckedProvDevice().get(variable);
                        ProvEntityModel agent = new ProvEntityModel();
                        if (rootDeviceTypes == null) {
                            rootDeviceTypes = daoContext.getDeviceDAO().getRootDeviceTypes(user);
                        }
                        URI rootType = rootDeviceTypes.get(checkedDevice.getType());
                        agent.setType(rootType);
                        agent.setUri(checkedDevice.getUri());
                        provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));

                    }

                    dataModel.setDate(parsedDateTimeMongo.getInstant());
                    dataModel.setOffset(parsedDateTimeMongo.getOffset());
                    dataModel.setIsDateTime(parsedDateTimeMongo.getIsDateTime());

                    if (object != null) {
                        dataModel.setTarget(object.getUri());
                    }
                    if (target != null) {
                        dataModel.setTarget(target.getUri());
                    }
                    dataModel.setProvenance(provenanceModel);
                    dataModel.setVariable(varURI);
                    DataValidateUtils.checkAndConvertValue(dataModel, varURI, values[colIndex].trim(), deviceContext.getMapVariableUriDataType().get(varURI), rowIndex, colIndex, csvValidation);

                    if (colIndex + 1 < values.length) {
                        if (headerByIndex.get(colIndex + 1).equalsIgnoreCase(RAW_DATA_HEADER) && values[colIndex + 1] != null) {
                            dataModel.setRawData(DataValidateUtils.returnValidRawData(varURI, values[colIndex + 1].trim(), deviceContext.getMapVariableUriDataType().get(varURI), rowIndex, colIndex + 1, csvValidation));
                        }
                    }

                    // check for duplicate data
                    URI targetUri = null;
                    URI deviceUri = null;
                    if (target != null) {
                        targetUri = target.getUri();
                    }
                    if (object != null) {
                        targetUri = object.getUri();
                    }
                    if (deviceFromDeviceColumn != null) {
                        deviceUri = deviceFromDeviceColumn.getUri();
                    }
                    ImportDataIndex importDataIndex = new ImportDataIndex(parsedDateTimeMongo.getInstant(), varURI, provenance.getUri(), targetUri, deviceUri);
                    if (!deviceContext.getDuplicateDataByIndex().contains(importDataIndex)) {
                        deviceContext.getDuplicateDataByIndex().add(importDataIndex);
                    } else {
                        String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';
                        CSVCell duplicateCell = new CSVCell(rowIndex, colIndex, values[colIndex].trim(), variableName);
                        csvValidation.addDuplicatedDataError(duplicateCell);
                    }
                    csvValidation.addData(dataModel, rowIndex);

                }

            }
        }
        // If an AnnotationModel was created on this row as well as a target, we need to set the Annotation's target
        if (annotationFromAnnotationColumn != null) {
            if (target == null && object == null) {
                CSVCell annotationCell = new CSVCell(rowIndex, annotationIndex, annotationFromAnnotationColumn.getDescription(), ANNOTATION_HEADER);
                csvValidation.addInvalidAnnotationError(annotationCell);
                validRow = false;
            } else {
                if (validRow) {
                    annotationFromAnnotationColumn.setTargets(Collections.singletonList(target == null ? object.getUri() : target.getUri()));
                    csvValidation.addToAnnotationsOnObjects(annotationFromAnnotationColumn);
                }
            }
        }

        if (missingTargetOrDevice) {
            //the device or the target is mandatory if there is no device in the provenance
            CSVCell cell1 = new CSVCell(rowIndex, deviceColIndex, null, DEVICE_HEADER);
            CSVCell cell2 = new CSVCell(rowIndex, targetColIndex, null, TARGET_HEADER);
            csvValidation.addMissingRequiredValue(cell1);
            csvValidation.addMissingRequiredValue(cell2);
        }

        return validRow;
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

    private void handleDataInsertion(DataLogic dataLogic, DataCSVValidationModel validation) throws Exception {
        Instant start = Instant.now();
        List<DataModel> data = new ArrayList<>(validation.getData().keySet());

        try {
            dataLogic.createManyFromImport(data, validation);
        } catch (NoSQLTooLargeSetException ex) {
            validation.setTooLargeDataset(true);
        } catch (MongoBulkWriteException duplicateError) {
            handleBulkWriteErrors(duplicateError, validation, data);
        } catch (MongoCommandException e) {
            addUnknownDuplicateError(validation);
        } catch (DataTypeException e) {
            handleDataTypeError(e, validation);
        }

        logInsertionTime(start);
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

    private void logInsertionTime(Instant start) {
        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
        LOGGER.debug("Insertion {} milliseconds elapsed", timeElapsed);
    }

    public DataCSVValidationDTO buildDataCSVValidationDTO(DataCSVValidationModel validation) {
        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();
        csvValidation.setDataErrors(validation);
        return csvValidation;
    }

    private String generateValidationId() {
        String currentDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        // UUID unique
        String randomUUID = UUID.randomUUID().toString().substring(0, 8);

        return user.getName() + "_" + currentDate + "_" + randomUUID;
    }

}
