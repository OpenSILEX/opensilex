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
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.util.*;

import static org.opensilex.core.data.dal.DataProvenanceModel.*;

public class DataFileDaoV2 extends MongoReadWriteDao<DataFileModel, DataFileSearchFilter> {

    public static final String COLLECTION_NAME = "file";
    public static final String FILE_PREFIX = "file";
    public static final String FS_FILE_PREFIX = "datafile";

    //TODO temporary old nosql service and sparql to delete once the BLL is done
    private final MongoDBService mongoDBService;
    private final SPARQLService sparqlService;

    // Provenance empty filters and fields
    protected static final Bson NO_EXPERIMENT_FILTER = Filters.eq(PROVENANCE_EXPERIMENT_FIELD, null);
    protected static final Bson NO_PROV_WAS_ASSOCIATED_WITH_FILTER = Filters.eq(PROVENANCE_AGENTS_URI_FIELD, null);

    public DataFileDaoV2(MongoDBService mongoDBService, SPARQLService sparqlService) {
        super(mongoDBService.getServiceV2(), DataFileModel.class, COLLECTION_NAME, COLLECTION_NAME);
        this.mongoDBService = mongoDBService;
        this.sparqlService = sparqlService;
    }

    public static Map<Bson, IndexOptions> getIndexes() {

        Bson dateDescIndex = Indexes.descending(DataModel.DATE_FIELD);
        Bson targetDescIndex = Indexes.ascending(DataModel.TARGET_FIELD);
        Bson experimentAscIndex = Indexes.ascending(PROVENANCE_EXPERIMENT_FIELD);
        Bson agentAscIndex = Indexes.ascending(PROVENANCE_AGENTS_URI_FIELD);
        Bson provenanceUriAscIndex = Indexes.ascending(PROVENANCE_URI_FIELD);

        Map<Bson, IndexOptions> indexes = new HashMap<>();

        // index on field : _id, URI and date
        indexes.put(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        indexes.put(dateDescIndex, null);

        // Index of field, sorted by date : (experiment, provenance, variable, target, provenance agent)
        indexes.put(Indexes.compoundIndex(experimentAscIndex, dateDescIndex), null);
        indexes.put(Indexes.compoundIndex(provenanceUriAscIndex, dateDescIndex), null);
        indexes.put(Indexes.compoundIndex(targetDescIndex, dateDescIndex), null);
        indexes.put(Indexes.compoundIndex(agentAscIndex, dateDescIndex), null);

        // Multi-fields indexes : Access by experiment and (variable, target, provenance agent). Add date to ensure index usage in case of sorting by date
        indexes.put(Indexes.compoundIndex(agentAscIndex, targetDescIndex, dateDescIndex), null);

        return indexes;
    }

    @Override
    public List<Bson> getBsonFilters(DataFileSearchFilter filter) {

        // #todo : Handle business logic (call to Experiment and provenance DAO) in a dedicated business class
        List<Bson> bsonFilters = super.getBsonFilters(filter);

        addExperimentFilter(bsonFilters, filter);

        if (!CollectionUtils.isEmpty(filter.getTargets())) {
            bsonFilters.add(Filters.in(DataModel.TARGET_FIELD, filter.getTargets()));
        }

        if (!CollectionUtils.isEmpty(filter.getProvenances())) {
            bsonFilters.add(Filters.in(PROVENANCE_URI_FIELD, filter.getProvenances()));
        }

        if (filter.getName() != null && !filter.getName().isEmpty()) {
            bsonFilters.add(Filters.regex("filename", filter.getName(), "i"));
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

        if (filter.getBatchUri() != null) {
            bsonFilters.add(Filters.eq(DataModel.BATCH_URI_FIELD, SPARQLDeserializers.getExpandedURI(filter.getBatchUri())));
        }

        addProvenanceAgentFilter(bsonFilters, filter);

        return bsonFilters;
    }

    protected void addExperimentFilter(List<Bson> bsonFilters, DataFileSearchFilter filter) {

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
            userExperiments = new ExperimentDAO(sparqlService, mongoDBService).getUserExperiments(user);
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
        } else{
            //Handle case where the user has 0 experiments, add only the NO_EXPERIMENT_FILTER,
            // otherwise he/she can see ALL experiments
            bsonFilters.add(NO_EXPERIMENT_FILTER);
        }
    }

    protected void addProvenanceAgentFilter(List<Bson> bsonFilters, DataFileSearchFilter filter) {

        // Filter on agent from devices or operator inside data provenance
        List<URI> dataProvenanceAgents = new LinkedList<>();
        dataProvenanceAgents.addAll(Optional.ofNullable(filter.getDevices()).orElse(Collections.emptyList()));
        dataProvenanceAgents.addAll(Optional.ofNullable(filter.getOperators()).orElse(Collections.emptyList()));

        Bson dataProvenanceFilter = Filters.in(PROVENANCE_AGENTS_URI_FIELD, dataProvenanceAgents);

        // Filter on agents from global provenances
        Set<URI> globalProvenanceAgents = dataProvenanceAgents.isEmpty() ?
                Collections.emptySet() :
                new ProvenanceDAO(mongoDBService, sparqlService).getProvenancesURIsByAgents(dataProvenanceAgents);

        Bson globalProvenanceFilter = Filters.in(PROVENANCE_URI_FIELD, globalProvenanceAgents);

        // Try to simplify the query : avoid a complex OR query with empty array
        if (!dataProvenanceAgents.isEmpty()) {
            if (globalProvenanceAgents.isEmpty()) {  // only match data provenance
                bsonFilters.add(dataProvenanceFilter);
            } else {
                bsonFilters.add(Filters.or(dataProvenanceFilter, globalProvenanceFilter));  // match agents from data provenance or from global provenance
            }
        }
    }

}
