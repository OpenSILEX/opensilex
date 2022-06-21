package org.opensilex.nosql.mongodb;

import org.opensilex.nosql.MongoInsertOptions;

/**
 * @author rcolin
 */
public interface MongoInserter {

    <T extends MongoModel> void create(MongoInsertOptions<T> options) throws Exception;

}
