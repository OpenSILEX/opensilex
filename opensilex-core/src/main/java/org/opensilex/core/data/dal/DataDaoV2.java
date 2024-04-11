package org.opensilex.core.data.dal;

import com.mongodb.client.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.XSD;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.core.data.api.DataComputedGetDTO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;

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

    // Provenance empty filters and fields

    protected static final Bson NO_EXPERIMENT_FILTER = Filters.eq(PROVENANCE_EXPERIMENT_FIELD, null);
    protected static final Bson NO_PROV_WAS_ASSOCIATED_WITH_FILTER = Filters.eq(PROVENANCE_AGENTS_URI_FIELD, null);

    public static Map<Bson, IndexOptions> getIndexes() {

        IndexOptions defaultOptions = new IndexOptions();

        Bson dateDescIndex = Indexes.descending(DataModel.DATE_FIELD);
        Bson variableAscIndex = Indexes.ascending(DataModel.VARIABLE_FIELD);
        Bson targetDescIndex = Indexes.ascending(DataModel.TARGET_FIELD);
        Bson experimentAscIndex = Indexes.ascending(PROVENANCE_EXPERIMENT_FIELD);
        Bson agentAscIndex = Indexes.ascending(PROVENANCE_AGENTS_URI_FIELD);
        Bson provenanceUriAscIndex = Indexes.ascending(PROVENANCE_URI_FIELD);

        Map<Bson, IndexOptions> indexes = new HashMap<>();

        // index on field : URI
        indexes.put(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        indexes.put(dateDescIndex, defaultOptions);

        // Index of field, sorted by date : (experiment, provenance, variable, target, provenance agent)
        indexes.put(Indexes.compoundIndex(experimentAscIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(provenanceUriAscIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(variableAscIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(targetDescIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(agentAscIndex, dateDescIndex), defaultOptions);

        // Multi-fields indexes : Access by experiment and (variable, target, provenance agent). Add date to ensure index usage in case of sorting by date
        indexes.put(Indexes.compoundIndex(experimentAscIndex, variableAscIndex, targetDescIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(experimentAscIndex, agentAscIndex, targetDescIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(experimentAscIndex, targetDescIndex, agentAscIndex, dateDescIndex), defaultOptions);

        // Compound index : ensure unicity #TODO delete this index (index on whole field,too big and not well used in query)
        indexes.put(Indexes.compoundIndex(variableAscIndex, Indexes.ascending(DataModel.PROVENANCE_FIELD), targetDescIndex, dateDescIndex), new IndexOptions().unique(true));

        return indexes;
    }

    public DataDaoV2(SPARQLService sparql, MongoDBService mongoDBService) {
        super(mongoDBService.getServiceV2(), DataModel.class, COLLECTION_NAME, "data");
        this.sparql = sparql;
        this.mongoDBService = mongoDBService;
    }

    @Override
    public List<Bson> getBsonFilters(DataSearchFilter filter) {

        // #todo : Handle business logic (call to Experiment and provenance DAO) in a dedicated business class
        List<Bson> bsonFilters = super.getBsonFilters(filter);

        addExperimentFilter(bsonFilters, filter);

        if (!CollectionUtils.isEmpty(filter.getTargets())) {
            bsonFilters.add(Filters.in(DataModel.TARGET_FIELD, filter.getTargets()));
        }

        if (!CollectionUtils.isEmpty(filter.getVariables())) {
            bsonFilters.add(Filters.in(DataModel.VARIABLE_FIELD, filter.getVariables()));
        }

        if (!CollectionUtils.isEmpty(filter.getProvenances())) {
            bsonFilters.add(Filters.in(PROVENANCE_URI_FIELD, filter.getProvenances()));
        }

        if (filter.getStartDate() != null) {
            bsonFilters.add(Filters.gte(DataModel.DATE_FIELD, filter.getStartDate()));
        }
        if (filter.getEndDate() != null) {
            bsonFilters.add(Filters.lte(DataModel.DATE_FIELD, filter.getEndDate()));
        }

        if (filter.getConfidenceMin() != null) {
            bsonFilters.add(Filters.gte(DataModel.CONFIDENCE_FIELD, filter.getConfidenceMin()));
        }
        if (filter.getConfidenceMax() != null) {
            bsonFilters.add(Filters.lte(DataModel.CONFIDENCE_FIELD, filter.getConfidenceMax()));
        }

        if (filter.getMetadata() != null) {
            filter.getMetadata().forEach((metadataKey, metadataValue) -> bsonFilters.add(Filters.eq(DataModel.METADATA_FIELD + "." + metadataKey, metadataValue)));
        }

        addProvenanceAgentFilter(bsonFilters, filter);

        return bsonFilters;
    }

    protected void addExperimentFilter(List<Bson> bsonFilters, DataSearchFilter filter) {

        AccountModel user = filter.getUser();
        if (user == null) {
            throw new IllegalStateException("Internal error. The current AccountModel must be provided to the search");
        }

        if (Boolean.TRUE.equals(user.isAdmin())) {
            if (!CollectionUtils.isEmpty(filter.getExperiments())) {
                bsonFilters.add(Filters.in(PROVENANCE_EXPERIMENT_FIELD, filter.getExperiments()));
            }
            return;
        }

        Set<URI> userExperiments;
        try {
            userExperiments = new ExperimentDAO(sparql, mongoDBService).getUserExperiments(user);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error when retrieving user experiments during data filter building", e);
        }

        if (!CollectionUtils.isEmpty(filter.getExperiments())) {

            // Keep only the provided experiment which belongs to the allowed user experiment set
            filter.getExperiments().retainAll(userExperiments);
            if (filter.getExperiments().isEmpty()) {
                throw new IllegalArgumentException("You can't access to the given experiments");
            }

            bsonFilters.add(Filters.in(PROVENANCE_EXPERIMENT_FIELD, filter.getExperiments()));
            return;
        }

        //  no experiment filter provided -> Search in (user experiment or data with no experiment)
        if (!CollectionUtils.isEmpty(userExperiments)) {
            bsonFilters.add(Filters.or(
                    Filters.in(PROVENANCE_EXPERIMENT_FIELD, userExperiments),
                    NO_EXPERIMENT_FILTER
            ));
        }
    }

    protected void addProvenanceAgentFilter(List<Bson> bsonFilters, DataSearchFilter filter) {

        // Filter on agent from devices or operator inside data provenance
        List<URI> dataProvenanceAgents = new LinkedList<>();
        dataProvenanceAgents.addAll(Optional.ofNullable(filter.getDevices()).orElse(Collections.emptyList()));
        dataProvenanceAgents.addAll(Optional.ofNullable(filter.getOperators()).orElse(Collections.emptyList()));

        Bson dataProvenanceFilter = Filters.in(PROVENANCE_AGENTS_URI_FIELD, dataProvenanceAgents);

        // Filter on agents from global provenances
        Set<URI> globalProvenanceAgents = dataProvenanceAgents.isEmpty() ?
                Collections.emptySet() :
                new ProvenanceDAO(mongoDBService, sparql).getProvenancesURIsByAgents(dataProvenanceAgents);

        Bson globalProvenanceFilter = Filters.in(PROVENANCE_URI_FIELD, globalProvenanceAgents);
        Bson globalProvenanceOrEmptyFilter = Filters.or(globalProvenanceFilter, NO_PROV_WAS_ASSOCIATED_WITH_FILTER);

        // Try to simplify the query : avoid a complex OR query with empty array
        if (dataProvenanceAgents.isEmpty()) {
            if (!globalProvenanceAgents.isEmpty()) { // only match global provenance
                bsonFilters.add(globalProvenanceOrEmptyFilter);
            }
        } else {
            if (globalProvenanceAgents.isEmpty()) {  // only match data provenance
                bsonFilters.add(dataProvenanceFilter);
            } else {
                bsonFilters.add(Filters.or(dataProvenanceFilter, globalProvenanceOrEmptyFilter));  // match agents from data provenance or from global provenance
            }
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

        Set<DataComputedModel> results = nosql.aggregate(DataDAO.DATA_COLLECTION_NAME, aggregations, DataComputedModel.class);

        lookupAggregation()
        return results.stream().collect(Collectors.toList());
    }

    @Override
    protected void addDefaultSort(Document sort) {
        sort.put(DataModel.DATE_FIELD, -1);
    }
}
