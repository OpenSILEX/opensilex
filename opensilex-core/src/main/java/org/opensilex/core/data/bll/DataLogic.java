//******************************************************************************
//                          DataLogic.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.bll;

import com.mongodb.client.model.CountOptions;
import com.mongodb.client.result.DeleteResult;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.api.DataExportDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.*;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.experiment.utils.ImportDataIndex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.StreamWithPagination;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class containing all logic used by DataAPI, handles usage of different DAOs
 */
public class DataLogic {
    //TODO Call other Logic classes, one day this class should only directly use a single Dao, DataDao(V2)

    //Data initialized in constructor :
    private final DataDaoV2 dao;
    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel user;

    public DataLogic(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user) {
        this.dao = new DataDaoV2(sparql, nosql, fs);
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.user = user;
    }

    //Private stored data
    private final Map<DeviceModel, List<URI>> variablesToDevices = new HashMap<>();
    private Map<URI, URI> rootDeviceTypes = null;
    private final String expHeader = "experiment";
    private final String targetHeader = "target";
    private final String dateHeader = "date";
    private final String deviceHeader = "device";
    private final String rawdataHeader = "raw_data";
    private final String soHeader = "scientific_object";
    private final String annotationHeader = "object_annotation";


    //PUBLIC METHODS ==================================================================================================

    /**
     *
     * Validates - throws or inserts the models
     */
    public List<URI> addListData(List<DataModel> modelList) throws Exception{

        modelList = validData(modelList);

        dao.create(modelList);
        if(!variablesToDevices.isEmpty()) {

            DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
            for (Map.Entry variablesToDevice : variablesToDevices.entrySet() ){

                deviceDAO.associateVariablesToDevice((DeviceModel) variablesToDevice.getKey(),(List<URI>)variablesToDevice.getValue(), user );

            }
        }
        List<URI> createdResources = new ArrayList<>();
        for (DataModel data : modelList) {
            createdResources.add(data.getUri());
        }
        return createdResources;
    }

    public DataModel get(URI uri) throws NoSQLInvalidURIException {
        return dao.get(uri);
    }

    /**
     * Fetches and return all URIs for the variable with the xsd:date datatype.
     *
     * @return
     * @throws Exception
     */
    public Set<URI> getAllDateVariables() throws Exception {
        return new HashSet<>(sparql.searchURIs(VariableModel.class, null, selectBuilder -> {
            Var uriVar = SPARQLQueryHelper.makeVar(VariableModel.URI_FIELD);
            selectBuilder.addWhere(uriVar, Oeso.hasDataType.asNode(), XSD.date.asNode());
        }));
    }

    public <T> ListWithPagination<T> getDataList(MongoSearchQuery<DataModel, DataSearchFilter, T> query){
        return dao.searchWithPagination(query);
    }

    public StreamWithPagination<DataModel> getDataListStream(DataSearchFilter filter){
        return dao.searchAsStreamWithPagination(filter);
    }

