package org.opensilex.core.data.dal;

import com.mongodb.client.model.Filters;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
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
import java.util.stream.Collectors;

public class DataDaoV2 extends MongoReadWriteDao<DataModel, DataSearchFilter> {

    protected static final String COLLECTION_NAME = "data";

    protected final SPARQLService sparql;
    protected final MongoDBService mongodb;

    // Provenance empty filters and fields
    protected static final String PROVENANCE_URI_FIELD = DataModel.PROVENANCE_FIELD + "." + MongoModel.URI_FIELD;
    protected static final String PROVENANCE_EXPERIMENT_FIELD = DataModel.PROVENANCE_FIELD + "." + DataProvenanceModel.EXPERIMENT_FIELD;
    protected static final Bson NO_EXPERIMENT_FILTER = Filters.eq(PROVENANCE_EXPERIMENT_FIELD, null);


    protected static final String PROVENANCE_PROV_USED_FIELD = DataModel.PROVENANCE_FIELD + "." + DataProvenanceModel.PROV_USED_FIELD;

    protected static final String PROVENANCE_PROV_WAS_ASSOCIATED_WITH_FIELD = DataModel.PROVENANCE_FIELD + "." + DataProvenanceModel.PROV_WAS_ASSOCIATED_WITH_FIELD;
    protected static final Bson NO_PROV_WAS_ASSOCIATED_WITH_FILTER = Filters.eq(PROVENANCE_PROV_WAS_ASSOCIATED_WITH_FIELD, null);



    public DataDaoV2(SPARQLService sparql, MongoDBService mongodb) {
        super(mongodb.getServiceV2(), DataModel.class, COLLECTION_NAME, "data");
        this.sparql = sparql;
        this.mongodb = mongodb;
    }

    @Override
    public List<Bson> getBsonFilters(DataSearchFilter filter) {

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
            filter.getMetadata().forEach((metadataKey, metadataValue) -> {
                bsonFilters.add(Filters.eq(DataModel.METADATA_FIELD + "." + metadataKey, metadataValue));
            });
        }

        addProvenanceAgentFilter(bsonFilters, filter);

        return bsonFilters;
    }

    protected void addExperimentFilter(List<Bson> bsonFilters, DataSearchFilter filter) {

        AccountModel user = filter.getUser();

        if (user.isAdmin()) {
            if (!CollectionUtils.isEmpty(filter.getExperiments())) {
                bsonFilters.add(Filters.in(PROVENANCE_EXPERIMENT_FIELD, filter.getExperiments()));
            }
            return;
        }

        Set<URI> userExperiments;
        try{
            userExperiments =  new ExperimentDAO(sparql, mongodb).getUserExperiments(user);
        }catch (Exception e){
            throw new RuntimeException("Unexpected error during data filter building ", e);
        }


        if (CollectionUtils.isEmpty(filter.getExperiments())) {

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

        List<URI> agents = new LinkedList<>();
        agents.addAll(Optional.ofNullable(filter.getDevices()).orElse(Collections.emptyList()));
        agents.addAll(Optional.ofNullable(filter.getOperators()).orElse(Collections.emptyList()));

        Set<URI> agentProvenances = agents.isEmpty() ?
                Collections.emptySet() :
                new ProvenanceDAO(mongodb, sparql).getProvenancesURIsByAgents(agents);

        Bson dataProvenanceFilter = Filters.in(PROVENANCE_PROV_WAS_ASSOCIATED_WITH_FIELD, agents);
        Bson globalProvenanceFilter = Filters.in(PROVENANCE_URI_FIELD, agentProvenances);
        Bson globalProvenanceOrEmptyFilter = Filters.or(globalProvenanceFilter, NO_PROV_WAS_ASSOCIATED_WITH_FILTER);

        if (agents.isEmpty()) {
            if (!agentProvenances.isEmpty()) {
                bsonFilters.add(globalProvenanceOrEmptyFilter); // only match global provenance
            }
        } else if (!agentProvenances.isEmpty()) {
            bsonFilters.add(Filters.or(dataProvenanceFilter, globalProvenanceOrEmptyFilter));  // match agents from data provenance or from global provenance
        } else {
            bsonFilters.add(dataProvenanceFilter);  // only match data provenance
        }
    }
}
