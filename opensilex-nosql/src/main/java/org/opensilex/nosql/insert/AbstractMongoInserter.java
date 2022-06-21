package org.opensilex.nosql.insert;

import org.opensilex.nosql.mongodb.MongoDBConfig;

public abstract class AbstractMongoInserter implements MongoInserter{

    private final MongoDBConfig config;

    protected AbstractMongoInserter(MongoDBConfig config){
        this.config = config;
    }

    @Override
    public MongoDBConfig getConfig() {
        return config;
    }

}
