package org.opensilex.core.data.dal;

import com.apicatalog.jsonld.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.XSD;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.core.data.api.CriteriaDTO;
import org.opensilex.core.data.api.DataComputedGetDTO;
import org.opensilex.core.data.api.SingleCriteriaDTO;
import org.opensilex.core.data.dal.aggregations.DataTargetAggregateModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.MathematicalOperator;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ExcludableUriList;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.core.data.dal.DataProvenanceModel.*;

/**
 * A Dao for handling {@link DataModel} by extending {@link MongoReadWriteDao}
 * #todo : handle write operations and other custom search methods
 *
 * @author rcolin
 */
public class DataDaoV2 extends MongoReadWriteDao<DataModel, DataSearchFilter> {


    public static final String COLLECTION_NAME = "data";

    protected final SPARQLService sparql;

    // Use the old MongoDBService since We need ExperimentDao and ProvenanceDao which requires this service
    protected final MongoDBService mongoDBService;

    protected final FileStorageService fs;

    //Used to get indexes and filters that are all the same apart from variable stuff
    private final DataFileDaoV2 dataFileDaoV2;

    public DataDaoV2(SPARQLService sparql, MongoDBService mongoDBService, FileStorageService fs) {
        super(mongoDBService.getServiceV2(), DataModel.class, COLLECTION_NAME, "data");
        this.sparql = sparql;
        this.dataFileDaoV2 = new DataFileDaoV2(mongoDBService, sparql);
        this.mongoDBService = mongoDBService;
        this.fs = fs;
    }

    /**
     * Get indexes for Data, gets all the indexes from DataFiles as they are all the same apart from ones that concern variables
     * @return the indexes
     */
    public static Map<Bson, IndexOptions> getIndexes() {

        Bson variableAscIndex = Indexes.ascending(DataModel.VARIABLE_FIELD);
        Bson dateDescIndex = Indexes.descending(DataModel.DATE_FIELD);
        Bson targetDescIndex = Indexes.ascending(DataModel.TARGET_FIELD);
        Bson experimentAscIndex = Indexes.ascending(PROVENANCE_EXPERIMENT_FIELD);

        Map<Bson, IndexOptions> indexes = DataFileDaoV2.getIndexes();

        // Index of field, sorted by date : (experiment, provenance, variable, target, provenance agent)
        indexes.put(Indexes.compoundIndex(variableAscIndex, dateDescIndex), null);

        // Multi-fields indexes : Access by experiment and (variable, target, provenance agent). Add date to ensure index usage in case of sorting by date
        indexes.put(Indexes.compoundIndex(experimentAscIndex, variableAscIndex, targetDescIndex, dateDescIndex), null);
        indexes.put(Indexes.compoundIndex(variableAscIndex, targetDescIndex, dateDescIndex), null);

        // Compound index : ensure unicity #TODO delete this index (index on whole field,too big and not well used in query)
        indexes.put(Indexes.compoundIndex(variableAscIndex, Indexes.ascending(DataModel.PROVENANCE_FIELD), targetDescIndex, dateDescIndex), new IndexOptions().unique(true));

        return indexes;
    }

    @Override
    public List<Bson> getBsonFilters(DataSearchFilter filter) {

        //Get all bysons from DataFiles as they are all the same apart from the variable filter

        List<Bson> bsonFilters = dataFileDaoV2.getBsonFilters(filter);


        if (!CollectionUtils.isEmpty(filter.getVariables())) {
            bsonFilters.add(Filters.in(DataModel.VARIABLE_FIELD, filter.getVariables()));
        }

        return bsonFilters;
    }

//TODO? All or most of these next methods concern opensilex logic and some are used by other Daos so put in data BLL

