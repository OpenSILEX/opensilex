package org.opensilex.nosql;

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
    private final ClientSession session;
    private final List<T> models;

    private final boolean commitTransaction;
    private boolean checkUriExist;
    private String uriGenerationPrefix;

    /**
     * @param client {@link MongoClient} used to generate new {@link ClientSession}, in case where session is null
     * @param collection the {@link MongoCollection} in which models must be inserted
     * @param session the {@link ClientSession} used to perform insertion.
     * @param models the list of models to insert.
     *
     * @throws IllegalArgumentException if
     * <lu>
     *     <li>client is null</li>
     *     <li>collection is null</li>
     *     <li>models is null or empty</li>
     * </lu>
     *
     * @see MongoClient#startSession()
     */
    public MongoInsertOptions(MongoClient client, MongoCollection<T> collection, ClientSession session, List<T> models) {

        Objects.requireNonNull(client);
        Objects.requireNonNull(collection);
        if (CollectionUtils.isEmpty(models)) {
            throw new IllegalArgumentException("Null or empty list : " + models);
        }

        this.collection = collection;

        // no session given, then consider the use of transaction with a new session
        if (session == null) {
            this.session = client.startSession();
            this.commitTransaction = true;
        } else {
            this.session = session;
            this.commitTransaction = false;
        }
        this.models = models;
        this.checkUriExist = true;
        this.uriGenerationPrefix = "";
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
     * @return the the list of {@link MongoModel} to insert.
     */
    public List<T> getModels() {
        return models;
    }

    public boolean isCheckUriExist() {
        return checkUriExist;
    }

    public MongoInsertOptions<T> setCheckUriExist(boolean checkUriExist) {
        this.checkUriExist = checkUriExist;
        return this;
    }

    /**
     *
     * @return the prefix used for URI generation of {@link MongoModel} from {@link #getModels()}
     */
    public String getUriGenerationPrefix() {
        return uriGenerationPrefix;
    }

    public MongoInsertOptions<T> setUriGenerationPrefix(String uriGenerationPrefix) {
        this.uriGenerationPrefix = uriGenerationPrefix;
        return this;
    }

    /**
     *
     * @return true if transaction must be handled, false else
     */
    public boolean commitTransaction() {
        return commitTransaction;
    }

}