    /**
     *
     * @param forWideFormat if true it's for wide export, else for long format
     * @return All of the data needed to perform an export
     */
    public DataExportInformation getDataExportInformation(boolean forWideFormat, DataSearchFilter filter, Logger logger) throws Exception {
        //Init everything needed for result
        Map<URI, VariableModel> variables = new HashMap<>();
        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();
        //Use this next map only in the case of wide format
        Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant = new HashMap<>();
        //And use this next map only when in long format
        HashMap<Instant, List<DataGetDTO>> dataByInstant = new HashMap<>();


        //Get data
        Instant start = Instant.now();
        List<DataModel> resultList = new ArrayList<>();
        //TODO optimization the prepare stuff can just go in the stream handler ?
        dao.searchAsStreamWithPagination(filter).forEach(resultList::add);

        Instant data = Instant.now();
        logger.debug(resultList.size() + " observations retrieved " + Long.toString(Duration.between(start, data).toMillis()) + " milliseconds elapsed");

        Set<URI> dateVariables = getAllDateVariables();


        //Prepare all the other things we have to fetch (variables, objects...)
        for (DataModel dataModel : resultList) {
            if (dataModel.getTarget() != null && !objects.containsKey(dataModel.getTarget())) {
                objects.put(dataModel.getTarget(), null);
            }

            if (!variables.containsKey(dataModel.getVariable())) {
                variables.put(dataModel.getVariable(), null);
            }

            if (!provenances.containsKey(dataModel.getProvenance().getUri())) {
                provenances.put(dataModel.getProvenance().getUri(), null);
            }

            if (forWideFormat && !dataByIndexAndInstant.containsKey(dataModel.getDate())) {
                dataByIndexAndInstant.put(dataModel.getDate(), new HashMap<>());
            }

            if(!forWideFormat){
                if (!dataByInstant.containsKey(dataModel.getDate())) {
                    dataByInstant.put(dataModel.getDate(), new ArrayList<>());
                }
                dataByInstant.get(dataModel.getDate()).add(DataGetDTO.getDtoFromModel(dataModel, dateVariables));
            }

            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
                    }
                    //Everything else in for loop is for wide format only
                    if(!forWideFormat){
                        continue;
                    }

                    ExportDataIndex exportDataIndex = new ExportDataIndex(
                            exp,
                            dataModel.getProvenance().getUri(),
                            dataModel.getTarget()
                    );

                    if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                        dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                    }
                    dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, exp, dateVariables));
                }
            } else if(forWideFormat) {
                //Everything in this else block is for wide format only
                ExportDataIndex exportDataIndex = new ExportDataIndex(
                        null,
                        dataModel.getProvenance().getUri(),
                        dataModel.getTarget()
                );

                if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                    dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                }
                dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, null, dateVariables));
            }
        }
        Instant dataTransform = Instant.now();
        logger.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");


        //Get other stuff we have to get (variables, objects, etc...)
        List<VariableModel> variablesModelList = new VariableDAO(sparql,nosql,fs).getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(new URI(SPARQLDeserializers.getShortURI(variableModel.getUri())), variableModel);
        }
        Instant variableTime = Instant.now();
        logger.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");

        OntologyDAO ontologyDao = new OntologyDAO(sparql);
        // Provide the experiment as context if there is only one, and only in wide format.
        URI context = null;
        if (forWideFormat && experiments.size() == 1) {
            context = experiments.keySet().stream().findFirst().get();
        }
        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), context);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        logger.debug("Get " + objectsList.size() + " target(s) " + Long.toString(Duration.between(variableTime, targetTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDaoV2 provenanceDao = new ProvenanceDaoV2(nosql.getServiceV2());
        List<ProvenanceModel> provenanceModels = provenanceDao.findByUris(provenances.keySet().parallelStream(), provenances.size());
        for (ProvenanceModel prov : provenanceModels) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        logger.debug("Get " + provenanceModels.size() + " provenance(s) " + Long.toString(Duration.between(targetTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        logger.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");


        //Handle return
        DataExportInformation result;
        if(forWideFormat){
            result = new DataWideExportInformation();
            ((DataWideExportInformation)result).setDataByIndexAndInstant(dataByIndexAndInstant);
        }else{
            result = new DataLongExportInformation();
            ((DataLongExportInformation)result).setDataByInstant(dataByInstant);
        }
        result.setVariables(variables)
                .setObjects(objects)
                .setProvenances(provenances)
                .setExperiments(experiments);

        return result;
    }

    public List<ProvenanceModel> searchUsedProvenances(
            List<URI> experiments,
            List<URI> targets,
            List<URI> variables,
            List<URI> devices) {

        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        filter.setExperiments(experiments);
        filter.setTargets(targets);
        filter.setVariables(variables);
        filter.setDevices(devices);

        List<URI> provenanceURIs = dao.distinct(null, "provenance.uri", URI.class, filter);
        List<ProvenanceModel> resultList = new ArrayList<>();

        if(!provenanceURIs.isEmpty()){
            //TODO dont use ProvenanceDaoV2 when provenance logic class has been created
            ProvenanceDaoV2 provenanceDao = new ProvenanceDaoV2(nosql.getServiceV2());
            resultList = provenanceDao.findByUris(provenanceURIs.stream(), provenanceURIs.size());
        }
        return resultList;
    }

    public long countData(DataSearchFilter filter, CountOptions countOptions){
        return dao.count(null, filter, countOptions);
    }

    public void delete(URI uri) throws NoSQLInvalidURIException {
        dao.delete(uri);
    }

    public void updateConfidence(URI dataUri, Float confidence) throws NoSQLInvalidURIException{
        DataModel data = dao.get(dataUri);
        data.setConfidence(confidence);
        dao.update(data);
    }

    public Stream<VariableModel> getUsedVariablesAsStream(DataSearchFilter filter){
        return dao.distinct(null, DataModel.VARIABLE_FIELD, VariableModel.class, filter)
                .stream();
    }

    public void update(DataModel model) throws Exception{
        validData(Collections.singletonList(model));
        dao.update(model);
    }

    public DeleteResult deleteManyByFilter(DataSearchFilter filter){
        return dao.deleteMany(filter);
    }

    /**
     * Validates the csv for data import after verifying provenance and experiment
     */
    public DataCSVValidationModel validateWholeCSV(boolean forInsertion, URI provenance, URI experiment, InputStream file, Logger logger) throws Exception {
        // test prov
        ProvenanceModel provenanceModel;
        ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        }

        // test exp
        if(experiment != null) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
            xpDAO.validateExperimentAccess(experiment, user);
        }

        //Validate csv
        DataCSVValidationModel validation;
        validation = validateWholeCSVInnerCode(provenanceModel, experiment, file, logger);

        //Set DataCSVValidationModel attributes
        validation.setValidCSV(!validation.hasErrors());
        if(forInsertion){
            validation.setInsertionStep(true);
        }else{
            validation.setValidationStep(true);
        }

        validation.setNbLinesToImport(validation.getData().size());
        return validation;
    }

    //PRIVATE METHODS ================================================================================================
    /**
     * Check variable data list before creation
     * Complete the prov_was_associated_with provenance attribut
     *
     * @param dataList
     * @throws Exception
     */
    private List<DataModel> validData(List<DataModel> dataList) throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql,nosql,fs);

        Map<URI, VariableModel> variableURIs = new HashMap<>();
        Set<URI> notFoundedVariableURIs = new HashSet<>();
        Set<URI> targetURIs = new HashSet<>();
        Set<URI> notFoundedTargetURIs = new HashSet<>();
        Set<URI> provenanceURIs = new HashSet<>();
        Set<URI> notFoundedProvenanceURIs = new HashSet<>();
        Set<URI> expURIs = new HashSet<>();
        Set<URI> notFoundedExpURIs = new HashSet<>();
        Map<DeviceModel, URI> variableCheckedDevice =  new HashMap<>();
        Map<URI, DeviceModel> provenanceToDevice =  new HashMap<>();

        List<DataModel> validData = new ArrayList<>();
        for (DataModel data : dataList) {

            boolean hasTarget = false;
            // check variable uri and datatype
            if (data.getVariable() != null) {  // and if null ?
                VariableModel variable = null;
                URI variableURI = data.getVariable();
                if (!variableURIs.containsKey(variableURI)) {
                    variable = variableDAO.get(variableURI);
                    if (variable == null) {
                        notFoundedVariableURIs.add(variableURI);
                    } else {
                        if (variable.getDataType() == null) {
                            throw new NoVariableDataTypeException(variableURI);
                        }
                        variableURIs.put(variableURI, variable);
                    }
                } else {
                    variable = variableURIs.get(variableURI);

                }
                if(!notFoundedVariableURIs.contains(variableURI)) {
                    setDataValidValue(variable, data);
                }
            }


            //check targets uri
            if (data.getTarget() != null) {
                hasTarget = true ;
                if (!targetURIs.contains(data.getTarget())) {
                    targetURIs.add(data.getTarget());
                    if (!sparql.uriExists((Node) null, data.getTarget())) {
                        hasTarget = false ;
                        notFoundedTargetURIs.add(data.getTarget());
                    }
                }
            }

            //check provenance uri and variables device association
            ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
            if (!provenanceURIs.contains(data.getProvenance().getUri())) {
                provenanceURIs.add(data.getProvenance().getUri());
                if (!provDAO.exists(data.getProvenance().getUri())) {
                    notFoundedProvenanceURIs.add(data.getProvenance().getUri());
                }
            }

            if(!notFoundedProvenanceURIs.contains(data.getProvenance().getUri())){
                variablesDeviceAssociation(provDAO, data, hasTarget, variableCheckedDevice, provenanceToDevice);
            }

            // check experiments uri
            if (data.getProvenance().getExperiments() != null) {
                for (URI exp : data.getProvenance().getExperiments()) {
                    if (!expURIs.contains(exp)) {
                        expURIs.add(exp);
                        if (!sparql.uriExists(ExperimentModel.class, exp)) {
                            notFoundedExpURIs.add(exp);
                        }
                    }
                }
            }
            validData.add(data);
        }

        if (!notFoundedVariableURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong variable uris: ", new ArrayList<>(notFoundedVariableURIs));// NOSQL Exception ? come from sparql request
        }
        if (!notFoundedTargetURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong target uris", new ArrayList<>(notFoundedTargetURIs)); // NOSQL Exception ? come from sparql request
        }
        if (!notFoundedProvenanceURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong provenance uris: ", new ArrayList<>(notFoundedProvenanceURIs));
        }
        if (!notFoundedExpURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong experiments uris: ", new ArrayList<>(notFoundedExpURIs)); // NOSQL Exception ? come from sparql request
        }

        return validData;
    }

    /**
     * Does the actual validating once we have loaded our provenance
     */
    private DataCSVValidationModel validateWholeCSVInnerCode(ProvenanceModel provenance, URI experiment, InputStream file, Logger logger) throws Exception {
        //TODO DAOs other than DataDao used in this function. Change when logic layer is done elsewhere

        DataCSVValidationModel csvValidation = new DataCSVValidationModel();
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        Map<String, SPARQLNamedResourceModel> nameURITargets = new HashMap<>();
        List<String> notExistingTargets = new ArrayList<>();
        List<String> duplicatedTargets = new ArrayList<>();

        ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
        Map<String, ExperimentModel> nameURIExperiments = new HashMap<>();
        List<String> notExistingExperiments = new ArrayList<>();
        List<String> duplicatedExperiments = new ArrayList<>();


        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);
        Map<String, SPARQLNamedResourceModel> nameURIScientificObjectsInXp = new HashMap<>();
        List<String> scientificObjectsNotInXp = new ArrayList<>();


        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        Map<String,DeviceModel> nameURIDevices = new HashMap<>();
        List<String> notExistingDevices = new ArrayList<>();
        List<String> duplicatedDevices = new ArrayList<>();

        List<AgentModel> agents = provenance.getAgents();
        boolean sensingDeviceFoundFromProvenance = false;
        if (agents !=  null) {
            for (AgentModel agent:agents) {
                if (agent.getRdfType() != null && deviceDAO.isDeviceType(agent.getRdfType())) {
                    sensingDeviceFoundFromProvenance = true;
                    break;
                }
            }
        }

        Map<String,DeviceModel> variableCheckedProvDevice =  new HashMap<>();
        List<String> checkedVariables = new ArrayList<>();

        Map<String, DeviceModel> variableCheckedDevice = new HashMap<>();

        Map<Integer, String> headerByIndex = new HashMap<>();

        List<ImportDataIndex> duplicateDataByIndex = new ArrayList<>();

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            logger.debug("Import data - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            // Line 1
            String[] ids = csvReader.parseNext();
            Set<String> headers = Arrays.stream(ids).filter(Objects::nonNull).map(id -> id.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
            if (!headers.contains(deviceHeader) && !headers.contains(targetHeader) && !headers.contains(soHeader) && !sensingDeviceFoundFromProvenance) {
                csvValidation.addMissingHeaders(Arrays.asList(deviceHeader + " or " + targetHeader + " or " + soHeader));
            }
            // Check that there is an soHeader or a targetHeader if there is an annotationHeader otherwise create error
            if(headers.contains(annotationHeader) && !headers.contains(targetHeader) && !headers.contains(soHeader)){
                csvValidation.addMissingHeaders(Arrays.asList(targetHeader + " or " + soHeader));
            }

            // 1. check variables
            HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
            VariableDAO dao = new VariableDAO(sparql,nosql,fs);

            if (ids != null) {

                for (int i = 0; i < ids.length; i++) {
                    String header = ids[i];
                    if (header == null) {
                        csvValidation.addEmptyHeader(i+1);
                    } else {

                        if (header.equalsIgnoreCase(expHeader) || header.equalsIgnoreCase(targetHeader)
                                || header.equalsIgnoreCase(dateHeader) || header.equalsIgnoreCase(deviceHeader) || header.equalsIgnoreCase(soHeader)
                                || header.equalsIgnoreCase(rawdataHeader) || header.equalsIgnoreCase(annotationHeader)) {
                            headerByIndex.put(i, header);

                        } else {
                            try {
                                if (!URIDeserializer.validateURI(header)) {
                                    csvValidation.addInvalidHeaderURI(i, header);
                                } else {
                                    VariableModel var = dao.get(URI.create(header));
                                    // boolean uriExists = sparql.uriExists(VariableModel.class, URI.create(header));
                                    if (var == null) {
                                        csvValidation.addInvalidHeaderURI(i, header);
                                    } else {
                                        mapVariableUriDataType.put(var.getUri(), var.getDataType());
                                        // TODO : Validate duplicate variable colonne
                                        headerByIndex.put(i, header);
                                    }
                                }
                            } catch (URISyntaxException e) {
                                csvValidation.addInvalidHeaderURI(i, ids[i]);
                            }
                        }
                    }
                }

                // 1.1 return error variables
                if (csvValidation.hasErrors()) {
                    return csvValidation;
                }
                csvValidation.setHeadersFromArray(ids);

                int rowIndex = 0;
                String[] values;

                // Line 2
                String[] headersLabels = csvReader.parseNext();
                csvValidation.setHeadersLabelsFromArray(headersLabels);

                // Line 3
                csvReader.parseNext();
                // Line 4
                int nbError = 0;
                boolean validateCSVRow = false;
                while ((values = csvReader.parseNext()) != null) {
                    try {
                        validateCSVRow = validateCSVRow(
                                provenance,
                                experiment,
                                sensingDeviceFoundFromProvenance,
                                variableCheckedDevice,
                                variableCheckedProvDevice,
                                checkedVariables,
                                values,
                                rowIndex,
                                csvValidation,
                                headerByIndex,
                                xpDAO,
                                notExistingExperiments,
                                duplicatedExperiments,
                                nameURIExperiments,
                                ontologyDAO,
                                notExistingTargets,
                                duplicatedTargets,
                                nameURITargets,
                                scientificObjectDAO,
                                nameURIScientificObjectsInXp,
                                scientificObjectsNotInXp,
                                deviceDAO,
                                notExistingDevices,
                                duplicatedDevices,
                                nameURIDevices,
                                mapVariableUriDataType,
                                duplicateDataByIndex);
                    } catch (CSVDataTypeException e) {
                        csvValidation.addInvalidDataTypeError(e.getCsvCell());
                    }
                    rowIndex++;
                    if (!validateCSVRow) {
                        nbError++;
                    }
                    if (nbError >= ExperimentAPI.CSV_NB_ERRORS_MAX) {
                        break;
                    }
                }
            }
        }

        if (csvValidation.getData().keySet().size() >  DataAPI.SIZE_MAX) {
            csvValidation.setTooLargeDataset(true);
        }

        return csvValidation;
    }

    private boolean validateCSVRow(
            ProvenanceModel provenance,
            URI experiment,
            boolean sensingDeviceFoundFromProvenance,
            Map<String, DeviceModel> variableCheckedDevice,
            Map<String,DeviceModel> variableCheckedProvDevice,
            List<String> checkedVariables,
            String[] values,
            int rowIndex,
            DataCSVValidationModel csvValidation,
            Map<Integer, String> headerByIndex,
            ExperimentDAO xpDAO,
            List<String> notExistingExperiments,
            List<String> duplicatedExperiments,
            Map<String, ExperimentModel> nameURIExperiments,
            OntologyDAO ontologyDAO,
            List<String> notExistingTargets,
            List<String> duplicatedTargets,
            Map<String, SPARQLNamedResourceModel> nameURITargets,
            ScientificObjectDAO scientificObjectDAO,
            Map<String, SPARQLNamedResourceModel> nameURIScientificObjects,
            List<String> scientificObjectsNotInXp,
            DeviceDAO deviceDAO,
            List<String> notExistingDevices,
            List<String> duplicatedDevices,
            Map<String, DeviceModel> nameURIDevices,
            HashMap<URI, URI> mapVariableUriDataType,
            List<ImportDataIndex> duplicateDataByIndex)
            throws CSVDataTypeException, TimezoneAmbiguityException, TimezoneException, URISyntaxException, Exception {

        boolean validRow = true;

        ParsedDateTimeMongo parsedDateTimeMongo = null;

        List<URI> experiments = new ArrayList<>();
        SPARQLNamedResourceModel target = null;

        Boolean missingTargetOrDevice = false;
        int targetColIndex = 0;
        int deviceColIndex = 0;

        AnnotationModel annotationFromAnnotationColumn = null;
        int annotationIndex = 0;

        DeviceModel deviceFromDeviceColumn = null;
        SPARQLNamedResourceModel object = null;
        if( experiment != null) {
            experiments.add(experiment);
        }

        //Set to remember which columns to do at end of row iteration (in case required columns like target are at the end).
        Set<Integer> colsToDoAtEnd = new HashSet<>();

        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (headerByIndex.get(colIndex).equalsIgnoreCase(expHeader)) {
                //check experiment column
                ExperimentModel exp = null;
                String expNameOrUri = values[colIndex];
                // test in uri list
                if (!StringUtils.isEmpty(expNameOrUri)) {
                    if (nameURIExperiments.containsKey(expNameOrUri)) {
                        exp = nameURIExperiments.get(expNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedExperiments.contains(expNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                            csvValidation.addDuplicateExperimentError(cell);
                            validRow = false;
                        } else if (!notExistingExperiments.contains(expNameOrUri)) {
                            try {
                                exp = getExperimentByNameOrURI(xpDAO, expNameOrUri);
                                if (exp == null) {
                                    if (!notExistingExperiments.contains(expNameOrUri)) {
                                        notExistingExperiments.add(expNameOrUri);
                                    }

                                    CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                                    csvValidation.addInvalidExperimentError(cell);
                                    validRow = false;
                                } else {
                                    nameURIExperiments.put(expNameOrUri, exp);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                                csvValidation.addDuplicateExperimentError(cell);
                                duplicatedExperiments.add(expNameOrUri);
                                validRow = false;
                            }
                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                            csvValidation.addInvalidExperimentError(cell);
                            validRow = false;

                        }
                    }
                }
                if (exp != null) {
                    experiments.add(exp.getUri());
                }


            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(targetHeader)) {
                //check target column
                String targetNameOrUri = values[colIndex];
                targetColIndex = colIndex;

                if (!StringUtils.isEmpty(targetNameOrUri)){
                    if (nameURITargets.containsKey(targetNameOrUri)) {
                        target = nameURITargets.get(targetNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedTargets.contains(targetNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                            csvValidation.addDuplicateTargetError(cell);
                            validRow = false;
                        } else if (!notExistingTargets.contains(targetNameOrUri)) {
                            try {
                                target = getTargetByNameOrURI(ontologyDAO, targetNameOrUri);
                                if (target == null) {
                                    if (!notExistingTargets.contains(targetNameOrUri)) {
                                        notExistingTargets.add(targetNameOrUri);
                                    }

                                    CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                                    csvValidation.addInvalidTargetError(cell);
                                    validRow = false;
                                } else {
                                    nameURITargets.put(targetNameOrUri, target);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                                csvValidation.addDuplicateTargetError(cell);
                                duplicatedTargets.add(targetNameOrUri);
                                validRow = false;
                            }

                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                            csvValidation.addInvalidTargetError(cell);
                            validRow = false;
                        }

                    }
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(soHeader)) {

                String objectNameOrUri = values[colIndex];
                // check if the object name/uri has been previously referenced -> if so, no need to re-perform a check with the Dao
                if (!StringUtils.isEmpty(objectNameOrUri) && nameURIScientificObjects.containsKey(objectNameOrUri)) {
                    object = nameURIScientificObjects.get(objectNameOrUri);
                } else {

                    SPARQLNamedResourceModel existingOs = null;
                    Node experimentNode = experiment == null ? null : SPARQLDeserializers.nodeURI(experiment);

                    // check if the object has been previously referenced as unknown, if not, then performs a check with Dao
                    if (!StringUtils.isEmpty(objectNameOrUri) && !scientificObjectsNotInXp.contains(objectNameOrUri)) {
                        existingOs = testNameOrURI(scientificObjectDAO, csvValidation, rowIndex, colIndex, experimentNode, objectNameOrUri);
                    }

                    if(existingOs == null){
                        validRow = false;
                        scientificObjectsNotInXp.add(objectNameOrUri);
                    }else{
                        object = existingOs;
                        // object exist, put it into name/URI cache
                        nameURIScientificObjects.put(objectNameOrUri,existingOs);
                    }
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(dateHeader)) {
                // check date
                // TODO : Validate timezone ambiguity
                parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(values[colIndex], null);
                if (parsedDateTimeMongo == null) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, values[colIndex], "DATE");
                    csvValidation.addInvalidDateError(cell);
                    validRow = false;
                    break;
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(deviceHeader)){
                // check device column
                String deviceNameOrUri = values[colIndex];
                deviceColIndex = colIndex;

                // test in uri list
                if (!StringUtils.isEmpty(deviceNameOrUri)) {
                    if (nameURIDevices.containsKey(deviceNameOrUri)) {
                        deviceFromDeviceColumn = nameURIDevices.get(deviceNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedDevices.contains(deviceNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                            csvValidation.addDuplicateDeviceError(cell);
                            validRow = false;
                        } else if (!notExistingDevices.contains(deviceNameOrUri)) {
                            try {
                                deviceFromDeviceColumn = getDeviceByNameOrURI(deviceDAO, deviceNameOrUri);
                                if (deviceFromDeviceColumn == null) {
                                    if (!notExistingDevices.contains(deviceNameOrUri)) {
                                        notExistingDevices.add(deviceNameOrUri);
                                    }
                                    CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                                    csvValidation.addInvalidDeviceError(cell);
                                    validRow = false;
                                } else {
                                    nameURIDevices.put(deviceNameOrUri, deviceFromDeviceColumn);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                                csvValidation.addDuplicateDeviceError(cell);
                                duplicatedDevices.add(deviceNameOrUri);
                                validRow = false;
                            }
                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                            csvValidation.addInvalidDeviceError(cell);
                            validRow = false;
                        }

                    }
                }

            }
            //If we are at the annotation column, and the cell isn't empty, create a new Annotation Model.
            //Set the motivation to commenting, and leave the target for now until we're sure that the target column has already been imported
            else if (headerByIndex.get(colIndex).equalsIgnoreCase(annotationHeader)){
                String annotation = values[colIndex];
                annotationIndex = colIndex;
                if(!StringUtils.isEmpty(annotation)){
                    annotationFromAnnotationColumn = new AnnotationModel();
                    annotationFromAnnotationColumn.setDescription(annotation.trim());
                    annotationFromAnnotationColumn.setPublisher(user.getUri());
                    MotivationModel motivationModel = new MotivationModel();
                    motivationModel.setUri(URI.create(OA.commenting.getURI()));
                    annotationFromAnnotationColumn.setMotivation(motivationModel);
                }
            }else if (!headerByIndex.get(colIndex).equalsIgnoreCase(rawdataHeader)) { // Variable/Value column
                if (headerByIndex.containsKey(colIndex)) {
                    // If value is not blank and null
                    if (!StringUtils.isEmpty(values[colIndex])) {
                        colsToDoAtEnd.add(colIndex);
                    }
                }
            }
        }
        //Do the variable value columns now that we know the target or device is loaded if the user correctly filled it
        for(Integer colIndex : colsToDoAtEnd){
            if (validRow) {
                String variable = headerByIndex.get(colIndex);
                URI varURI = URI.create(variable);
                if (deviceFromDeviceColumn == null && target == null && object == null) {
                    missingTargetOrDevice = true;
                    validRow = false;
                    break;
                }
                if (deviceFromDeviceColumn != null) {
                    boolean variableIsChecked = variableCheckedDevice.containsKey(variable) && variableCheckedDevice.get(variable) == deviceFromDeviceColumn;
                    if (!variableIsChecked) {
                        if (!variableIsAssociatedToDevice(deviceFromDeviceColumn, varURI)) {
                            csvValidation.addVariableToDevice(deviceFromDeviceColumn, varURI);
                        }
                        variableCheckedDevice.put(variable, deviceFromDeviceColumn);
                    }

                } else if (sensingDeviceFoundFromProvenance) {
                    if (!checkedVariables.contains(variable)) { // do it one time but write the error on each row if there is one
                        List<DeviceModel> devices = new ArrayList<>();
                        List<DeviceModel> linkedDevice = new ArrayList<>();
                        DeviceModel dev = null;
                        for (AgentModel agent : provenance.getAgents()) {
                            if (agent.getRdfType() != null && deviceDAO.isDeviceType(agent.getRdfType())) {
                                dev = deviceDAO.getDeviceByURI(agent.getUri(), user);
                                if (dev != null) {

                                    if (variableIsAssociatedToDevice(dev, varURI)) {
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
                                    CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID");  // add specific exception
                                    csvValidation.addDeviceChoiceAmbiguityError(cell);
                                    validRow = false;
                                    break;
                                } else {
                                    if (!devices.isEmpty()) {
                                        csvValidation.addVariableToDevice(devices.get(0), varURI);
                                        variableCheckedProvDevice.put(variable, devices.get(0));
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

                                variableCheckedProvDevice.put(variable, linkedDevice.get(0));
                                break;

                            default:
                                //which device to choose ?
                                CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID"); // add specific exception
                                csvValidation.addDeviceChoiceAmbiguityError(cell);
                                validRow = false;
                                break;
                        }
                        checkedVariables.add(variable);
                    } else {
                        if (!variableCheckedProvDevice.containsKey(variable)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID");  // add specific exception
                            csvValidation.addDeviceChoiceAmbiguityError(cell);
                            break;
                        }
                    }
                }
                if(validRow) {
                    DataModel dataModel = new DataModel();
                    DataProvenanceModel provenanceModel = new DataProvenanceModel();
                    provenanceModel.setUri(provenance.getUri());

                    if (!experiments.isEmpty()) {
                        provenanceModel.setExperiments(experiments);
                    }

                    if (deviceFromDeviceColumn != null) {
                        ProvEntityModel agent = new ProvEntityModel();
                        if (rootDeviceTypes == null) {
                            rootDeviceTypes = getRootDeviceTypes();
                        }
                        URI rootType = rootDeviceTypes.get(deviceFromDeviceColumn.getType());
                        agent.setType(rootType);
                        agent.setUri(deviceFromDeviceColumn.getUri());
                        provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));

                    } else if (sensingDeviceFoundFromProvenance) {

                        DeviceModel checkedDevice = variableCheckedProvDevice.get(variable);
                        ProvEntityModel agent = new ProvEntityModel();
                        if (rootDeviceTypes == null) {
                            rootDeviceTypes = getRootDeviceTypes();
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
                    DataValidateUtils.checkAndConvertValue(dataModel, varURI, values[colIndex].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex, csvValidation);

                    if (colIndex + 1 < values.length) {
                        if (headerByIndex.get(colIndex + 1).equalsIgnoreCase(rawdataHeader) && values[colIndex + 1] != null) {
                            dataModel.setRawData(DataValidateUtils.returnValidRawData(varURI, values[colIndex + 1].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex + 1, csvValidation));
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
                    if(deviceFromDeviceColumn != null) {
                        deviceUri = deviceFromDeviceColumn.getUri();
                    }
                    ImportDataIndex importDataIndex = new ImportDataIndex(parsedDateTimeMongo.getInstant(), varURI, provenance.getUri(), targetUri, deviceUri);
                    if (!duplicateDataByIndex.contains(importDataIndex)) {
                        duplicateDataByIndex.add(importDataIndex);
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
        if( annotationFromAnnotationColumn != null ){
            if(target == null && object == null){
                CSVCell annotationCell = new CSVCell(rowIndex, annotationIndex, annotationFromAnnotationColumn.getDescription(), annotationHeader);
                csvValidation.addInvalidAnnotationError(annotationCell);
                validRow = false;
            }else{
                if(validRow){
                    annotationFromAnnotationColumn.setTargets(Collections.singletonList( target==null ? object.getUri() : target.getUri()));
                    csvValidation.addToAnnotationsOnObjects(annotationFromAnnotationColumn);
                }
            }
        }

        if (missingTargetOrDevice) {
            //the device or the target is mandatory if there is no device in the provenance
            CSVCell cell1 = new CSVCell(rowIndex, deviceColIndex, null, deviceHeader);
            CSVCell cell2 = new CSVCell(rowIndex, targetColIndex, null, targetHeader);
            csvValidation.addMissingRequiredValue(cell1);
            csvValidation.addMissingRequiredValue(cell2);
        }

        return validRow;
    }

    private ExperimentModel getExperimentByNameOrURI(ExperimentDAO xpDAO, String expNameOrUri) throws Exception {
        ExperimentModel exp = null;
        if (URIDeserializer.validateURI(expNameOrUri)) {
            URI expUri = URI.create(expNameOrUri);
            try {
                exp = xpDAO.get(expUri, user);
            } catch (Exception ex) {

            }
        } else {
            exp = xpDAO.getByName(expNameOrUri);
        }
        return exp;
    }

    private DeviceModel getDeviceByNameOrURI(DeviceDAO deviceDAO, String deviceNameOrUri) throws Exception {
        DeviceModel device;
        if (URIDeserializer.validateURI(deviceNameOrUri)) {
            URI deviceURI = URI.create(deviceNameOrUri);
            device = deviceDAO.getDeviceByURI(deviceURI, user);
        } else {
            try {
                device = deviceDAO.getByName(deviceNameOrUri);
            } catch (Exception ex) {
                throw ex;
            }

        }
        return device;
    }

    private SPARQLNamedResourceModel<?> getTargetByNameOrURI(OntologyDAO dao, String targetNameOrUri) throws Exception {
        SPARQLNamedResourceModel<?> target = new SPARQLNamedResourceModel<>();
        if (URIDeserializer.validateURI(targetNameOrUri)) {
            URI targetUri = URI.create(targetNameOrUri);
            if (sparql.executeAskQuery(new AskBuilder()
                    .addWhere(SPARQLDeserializers.nodeURI(targetUri), RDFS.label, "?label")
            )) {
                target.setUri(targetUri);
            } else {
                target = null;
            }
        } else {
            List<SPARQLNamedResourceModel> results = dao.getByName(targetNameOrUri);
            if (results.size()>1) {
                throw new DuplicateNameException(targetNameOrUri);
            } else {
                if(!results.isEmpty()) {
                    target = results.get(0);
                } else {
                    target = null ;
                }
            }
        }
        return target;
    }


    private SPARQLNamedResourceModel testNameOrURI(ScientificObjectDAO dao, CSVValidationModel validation, int rowIndex, int colIndex, Node experiment, String nameOrUri) throws Exception {

        // check if object exist by URI inside experiment
        if (URIDeserializer.validateURI(nameOrUri)) {
            URI objectUri = URI.create(nameOrUri);

            SPARQLNamedResourceModel existingObject = sparql.getByURI(experiment, ScientificObjectModel.class,objectUri,null);
            if (existingObject == null) {
                validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, "OBJECT_ID"));
                return null;
            }
            return existingObject;

            // check if object exist by name inside experiment
        } else if (experiment != null) {
            SPARQLNamedResourceModel existingObject = dao.getUriByNameAndGraph(experiment, nameOrUri);
            if (existingObject == null) {
                validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, "OBJECT_ID"));
                return null;
            }
            return existingObject;
        } else {
            // ambiguity about name inside global OS graph, indeed, several OS can have the same name inside the global graph,
            // so there are no guarantee that a unique OS URI will be found with this name
            validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, "OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT"));
            return null;
        }
    }

    // Map who associate each type with its root type
    private Map<URI, URI> getRootDeviceTypes() throws URISyntaxException, Exception {

        SPARQLTreeListModel<ClassModel> treeList = SPARQLModule.getOntologyStoreInstance().searchSubClasses(new URI(Oeso.Device.toString()), null, user.getLanguage(), true);
        List<ResourceTreeDTO> treeDtos = ResourceTreeDTO.fromResourceTree(treeList);

        Map<URI, URI> map = new HashMap<>();

        for (ResourceTreeDTO tree : treeDtos) {
            URI agentRootType = tree.getUri();
            List<ResourceTreeDTO> children = tree.getChildren();
            if (!children.isEmpty()) {
                childrenToRoot(children, map, agentRootType);
            }

            // Push root type inside map
            // It allow to recognize device with a type included inside the root types list
            map.put(tree.getUri(),tree.getUri());
        }

        return map;
    }

    /**
     * check that value is coherent with the variable datatype
     *
     * @param variable
     * @param data
     * @throws Exception
     */
    private void setDataValidValue(VariableModel variable, DataModel data) throws Exception {
        if (data.getValue() != null) {
            URI variableUri = variable.getUri();
            URI dataType = variable.getDataType();
            Object value = data.getValue();
            DataValidateUtils.checkAndConvertValue(data, variableUri, value, dataType);
        }
    }

    private void childrenToRoot( List<ResourceTreeDTO> children,Map<URI, URI> map, URI agentRootType){
        for (ResourceTreeDTO subTree : children) {
            map.put(subTree.getUri(), agentRootType);
            List<ResourceTreeDTO> child = subTree.getChildren();
            if (!child.isEmpty()) {
                childrenToRoot(child, map, agentRootType);
            }
        }
    }

    /**
     * First check in the data provenance if there is a device
     * Then check in the provenance and fill the data provenance with the device
     * If no device and no target return an exception
     *
     * @param provDAO
     * @param data
     * @param hasTarget
     * @param variableCheckedDevice
     * @param provenanceToDevice

     * @throws Exception
     */
    private void variablesDeviceAssociation(ProvenanceDaoV2 provDAO, DataModel data, boolean hasTarget, Map<DeviceModel, URI> variableCheckedDevice, Map<URI, DeviceModel> provenanceToDevice) throws Exception{

        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        URI provenanceURI = data.getProvenance().getUri();
        DeviceModel deviceFromProvWasAssociated = checkAndReturnDeviceFromDataProvenance(data, deviceDAO);
        if (deviceFromProvWasAssociated == null) {

            DeviceModel device = null;
            if(provenanceToDevice.containsKey(provenanceURI)) {
                device = provenanceToDevice.get(provenanceURI);
                //check
            } else {
                device = checkAndReturnDeviceFromProvenance(deviceDAO, provDAO, data);
                provenanceToDevice.put(provenanceURI,device);
            }

            if (device != null) {

                if (!variableIsAssociatedToDevice(device, data.getVariable())) {
                    addVariableToDevice(device,data.getVariable()); // add variable/device
                }

                if (rootDeviceTypes == null) {
                    rootDeviceTypes = getRootDeviceTypes();
                }

                DataProvenanceModel provMod = data.getProvenance();
                List<ProvEntityModel> agents = null;
                if (provMod.getProvWasAssociatedWith() == null) {
                    agents = new ArrayList<>();
                } else {
                    agents = provMod.getProvWasAssociatedWith();
                }

                ProvEntityModel agent = new ProvEntityModel();
                agent.setUri(device.getUri());
                URI rootType = rootDeviceTypes.get(device.getType());
                agent.setType(rootType);
                agents.add(agent);
                provMod.setProvWasAssociatedWith(agents);
                data.setProvenance(provMod);
            } else {
                if(!hasTarget) {
                    throw new DeviceOrTargetToDataException(data);
                }
            }

        } else {
            boolean deviceIsChecked = variableCheckedDevice.containsKey(deviceFromProvWasAssociated) && variableCheckedDevice.get(deviceFromProvWasAssociated) == data.getVariable() ;
            if(!deviceIsChecked){
                if (!variableIsAssociatedToDevice(deviceFromProvWasAssociated, data.getVariable())) {
                    addVariableToDevice(deviceFromProvWasAssociated,data.getVariable()); // add variable/device
                }
                variableCheckedDevice.put(deviceFromProvWasAssociated,data.getVariable());

            }
        }
    }

    //TODO Duplicate method in DataCSVValidation model, it seems to be exactly the same
    private void addVariableToDevice(DeviceModel device, URI variable) {

        if (!variablesToDevices.containsKey(device)) {
            List<URI> list = new ArrayList<>();
            list.add(variable);
            variablesToDevices.put(device, list);
        } else {
            if (!variablesToDevices.get(device).contains(variable)) {
                variablesToDevices.get(device).add(variable);
            }
        }
    }

    /**
     * check if variable is associated to device
     * @param device
     * @param variable
     * @throws Exception
     */
    private boolean variableIsAssociatedToDevice(DeviceModel device, URI variable){
        List<SPARQLModelRelation> variables = device.getRelations(Oeso.measures).collect(Collectors.toList());

        if (!variables.isEmpty()) {
            if (variables.stream().anyMatch(var -> (SPARQLDeserializers.compareURIs(var.getValue(), variable.toString())))) {
                return true;
            }
        }
        return false;

    }

    /**
     * check and return Device from Data Provenance if no ambiguity
     * Exception if two devices as provenance agent
     *
     * @param deviceDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromDataProvenance(DataModel data, DeviceDAO deviceDAO) throws Exception{

        boolean deviceIsLinked = false; // to test if there are 2 devices
        URI agentToReturn = null;
        DeviceModel device = null;
        if(data.getProvenance().getProvWasAssociatedWith()!= null && !data.getProvenance().getProvWasAssociatedWith().isEmpty()){
            for (ProvEntityModel agent : data.getProvenance().getProvWasAssociatedWith()) {

                if(agent.getType() == null) {
                    throw new ProvenanceAgentTypeException(agent.getUri().toString());
                }

                if (deviceDAO.isDeviceType(agent.getType())) {
                    if(!deviceIsLinked) {
                        deviceIsLinked = true;
                        agentToReturn = agent.getUri();

                    } else {
                        throw new DeviceProvenanceAmbiguityException(data.getProvenance().getUri().toString());
                    }
                }
            }
            if(agentToReturn != null){
                device = deviceDAO.getDeviceByURI(agentToReturn, user);
            }
        }
        return device;
    }

    /**
     * check and return Device from Provenance if no ambiguity
     *
     * @param deviceDAO
     * @param provDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromProvenance(DeviceDAO deviceDAO, ProvenanceDaoV2 provDAO, DataModel data) throws Exception {

        ProvenanceModel provenance = provDAO.get(data.getProvenance().getUri());

        DeviceModel deviceToReturn = null ;
        List<DeviceModel> devices = new ArrayList<>();
        List<DeviceModel> linkedDevices = new ArrayList<>();

        if (provenance.getAgents() != null && !provenance.getAgents().isEmpty()) {

            for (AgentModel agent : provenance.getAgents()) {
                if(agent.getRdfType() == null) {
                    throw new ProvenanceAgentTypeException(agent.getUri().toString());
                }
                if (deviceDAO.isDeviceType(agent.getRdfType())) {
                    DeviceModel device = deviceDAO.getDeviceByURI(agent.getUri(), user);
                    if (device != null) {
                        devices.add(device);

                        if(variableIsAssociatedToDevice(device, data.getVariable())){
                            linkedDevices.add(device);
                        }
                    }
                }
            }

            switch (linkedDevices.size()) {
                case 0:
                    if (devices.size() > 1) {
                        throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());
                    } else {
                        if (!devices.isEmpty()) {
                            deviceToReturn = devices.get(0);
                        }
                    }
                    break;
                case 1:
                    deviceToReturn = linkedDevices.get(0);
                    break;
                default :
                    throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());

            }

        }
        return deviceToReturn;
    }
}
