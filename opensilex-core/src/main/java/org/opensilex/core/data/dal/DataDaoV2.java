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


    @Override
    protected void addDefaultSort(Document sort) {
        sort.put(DataModel.DATE_FIELD, -1);
    }
}
