package org.opensilex.nosql.insert;

import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadPoolBasedMongoInserter extends AbstractMongoInserter{

    private final ExecutorService executorService;
    protected static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolBasedMongoInserter.class);
    private final MongoInserter delegatedInserter;

    public ThreadPoolBasedMongoInserter(MongoDBConfig config) {
        super(config);
        executorService = Executors.newFixedThreadPool(config.maxConcurrentTransaction());
        delegatedInserter = new DefaultMongoInserter(config);
    }

    @Override
    public <T extends MongoModel> void create(MongoInsertOptions<T> options) throws Exception {
        
        AtomicReference<Exception> e = new AtomicReference<>();
        
        Future<?> future = executorService.submit(() -> {
            try {
                delegatedInserter.create(options);
            } catch (Exception e1) {
                e.set(e1);
            }
        });

        future.get();

        if(e.get() != null){
            throw e.get();
        }
    }

}
