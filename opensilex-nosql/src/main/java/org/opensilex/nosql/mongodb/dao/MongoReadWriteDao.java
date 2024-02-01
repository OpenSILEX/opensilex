package org.opensilex.nosql.mongodb.dao;

import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

public class MongoReadWriteDao<T extends MongoModel,F extends MongoSearchFilter> extends AbstractMongoReadWriteDao<T,F> {

    public MongoReadWriteDao(MongoDBServiceV2 mongodb, Class<T> modelClass, String collectionName, String createPrefix, boolean checkUriGeneration, boolean checkUriExistence) {
        super(mongodb, modelClass, collectionName, createPrefix, checkUriGeneration, checkUriExistence);
    }

    public MongoReadWriteDao(MongoDBServiceV2 mongodb, Class<T> modelClass, String collectionName, String createPrefix
    ) {
        this(mongodb, modelClass, collectionName, createPrefix, true, true);
    }

}
