//******************************************************************************
//                          DataDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.opencsv.CSVWriter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.XSD;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.core.data.api.DataExportDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.datafile.dal.DataFileSearchFilter;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author sammy
 */
public class DataDAO extends MongoReadWriteDao<DataModel, DataSearchFilter> {

    public static final String DATA_COLLECTION_NAME = "data";
    public static final String FILE_COLLECTION_NAME = "file";

    public static final String DATA_PREFIX = "data";

    public static final String FS_FILE_PREFIX = "datafile";

    protected final SPARQLService sparql;

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDAO.class);

    public DataDAO(MongoDBService mongodb, SPARQLService sparql) {
        super(mongodb, DataModel.class, DATA_COLLECTION_NAME, DATA_PREFIX);
        this.sparql = sparql;
    }

    @Override
    protected void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        collection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), unicityOptions);
        collection.createIndex(Indexes.ascending(DataModel.DATE_FIELD), unicityOptions);

        collection.createIndex(Indexes.ascending(DataModel.VARIABLE_FIELD, DataModel.PROVENANCE_FIELD, DataModel.TARGET_FIELD, DataModel.DATE_FIELD), unicityOptions);
        collection.createIndex(Indexes.ascending(DataModel.VARIABLE_FIELD, DataModel.TARGET_FIELD, DataModel.DATE_FIELD), unicityOptions);

        collection.createIndex(Indexes.compoundIndex(
                Arrays.asList(Indexes.ascending(DataModel.VARIABLE_FIELD),Indexes.descending( DataModel.DATE_FIELD)))
        );
    }

    @Override
    public List<Bson> getBsonFilters(DataSearchFilter searchFilter) throws Exception {

        List<Bson> filters = super.getBsonFilters(searchFilter);

        // handle case some case in which some service/dao must have access to data collection, even if the user don't have direct access to xp
        if(searchFilter.getUser() != null){
            Bson xpFilter = appendExperimentUserAccessFilter(searchFilter.getUser(), searchFilter.getExperiments());
            if(xpFilter != null){
                filters.add(xpFilter);
            }
        }

        if (!CollectionUtils.isEmpty(searchFilter.getTargets())) {
            filters.add(Filters.in(DataModel.TARGET_FIELD, searchFilter.getTargets()));
        }
        if (!CollectionUtils.isEmpty(searchFilter.getVariables())) {
            filters.add(Filters.in(DataModel.VARIABLE_FIELD, searchFilter.getVariables()));
        }
        if (!CollectionUtils.isEmpty(searchFilter.getProvenances())) {
            filters.add(Filters.in(DataModel.PROVENANCE_URI_FIELD, searchFilter.getProvenances()));
        }

        if (searchFilter.getStartDate() != null) {
            filters.add(Filters.gte(DataModel.DATE_FIELD, searchFilter.getStartDate()));
        }
        if (searchFilter.getEndDate() != null) {
            filters.add(Filters.lt(DataModel.DATE_FIELD, searchFilter.getEndDate()));
        }

        if (searchFilter.getConfidenceMin() != null) {
            filters.add(Filters.gte(DataModel.CONFIDENCE_FIELD, searchFilter.getConfidenceMin()));
        }
        if (searchFilter.getConfidenceMax() != null) {
            filters.add(Filters.lt(DataModel.CONFIDENCE_FIELD, searchFilter.getConfidenceMax()));
        }

        if (searchFilter.getMetadata() != null) {

            // append an eq filter for each metadata (field,value) pair
            List<Bson> eqFilters = new ArrayList<>(searchFilter.getMetadata().size());
            searchFilter.getMetadata().forEach((key, value) ->
                    eqFilters.add(Filters.eq("metadata." + key, value))
            );

            // use a logical AND since we want that document match each (field,value) provided in metadata
            filters.add(Filters.and(eqFilters));
        }

        if (!CollectionUtils.isEmpty(searchFilter.getDevices())) {
            //Get all data that have :
            //    provenance.provWasAssociatedWith.uri IN deviceURIs
            // OR ( provenance.uri IN deviceProvenances list && provenance.provWasAssociatedWith.uri isEmpty or not exists)
            ProvenanceDAO provDAO = new ProvenanceDAO(mongodb, sparql);

            // 1st case :  provenance.provWasAssociatedWith.uri IN deviceURIs
            Bson directProvFilter = Filters.in("provenance.provWasAssociatedWith.uri",searchFilter.getDevices());

            // 2nd case : provenance.uri IN deviceProvenances && no provenance.provWasAssociatedWith
            Set<URI> deviceProvenances = provDAO.getProvenancesURIsByAgents(searchFilter.getDevices());

            Bson globalProvUsed = Filters.and(
                    Filters.in(DataModel.PROVENANCE_URI_FIELD,deviceProvenances),
                    Filters.or(
                            Filters.exists("provenance.provWasAssociatedWith",false),
                            Filters.empty()
                    )
            );
            filters.add(Filters.or(directProvFilter, globalProvUsed));
        }

        return filters;
    }

    public Bson appendExperimentUserAccessFilter(AccountModel user, List<URI> experiments) throws Exception {
        String experimentField = "provenance.experiments";

        //user access
        if (!user.isAdmin()) {
            ExperimentDAO expDAO = new ExperimentDAO(sparql, mongodb);
            Set<URI> userExperiments = expDAO.getUserExperiments(user);

            if (! CollectionUtils.isEmpty(experiments)) {

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
                    throw new IllegalAccessException("you can't access to the given experiments");
                } else {
                    return Filters.in(experimentField,longExpURIs);
                }
            } else {
                Bson filterOnExp = Filters.in(experimentField,userExperiments);
                Bson notExistingExpFilter = Filters.exists(experimentField,false);
                Bson emptyExpFilter =  Filters.empty();
                return Filters.or(Arrays.asList(filterOnExp, notExistingExpFilter, emptyExpFilter));
            }
        } else {
            if (! CollectionUtils.isEmpty(experiments)) {
                return Filters.in(experimentField,experiments);
            }
        }
        return null;
    }

    @Deprecated
    public ListWithPagination<VariableModel> getVariablesByExperiment(AccountModel user, URI xpUri, Integer page, Integer pageSize) throws Exception {
        List<URI> experiments = Collections.singletonList(xpUri);

        Document filter = filterToDocument(
                new DataFileSearchFilter()
                        .setUser(user)
                        .setExperiments(experiments)
        );
        Set<URI> variableURIs = mongodb.distinct(DataModel.VARIABLE_FIELD, URI.class, DATA_COLLECTION_NAME, filter);

        if (variableURIs.isEmpty()) {
            return new ListWithPagination<>(Collections.emptyList(), page, pageSize, 0);
        }

        int total = variableURIs.size();

        List<URI> list = new ArrayList<>(variableURIs);
        List<URI> listToSend = new ArrayList<>();
        if (page * pageSize < total) {
            if (page < 0) {
                page = 0;
            }
            int fromIndex = page * pageSize;
            int toIndex;
            if (total > fromIndex + pageSize) {
                toIndex = fromIndex + pageSize;
            } else {
                toIndex = total;
            }
            listToSend = list.subList(fromIndex, toIndex);
        }

        List<VariableModel> variables = sparql.getListByURIs(VariableModel.class, listToSend, user.getLanguage());
        return new ListWithPagination<>(variables, page, pageSize, total);

    }

    /**
     *
     * @param user
     * @param xpUri
     * @return
     * @throws Exception
     * @deprecated better use directly the method getUsedProvenances
     */
    @Deprecated
    public Set<URI> getProvenancesByExperiment(AccountModel user, URI xpUri) throws Exception {
        List<URI> experiments = Collections.singletonList(xpUri);
        Document filter = filterToDocument(
                new DataFileSearchFilter()
                        .setUser(user)
                        .setExperiments(experiments)
        );
        return mongodb.distinct(DataModel.PROVENANCE_URI_FIELD, URI.class, DATA_COLLECTION_NAME, filter);
    }

