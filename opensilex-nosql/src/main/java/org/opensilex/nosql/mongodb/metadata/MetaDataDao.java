package org.opensilex.nosql.mongodb.metadata;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;

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

    public MetaDataModel getByUri(URI uri, String collectionName) throws NoSQLInvalidURIException {
        return mongodb.findByURI(MetaDataModel.class, collectionName, uri);
    }

    public void create(MongoCollection<MetaDataModel> metaDataCollection,
                       ClientSession session,
                       MetaDataModel newModel){

        Objects.requireNonNull(newModel.getUri());
        metaDataCollection.insertOne(session,newModel);
    }

    public void update(MongoCollection<MetaDataModel> metaDataCollection,
                       ClientSession session,
                       MetaDataModel newModel,
                       MetaDataModel oldModel,
                       String idField
                       ) {

        if (newModel != null && ! MapUtils.isEmpty(newModel.getAttributes())) {

            // replace existing old model by new model
            if (oldModel != null) {
                metaDataCollection.findOneAndReplace(session, eq(idField, oldModel.getUri()), newModel);
            } else {
                // create new model
                metaDataCollection.insertOne(session, newModel);
            }
        } else {
            // delete old model
            metaDataCollection.findOneAndDelete(session, eq(idField, oldModel.getUri()));
        }
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
        Map<String, MetaDataModel> index = new PatriciaTrie<>();
        for (MetaDataModel metaDataModel : attributesIt) {
            index.put(metaDataModel.getUri().toString(), metaDataModel);
        }

        for (T model : models) {
            MetaDataModel metadata = index.get(URIDeserializer.formatURIAsStr(model.getUri().toString()));

            if (metadata != null) {
                // use lambda in order to let setMetadata() caller determine how to use returned metadata
                consumer.accept(model, metadata);
            }
        }
    }

     public <T extends SPARQLResourceModel> void getMetaDataAssociatedTo(MongoCollection<MetaDataModel> collection,
                                                                          String idField,
                                                                          T model,
                                                                          BiConsumer<T,MetaDataModel> consumer) throws NoSQLInvalidURIException {

        MetaDataModel metaDataModel = mongodb.findByURI(collection,model.getUri(),idField);
        if(metaDataModel != null){
            consumer.accept(model,metaDataModel);
        }

    }


}
