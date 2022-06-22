package org.opensilex.nosql.insert;

import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Interface which allow to define how to insert any {@link MongoModel}.
 * @author rcolin
 */
public interface MongoInserter {

    /**
     *
     * @param insertOptions insert options (collection, models, session settings)
     * @param <T> the type of {@link MongoModel}
     *
     * @apiNote if {@link MongoInsertOptions#useTransaction()} is true, then models from {@link MongoInsertOptions#getModels()}
     * must be inserted without transaction.
     */
    <T extends MongoModel> void create(MongoInsertOptions<T> insertOptions) throws Exception;

    /**
     *
     * @return the config used by this inserter
     */
    MongoDBConfig getConfig();

    default void shutdown(){

    }

}
