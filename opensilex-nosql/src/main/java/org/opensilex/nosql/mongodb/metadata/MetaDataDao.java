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
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

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
 * <p>
 * Moreover, for any write operations (update, create, delete), then the corresponding function can deal with lambda
 * in order to handle all the logic related to MetaData (check validity, nullity and non-emptiness, handle transaction) and by
 * letting the caller of these function, define which specific operation must be done.
 * <p>
 * E.g: DAO or API which call this DAO, can pass lambda function to define how to create a model which have metadata.
 * Since this operation is not the responsibility of this class, these it's defined by this class caller.
 * </p>
 *
 * @author rcolin
 */
public class MetaDataDao {

    private final MongoDBService mongodb;
    private final SPARQLService sparql;

    public MetaDataDao(MongoDBService mongodb, SPARQLService sparql) {
        this.mongodb = mongodb;
        this.sparql = sparql;
    }

    /**
     * @param metaDataCollection the collection on which insert a new {@link MetaDataModel} if not null or empty
     * @param newMetadata        the metadata to insert
     * @param model              {@link SPARQLResourceModel} on which metadata are associated.
     * @throws IllegalArgumentException if :
     *                                  <ul>
     *                                      <li>metaDataCollection is null</li>
     *                                      <li>sparqlCreateFunction is null</li>
     *                                  </ul>
     */
    public <T extends SPARQLResourceModel> ThrowingConsumer<ClientSession,Exception> getCreateConsumer(MongoCollection<MetaDataModel> metaDataCollection,
                                                                           MetaDataModel newMetadata,
                                                                           T model){

        if (newMetadata == null || MapUtils.isEmpty(newMetadata.getAttributes())) {
            return null;
        }
        Objects.requireNonNull(metaDataCollection);
        return (ClientSession session) -> {
            if (newMetadata.getUri() == null) {

                // ensure that at this point, model URI is set or generated
                Objects.requireNonNull(model.getUri());
                newMetadata.setUri(model.getUri());
            }
            metaDataCollection.insertOne(session, newMetadata);
        };
    }

    /**
     * @param metaDataCollection   the collection on which update a new {@link MetaDataModel} if not null or empty
     * @param newMetadata          the metadata to insert
     * @param model {@link SPARQLResourceModel} on which metadata are associated.
     * @throws IllegalArgumentException if :
     *                                  <ul>
     *                                      <li>metaDataCollection is null</li>
     *                                      <li>sparqlCreateFunction is null</li>
     *                                      <li>model URI is null</li>
     *                                  </ul>
     */
    public <T extends SPARQLResourceModel> ThrowingConsumer<ClientSession,Exception> getUpdateConsumer(MongoCollection<MetaDataModel> metaDataCollection,
                                                                                                       MetaDataModel newMetadata,
                                                                                                       T model){

        Objects.requireNonNull(metaDataCollection);
        Objects.requireNonNull(model.getUri());

        MetaDataModel oldMetadata = metaDataCollection.find(eq(MongoModel.URI_FIELD, model.getUri())).first();

        // no metadata (old and new)
        if ((newMetadata == null || MapUtils.isEmpty(newMetadata.getAttributes())) && oldMetadata == null) {
            return null;
        }
        return (ClientSession session) -> update(
                metaDataCollection,
                session,
                newMetadata,
                oldMetadata,
                model.getUri()
        );
    }

    /**
     * @param metaDataCollection   the collection on which update a new {@link MetaDataModel} if not null or empty
     * @param uri URI of the {@link MetaDataModel} to remove from metaDataCollection
     * @throws IllegalArgumentException if :
     *                                  <ul>
     *                                      <li>metaDataCollection is null</li>
     *                                      <li>sparqlCreateFunction is null</li>
     *                                      <li>model URI is null</li>
     *                                  </ul>
     */
    public <T extends SPARQLResourceModel> ThrowingConsumer<ClientSession,Exception> getDeleteConsumer(MongoCollection<MetaDataModel> metaDataCollection,
                                                                                                       URI uri){

        MetaDataModel metadata = get(metaDataCollection, uri, MongoModel.URI_FIELD);
        if (metadata == null || MapUtils.isEmpty(metadata.getAttributes())) {
           return null;
        }
        return (ClientSession session) -> metaDataCollection.findOneAndDelete(session, eq(MongoModel.URI_FIELD, uri));
    }

    /**
     * If the new model is not empty, then replace the old model with the new model. If the new model is empty, then
     * delete the old model
     *
     * @param metaDataCollection The MongoDB collection to update
     * @param session            the session object
     * @param newModel           The new model that will be saved in the mongo database.
     * @param oldModel           the old model that was in the mongo database
     * @param uri                URI of the new/old {@link MetaDataModel}
     */
    private void update(MongoCollection<MetaDataModel> metaDataCollection,
                        ClientSession session,
                        MetaDataModel newModel,
                        MetaDataModel oldModel,
                        URI uri
    ) {

        if (newModel != null && !MapUtils.isEmpty(newModel.getAttributes())) {

            Objects.requireNonNull(uri);
            newModel.setUri(uri);

            // replace existing old model by new model
            if (oldModel != null) {
                metaDataCollection.findOneAndReplace(session, eq(MongoModel.URI_FIELD, oldModel.getUri()), newModel);
            } else {
                // create new model
                metaDataCollection.insertOne(session, newModel);
            }
        } else {
            // delete old model
            metaDataCollection.findOneAndDelete(session, eq(MongoModel.URI_FIELD, oldModel.getUri()));
        }
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

    public MetaDataModel get(MongoCollection<MetaDataModel> collection,URI uri, String idField){
        return collection.find(eq(idField, uri)).first();
    }

}
