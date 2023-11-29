package org.opensilex.nosql.mongodb.dao;

import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.service.v2.MongoDBService;

public class MongoReadWriteDao<T extends MongoModel,F extends MongoSearchFilter> extends AbstractMongoReadWriteDao<T,F> {

    public MongoReadWriteDao(MongoDBService mongodb, Class<T> modelClass, String collectionName, String createPrefix) {
        super(mongodb, modelClass, collectionName, createPrefix);
    }

}
