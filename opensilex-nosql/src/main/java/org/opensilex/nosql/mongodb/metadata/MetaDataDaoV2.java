package org.opensilex.nosql.mongodb.metadata;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.opensilex.nosql.mongodb.MongoModel.MONGO_ID_FIELD;

/**
 * @author mhart
 * Dao used to handle {@link MetaDataModel}
 */
public class MetaDataDaoV2 extends MongoReadWriteDao<MetaDataModel, MetadataSearchFilter> {

    private final MongoDBService mongodb;

    public MetaDataDaoV2(MongoDBService mongodb, String collectionName) {
        super(mongodb.getServiceV2(), MetaDataModel.class, collectionName, collectionName);
        this.mongodb = mongodb;
    }

    public static Map<Bson, IndexOptions> getIndexes() {
        Map<Bson, IndexOptions> indexes = new HashMap<>();
        indexes.put(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        return indexes;
    }

    @Override
    public List<Bson> getBsonFilters(MetadataSearchFilter searchQuery) {
        List<Bson> result = super.getBsonFilters(searchQuery);

        if(searchQuery.getAttributes() != null){
            for (String key : searchQuery.getAttributes().keySet()) {
                result.add(Filters.eq("attributes." + key, searchQuery.getAttributes().get(key)));
            }
        }
        return result;
    }
}
