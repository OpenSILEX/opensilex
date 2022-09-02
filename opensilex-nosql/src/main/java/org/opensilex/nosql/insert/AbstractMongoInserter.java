package org.opensilex.nosql.insert;

import com.mongodb.client.MongoClient;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstract {@link MongoInserter} which implements {@link #create(MongoInsertOptions)} by
 * using transaction {@link #insertWithTransaction(MongoInsertOptions)} or not {@link #insertWithoutTransaction(MongoInsertOptions)}
 *
 * @author rcolin
 */
public abstract class AbstractMongoInserter implements MongoInserter{

    private final MongoDBConfig config;
    private final MongoClient mongoClient;
    private final Logger logger;

    protected AbstractMongoInserter(MongoClient mongoClient, MongoDBConfig config){
        this.config = config;
        this.mongoClient = mongoClient;
        logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public MongoDBConfig getConfig() {
        return config;
    }

    @Override
    public <T extends MongoModel> void create(MongoInsertOptions<T> insertOptions) throws Exception {

        logger.info("Mongo insert [IN-PROGRESS] collection: {}, length: {}", insertOptions.getCollectionName(), insertOptions.getModels().size());
        Instant start = Instant.now();

        // no session given, then consider the use of transaction with a new generated session
        if (insertOptions.getSession() == null) {
            insertOptions.setSession(mongoClient.startSession());
            insertWithTransaction(insertOptions);
        } else {
            // a session was provided, so this class will not handle exception
            // we consider that transaction are handled out of here
            insertWithoutTransaction(insertOptions);
        }

        Duration duration = Duration.between(start, Instant.now());
        logger.info("Mongo insert [OK] collection: {}, duration: {} ms, length: {}, insert_rate: {}/s",
                insertOptions.getCollectionName(),
                duration.toMillis(),
                insertOptions.getModels().size(),
                (insertOptions.getModels().size() * 1000L) / duration.toMillis()
        );
    }

    /**
     * Define how to insert models without handling transaction
     * @param insertOptions insert options
     * @param <T> the type of {@link MongoModel}
     * @throws Exception if some Exception is encountered during insertion.
     * In this case no transaction rollback and session closing are performed inside this method
     */
    protected abstract <T extends MongoModel> void insertWithoutTransaction(MongoInsertOptions<T> insertOptions) throws Exception;

    /**
     * Define how to insert models by handling transaction
     * @param insertOptions insert options
     * @param <T> the type of {@link MongoModel}
     * @throws Exception if some Exception is encountered during insertion.
     * In this case transaction rollback and session closing are performed inside this method
     */
    protected abstract <T extends MongoModel> void insertWithTransaction(MongoInsertOptions<T> insertOptions) throws Exception;

}
