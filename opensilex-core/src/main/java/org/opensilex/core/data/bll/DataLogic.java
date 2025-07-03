//******************************************************************************
//                          DataLogic.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.bll;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.result.DeleteResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.opensilex.core.annotation.dal.AnnotationDAO;
import org.opensilex.core.data.api.*;
import org.opensilex.core.data.bll.dataImport.BatchHistoryLogic;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.dal.aggregations.DataTargetAggregateModel;
import org.opensilex.core.data.utils.MathematicalOperator;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.utils.ApiUtils;
import org.opensilex.core.variable.api.VariableDetailsDTO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.dal.URITypesModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ExcludableUriList;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    //If client session is null then we know we need to handle transactions
    private ClientSession clientSession;

    //TODO these daos are the ones that will need to be deleted in the class when logic classes are done.
    //VariableDAO
    //DeviceDAO
    //ExperimentDAO
    //ScientificObjectDAO
    //ProvenanceDaoV2
    //OntologyDAO

    //#region constructors

    public DataLogic(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user) {
        this.dao = new DataDaoV2(sparql, nosql, fs);
        this.sparql = sparql;
        this.nosql = nosql;
        this.user = user;
        this.fs = fs;
        this.clientSession = null;
    }

    public DataLogic(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user, ClientSession clientSession) {
        this.dao = new DataDaoV2(sparql, nosql, fs);
        this.sparql = sparql;
        this.nosql = nosql;
        this.user = user;
        this.fs = fs;
        this.clientSession = clientSession;
    }

    //#endregion
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

    public void update(DataModel model) throws Exception {
        DataValidation validation = new DataValidation(Collections.singletonList(model), sparql, nosql, user);
        validation.validate();
        dao.update(model);
    }

    /**
     * Deletes the found data, then, if a batch uri was passed in the filter,
     * handles deleting batch and the setting of imported dataset to deprecated.
     * Does this in a TRANSACTION.
     *
     * @param filter to search by
     * @return The DeleteResult if the delete was successful
     * @throws Exception
     */
    public DeleteResult deleteManyByFilter(DataSearchFilter filter) throws Exception {
        return new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session -> {
            DeleteResult deleteResult = dao.deleteMany(filter);
            //If no batch was passed or if nothing was deleted we do not need to handle deletion of the batch.
            if(filter.getBatchUri() == null || deleteResult.getDeletedCount() == 0){
                return deleteResult;
            }
            //If we deleted via batch uri then perform some other operations
            handleBatchAndDocumentAfterDeleteByBatch(filter);
            return deleteResult;
        });

    }

    public List<URI> createMany(List<DataModel> modelList) throws Exception {
        return createMany(modelList, false, null);
    }

    public void createManyFromImport(List<DataModel> modelList, DataCSVValidationModel csvValidationModel) throws Exception {
        createMany(modelList, true, csvValidationModel);
    }

    /**
     *
     * @param devices to filter the data by device
     * @param variables to filter the data by variable
     * @param experiments to filter the data by experiment
     * @param typeOfTarget Retain only targets of this type, if NULL then we return all types of target
     *
     * @return A list of target URIS who are target's of the filtered data
     * @throws Exception
     */
    public List<URI> getUsedTargets(List<URI> devices, List<URI> variables, List<URI> experiments, URI typeOfTarget) throws Exception {
        DataSearchFilter dataSearchFilter = new DataSearchFilter().setVariables(variables);
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices);
        List<URI> usedTargetUris = dao.distinct(null, DataModel.TARGET_FIELD, URI.class, dataSearchFilter);

        if(typeOfTarget != null && CollectionUtils.isNotEmpty(usedTargetUris)){
            OntologyDAO ontologyDao = new OntologyDAO(sparql);
            List<URITypesModel> superTypesByUri = ontologyDao.getSuperClassesByURI(usedTargetUris).stream().filter(
                    e -> e.getRdfTypes().stream().anyMatch(
                            f -> SPARQLDeserializers.compareURIs(f, typeOfTarget)
                    )
            ).toList();
            return superTypesByUri.stream().map(URITypesModel::getUri).toList();

        }
        return usedTargetUris;
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
                            (experiment == null ? null : Collections.singletonList(experiment)),
                            null
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

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    //#endregion

    //#region PRIVATE METHODS

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
     * Validates - throws or inserts the models
     * Handles setting of publisher
     * Handles Annotation creation in the case of import
     *
     * @param models          models to insert
     * @param csvImport   so we know to run any extra code
     * @param csvValidation      null if not for import, otherwise used to set nbOfImportedLines and to fetch any Annotations that need to be created
     * @return a list of uris if the caller wasn't the import function
     */
    private List<URI> createMany(List<DataModel> models, boolean csvImport, DataCSVValidationModel csvValidation) throws Exception {

        DataPostInsert postInsert;
        if (!csvImport) {
            DataValidation validation = new DataValidation(models, sparql, nosql, user);
            postInsert = validation.validate();
        }else{
            postInsert = new DataPostInsert();
            csvValidation.getVariablesToDevices().forEach((device, variables) -> {
                postInsert.devicesToVariables.computeIfAbsent(device.getUri(), key -> new HashSet<>()).addAll(variables);
            });
            models.forEach(dataModel -> dataModel.setPublisher(user.getUri()));
        }

        //Transaction to add data and to add link to devices/annotations, if we were not already in a transaction
        ApiUtils.wrapWithTransaction(
                (session) -> createManyNoTransaction(
                        session,
                        models,
                        postInsert,
                        csvImport,
                        csvValidation
                ),
                this.clientSession,
                sparql,
                nosql
        );

        if (csvImport) {
            return Collections.emptyList();
        }
        return models.stream()
                .map(MongoModel::getUri)
                .collect(Collectors.toList());
    }

    private int createManyNoTransaction(
            ClientSession session,
            List<DataModel> models,
            DataPostInsert postInsert,
            boolean csvImport,
            DataCSVValidationModel csvValidation
    ) throws Exception {
        //Create data and device links
        dao.create(session, models);
        new DeviceDAO(sparql, nosql, fs).linkDevicesToVariables(postInsert.devicesToVariables);

        //In the case of import set number of imported lines and create any annotations that were imported on the targets
        if (csvImport) {
            csvValidation.setNbLinesImported(models.size());
            //If the data import was successful, post the annotations on objects
            AnnotationDAO annotationDAO = new AnnotationDAO(sparql);
            annotationDAO.create(csvValidation.getAnnotationsOnObjects());
        }
        return 0;
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
     * If there are no data left after deleting data via batch uri. Then runs a transaction to delete the batch and
     * sets the document to deprecated
     *
     * @param filter used to determine if we deleted with batch AND experiment
     * @throws Exception
     */
    private void handleBatchAndDocumentAfterDeleteByBatch(DataSearchFilter filter) throws Exception {
        //If we are deleting within experiment then verify if any data from this batch still exist before deleting it
        boolean doDeleteBatch = true;
        if(!CollectionUtils.isEmpty(filter.getExperiments())){
            filter.setExperiments(null);
            if(dao.count(filter) > 0){
                doDeleteBatch = false;
            }
        }
        //If required delete batch and set document status to outdated
        if(doDeleteBatch){
            BatchHistoryLogic batchHistoryLogic = new BatchHistoryLogic(nosql);
            URI documentUri = batchHistoryLogic.get(filter.getBatchUri()).getDocumentUri();
            DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
            DocumentModel oldDoc = documentDAO.getMetadata(documentUri, user);
            //The document could have been deleted since so handle that
            if(oldDoc != null){
                oldDoc.setDeprecated("true");
                documentDAO.update(oldDoc, user);
            }

            batchHistoryLogic.deleteBatchHistoryByURI(filter.getBatchUri());
        }
    }

    //#endregion

}
