package org.opensilex.core.data.dal;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;

import java.util.HashMap;
import java.util.Map;

import static org.opensilex.core.data.dal.DataProvenanceModel.*;

public class DataFileDaoV2 extends MongoReadWriteDao<DataFileModel, DataFileSearchFilter> {

    public static final String COLLECTION_NAME = "file";
    private final MongoDBService mongodb;

    public DataFileDaoV2(MongoDBService mongoDBService) {
        super(mongoDBService.getServiceV2(), DataFileModel.class, COLLECTION_NAME, COLLECTION_NAME);
        this.mongodb = mongoDBService;
    }

    //TODO is there a search, add getBsonFilters if so

    public static Map<Bson, IndexOptions> getIndexes() {

        IndexOptions defaultOptions = new IndexOptions();

        Bson dateDescIndex = Indexes.descending(DataModel.DATE_FIELD);
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
        indexes.put(Indexes.compoundIndex(targetDescIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(agentAscIndex, dateDescIndex), defaultOptions);

        // Multi-fields indexes : Access by experiment and (variable, target, provenance agent). Add date to ensure index usage in case of sorting by date
        indexes.put(Indexes.compoundIndex(experimentAscIndex, agentAscIndex, targetDescIndex, dateDescIndex), defaultOptions);
        indexes.put(Indexes.compoundIndex(experimentAscIndex, targetDescIndex, agentAscIndex, dateDescIndex), defaultOptions);

        return indexes;
    }
}
