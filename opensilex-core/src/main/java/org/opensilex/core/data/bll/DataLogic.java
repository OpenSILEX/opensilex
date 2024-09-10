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
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.OA;
import org.bson.conversions.Bson;
import org.opensilex.core.annotation.dal.AnnotationDAO;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.core.data.api.*;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.dal.aggregations.DataTargetAggregateModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.MathematicalOperator;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.*;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.experiment.utils.ImportDataIndex;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.api.VariableDetailsDTO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.ExcludableUriList;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.core.data.utils.DataMathFunctions.computeMedianPerHour;

/**
 * Class containing all logic used by DataAPI, handles usage of different DAOs
 */
public class DataLogic {
    //TODO Call other Logic classes, one day this class should only directly use a single Dao, DataDao(V2)

    //Data initialized in constructor :
    private final DataDaoV2 dao;
    private final AccountModel user;
    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;

    //TODO these daos are the ones that will need to be deleted in the class when logic classes are done.
    //VariableDAO
    //DeviceDAO
    //ExperimentDAO
    //ScientificObjectDAO
    //ProvenanceDaoV2
    //OntologyDAO


    public DataLogic(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user) {
        this.dao = new DataDaoV2(sparql, nosql, fs);
        this.sparql = sparql;
        this.nosql = nosql;
        this.user = user;
        this.fs = fs;
    }

    //Private stored data
    private final Map<DeviceModel, List<URI>> variablesToDevices = new HashMap<>();
    private Map<URI, URI> rootDeviceTypes = null;
    private final String EXPERIMENT_HEADER = "experiment";
    private final String TARGET_HEADER = "target";
    private final String DATE_HEADER = "date";
    private final String DEVICE_HEADER = "device";
    private final String RAWDATA_HEADER = "raw_data";
    private final String SCIENTIFICOBJ_HEADER = "scientific_object";
    private final String ANNOTATION_HEADER = "object_annotation";

    //#region PUBLIC METHODS

    public DataModel get(URI uri) throws NoSQLInvalidURIException {
        return dao.get(uri);
    }

