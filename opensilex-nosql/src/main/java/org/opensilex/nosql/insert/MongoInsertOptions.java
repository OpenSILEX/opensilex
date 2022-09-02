package org.opensilex.nosql.insert;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.nosql.mongodb.MongoModel;

import java.util.List;
import java.util.Objects;

/**
 * Utility class used to group all setting relative to the insertion of any {@link MongoModel}
 *
 * @param <T> the type of {@link MongoModel}
 * @author rcolin
 */
public class MongoInsertOptions<T extends MongoModel> {

    private final MongoCollection<T> collection;
    private final String collectionName;
    private ClientSession session;
    private final List<T> models;

    private boolean checkUriExist;
    private String uriGenerationPrefix;

    /**
     * @param collection the {@link MongoCollection} in which models must be inserted
     * @param session the {@link ClientSession} used to perform insertion.
     * @param models the list of models to insert.
     *
     * @throws IllegalArgumentException if
     * <lu>
     *     <li>client and session are null </li>
     *     <li>collection is null</li>
     *     <li>models is null or empty</li>
     * </lu>
     *
     * @see MongoClient#startSession()
     */
    public MongoInsertOptions(MongoCollection<T> collection, ClientSession session, List<T> models) {

        Objects.requireNonNull(collection);

        if (CollectionUtils.isEmpty(models)) {
            throw new IllegalArgumentException("Null or empty list : " + models);
        }

        this.collection = collection;
        this.collectionName = collection.getNamespace().getCollectionName();
        this.models = models;
        this.checkUriExist = true;
        this.uriGenerationPrefix = "";

        this.session = session;
    }

    /**
     *
     * @return the {@link ClientSession} to use for insert
     */
    public ClientSession getSession() {
        return session;
    }

    /**
     *
     * @return the {@link MongoCollection} in which models must be inserted
     */
    public MongoCollection<T> getCollection() {
        return collection;
    }

    /**
     *
     * @return the list of {@link MongoModel} to insert.
     */
    public List<T> getModels() {
        return models;
    }

    /**
     * @return if URI uniqueness of models must be checked or not
     */
    public boolean isCheckUriExist() {
        return checkUriExist;
    }

    public MongoInsertOptions<T> setCheckUriExist(boolean checkUriExist) {
        this.checkUriExist = checkUriExist;
        return this;
    }

    /**
     * @return the prefix used for URI generation of {@link MongoModel} from {@link #getModels()}
     */
    public String getUriGenerationPrefix() {
        return uriGenerationPrefix;
    }

    public MongoInsertOptions<T> setUriGenerationPrefix(String uriGenerationPrefix) {
        Objects.requireNonNull(uriGenerationPrefix);
        this.uriGenerationPrefix = uriGenerationPrefix;
        return this;
    }

    public MongoInsertOptions<T> setSession(ClientSession session) {
        this.session = session;
        return this;
    }

    /**
     * @return the name of the collection on which insert {{@link #getModels()}}
     */
    public String getCollectionName() {
        return collectionName;
    }
}
