//******************************************************************************
//                          DataLogic.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.bll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Field;
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
import org.bson.Document;
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
import org.opensilex.core.ontology.Oeso;
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
import org.opensilex.utils.ExcludableUriList;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.StreamWithPagination;
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
    private final String expHeader = "experiment";
    private final String targetHeader = "target";
    private final String dateHeader = "date";
    private final String deviceHeader = "device";
    private final String rawdataHeader = "raw_data";
    private final String soHeader = "scientific_object";
    private final String annotationHeader = "object_annotation";


    //PUBLIC METHODS ==================================================================================================

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

    public List<URI> addListData(List<DataModel> modelList) throws Exception {
        return addListDataInnerCode(modelList, variablesToDevices, false, null);
    }

    public void addListDataFromImport(List<DataModel> modelList, DataCSVValidationModel csvValidationModel) throws Exception {
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

    public List<URI> getUsedTargets(List<URI> devices, List<URI> variables, List<URI> experiments) throws Exception {
        DataSearchFilter dataSearchFilter = new DataSearchFilter().setVariables(variables);
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices);
        return dao.distinct(null, DataModel.TARGET_FIELD, URI.class, dataSearchFilter);
    }

    public List<VariableModel> getUsedVariables(List<URI> experiments, List<URI> objects, List<URI> provenances, List<URI> devices) throws Exception {
        DataSearchFilter dataSearchFilter = new DataSearchFilter();
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices).setTargets(objects).setProvenances(provenances);
        Set<URI> variableURIs = new HashSet<>(dao.distinct(null, DataModel.VARIABLE_FIELD, URI.class, dataSearchFilter));
        String userLanguage = null;
        if(user != null){
            userLanguage = user.getLanguage();
        }
        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);
        return variableDAO.getList(new ArrayList<>(variableURIs), userLanguage);
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
                dto.getVariableUri() != null && dto.getCriteria() != null && (dto.getCriteria() == MathematicalOperator.NotMeasured || com.apicatalog.jsonld.StringUtils.isNotBlank(dto.getValue())));
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
        List<Bson> aggregationDocs = this.createCriteriaSearchAggregation(criteriaDTO, experiment,user, new VariableDAO(sparql, nosql, fs, user));

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
        List<DataComputedModel> dataModels = computeAllMediansPerHour(
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

            List<DataComputedModel> averageSerie = computeAllMeanPerDay(
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

    //PRIVATE METHODS ================================================================================================
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
            if (!headers.contains(deviceHeader) && !headers.contains(targetHeader) && !headers.contains(soHeader) && !sensingDeviceFoundFromProvenance) {
                csvValidation.addMissingHeaders(Arrays.asList(deviceHeader + " or " + targetHeader + " or " + soHeader));
            }
            // Check that there is an soHeader or a targetHeader if there is an annotationHeader otherwise create error
            if(headers.contains(annotationHeader) && !headers.contains(targetHeader) && !headers.contains(soHeader)){
                csvValidation.addMissingHeaders(Arrays.asList(targetHeader + " or " + soHeader));
            }

            // 1. check variables
            HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();

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

    /**
     * <p>
     * Translates the dto into a search filter permitting the acquisition of data needed to get the right objects.
     * Or creates error lists if there are errors in the request.
     *
     * @return List of Bson for the aggregation, or null if a contradiction was identified
     * </p>
     *
     * <p>
     * Example of a request : If we want all objects who have a Variable A > 10 and a Variable B < 5.
     * Then we need to fetch all data concerning A where value > 10 as well as all data concerning B where value < 5.
     * Even though in the end we will only retain objects if they have data that validates both.
     * Precondition : criteriaDTO is of type And for first version
     * </p>
     *
     * @apiNote
     * <ul>
     * <li> In the "group" pipeline stage, the name of the field which contains target is "targets" </li>
     * <li> This is the mongo request that is created for an example with two criteria </li>
     * </ul>
     *
     * <pre>
     * {@code
     *       db.getCollection('data').aggregate(
     *    [
     *        {$match : { $or:
     *     [
     *         { $and: [
     *            {"variable":"http://vegetalunit.inrae.fr/vigne/id/variable/lot_weight_balance_kilogramm"} ,
     *            {"value":{ $lt: 1000 }}
     *             ] },
     *        { $and: [
     *                {"variable":"http://vegetalunit.inrae.fr/vigne/id/variable/must_density_hydromtre_kilogramm_per_litre"} ,
     *                {"value":{ $lt: 1000 }}
     *                     ] },
     *         {"variable":"http://vegetalunit.inrae.fr/vigne/id/variable/must_istall_standard_method_trueorfalse"},
     *         {"variable":"http://vegetalunit.inrae.fr/vigne/id/variable/must_readytodrink_datenotime"}
     *     ] }},
     *    {
     *         $group : {_id : {target : "$target"},
     *             variables: {$addToSet: "$variable"}}
     *         },
     *         {
     *            $match: {
     *               $expr:
     *               { $and: [
     *                   {$eq: [{ $size: "$variables" }, 2]},
     *                   { $not:
     *                       [
     *                         { $or: [
     *                                 { $in: ["http://vegetalunit.inrae.fr/vigne/id/variable/must_istall_standard_method_trueorfalse","$variables" ] },
     *                                 { $in: ["http://vegetalunit.inrae.fr/vigne/id/variable/must_readytodrink_datenotime", "$variables" ] }
     *                             ] }
     *                       ] }
     *                   ] }
     *             }
     *        },
     *        {
     *    $group: {
     *       _id: null,
     *       targets: { $addToSet: "$_id.target" }
     *     }
     *   }
     *  ]
     * ).pretty();
     * }
     * </pre>
     *
     */
    private List<Bson> createCriteriaSearchAggregation(CriteriaDTO criteriaDTO, URI experiment, AccountModel user, VariableDAO variableDAO) throws Exception {
        GetScientificObjectsByDataCriteriaRequestErrors errors = new GetScientificObjectsByDataCriteriaRequestErrors();

        List<Document> criteriaDocuments = new ArrayList<>();
        Map<String, List<Document>> criteriaDocumentPerVariable = new HashMap<>();
        //List to remember which vars we are testing to be not measured so that we can identify impossible request if we try to compare this var with a value
        Set<String> varsWhereWeWantNoData = new HashSet<>();

        //Make the aggregation docs
        for(SingleCriteriaDTO singleCriteriaDTO : criteriaDTO.getCriteriaList()){
            URI currentVariableUri = singleCriteriaDTO.getVariableUri();
            MathematicalOperator criteriaType = singleCriteriaDTO.getCriteria();
            String valueString = singleCriteriaDTO.getValue();
            //If line is complete
            if(currentVariableUri != null && criteriaType != null && (criteriaType == MathematicalOperator.NotMeasured || !valueString.trim().isEmpty())){
                URI varDataTypeUri = null;
                //Create variable filter and a new list of filters if this is the first time we've crossed this variable, add to existing list otherwise
                String currentVariableStringUri = SPARQLDeserializers.getExpandedURI(currentVariableUri);
                try{
                    varDataTypeUri = variableDAO.get(currentVariableUri).getDataType();
                }catch (Exception e){
                    errors.addInvalidVariable(singleCriteriaDTO, currentVariableUri);
                }

                if(!criteriaDocumentPerVariable.containsKey(currentVariableStringUri)){
                    Document currentCriteriaDocument = new Document();
                    currentCriteriaDocument.put(DataModel.VARIABLE_FIELD, currentVariableStringUri);
                    List<Document> variableDocuments = new ArrayList<>();
                    variableDocuments.add(currentCriteriaDocument);
                    criteriaDocumentPerVariable.put(currentVariableStringUri, variableDocuments);

                }

                List<Document> currentCriteriaDocuments = criteriaDocumentPerVariable.get(currentVariableStringUri);

                if(criteriaType == MathematicalOperator.NotMeasured ){
                    //If size of currentCriteriaDocuments is bigger than one it means we already compared this var to a value so contradiction, so return null
                    if(currentCriteriaDocuments.size() > 1){
                        return null;
                    }
                    varsWhereWeWantNoData.add(currentVariableStringUri);
                }else{
                    //If we previously said this var needs to not be measured then contradiction so return null
                    if(varsWhereWeWantNoData.contains(currentVariableStringUri)){
                        return null;
                    }
                    Object parsedValue = null;
                    //Add to invalid value datatype errors if parse fails
                    try{
                        parsedValue = DataValidateUtils.convertData(varDataTypeUri, valueString);
                    } catch(Exception e){
                        errors.addInvalidValueDatatypeError(singleCriteriaDTO, valueString);
                    }

                    if(criteriaType == MathematicalOperator.EqualToo ){
                        if(currentCriteriaDocuments != null && parsedValue != null){
                            currentCriteriaDocuments.add(new Document(DataModel.VALUE_FIELD, parsedValue));
                        }
                    }else{
                        //If filterType stays null then it means the criteriaUri wasn't recognized, so add to invalid criteria uris
                        String filterType = null;
                        if(criteriaType == MathematicalOperator.MoreOrEqualThan ){
                            filterType = "$gte";
                        }else if(criteriaType == MathematicalOperator.MoreThan ){
                            filterType = "$gt";
                        }else if(criteriaType == MathematicalOperator.LessThan ){
                            filterType = "$lt";
                        }else if(criteriaType == MathematicalOperator.LessOrEqualThan){
                            filterType = "$lte";
                        }
                        if(filterType == null){
                            errors.addInvalidCriteriaOperator(singleCriteriaDTO, criteriaType.toString());
                        } else{
                            if(currentCriteriaDocuments != null && parsedValue != null){
                                Document valueConstraintFilter = new Document();
                                valueConstraintFilter.put(filterType, parsedValue);
                                currentCriteriaDocuments.add(new Document(DataModel.VALUE_FIELD, valueConstraintFilter));
                            }
                        }
                    }
                }
            }
        }//End of criteria list for loop
        if (errors.hasErrors()){
            throw new IllegalArgumentException(errors.generateErrorMessage());
        }

        for(List<Document> varDocs : criteriaDocumentPerVariable.values()){
            if(varDocs.size()==1){
                criteriaDocuments.add(varDocs.get(0));
            }else{
                criteriaDocuments.add(new Document("$and", varDocs));
            }
        }
        final List<Bson> aggregationDocuments = new ArrayList<>();
        //Data search filter
        Document dataSearchFilter = new Document();
        //Some strings used to create the request :
        final String variables = "variables";
        dataSearchFilter.put("$or", criteriaDocuments);
        if(experiment == null){
            aggregationDocuments.add(new Document("$match", dataSearchFilter));
        }else{
            Document experimentFilter = new Document();
            appendExperimentUserAccessFilter(experimentFilter, user, Collections.singletonList(experiment));
            List<Document> andList = new ArrayList<>();
            andList.add(dataSearchFilter);
            andList.add(experimentFilter);
            Document dataSearchFilterWithExperiment = new Document("$and", andList);
            aggregationDocuments.add(new Document("$match", dataSearchFilterWithExperiment));
        }

        //Group by target and create list of variables that have returned data for each target
        Document groupByTargetDoc = new Document();
        Document groupByIdDoc = new Document();
        groupByIdDoc.put(DataModel.TARGET_FIELD, "$" + DataModel.TARGET_FIELD);
        groupByTargetDoc.put("_id", groupByIdDoc);
        Document groupByVarListDoc = new Document();
        groupByVarListDoc.put("$addToSet", "$" + DataModel.VARIABLE_FIELD);
        groupByTargetDoc.put(variables, groupByVarListDoc);
        aggregationDocuments.add(new Document("$group", groupByTargetDoc));


        //Keep only the ones that have every tested variable present but not the ones that need to not be measured
        Document sizeExpr = new Document("$size", "$" + variables);
        Document sizeCondition = new Document("$eq", Arrays.asList(sizeExpr, criteriaDocumentPerVariable.size() - varsWhereWeWantNoData.size()));
        List<Document> inNotMeasuredVarsListDocs = varsWhereWeWantNoData.stream().map(
                notMeasuredVar -> new Document("$in", Arrays.asList(notMeasuredVar, "$" + variables))).collect(Collectors.toList());
        Document notInNotMeasuredVarsListDoc = new Document("$not", Collections.singletonList(new Document("$or", inNotMeasuredVarsListDocs)));
        Document correctSizeAndNoNotMeasuredVars = new Document("$and", Arrays.asList(sizeCondition, notInNotMeasuredVarsListDoc));
        aggregationDocuments.add(new Document("$match", new Document("$expr", correctSizeAndNoNotMeasuredVars)));


        //group by nothing and create a list of targets that validate the criteria
        Document targetListCreator = new Document();
        targetListCreator.put("_id", null);
        targetListCreator.put(DataModel.TARGET_FIELD + "s", new Document("$addToSet", "$_id." + DataModel.TARGET_FIELD));
        aggregationDocuments.add(new Document("$group", targetListCreator));

        return aggregationDocuments;

    }


    //TODO ? This is redundant for most cases with the new method addExperimentFilter in DataFileDao. But i use it in an aggregation so need to see how i handle
    public void appendExperimentUserAccessFilter(Document filter, AccountModel user, List<URI> experiments) throws Exception {
        String experimentField = "provenance.experiments";

        //user access
        if (!user.isAdmin()) {
            Set<URI> userExperiments = new ExperimentDAO(sparql, nosql).getUserExperiments(user);

            if (experiments != null && !experiments.isEmpty()) {

                //Transform experiments and userExperiments in long format to compare the two lists
                Set<URI> longUserExp = new HashSet<>();
                for (URI exp:userExperiments) {
                    longUserExp.add(new URI(SPARQLDeserializers.getExpandedURI(exp)));
                }
                Set <URI> longExpURIs = new HashSet<>();
                for (URI exp:experiments) {
                    longExpURIs.add(new URI(SPARQLDeserializers.getExpandedURI(exp)));
                }
                longExpURIs.retainAll(longUserExp); //keep in the list only the experiments the user has access to

                if (longExpURIs.isEmpty()) {
                    throw new Exception("you can't access to the given experiments");
                } else {
                    Document inFilter = new Document();
                    inFilter.put("$in", longExpURIs);
                    filter.put("provenance.experiments", inFilter);
                }
            } else {
                Document filterOnExp = new Document(experimentField, new Document("$in", userExperiments));
                Document notExistingExpFilter = new Document(experimentField, new Document("$exists", false));
                Document emptyExpFilter = new Document(experimentField, new ArrayList());
                List<Document> expFilter = Arrays.asList(filterOnExp, notExistingExpFilter, emptyExpFilter);

                filter.put("$or", expFilter);
            }
        } else {
            if (experiments != null && !experiments.isEmpty()) {
                Document inFilter = new Document();
                inFilter.put("$in", experiments);
                filter.put("provenance.experiments", inFilter);
            }
        }
    }

    /**
     * Retrieve median per hour data series
     * @details
     *  data are collected and grouped by [target, variable, provenance].
     *  The median per hour is then computed for each data series
     * @param target
     * @param variable
     * @param startDate
     * @param endDate
     * @return a list of median values
     * @throws Exception
     */
    private List<DataComputedModel> computeAllMediansPerHour(URI target,
                                                             URI variable,
                                                             Instant startDate,
                                                             Instant endDate) {

        List<Bson> aggregations = new ArrayList<>();

        //$match
        //{
        //	variable: "http://opensilex.dev/id/variable/air_temprature_degree_celsius",
        //	target: "http://opensilex.dev/id/organization/facility.phenoarch",
        //	date:
        //	{
        //		"$gte": ISODate("2022-05-31T11:26:16.856Z"),
        //		"$lt": ISODate("2023-05-31T11:26:16.856Z")
        //	}
        //}
        Document filter = new Document();
        filter.put(DataModel.VARIABLE_FIELD, URIDeserializer.getExpandedURI(variable));
        filter.put(DataModel.TARGET_FIELD, URIDeserializer.getExpandedURI(target));
        if (startDate != null || endDate != null) {
            Document dateFilter = new Document();
            if (startDate != null) {
                dateFilter.put("$gte", startDate);
            }
            if (endDate != null) {
                dateFilter.put("$lt", endDate);
            }
            filter.put("date", dateFilter);
        }
        Bson match = Aggregates.match(filter);

        //$project
        //{
        //  "y":{"$year":"$date"},
        //  "m":{"$month":"$date"},
        //  "d":{"$dayOfMonth":"$date"},
        //  "h":{"$hour":"$date"},
        //  "date": "$date",
        //  "value": "$value"
        //}
        Document splitDateProj = new Document();
        splitDateProj.put("y", new Document("$year", "$date"));
        splitDateProj.put("m", new Document("$month", "$date"));
        splitDateProj.put("d", new Document("$dayOfMonth", "$date"));
        splitDateProj.put("h", new Document("$hour", "$date"));
        splitDateProj.put("provenance", "$provenance");
        splitDateProj.put("date", "$date");
        splitDateProj.put("value", "$value");
        Bson projectSplitDate = Aggregates.project(splitDateProj);

        //$group
        //{
        //  _id: {"year":"$y","month":"$m","day":"$d","hour":"$h","provenance":"$provenance"},
        //  count: {
        //    $sum: 1
        //  },
        //  dates: {
        //    $push: "$date"
        //  },
        //  values: {
        //    $push: "$value"
        //  }
        //}
        Document groupDateAndProvId = new Document();
        groupDateAndProvId.put("year", "$y");
        groupDateAndProvId.put("month", "$m");
        groupDateAndProvId.put("day", "$d");
        groupDateAndProvId.put("hour", "$h");
        groupDateAndProvId.put("provenance", "$provenance");
        BsonField sizeAcc = new BsonField("size", new Document("$sum", 1));
        BsonField datesAcc = new BsonField("dates", new Document("$push", "$date"));
        BsonField valuesAcc = new BsonField("values", new Document("$push", "$value"));
        Bson groupDateAndProv = Aggregates.group(groupDateAndProvId, sizeAcc, datesAcc, valuesAcc);

        //$project
        //{
        //  size: 1,
        //  values: 1,
        //  date: { "$arrayElemAt": ["$values", 0] },
        //  isEvenLength: { "$eq": [{ "$mod": ["$size", 2] }, 0 ] },
        //  middlePoint: { "$trunc": { "$divide": ["$size", 2] } }
        //}
        Document sizeProj = new Document();
        sizeProj.put("size", 1);
        sizeProj.put("values", 1);
        sizeProj.put("date", new Document("$arrayElemAt", Arrays.asList("$dates", 0)));
        Document mod = new Document("$mod", Arrays.asList("$size", 2));
        Document eq = new Document("eq", Arrays.asList(mod, 0));
        sizeProj.put("isEvenLength", eq);
        Document divide = new Document("$divide", Arrays.asList("$size", 2));
        sizeProj.put("middlePoint", new Document("$trunc", divide));
        Bson projectArraySize = Aggregates.project(sizeProj);

        //$addFields
        //{
        //  beginMiddle: { "$subtract": [ "$middlePoint", 1] },
        //  endMiddle: "$middlePoint"
        //}
        Document subtract = new Document("$subtract", Arrays.asList("$middlePoint", 1));
        Field beginMiddle = new Field("beginMiddle", subtract);
        Field endMiddle = new Field("endMiddle", "$middlePoint");
        Bson projectMiddle = Aggregates.addFields(beginMiddle, endMiddle);

        //$addFields
        //{
        //  "beginValue": { "$arrayElemAt": ["$values", "$beginMiddle"] },
        //  "endValue": { "$arrayElemAt": ["$values", "$endMiddle"] }
        //}
        Document arrayElemAtBegin = new Document("$arrayElemAt", Arrays.asList("$values", "$beginMiddle"));
        Document arrayElemAtEnd = new Document("$arrayElemAt", Arrays.asList("$values", "$endMiddle"));
        Field beginValue = new Field("beginValue", arrayElemAtBegin);
        Field endValue = new Field("endValue", arrayElemAtEnd);
        Bson projectMiddleValues = Aggregates.addFields(beginValue, endValue);

        //$addFields
        //{
        //  "middleSum": { "$add": ["$beginValue", "$endValue"] }
        //}
        Document sum = new Document("$add", Arrays.asList("$beginValue", "$endValue"));
        Field middleSum = new Field("middleSum", sum);
        Bson projectMiddleSum = Aggregates.addFields(middleSum);

        //$project
        //{
        //  date: 1,
        //  value: {
        //    "$cond": {
        //      if: "$isEvenLength",
        //      then: { "$divide": ["$middleSum", 2] },
        //      else:  { "$arrayElemAt": ["$values", "$middlePoint"] }
        //    }
        //  }
        //}
        Document finalProj = new Document();
        finalProj.put("_id", 0);
        finalProj.put("date", 1);
        finalProj.put("provenance", "$_id.provenance");
        Document condContent = new Document();
        condContent.put("if", "$isEvenLength");
        condContent.put("then", new Document("$divide", Arrays.asList("$middleSum", 2)));
        condContent.put("else", new Document("$arrayElemAt", Arrays.asList("$values", "$middlePoint")));
        Document cond = new Document("$cond", condContent);
        finalProj.put("value", cond);
        Bson projectFinal = Aggregates.project(finalProj);


        aggregations.add(match);
        aggregations.add(projectSplitDate);
        aggregations.add(groupDateAndProv);
        aggregations.add(projectArraySize);
        aggregations.add(projectMiddle);
        aggregations.add(projectMiddleValues);
        aggregations.add(projectMiddleSum);
        aggregations.add(projectFinal);

        return dao.aggregate(aggregations, DataComputedModel.class);
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

    /**
     * Compute the daily average from data
     * @param target
     * @param variable
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    private List<DataComputedModel> computeAllMeanPerDay(URI target,
                                                        URI variable,
                                                        Instant startDate,
                                                        Instant endDate) {

        List<Bson> aggregations = new ArrayList<>();

        //$match
        //{
        //	variable: "http://opensilex.dev/id/variable/air_temprature_degree_celsius",
        //	target: "http://opensilex.dev/id/organization/facility.phenoarch",
        //  date:
        //	{
        //		"$gte": ISODate("2022-05-31T11:26:16.856Z"),
        //		"$lt": ISODate("2023-05-31T11:26:16.856Z")
        //	}
        //}
        Document filter = new Document();
        filter.put(DataModel.VARIABLE_FIELD, URIDeserializer.getExpandedURI(variable));
        filter.put(DataModel.TARGET_FIELD, URIDeserializer.getExpandedURI(target));
        if (startDate != null || endDate != null) {
            Document dateFilter = new Document();
            if (startDate != null) {
                dateFilter.put("$gte", startDate);
            }
            if (endDate != null) {
                dateFilter.put("$lt", endDate);
            }
            filter.put("date", dateFilter);
        }
        Bson match = Aggregates.match(filter);

        //$project
        //{
        //  "y":{"$year":"$date"},
        //  "m":{"$month":"$date"},
        //  "d":{"$dayOfMonth":"$date"},
        //  "date": "$date",
        //  "value": "$value"
        //}
        Document splitDateProj = new Document();
        splitDateProj.put("y", new Document("$year", "$date"));
        splitDateProj.put("m", new Document("$month", "$date"));
        splitDateProj.put("d", new Document("$dayOfMonth", "$date"));
        splitDateProj.put("date", "$date");
        splitDateProj.put("value", "$value");
        Bson projectSplitDate = Aggregates.project(splitDateProj);

        //$group
        //{
        //  _id: {"year":"$y","month":"$m","day":"$d"},
        //  dates: {
        //    $push: "$date"
        //  },
        //  value: {
        //    $avg: "$value"
        //  }
        //}
        Document groupDateAndProvId = new Document();
        groupDateAndProvId.put("year", "$y");
        groupDateAndProvId.put("month", "$m");
        groupDateAndProvId.put("day", "$d");
        BsonField datesAcc = new BsonField("dates", new Document("$push", "$date"));
        BsonField avgAcc = new BsonField("value", new Document("$avg", "$value"));
        Bson groupDateAndProv = Aggregates.group(groupDateAndProvId, datesAcc, avgAcc);

        //$project
        //{
        //  _id: 0,
        //  date: { "$arrayElemAt": ["$dates", 0]},
        //  value: 1
        //}
        Document finalProj = new Document();
        finalProj.put("_id", 0);
        finalProj.put("date", new Document("$arrayElemAt", Arrays.asList("$dates", 0)));
        finalProj.put("value", 1);
        Bson projectFinal = Aggregates.project(finalProj);


        aggregations.add(match);
        aggregations.add(projectSplitDate);
        aggregations.add(groupDateAndProv);
        aggregations.add(projectFinal);

        return dao.aggregate(aggregations, DataComputedModel.class);
    }

    //Embedded classes for complex return types
    /**
     * Contains one main map providing an identifier to each criteria triplet that is invalid.
     * And 3 other maps to identify what type of error each invalid triplet has.
     * The 3 add error functions will automatically create the identifier if the triplet didn't already have an error.
     * (Variable uri not found, Criteria uri not found or Invalid datatype for the given variable.
     */
    private static class GetScientificObjectsByDataCriteriaRequestErrors{
        private final Map<Integer, URI> invalidVariables;
        private final Map<Integer, String> invalidCriteriaOperators;
        private final Map<Integer, String> invalidValueDatatypes;
        private final Map<SingleCriteriaDTO, Integer> invalidCriterias;
        int currentIdentifier;
        public GetScientificObjectsByDataCriteriaRequestErrors(){
            this.invalidVariables = new HashMap<>();
            this.invalidCriteriaOperators = new HashMap<>();
            this.invalidValueDatatypes = new HashMap<>();
            this.invalidCriterias = new HashMap<>();
            this.currentIdentifier = 0;
        }
        public boolean hasErrors(){
            return !invalidCriterias.isEmpty();
        }
        public void addInvalidVariable(SingleCriteriaDTO invalidCriteria, URI invalidVariable){
            Integer id = invalidCriterias.computeIfAbsent(invalidCriteria, (criteria) -> {this.currentIdentifier ++; return this.currentIdentifier;});
            this.invalidVariables.put(id, invalidVariable);
        }
        public void addInvalidCriteriaOperator(SingleCriteriaDTO invalidCriteria, String invalidCriteriaOperator){
            Integer id = invalidCriterias.computeIfAbsent(invalidCriteria, (criteria) -> {this.currentIdentifier ++; return this.currentIdentifier;});
            this.invalidCriteriaOperators.put(id, invalidCriteriaOperator);
        }
        public void addInvalidValueDatatypeError(SingleCriteriaDTO invalidCriteria, String invalidValue){
            Integer id = invalidCriterias.computeIfAbsent(invalidCriteria, (criteria) -> {this.currentIdentifier ++; return this.currentIdentifier;});
            this.invalidValueDatatypes.put(id, invalidValue);
        }
        public String generateErrorMessage(){
            StringBuilder result = new StringBuilder("Errors were found in the following criteria : \n");
            ObjectMapper objectMapper = new ObjectMapper();
            for (SingleCriteriaDTO singleCriteriaDTO : invalidCriterias.keySet()) {
                try {
                    result.append(objectMapper.writeValueAsString(singleCriteriaDTO));
                } catch (JsonProcessingException e) {
                    result.append("{singleCriteriaDTO json serialization failed}");
                }
                Integer id = invalidCriterias.get(singleCriteriaDTO);
                result.append((invalidVariables.get(id) == null ? "" : "variable uri not found, "));
                result.append((invalidCriteriaOperators.get(id) == null ? "" : "criteria operator not found, "));
                result.append((invalidValueDatatypes.get(id) == null ? "" : "value does not match required data-type of variable. "));
                result.append("\n");
            }
            return result.toString();

        }
    }

}
