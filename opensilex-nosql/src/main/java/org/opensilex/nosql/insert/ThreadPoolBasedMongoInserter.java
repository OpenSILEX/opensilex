package org.opensilex.nosql.insert;

import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation which use an {@link ExecutorService} in order to manage concurrent insertion with a limit on the number of maximum concurrent insertion.
 * This limit is defined according {@link MongoDBConfig#maxConcurrentTransaction()} value
 * @author rcolin
 *
 * @apiNote This implementation delegate insertion and transaction logic to the {@link DefaultMongoInserter}.
 * It's just encapsulate these operations by using {@link ExecutorService}
 */
public class ThreadPoolBasedMongoInserter extends AbstractMongoInserter {

    private final ExecutorService executorService;
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
        insertWithPool(() -> delegatedInserter.insertWithTransaction(insertOptions));
    }

    void insertWithPool(Runnable task) throws ExecutionException, InterruptedException {
        Future<?> future = executorService.submit(task);

        // wait for completion
        future.get();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        executorService.shutdownNow();
    }
}