//    /**
//     *
//     * @param user
//     * @param uri
//     * @param collectionName
//     * @return
//     * @throws Exception
//     * @deprecated better use directly the method getUsedProvenances
//     */
//    @Deprecated
//    public List<ProvenanceModel> getProvenancesByScientificObject(AccountModel user, URI uri, String collectionName) throws Exception {
//        List<URI> scientificObjects = Collections.singletonList(uri);
//
//        Document filter = filterToDocument(new DataSearchFilter()
//                .setUser(user)
//                .setTargets(scientificObjects)
//        );
//        Set<URI> provenancesURIs = mongodb.distinct(DataModel.PROVENANCE_URI_FIELD, URI.class, collectionName, filter);
//
//        return mongodb.findByURIs(
//                mongodb.getDatabase().getCollection(ProvenanceDAO.PROVENANCE_COLLECTION_NAME,ProvenanceModel.class),
//                provenancesURIs
//        );
//    }

//    /**
//     *
//     * @param user
//     * @param uri
//     * @param collectionName
//     * @return
//     * @throws Exception
//     * @deprecated better use directly the method getUsedProvenances
//     */
//    @Deprecated
//    public List<ProvenanceModel> getProvenancesByDevice(AccountModel user, URI uri, String collectionName) throws Exception {
//        Document filter = filterToDocument(user, null, null, null, null, Arrays.asList(uri), null, null, null, null, null);
//        Set<URI> provenancesURIs = mongodb.distinct(DataModel.PROVENANCE_URI_FIELD, URI.class, collectionName, filter);
//        return mongodb.findByURIs(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new ArrayList<>(provenancesURIs));
//    }

    public List<VariableModel> getUsedVariables(DataSearchFilter searchFilter) throws Exception {

        Set<URI> variableURIs = getUsedVariablesURIs(searchFilter);

        // #TODO don't invoke Variable dao here
        return new VariableDAO(sparql, mongodb).getList(new ArrayList<>(variableURIs), searchFilter.getUser().getLanguage());
    }

    public Set<URI> getUsedVariablesURIs(DataSearchFilter searchFilter) throws Exception {
        Objects.requireNonNull(searchFilter.getUser());

        Document filter = filterToDocument(searchFilter);
        return mongodb.distinct(DataModel.VARIABLE_FIELD, URI.class, DATA_COLLECTION_NAME, filter);
    }

    public Response prepareCSVWideExportResponse(List<DataModel> resultList, AccountModel user, boolean withRawData) throws Exception {
        Instant data = Instant.now();

        Set<URI> dateVariables = getAllDateVariables();

        List<URI> variables = new ArrayList<>();

        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap<>();

        for (DataModel dataModel : resultList) {
            if (dataModel.getTarget() != null && !objects.containsKey(dataModel.getTarget())) {
                objects.put(dataModel.getTarget(), null);
            }

            if (!variables.contains(dataModel.getVariable())) {
                variables.add(dataModel.getVariable());
            }

            if (!provenances.containsKey(dataModel.getProvenance().getUri())) {
                provenances.put(dataModel.getProvenance().getUri(), null);
            }

            if (!dataByIndexAndInstant.containsKey(dataModel.getDate())) {
                dataByIndexAndInstant.put(dataModel.getDate(), new HashMap<>());
            }

            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
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
            } else {
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
        LOGGER.debug("Data conversion " + Duration.between(data, dataTransform).toMillis() + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();

        // first static columns

        defaultColumns.add("Experiment");
        defaultColumns.add("Target");
        defaultColumns.add("Date");

        List<String> methods = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            methods.add("");
        }
        methods.add("Method");
        List<String> units = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            units.add("");
        }
        units.add("Unit");
        List<String> variablesList = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            variablesList.add("");
        }
        variablesList.add("Variable");

        List<VariableModel> variablesModelList = new VariableDAO(sparql, mongodb).getList(variables);

        Map<URI, Integer> variableUriIndex = new HashMap<>();
        for (VariableModel variableModel : variablesModelList) {

            MethodModel method = variableModel.getMethod();

            if (method != null) {
                String methodID = method.getName() + " (" + method.getUri().toString() + ")";
                methods.add(methodID);
            } else {
                methods.add("");
            }
            UnitModel unit = variableModel.getUnit();

            String unitID = unit.getName() + " (" + unit.getUri().toString() + ")";
            units.add(unitID);
            variablesList.add(variableModel.getName() + " (" + variableModel.getUri() + ")");
            defaultColumns.add(variableModel.getName());
            if (withRawData) {
                defaultColumns.add("Raw data");
                methods.add("");
                units.add("");
                variablesList.add("");
                variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 2));
            } else {
                variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 1));
            }
        }
        // static supplementary columns
        defaultColumns.add("Provenance");
        defaultColumns.add("Experiment URI");
        defaultColumns.add("Target URI");
        defaultColumns.add("Provenance URI");

        Instant variableTime = Instant.now();
        LOGGER.debug("Get " + variables.size() + " variable(s) " + Duration.between(dataTransform, variableTime).toMillis() + " milliseconds elapsed");
        OntologyDAO ontologyDao = new OntologyDAO(sparql);

        // Provides the experiment as context if there is only one
        URI context = null;
        if (experiments.size() == 1) {
            context = experiments.keySet().stream().findFirst().get();
        }

        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), context);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        LOGGER.debug("Get " + objectsList.size() + " target(s) " + Duration.between(variableTime, targetTime).toMillis() + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(mongodb, sparql);
            List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Duration.between(targetTime, provenancesTime).toMillis() + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        LOGGER.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");

        // ObjectURI, ObjectName, Factor, Date, Confidence, Variable n ...
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            // Method
            // Unit
            // Variable
            writer.writeNext(methods.toArray(new String[methods.size()]));
            writer.writeNext(units.toArray(new String[units.size()]));
            writer.writeNext(variablesList.toArray(new String[variablesList.size()]));
            // empty line
            writer.writeNext(new String[defaultColumns.size()]);
            // headers
            writer.writeNext(defaultColumns.toArray(new String[defaultColumns.size()]));

            // Search in map indexed by date for exp, prov, object and data
            for (Map.Entry<Instant, Map<ExportDataIndex, List<DataExportDTO>>> instantProvUriDataEntry : dataByIndexAndInstant.entrySet()) {
                Map<ExportDataIndex, List<DataExportDTO>> mapProvUriData = instantProvUriDataEntry.getValue();
                // Search in map indexed by  prov and data
                for (Map.Entry<ExportDataIndex, List<DataExportDTO>> provUriObjectEntry : mapProvUriData.entrySet()) {
                    List<DataExportDTO> val = provUriObjectEntry.getValue();

                    ArrayList<String> csvRow = new ArrayList<>();
                    //first is used to have value with the same dates on the same line
                    boolean first = true;

                    for (DataExportDTO dataGetDTO : val) {


                        if (first) {

                            csvRow = new ArrayList<>();

                            // experiment
                            ExperimentModel experiment = null;
                            if (dataGetDTO.getProvenance().getExperiments() != null && !dataGetDTO.getProvenance().getExperiments().isEmpty()) {
                                experiment = experiments.get(dataGetDTO.getExperiment());
                            }

                            if (experiment != null) {
                                csvRow.add(experiment.getName());
                            } else {
                                csvRow.add("");
                            }

                            // target
                            SPARQLNamedResourceModel target = null;
                            if(dataGetDTO.getTarget() != null){
                               target = objects.get(dataGetDTO.getTarget());
                            }

                            if(target != null){
                                csvRow.add(target.getName());
                            }else{
                                csvRow.add("");
                            }

                            // date
                            csvRow.add(dataGetDTO.getDate());

                            // write blank columns for value and rawData
                            for (int i = 0; i < variables.size(); i++) {
                                csvRow.add("");
                                if (withRawData) {
                                    csvRow.add("");
                                }
                            }

                            // provenance
                            if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                                csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
                            } else {
                                csvRow.add("");
                            }

                            // experiment URI
                            if (experiment != null) {
                                csvRow.add(experiment.getUri().toString());
                            } else {
                                csvRow.add("");
                            }

                            // target URI
                             if(target != null){
                                csvRow.add(target.getUri().toString());
                            }else{
                                csvRow.add("");
                            }

                            // provenance Uri
                            csvRow.add(dataGetDTO.getProvenance().getUri().toString());

                            first = false;
                        }
                        // compare only expended URIs
                        URI dataGetDTOVariable = URI.create(SPARQLDeserializers.getExpandedURI(dataGetDTO.getVariable()));

                        // value
                        if (dataGetDTO.getValue() == null){
                            csvRow.set(variableUriIndex.get(dataGetDTOVariable), null);
                        } else {
                            csvRow.set(variableUriIndex.get(dataGetDTOVariable), dataGetDTO.getValue().toString());
                        }

                        // raw data
                        if (withRawData) {
                            if (dataGetDTO.getRawData() == null){
                                csvRow.set(variableUriIndex.get(dataGetDTOVariable)+1, null);
                            } else {
                                csvRow.set(variableUriIndex.get(dataGetDTOVariable)+1, Arrays.toString(dataGetDTO.getRawData().toArray()).replace("[", "").replace("]", ""));
                            }
                        }

                    }


                    String[] row = csvRow.toArray(new String[csvRow.size()]);
                    writer.writeNext(row);

                }
            }

            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_wide_format" + dtf.format(date) + ".csv";

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Duration.between(provenancesTime, writeCSVTime).toMillis() + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=" + fileName )
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    public Response prepareCSVLongExportResponse(List<DataModel> resultList, AccountModel user, boolean withRawData) throws Exception {
        Instant data = Instant.now();

        Set<URI> dateVariables = getAllDateVariables();

        Map<URI, VariableModel> variables = new HashMap<>();
        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap<>();

        HashMap<Instant, List<DataGetDTO>> dataByInstant = new HashMap<>();
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
            if (!dataByInstant.containsKey(dataModel.getDate())) {
                dataByInstant.put(dataModel.getDate(), new ArrayList<>());
            }
            dataByInstant.get(dataModel.getDate()).add(DataGetDTO.getDtoFromModel(dataModel, dateVariables));
            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
                    }
                }
            }
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();

        defaultColumns.add("Experiment");
        defaultColumns.add("Target");
        defaultColumns.add("Date");
        defaultColumns.add("Variable");
        defaultColumns.add("Method");
        defaultColumns.add("Unit");
        defaultColumns.add("Value");
        if (withRawData) {
            defaultColumns.add("Raw data");
        }
        defaultColumns.add("Data Description");
        defaultColumns.add("");
        defaultColumns.add("Experiment URI");
        defaultColumns.add("Target URI");
        defaultColumns.add("Variable URI");
        defaultColumns.add("Data Description URI");

        Instant variableTime = Instant.now();
        List<VariableModel> variablesModelList = new VariableDAO(sparql, mongodb).getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(new URI(SPARQLDeserializers.getShortURI(variableModel.getUri())), variableModel);
        }
        LOGGER.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        OntologyDAO ontologyDao = new OntologyDAO(sparql);
        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), null);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        LOGGER.debug("Get " + objectsList.size() + " target(s) " + Duration.between(variableTime, targetTime).toMillis() + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(mongodb, sparql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Duration.between(targetTime, provenancesTime).toMillis() + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        LOGGER.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");

        // See defaultColumns order
        //        Target
        //        Date
        //        Variable
        //        Method
        //        Unit
        //        Value
        //        Data Description
        //
        //        Target URI
        //        Variable URI
        //        Data Description URI
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(defaultColumns.toArray(new String[defaultColumns.size()]));
            for (Map.Entry<Instant, List<DataGetDTO>> entry : dataByInstant.entrySet()) {
                List<DataGetDTO> val = entry.getValue();
                for (DataGetDTO dataGetDTO : val) {

                    //1 row per experiment
                    int maxRows = 1;
                    if (dataGetDTO.getProvenance().getExperiments() != null && dataGetDTO.getProvenance().getExperiments().size()>1) {
                        maxRows = dataGetDTO.getProvenance().getExperiments().size();
                    }

                    for (int j=0; j<maxRows; j++) {
                        ArrayList<String> csvRow = new ArrayList<>();
                        // experiment
                        ExperimentModel experiment = null;
                        if (dataGetDTO.getProvenance().getExperiments() != null && !dataGetDTO.getProvenance().getExperiments().isEmpty()) {
                            experiment = experiments.get(dataGetDTO.getProvenance().getExperiments().get(j));
                        }

                        if (experiment != null) {
                            csvRow.add(experiment.getName());
                        } else {
                            csvRow.add("");
                        }

                        SPARQLNamedResourceModel target = null;
                        if(dataGetDTO.getTarget() != null){
                           target = objects.get(dataGetDTO.getTarget());
                        }
                        // target name
                        if(target != null){
                            csvRow.add(target.getName());
                        }else{
                            csvRow.add("");
                        }

                        // date
                        csvRow.add(dataGetDTO.getDate());
                        // variable
                        csvRow.add(variables.get(dataGetDTO.getVariable()).getName());
                        // method
                        if (variables.get(dataGetDTO.getVariable()).getMethod() != null) {
                            csvRow.add(variables.get(dataGetDTO.getVariable()).getMethod().getName());
                        } else {
                            csvRow.add("");
                        }
                        // unit
                        csvRow.add(variables.get(dataGetDTO.getVariable()).getUnit().getName());
                        // value
                        if(dataGetDTO.getValue() == null){
                            csvRow.add(null);
                        }else{
                            csvRow.add(dataGetDTO.getValue().toString());
                        }

                        // rawData
                        if (withRawData) {
                            if(dataGetDTO.getRawData() == null){
                                csvRow.add(null);
                            }else{
                                csvRow.add(Arrays.toString(dataGetDTO.getRawData().toArray()).replace("[", "").replace("]", ""));
                            }
                        }

                        // provenance
                        if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                            csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
                        } else {
                            csvRow.add("");
                        }
                        csvRow.add("");

                        // experiment URI
                        if (experiment != null) {
                            csvRow.add(experiment.getUri().toString());
                        } else {
                            csvRow.add("");
                        }

                        // target uri
                        if (target != null) {
                            csvRow.add(target.getUri().toString());
                        } else {
                            csvRow.add("");
                        }

                        // variable uri
                        csvRow.add(dataGetDTO.getVariable().toString());
                        // provenance Uri
                        csvRow.add(dataGetDTO.getProvenance().getUri().toString());

                        String[] row = csvRow.toArray(new String[csvRow.size()]);
                        writer.writeNext(row);
                    }
                }
            }
            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_long_format" + dtf.format(date) + ".csv";

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Duration.between(provenancesTime, writeCSVTime).toMillis() + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    public Set<URI> getUsedProvenancesURIs(DataSearchFilter searchFilter) throws Exception {
        Document filter = filterToDocument(searchFilter);
        return mongodb.distinct(DataModel.PROVENANCE_URI_FIELD, URI.class, DATA_COLLECTION_NAME, filter);
    }


    /**
     * Fetches and return all URIs for the variable with the xsd:date datatype.
     *
     * @return
     * @throws Exception
     */
    private Set<URI> getAllDateVariables() throws Exception {
        return new HashSet<>(sparql.searchURIs(VariableModel.class, null, selectBuilder -> {
            Var uriVar = SPARQLQueryHelper.makeVar(VariableModel.URI_FIELD);
            selectBuilder.addWhere(uriVar, Oeso.hasDataType.asNode(), XSD.date.asNode());
        }));
    }

    /**
     * Converts a {@link DataModel} to a {@link DataGetDTO}, with the correct conversion if the value is of
     * type xsd:date. This method should be called as few times as possible (ideally only once), as it performs
     * a SPARQL query. To convert multiple models, please use {@link #modelListToDTO(ListWithPagination)} instead.
     *
     * @param model
     * @return
     * @throws Exception
     */
    public DataGetDTO modelToDTO(DataModel model) throws Exception {
        Set<URI> dateVariables = getAllDateVariables();
        return DataGetDTO.getDtoFromModel(model, dateVariables);
    }

    /**
     * Converts a list of {@link DataModel} to a lislt of {@link DataGetDTO}, with the correct conversion if the value is of
     * type xsd:date. This method should be called as few times as possible (ideally only once), as it performs
     * a SPARQL query.
     *
     * @param modelList
     * @return
     * @throws Exception
     */
    public ListWithPagination<DataGetDTO> modelListToDTO(ListWithPagination<DataModel> modelList) throws Exception {
        Set<URI> dateVariables = getAllDateVariables();
        List<DataGetDTO> dtoList = new ArrayList<>();
        modelList.getList().forEach(dataModel -> {
            DataGetDTO dto = DataGetDTO.getDtoFromModel(dataModel, dateVariables);
            if (Objects.nonNull(dataModel.getPublisher())) {
                try {
                    dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(dataModel.getPublisher())));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            dtoList.add(dto);
        });
        return new ListWithPagination<>(dtoList, modelList.getPage(), modelList.getPageSize(), modelList.getTotal());
    }

    /**
     * Retrieve median per hour data series
     * @details
     *  data are collected and grouped by [target, variable, provenance].
     *  The median per hour is then computed for each data series
     * @param user
     * @param target
     * @param variable
     * @param startDate
     * @param endDate
     * @return a list of median values
     * @throws Exception
     */
    public List<DataComputedModel> computeAllMediansPerHour(AccountModel user,
                                                   URI target,
                                                   URI variable,
                                                   Instant startDate,
                                                   Instant endDate) throws Exception {

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

        Set<DataComputedModel> results = nosql.aggregate(DataDAO.DATA_COLLECTION_NAME, aggregations, DataComputedModel.class);

        return results.stream().collect(Collectors.toList());
    }

    /**
     * Compute the daily average from data
     * @param user
     * @param target
     * @param variable
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public List<DataComputedModel> computeAllMeanPerDay(AccountModel user,
                                                            URI target,
                                                            URI variable,
                                                            Instant startDate,
                                                            Instant endDate) throws Exception {

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

        Set<DataComputedModel> results = nosql.aggregate(DataDAO.DATA_COLLECTION_NAME, aggregations, DataComputedModel.class);

        return results.stream().collect(Collectors.toList());
    }

    /**
     * Return the last data stored in the system
     * @details In the case there are multiple last data, keep only the first returned
     * @param user
     * @param experiments
     * @param targets
     * @param variables
     * @param provenances
     * @param devices
     * @param startDate
     * @param endDate
     * @param confidenceMin
     * @param confidenceMax
     * @param metadata
     * @param operators
     * @return the last data
     * @throws Exception
     */
    public DataComputedGetDTO getLastDataFound(AccountModel user,
                                             List<URI> experiments,
                                             List<URI> targets,
                                             List<URI> variables,
                                             List<URI> provenances,
                                             List<URI> devices,
                                             Instant startDate,
                                             Instant endDate,
                                             Float confidenceMin,
                                             Float confidenceMax,
                                             Document metadata,
                                             List<URI> operators) throws Exception {

        List<Bson> aggregations = new ArrayList<>();

        Bson match = Aggregates.match(searchFilter(
                user,
                experiments,
                targets,
                variables,
                provenances,
                devices,
                startDate,
                endDate,
                confidenceMin,
                confidenceMax,
                metadata,
                operators));
        Bson sort = Aggregates.sort(new Document("date", -1));
        Bson limit = Aggregates.limit(1);

        aggregations.add(match);
        aggregations.add(sort);
        aggregations.add(limit);

        Set<DataModel> results = nosql.aggregate(DataDAO.DATA_COLLECTION_NAME, aggregations, DataModel.class);

        return results.stream().findFirst()
                .map(DataComputedGetDTO::getDtoFromModel)
                .orElse(null);

    }
}
