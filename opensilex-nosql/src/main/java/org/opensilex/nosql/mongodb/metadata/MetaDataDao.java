package org.opensilex.nosql.mongodb.metadata;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author rcolin
 * Dao used to handle {@link MetaDataModel}
 * #TODO use this Dao for each CRUD method
 */
public class MetaDataDao {

    private final MongoDBService mongodb;

    public MetaDataDao(MongoDBService mongodb) {
        this.mongodb = mongodb;
    }

    /**
     *
     * @param collection collection which contains {@link MetaDataModel}
     * @param idField name of the field on which filter models
     * @param models list of models. This method use models URIS for filtering and then update models
     * @param consumer Consumer which allow the function caller to determine how to use model and MetaDataModel
     * @param <T> Type of {@link SPARQLResourceModel} for which we want to set {@link MetaDataModel}
     */
    public <T extends SPARQLResourceModel> void getMetaDataAssociatedTo(MongoCollection<MetaDataModel> collection,
                                                                        String idField,
                                                                        Collection<T> models,
                                                                        BiConsumer<T,MetaDataModel> consumer) {

        FindIterable<MetaDataModel> attributesIt = mongodb.findIterableByURIs(
                idField,
                collection,
                models.stream().map(SPARQLResourceModel::getUri)
        );

        // use a index in order to map incoming mongodb results with input URIs
        // O(n) time/space complexity in local
        Map<String,MetaDataModel> index = new PatriciaTrie<>();
        for(MetaDataModel metaDataModel : attributesIt){
            index.put(metaDataModel.getUri().toString(), metaDataModel);
        }

        for(T model : models){
            MetaDataModel metadata = index.get(URIDeserializer.formatURIAsStr(model.getUri().toString()));

            if (metadata != null) {
                // use lambda in order to let setMetadata() caller determine how to use returned metadata
                consumer.accept(model,metadata);
            }
        }

    }

}
