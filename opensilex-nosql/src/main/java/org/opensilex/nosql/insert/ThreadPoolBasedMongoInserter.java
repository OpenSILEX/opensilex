package org.opensilex.nosql.insert;

import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadPoolBasedMongoInserter extends AbstractMongoInserter{

    private final ExecutorService executorService;
    protected static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolBasedMongoInserter.class);
    private final DefaultMongoInserter delegatedInserter;

    public ThreadPoolBasedMongoInserter(MongoDBConfig config) {
        super(config);
        executorService = Executors.newFixedThreadPool(config.maxConcurrentTransaction());
        delegatedInserter = new DefaultMongoInserter(config);
    }

    @Override
    <T extends MongoModel> void insertWithoutTransaction(MongoInsertOptions<T> insertOptions) throws Exception {
        insertWithPool(() -> delegatedInserter.insertWithoutTransaction(insertOptions));
    }

    @Override
    <T extends MongoModel> void insertWithTransaction(MongoInsertOptions<T> insertOptions) throws Exception {
        executorService.submit(() -> delegatedInserter.insertWithTransaction(insertOptions));
    }

    void insertWithPool(Runnable task) throws ExecutionException, InterruptedException {
            Future<?> future = executorService.submit(task);
            future.get();
    }
}
