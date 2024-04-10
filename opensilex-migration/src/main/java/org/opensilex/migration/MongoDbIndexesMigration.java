package org.opensilex.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.SetUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.update.AbstractOpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;

import java.util.*;

/**
 * @author rcolin
 */
public class MongoDbIndexesMigration extends AbstractOpenSilexModuleUpdate {

    @Override
    public String getDescription() {
        return "Update MongoDB indexes";
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {

        MongoDBServiceV2 mongoDBServiceV2 = opensilex.getServiceInstance(MongoDBServiceV2.DEFAULT_SERVICE, MongoDBServiceV2.class);
        MongoDatabase database = mongoDBServiceV2.getDatabase();

        // for each collection, ensure that indexes correspond to those that the OpenSILEX application register
        for(var indexesByCollection : mongoDBServiceV2.getIndexRegister().entrySet()){

            // Get Set of required indexes, Set of existing index and Compute Set of indexes to create/delete by using set difference
            MongoCollection<?> collection = database.getCollection(indexesByCollection.getKey());
            Map<Bson, IndexOptions> indexesOptions = indexesByCollection.getValue();

            Set<Bson> existingIndexes = new HashSet<>();
            collection.listIndexes().forEach(bson -> {
                Document existingIndex = bson.get("key", Document.class);
                existingIndexes.add(getIndexSpecificationWithoutOption(existingIndex));
            });

            // Delete old indexes and create new indexes
            Set<Bson> indexesToDelete = SetUtils.difference(existingIndexes, indexesOptions.keySet());
            for(Bson index : indexesToDelete){
                mongoDBServiceV2.dropIndex(collection, index);
            }

            Set<Bson> indexesToCreate = SetUtils.difference(indexesOptions.keySet(), existingIndexes);
            for(Bson index : indexesToCreate){
                mongoDBServiceV2.createIndex(collection, index, indexesOptions.get(index));
            }
        }
    }

    /**
     * 
     * @param existingIndex Document returned from a {@link MongoCollection#listIndexes()}
     * @return a Bson document matching the index format when passing to {@link MongoCollection#createIndex(Bson, IndexOptions)} or {@link MongoCollection#dropIndex(Bson)}
     */

    private Bson getIndexSpecificationWithoutOption(Document existingIndex){

        List<Bson> indexes = new LinkedList<>();
        existingIndex.keySet().forEach(indexField -> {
            int order = existingIndex.getInteger(indexField);
            if (order == 1) {
                indexes.add(Indexes.ascending(indexField));
            } else {
                indexes.add(Indexes.descending(indexField));
            }
        });

        if (indexes.size() == 1) {
            return indexes.get(0);
        } else {
            return Indexes.compoundIndex(indexes);
        }
    }
}
