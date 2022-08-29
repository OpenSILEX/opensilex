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

/**
 * <p>
 * Dao used to handle the management of {@link MetaDataModel} for any other model.
 * This class implements creation, update, deletion and retrieval (get/search) of {@link MetaDataModel}
 * associated to any {@link SPARQLResourceModel}
 * </p>
 *
 * <p>
 * Since metadata handling could be add to any other model, then this dao always need to known on which {@link MongoCollection} work.
 * So, unlike in other Dao, {@link MetaDataModel} are not stored into a single collection.
 * </p>
 *
 *
 * @author rcolin
 */
public class MetaDataDao {

    private final MongoDBService mongodb;

    public MetaDataDao(MongoDBService mongodb) {
        this.mongodb = mongodb;
    }


    /**
     * Create metadata into the given collection
     * @param metaDataCollection metadata collection name
     * @param newMetadata new model
     * @param uri uri of the new model
     * @param session Mongo session for transaction handling
     */
    public void create(
            MongoCollection<MetaDataModel> metaDataCollection,
            MetaDataModel newMetadata,
            URI uri,
            ClientSession session
    ) {

        Objects.requireNonNull(uri);

        // ensure that at this point, model URI is set or generated
        newMetadata.setUri(uri);
        metaDataCollection.insertOne(session, newMetadata);
    }

    /**
     *
     * @param metaDataModel the model to check
     * @return true if metadata are not null and have attributes, false else
     * @see MetaDataModel#getAttributes()
     */
    public static boolean hasMetaData(MetaDataModel metaDataModel){
        return metaDataModel != null && ! MapUtils.isEmpty(metaDataModel.getAttributes());
    }

    /**
     * If the new model is not empty, then replace the old model with the new model. If the new model is empty, then
     * delete the old model
     * @param metaDataCollection   the collection on which update a new {@link MetaDataModel} if not null or empty
     * @param newMetadata          the metadata to insert/to update
     * @param uri uri of the new model
     * @param session Mongo session for transaction handling
     *
     * @throws IllegalArgumentException if :
     *                                  <ul>
     *                                      <li>metaDataCollection is null</li>
=     *                                      <li>model URI is null</li>
     *                                  </ul>
     */
    public void update(MongoCollection<MetaDataModel> metaDataCollection,
                       MetaDataModel newMetadata,
                       URI uri,
                       ClientSession session) {

        Objects.requireNonNull(metaDataCollection);
        Objects.requireNonNull(uri);

        MetaDataModel oldMetadata = metaDataCollection.find(eq(MongoModel.URI_FIELD,uri)).first();

        if (newMetadata != null && !MapUtils.isEmpty(newMetadata.getAttributes())) {

            // replace existing old model by new model
            if (oldMetadata != null) {
                metaDataCollection.findOneAndReplace(session, eq(MongoModel.URI_FIELD, uri), newMetadata);
            } else {
                // create new model
                metaDataCollection.insertOne(session, newMetadata);
            }
        } else {
            // delete old model
            metaDataCollection.findOneAndDelete(session, eq(MongoModel.URI_FIELD, uri));
        }

    }


    /**
     * delete a metadata model from the given collection
     * @param metaDataCollection metadata collection name
     * @param uri uri of the metadata model to delete
     * @param session Mongo session for transaction handling
     */
    public void delete(MongoCollection<MetaDataModel> metaDataCollection,
                                                       URI uri,
                                                       ClientSession session) {
        metaDataCollection.findOneAndDelete(session, eq(MongoModel.URI_FIELD, uri));
    }

    /**
     * @param collection collection which contains {@link MetaDataModel}
     * @param idField    name of the field on which filter models
     * @param models     list of models. This method use models URIS for filtering and then update models
     * @param consumer   Consumer which allow the function caller to determine how to use model and MetaDataModel
     * @param <T>        Type of {@link SPARQLResourceModel} for which we want to set {@link MetaDataModel}
     */
    public <T extends SPARQLResourceModel> void getMetaDataAssociatedTo(MongoCollection<MetaDataModel> collection,
                                                                        String idField,
                                                                        Collection<T> models,
                                                                        BiConsumer<T, MetaDataModel> consumer) {

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


    /**
     * This function retrieves the metadata associated to a given resource and applies a consumer function to the
     * resource and the metadata
     *
     * @param collection the MongoDB collection to query
     * @param idField    The field in the collection that is used to identify the document.
     * @param model      The model that you want to get the metadata for.
     * @param consumer   a function that takes a model and a metadata model and does something with them.
     */
    public <T extends SPARQLResourceModel> void getMetaDataAssociatedTo(MongoCollection<MetaDataModel> collection,
                                                                        String idField,
                                                                        T model,
                                                                        BiConsumer<T, MetaDataModel> consumer) throws NoSQLInvalidURIException {

        MetaDataModel metaDataModel = mongodb.findByURI(collection, model.getUri(), idField);
        if (metaDataModel != null) {
            consumer.accept(model, metaDataModel);
        }
    }

    /**
     *
     * @param collection the Mongo collection in which metadata are searched
     * @param uri URI of the {@link MetaDataModel}
     * @param idField name of the id field of the {@link MetaDataModel}
     * @return the {@link MetaDataModel} from the given collection if found, null else
     */
    public MetaDataModel get(MongoCollection<MetaDataModel> collection,URI uri, String idField){
        return collection.find(eq(idField, uri)).first();
    }

}