    public <T> ListWithPagination<T> searchWithPagination(MongoSearchQuery<DataModel, DataSearchFilter, T> query){
        return dao.searchWithPagination(query);
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

        //Get data and prepare all the other things we have to fetch (variables, objects...)

        Set<URI> dateVariables = new VariableDAO(sparql, nosql, fs, user).getAllDateVariables();

        dao.searchAsStreamWithPagination(filter).forEach(dataModel -> {
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
        });

        Instant data = Instant.now();

        Instant dataTransform = Instant.now();
        logger.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        //Get other stuff we have to get (variables, objects, etc...)
        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);
        List<VariableModel> variablesModelList = variableDAO.getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(new URI(SPARQLDeserializers.getShortURI(variableModel.getUri())), variableModel);
        }
        Instant variableTime = Instant.now();
        logger.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");

        // Provide the experiment as context if there is only one, and only in wide format.
        URI context = null;
        if (forWideFormat && experiments.size() == 1) {
            context = experiments.keySet().stream().findFirst().get();
        }
        List<SPARQLNamedResourceModel> objectsList = new OntologyDAO(sparql).getURILabels(objects.keySet(), user.getLanguage(), context);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        logger.debug("Get " + objectsList.size() + " target(s) " + Long.toString(Duration.between(variableTime, targetTime).toMillis()) + " milliseconds elapsed");

        List<ProvenanceModel> provenanceModels = new ProvenanceDaoV2(nosql.getServiceV2()).findByUris(provenances.keySet().parallelStream(), provenances.size());
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
            resultList = new ProvenanceDaoV2(nosql.getServiceV2()).findByUris(provenanceURIs.stream(), provenanceURIs.size());
        }
        return resultList;
    }

    public long countData(DataSearchFilter filter, CountOptions countOptions){
        return dao.count(null, filter, countOptions);
    }

    public long countData(DataSearchFilter filter){
        return dao.count(filter);
    }

    public void delete(URI uri) throws NoSQLInvalidURIException {
        dao.delete(uri);
    }

    public void updateConfidence(URI dataUri, Float confidence) throws NoSQLInvalidURIException{
        DataModel data = dao.get(dataUri);
        data.setConfidence(confidence);
        dao.update(data);
    }

    public List<VariableModel> getUsedVariables(List<URI> experiments, List<URI> objects, List<URI> provenances, List<URI> devices) throws Exception {
        DataSearchFilter dataSearchFilter = new DataSearchFilter();
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices).setTargets(objects).setProvenances(provenances);
        return getUsedVariablesByFilter(dataSearchFilter);
    }

    public void update(DataModel model) throws Exception{
        validData(Collections.singletonList(model));
        dao.update(model);
    }

    public DeleteResult deleteManyByFilter(DataSearchFilter filter){
        return dao.deleteMany(filter);
    }

    public List<URI> createMany(List<DataModel> modelList) throws Exception {
        return addListDataInnerCode(modelList, variablesToDevices, false, null);
    }

    public void createManyFromImport(List<DataModel> modelList, DataCSVValidationModel csvValidationModel) throws Exception {
        addListDataInnerCode(modelList, csvValidationModel.getVariablesToDevices(), true, csvValidationModel);
    }

    /**
     * Validates the csv for data import after verifying provenance and experiment
     */
    public DataCSVValidationModel validateWholeCSV(boolean forInsertion, URI provenance, URI experiment, InputStream file, Logger logger) throws Exception {
        // test prov
        ProvenanceModel provenanceModel;
        try {
            provenanceModel = new ProvenanceDaoV2(nosql.getServiceV2()).get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        }

        // test exp
        if(experiment != null) {
            new ExperimentDAO(sparql, nosql).validateExperimentAccess(experiment, user);
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

    public List<URI> getUsedTargets(List<URI> devices, List<URI> variables, List<URI> experiments) {
        DataSearchFilter dataSearchFilter = new DataSearchFilter().setVariables(variables);
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices);
        return dao.distinct(null, DataModel.TARGET_FIELD, URI.class, dataSearchFilter);
    }

    public Set<URI> getUsedVariablesByExpeSoDevice(List<URI> experiments, List<URI> objects, List<URI> devices ) {
        DataSearchFilter dataSearchFilter = new DataSearchFilter();
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices).setTargets(objects);
        return new HashSet<>(dao.distinct(null, DataModel.VARIABLE_FIELD, URI.class, dataSearchFilter));
    }

    //TODO this next function would make more sense being in the future ScientificObjectLogic class
    /**
     *
     * @param criteriaDTO
     * @return Null if criteria dto had no valid criteria, Object containing empty list if valid criteria but no results,
     * or a list of object uris if criteria were valid and results were found
     * The return object also contains a boolean to say if we need to keep only elements in the the list, or exclude them
     */
    public ExcludableUriList getScientificObjectsThatMatchDataCriteria(CriteriaDTO criteriaDTO, URI experiment) throws Exception {

        //Verify if that there is at least one complete line and if every line is a "NotMeasured"
        boolean atLeastOneCompleteSingle = criteriaDTO.getCriteriaList().stream().anyMatch(dto ->
                dto.getVariableUri() != null && dto.getCriteria() != null && (dto.getCriteria() == MathematicalOperator.NotMeasured || StringUtils.isNotBlank(dto.getValue())));
        boolean everyLineIsNotMeausured =
                criteriaDTO.getCriteriaList().stream().allMatch(dto -> dto.getCriteria() == MathematicalOperator.NotMeasured);

        if(!atLeastOneCompleteSingle){
            return null;
        }

        //If every line is "NotMeausured" then we perform a different operation
        //We simply get all the objects that are measured and return that we need to exclude them
        if(everyLineIsNotMeausured){
            return new ExcludableUriList(
                    true,
                    getUsedTargets(
                            null,
                            criteriaDTO.getCriteriaList().stream().map(SingleCriteriaDTO::getVariableUri).collect(Collectors.toList()),
                            (experiment == null ? null : Collections.singletonList(experiment))
                    ).stream().filter(Objects::nonNull).collect(Collectors.toList())
            );
        }
        //If every line wasn't "NotMeasured" :
        List<Bson> aggregationDocs = dao.createCriteriaSearchAggregation(criteriaDTO, experiment,user, new VariableDAO(sparql, nosql, fs, user));

        //If aggregationDocs is null then it means there was a contradiction
        if(aggregationDocs == null){
            return new ExcludableUriList(false, Collections.emptyList());
        }

        Set<DataTargetAggregateModel> criteriaSearchedResult = new HashSet<>(dao.aggregate(aggregationDocs, DataTargetAggregateModel.class));
        //If result is empty return an empty list otherwise take first and only element's targets value because of the
        // mongo request always returns a single Document with a list of targets validating the criteria

        if(criteriaSearchedResult.isEmpty()){
            return new ExcludableUriList(false, Collections.emptyList());
        } else if (criteriaSearchedResult.size() > 1) {
            throw new IllegalStateException("Unexpected error: the aggregation should have return only one results");
        }

        // no need to check Optional#isPresent() since if the initial list is not empty, then it has an item in the corresponding Stream
        return new ExcludableUriList(
                false,
                criteriaSearchedResult.stream().findAny()
                        .get()
                        .getTargets()
                        .stream().filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }

    public DataVariableSeriesGetDTO getDataSeriesByFacility(
            URI variableUri,
            URI facilityUri,
            String startDate,
            String endDate,
            Boolean calculatedOnly,
            Logger logger
    ) throws Exception {

        Instant start, end;

        Instant startInstant = (startDate != null) ? Instant.parse(startDate) : null;
        Instant endInstant = (endDate != null) ? Instant.parse(endDate) : Instant.now();

        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);
        VariableDetailsDTO variable = new VariableDetailsDTO(variableDAO.get(variableUri));
        DataVariableSeriesGetDTO dto = new DataVariableSeriesGetDTO(variable);

        /// Get last stored data
        DataSearchFilter getLastFoundDataFilter = new DataSearchFilter();
        getLastFoundDataFilter.setUser(user);
        getLastFoundDataFilter.setTargets(Collections.singletonList(facilityUri));
        getLastFoundDataFilter.setVariables(Collections.singletonList(variableUri));

        DataComputedGetDTO lastData = dao.getLastDataFound(getLastFoundDataFilter);
        dto.setLastData(lastData);

        /// Retrieve median series
        start = Instant.now();
        List<DataComputedModel> dataModels = dao.computeAllMediansPerHour(
                facilityUri,
                variableUri,
                startInstant,
                endInstant);
        end = Instant.now();
        logger.debug(dataModels.size() + " data retrieved from mongo : " + Long.toString(Duration.between(start, end).toMillis()) + " milliseconds elapsed");

        Map<DataProvenanceModel, List<DataComputedModel>> provenancesMap;

        List<DataSerieGetDTO> dataSeriesDTOs = new ArrayList<>();
        List<DataComputedModel> medians = new ArrayList<>();

        provenancesMap = dataModels.stream().collect(Collectors.groupingBy(DataComputedModel::getProvenance));

        for (Map.Entry<DataProvenanceModel, List<DataComputedModel>> entryProv : provenancesMap.entrySet()) {

            List<DataComputedModel> medianSerie = entryProv.getValue()
                    .stream()
                    .sorted(Comparator.comparing(DataComputedModel::getDate))
                    .collect(Collectors.toList());

            // adjust datetime for median data by setting it to the middle of the hour it represents
            medianSerie.forEach(data -> {
                Instant middleDate = data.getDate().truncatedTo(ChronoUnit.HOURS);
                data.setDate(middleDate.plus(30, ChronoUnit.MINUTES));
            });

            medians.addAll(medianSerie);

            DataSimpleProvenanceGetDTO provenance = createDataSimpleProvenance(entryProv.getKey());

            List<DataComputedGetDTO> medianSerieDTO = medianSerie.stream()
                    .map(DataComputedGetDTO::getDtoFromModel)
                    .collect(Collectors.toList());
            DataSerieGetDTO dataSerie = new DataSerieGetDTO(provenance, medianSerieDTO);
            dataSeriesDTOs.add(dataSerie);
        }

        if (!calculatedOnly) {
            dto.setDataSeries(dataSeriesDTOs);
        }
        else if (dataSeriesDTOs.size() == 1) {
            dto.setCalculatedSeries(dataSeriesDTOs);
        }

        /// Compute calculated series

        if (dataSeriesDTOs.size() > 1) {

            List<DataSerieGetDTO> dataCalculatedSeriesDTOs = new ArrayList<>();

            DataSimpleProvenanceGetDTO provMedian = new DataSimpleProvenanceGetDTO();
            provMedian.setName("median_per_hour");

            List<DataComputedModel> medianOfMedians = computeMedianPerHour(medians);
            List<DataComputedGetDTO> medianOfMediansDTO = medianOfMedians.stream()
                    .map(DataComputedGetDTO::getDtoFromModel)
                    .collect(Collectors.toList());
            dataCalculatedSeriesDTOs.add(new DataSerieGetDTO(provMedian, medianOfMediansDTO));

            DataSimpleProvenanceGetDTO provAverage = new DataSimpleProvenanceGetDTO();
            provAverage.setName("mean_per_day");

            List<DataComputedModel> averageSerie = dao.computeAllMeanPerDay(
                    facilityUri,
                    variableUri,
                    startInstant,
                    endInstant);
            List<DataComputedGetDTO> averageSerieDtos = averageSerie
                    .stream()
                    .map((d) -> DataComputedGetDTO.getDtoFromModel(d))
                    .sorted(Comparator.comparing(DataComputedGetDTO::getDate))
                    .collect(Collectors.toList());
            dataCalculatedSeriesDTOs.add(new DataSerieGetDTO(provAverage, averageSerieDtos));

            dto.setCalculatedSeries(dataCalculatedSeriesDTOs);
        }
        return dto;
    }

    //#endregion

    //#region PRIVATE METHODS
    /**
     * Check variable data list before creation
     * Complete the prov_was_associated_with provenance attribut
     *
     * @param dataList
     * @throws Exception
     */
    private List<DataModel> validData(List<DataModel> dataList) throws Exception {

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
                    variable = new VariableDAO(sparql, nosql, fs, user).get(variableURI);
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

    public List<VariableModel> getUsedVariablesByFilter(DataSearchFilter filter) throws Exception {
        Set<URI> variableURIs = new HashSet<>(dao.distinct(null, DataModel.VARIABLE_FIELD, URI.class, filter));
        String userLanguage = null;
        if(user != null){
            userLanguage = user.getLanguage();
        }
        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);
        return variableDAO.getList(new ArrayList<>(variableURIs), userLanguage);
    }

    /**
     *
     * Validates - throws or inserts the models
     * Handles setting of publisher
     * Handles Annotation creation in the case of import
     * @param modelList models to insert
     * @param variablesToDevices device, variable map to add the links to devices
     * @param calledFromImport so we know to run any extra code
     * @param csvValidation null if not for import, otherwise used to set nbOfImportedLines and to fetch any Annotations that need to be created
     * @return a list of uris if it the caller wasn't the import function
     */
    private List<URI> addListDataInnerCode(List<DataModel> modelList, Map<DeviceModel, List<URI>> variablesToDevices, boolean calledFromImport, DataCSVValidationModel csvValidation) throws Exception{

        //TODO can the validations done for import be refactord with the validData method ?
        if(!calledFromImport){
            modelList = validData(modelList);
        }
        //Set publisher
        modelList.forEach(dataModel -> dataModel.setPublisher(user.getUri()));

        //Transaction to add data and to add link to device
        List<DataModel> finalModelList = modelList;
        new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session->{
            //Create data
            dao.create(session, finalModelList);
            //Create device links
            if(!variablesToDevices.isEmpty()) {
                for (Map.Entry variablesToDevice : variablesToDevices.entrySet() ){
                    new DeviceDAO(sparql, nosql, fs).associateVariablesToDevice((DeviceModel) variablesToDevice.getKey(),(List<URI>)variablesToDevice.getValue(), user );
                }
            }
            //In the case of import set number of imported lines and create any annotations that were imported on the targets
            if(calledFromImport){
                csvValidation.setNbLinesImported(finalModelList.size());
                //If the data import was successful, post the annotations on objects
                AnnotationDAO annotationDAO = new AnnotationDAO(sparql);
                annotationDAO.create(csvValidation.getAnnotationsOnObjects());
            }
            return 0;
        });
        if(!calledFromImport){
            return finalModelList.stream().map(MongoModel::getUri).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Does the actual validating once we have loaded our provenance
     */
    private DataCSVValidationModel validateWholeCSVInnerCode(ProvenanceModel provenance, URI experiment, InputStream file, Logger logger) throws Exception {
        //TODO DAOs other than DataDao used in this function. Change when logic layer is done elsewhere
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);

        DataCSVValidationModel csvValidation = new DataCSVValidationModel();
        Map<String, SPARQLNamedResourceModel> nameURITargets = new HashMap<>();
        List<String> notExistingTargets = new ArrayList<>();
        List<String> duplicatedTargets = new ArrayList<>();

        Map<String, ExperimentModel> nameURIExperiments = new HashMap<>();
        List<String> notExistingExperiments = new ArrayList<>();
        List<String> duplicatedExperiments = new ArrayList<>();

        Map<String, SPARQLNamedResourceModel> nameURIScientificObjectsInXp = new HashMap<>();
        List<String> scientificObjectsNotInXp = new ArrayList<>();

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
            if (!headers.contains(DEVICE_HEADER) && !headers.contains(TARGET_HEADER) && !headers.contains(SCIENTIFICOBJ_HEADER) && !sensingDeviceFoundFromProvenance) {
                csvValidation.addMissingHeaders(Arrays.asList(DEVICE_HEADER + " or " + TARGET_HEADER + " or " + SCIENTIFICOBJ_HEADER));
            }
            // Check that there is an SCIENTIFICOBJ_HEADER or a TARGET_HEADER if there is an ANNOTATION_HEADER otherwise create error
            if(headers.contains(ANNOTATION_HEADER) && !headers.contains(TARGET_HEADER) && !headers.contains(SCIENTIFICOBJ_HEADER)){
                csvValidation.addMissingHeaders(Arrays.asList(TARGET_HEADER + " or " + SCIENTIFICOBJ_HEADER));
            }

            // 1. check variables
            HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();

            if (ids != null) {

                for (int i = 0; i < ids.length; i++) {
                    String header = ids[i];
                    if (header == null) {
                        csvValidation.addEmptyHeader(i+1);
                    } else {

                        if (header.equalsIgnoreCase(EXPERIMENT_HEADER) || header.equalsIgnoreCase(TARGET_HEADER)
                                || header.equalsIgnoreCase(DATE_HEADER) || header.equalsIgnoreCase(DEVICE_HEADER) || header.equalsIgnoreCase(SCIENTIFICOBJ_HEADER)
                                || header.equalsIgnoreCase(RAWDATA_HEADER) || header.equalsIgnoreCase(ANNOTATION_HEADER)) {
                            headerByIndex.put(i, header);

                        } else {
                            try {
                                if (!URIDeserializer.validateURI(header)) {
                                    csvValidation.addInvalidHeaderURI(i, header);
                                } else {
                                    VariableModel var = new VariableDAO(sparql, nosql, fs, user).get(URI.create(header));
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
                                new ExperimentDAO(sparql, nosql),
                                notExistingExperiments,
                                duplicatedExperiments,
                                nameURIExperiments,
                                new OntologyDAO(sparql),
                                notExistingTargets,
                                duplicatedTargets,
                                nameURITargets,
                                new ScientificObjectDAO(sparql, nosql),
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

    //TODO sort this mess out
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
            throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);

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
            if (headerByIndex.get(colIndex).equalsIgnoreCase(EXPERIMENT_HEADER)) {
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
                                exp = xpDAO.getExperimentByNameOrURI(expNameOrUri, user);
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


            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(TARGET_HEADER)) {
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
                                target = ontologyDAO.getTargetByNameOrURI(targetNameOrUri);
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
                            } catch (Exception e) {
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

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(SCIENTIFICOBJ_HEADER)) {

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

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(DEVICE_HEADER)){
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
                                deviceFromDeviceColumn = deviceDAO.getDeviceByNameOrURI(user, deviceNameOrUri);
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
            else if (headerByIndex.get(colIndex).equalsIgnoreCase(ANNOTATION_HEADER)){
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
            }else if (!headerByIndex.get(colIndex).equalsIgnoreCase(RAWDATA_HEADER)) { // Variable/Value column
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
                        if (!variableDAO.variableIsAssociatedToDevice(deviceFromDeviceColumn, varURI)) {
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
                            rootDeviceTypes = deviceDAO.getRootDeviceTypes(user);
                        }
                        URI rootType = rootDeviceTypes.get(deviceFromDeviceColumn.getType());
                        agent.setType(rootType);
                        agent.setUri(deviceFromDeviceColumn.getUri());
                        provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));

                    } else if (sensingDeviceFoundFromProvenance) {

                        DeviceModel checkedDevice = variableCheckedProvDevice.get(variable);
                        ProvEntityModel agent = new ProvEntityModel();
                        if (rootDeviceTypes == null) {
                            rootDeviceTypes = deviceDAO.getRootDeviceTypes(user);
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
                        if (headerByIndex.get(colIndex + 1).equalsIgnoreCase(RAWDATA_HEADER) && values[colIndex + 1] != null) {
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
                CSVCell annotationCell = new CSVCell(rowIndex, annotationIndex, annotationFromAnnotationColumn.getDescription(), ANNOTATION_HEADER);
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

            SPARQLNamedResourceModel existingObject = sparql.getByURI(experiment, ScientificObjectModel.class,objectUri,null);
            if (existingObject == null) {
                validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, "OBJECT_ID"));
                return null;
            }
            return existingObject;

            // check if object exist by name inside experiment
        } else if (experiment != null) {
            SPARQLNamedResourceModel existingObject = scientificObjectDAO.getUriByNameAndGraph(experiment, nameOrUri);
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
        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);
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

                if (!variableDAO.variableIsAssociatedToDevice(device, data.getVariable())) {
                    DataCSVValidationModel.addVariableToDeviceMap(device,data.getVariable(), variablesToDevices); // add variable/device
                }

                if (rootDeviceTypes == null) {
                    rootDeviceTypes = deviceDAO.getRootDeviceTypes(user);
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
                if (!variableDAO.variableIsAssociatedToDevice(deviceFromProvWasAssociated, data.getVariable())) {
                    DataCSVValidationModel.addVariableToDeviceMap(deviceFromProvWasAssociated,data.getVariable(), variablesToDevices); // add variable/device
                }
                variableCheckedDevice.put(deviceFromProvWasAssociated,data.getVariable());

            }
        }
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
        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);

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

                        if(variableDAO.variableIsAssociatedToDevice(device, data.getVariable())){
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

    /**
     * Create a DataSimpleProvenanceGetDTO with uri and name from a data provenance model.
     * @detail
     * Analyze the provenance from the data model and do as follows:
     *  if there is one agent (device or operator), retrieve the uri and name of the agent
     *  otherwise, take the uri and name from the provenance model
     * @param dataProvModel
     * @return a simple data provenance with uri and name attributes
     * @throws Exception
     */
    private DataSimpleProvenanceGetDTO createDataSimpleProvenance(DataProvenanceModel dataProvModel)
            throws Exception {
        DataSimpleProvenanceGetDTO dto = new DataSimpleProvenanceGetDTO();

        List<ProvEntityModel> provEntityList = dataProvModel.getProvWasAssociatedWith();

        if (provEntityList != null && provEntityList.size() == 1) {
            URI uri = provEntityList.get(0).getUri();
            dto.setUri(uri);
            dto.setName(new OntologyDAO(sparql).getURILabel(uri, user.getLanguage()));
        }
        else {
            ProvenanceModel provModel = new ProvenanceDaoV2(nosql.getServiceV2()).get(dataProvModel.getUri());
            dto.setUri(provModel.getUri());
            dto.setName(provModel.getName());
        }

        return dto;
    }

    //#endregion

}
