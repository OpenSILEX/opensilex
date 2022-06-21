package org.opensilex.nosql.insert;

import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * @author rcolin
 */
public interface MongoInserter {

    <T extends MongoModel> void create(MongoInsertOptions<T> options) throws Exception;

    MongoDBConfig getConfig();

}
