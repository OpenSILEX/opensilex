package org.opensilex.core.data.dal;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.conversions.Bson;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Dao for handling {@link DataModel} by extending {@link MongoReadWriteDao}
 * #todo : handle write operations and other custom search methods
 *
 * @author rcolin
 */
public class DataDaoV2 extends MongoReadWriteDao<DataModel, DataSearchFilter> {


    protected static final String COLLECTION_NAME = "data";

    protected final SPARQLService sparql;

    // Use the old MongoDBService since We need ExperimentDao and ProvenanceDao which requires this service
    protected final MongoDBService mongoDBService;

    // Provenance empty filters and fields
    protected static final String PROVENANCE_URI_FIELD = DataModel.PROVENANCE_FIELD + "." + MongoModel.URI_FIELD;
    protected static final String PROVENANCE_EXPERIMENT_FIELD = DataModel.PROVENANCE_FIELD + "." + DataProvenanceModel.EXPERIMENT_FIELD;
    protected static final Bson NO_EXPERIMENT_FILTER = Filters.eq(PROVENANCE_EXPERIMENT_FIELD, null);

    protected static final String PROVENANCE_PROV_WAS_ASSOCIATED_WITH_FIELD = DataModel.PROVENANCE_FIELD + "." + DataProvenanceModel.PROV_WAS_ASSOCIATED_WITH_FIELD;
    protected static final Bson NO_PROV_WAS_ASSOCIATED_WITH_FILTER = Filters.eq(PROVENANCE_PROV_WAS_ASSOCIATED_WITH_FIELD, null);


    // Register indexes for collection
    static {
        MongoDBServiceV2.registerIndexes(
                COLLECTION_NAME,
                Map.of(
                        Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true),
                        Indexes.ascending(DataModel.VARIABLE_FIELD, DataModel.PROVENANCE_FIELD, DataModel.TARGET_FIELD, DataModel.DATE_FIELD), new IndexOptions().unique(true),

                        Indexes.descending(DataModel.DATE_FIELD), new IndexOptions(),
                        Indexes.descending(DataModel.TARGET_FIELD), new IndexOptions(),

                        Indexes.ascending(DataModel.VARIABLE_FIELD, DataModel.DATE_FIELD), new IndexOptions(),
                        Indexes.ascending(DataModel.VARIABLE_FIELD, DataModel.TARGET_FIELD, DataModel.DATE_FIELD), new IndexOptions(),

                        Indexes.ascending(PROVENANCE_EXPERIMENT_FIELD), new IndexOptions(),
                        Indexes.ascending(PROVENANCE_URI_FIELD), new IndexOptions()
                )
        );
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
        if(user == null){
            throw new IllegalStateException("Internal error. The current AccountModel must be provided to the search");
        }

        if (Boolean.TRUE.equals(user.isAdmin())) {
            if (!CollectionUtils.isEmpty(filter.getExperiments())) {
                bsonFilters.add(Filters.in(PROVENANCE_EXPERIMENT_FIELD, filter.getExperiments()));
            }
            return;
        }

        Set<URI> userExperiments;
        try{
            userExperiments =  new ExperimentDAO(sparql, mongoDBService).getUserExperiments(user);
        }catch (Exception e){
            throw new RuntimeException("Unexpected error when retrieving user experiments during data filter building", e);
        }

        if (! CollectionUtils.isEmpty(filter.getExperiments())) {

            // Keep only the provided experiment which belongs to the allowed user experiment set
            Set<URI> allowedExperiments = filter.getExperiments()
                    .stream()
                    .map(xp -> URI.create(SPARQLDeserializers.formatURI(xp.toString()))) // format incoming URI to ensure equality with URIs from the Experiment DAO
                    .filter(userExperiments::contains)
                    .map(xp -> URI.create(SPARQLDeserializers.getExpandedURI(xp))) // use long URI because there are stored in this format in MongoDB
                    .collect(Collectors.toSet());

            if (allowedExperiments.isEmpty()) {
                throw new IllegalArgumentException("You can't access to the given experiments");
            }
            bsonFilters.add(Filters.in(PROVENANCE_EXPERIMENT_FIELD, allowedExperiments));
            return;
        }

        //  no experiment filter provided -> Search in (user experiment or data with no experiment)
        if (CollectionUtils.isEmpty(userExperiments)) {
            bsonFilters.add(NO_EXPERIMENT_FILTER);
        } else {
            bsonFilters.add(Filters.or(
                    Filters.in(PROVENANCE_EXPERIMENT_FIELD, userExperiments),
                    NO_EXPERIMENT_FILTER
            ));
        }
    }

    protected void addProvenanceAgentFilter(List<Bson> bsonFilters, DataSearchFilter filter){

        // Filter on agent from devices or operator inside data provenance
        List<URI> dataProvenanceAgents = new LinkedList<>();
        dataProvenanceAgents.addAll(Optional.ofNullable(filter.getDevices()).orElse(Collections.emptyList()));
        dataProvenanceAgents.addAll(Optional.ofNullable(filter.getOperators()).orElse(Collections.emptyList()));

        Bson dataProvenanceFilter = Filters.in(PROVENANCE_PROV_WAS_ASSOCIATED_WITH_FIELD, dataProvenanceAgents);

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
}
