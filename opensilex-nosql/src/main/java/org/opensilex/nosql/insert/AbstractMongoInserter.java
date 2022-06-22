package org.opensilex.nosql.insert;

import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public abstract class AbstractMongoInserter implements MongoInserter{

    private final MongoDBConfig config;
    private final Logger LOGGER;

    protected AbstractMongoInserter(MongoDBConfig config){
        this.config = config;
        LOGGER = LoggerFactory.getLogger(getClass());
    }

    @Override
    public MongoDBConfig getConfig() {
        return config;
    }

    @Override
    public <T extends MongoModel> void create(MongoInsertOptions<T> insertOptions) throws Exception {

        LOGGER.info("Mongo insert [IN-PROGRESS] collection: {}, length: {}", insertOptions.getCollectionName(), insertOptions.getModels().size());
        Instant start = Instant.now();

        if (insertOptions.useTransaction()) {
            insertWithTransaction(insertOptions);
        } else {
            insertWithoutTransaction(insertOptions);
        }

        Duration duration = Duration.between(start, Instant.now());
        LOGGER.info("Mongo insert [OK] collection: {}, duration: {} ms, length: {}, insert_rate: {}/s",
                insertOptions.getCollectionName(),
                duration.toMillis(),
                insertOptions.getModels().size(),
                (insertOptions.getModels().size() * 1000L) / duration.toMillis()
        );
    }

    abstract <T extends MongoModel> void insertWithoutTransaction(MongoInsertOptions<T> insertOptions) throws Exception;

    abstract <T extends MongoModel> void insertWithTransaction(MongoInsertOptions<T> insertOptions) throws Exception;

}