    public List<URI> getUsedTargets(AccountModel user, List<URI> devices, List<URI> variables, List<URI> experiments) throws Exception {
        DataSearchFilter dataSearchFilter = new DataSearchFilter().setVariables(variables);
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices);
        return distinct(null, DataModel.TARGET_FIELD, URI.class, dataSearchFilter);
    }

    public List<VariableModel> getUsedVariables(AccountModel user, List<URI> experiments, List<URI> objects, List<URI> provenances, List<URI> devices) throws Exception {
        DataSearchFilter dataSearchFilter = new DataSearchFilter();
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices).setTargets(objects).setProvenances(provenances);
        Set<URI> variableURIs = new HashSet<>(distinct(null, DataModel.VARIABLE_FIELD, URI.class, dataSearchFilter));
        String userLanguage = null;
        if(user != null){
            userLanguage = user.getLanguage();
        }
        // #TODO don't invoke Variable dao here
        return new VariableDAO(sparql, mongoDBService, fs).getList(new ArrayList<>(variableURIs), userLanguage);
    }

    public Set<URI> getUsedVariablesByExpeSoDevice(AccountModel user, List<URI> experiments, List<URI> objects, List<URI> devices) {
        DataSearchFilter dataSearchFilter = new DataSearchFilter();
        dataSearchFilter.setUser(user).setExperiments(experiments).setDevices(devices).setTargets(objects);
        return new HashSet<>(distinct(null, DataModel.VARIABLE_FIELD, URI.class, dataSearchFilter));
    }

    /**
     *
     * @param criteriaDTO
     * @param variableDAO so we know how to fetch datatype of each variable
     * @return Null if criteria dto had no valid criteria, Object containing empty list if valid criteria but no results,
     * or a list of object uris if criteria were valid and results were found
     * The return object also contains a boolean to say if we need to keep only elements in the the list, or exclude them
     */
    public ExcludableUriList getScientificObjectsThatMatchDataCriteria(CriteriaDTO criteriaDTO, URI experiment, AccountModel user , VariableDAO variableDAO) throws Exception {

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
                            user,
                            null,
                            criteriaDTO.getCriteriaList().stream().map(SingleCriteriaDTO::getVariableUri).collect(Collectors.toList()),
                            (experiment == null ? null : Collections.singletonList(experiment))
                    ).stream().filter(Objects::nonNull).collect(Collectors.toList())
            );
        }
        //If every line wasn't "NotMeasured" :
        List<Bson> aggregationDocs = this.createCriteriaSearchAggregation(criteriaDTO, experiment,user, variableDAO);

        //If aggregationDocs is null then it means there was a contradiction
        if(aggregationDocs == null){
            return new ExcludableUriList(false, Collections.emptyList());
        }

        Set<DataTargetAggregateModel> criteriaSearchedResult = new HashSet<>(this.aggregate(aggregationDocs, DataTargetAggregateModel.class));
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
            ExperimentDAO expDAO = new ExperimentDAO(sparql, mongoDBService);
            Set<URI> userExperiments = expDAO.getUserExperiments(user);

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

    /**
     * Return the last data stored in the system
     * @details In the case there are multiple last data, keep only the first returned
     * @param filter
     * @return the last data
     * @throws Exception
     */
    public DataComputedGetDTO getLastDataFound(DataSearchFilter filter) throws Exception {

        List<Bson> aggregations = new ArrayList<>();

        Bson match = Aggregates.match(Filters.and(getBsonFilters(filter)));
        Bson sort = Aggregates.sort(new Document("date", -1));
        Bson limit = Aggregates.limit(1);

        aggregations.add(match);
        aggregations.add(sort);
        aggregations.add(limit);

        return aggregateAsStream(aggregations).findFirst()
                .map(DataComputedGetDTO::getDtoFromModel)
                .orElse(null);

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

        return aggregate(aggregations, DataComputedModel.class);
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

        return aggregate(aggregations, DataComputedModel.class);
    }

    @Override
    protected void addDefaultSort(Document sort) {
        sort.put(DataModel.DATE_FIELD, -1);
    }
}
