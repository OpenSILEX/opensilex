package org.opensilex.benchmark.mongodb.write;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.opensilex.nosql.mongodb.MongoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author rcolin
 * Represent a {@link Callable} task for inserting MongoDB documents
 */
public abstract class InsertTask<T extends MongoModel> implements Callable<Integer> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(InsertTask.class);

    private final int taskId;
    protected final Supplier<List<T>> modelsGenerator;
    protected final MongoClient client;
    protected final MongoCollection<T> collection;

    protected InsertTask(int taskId,Supplier<List<T>> modelsGenerator, MongoClient client, MongoCollection<T> collection) {
        this.taskId = taskId;
        this.modelsGenerator = modelsGenerator;
        this.client = client;
        this.collection = collection;
    }

    abstract void insertModels(List<T> models) throws Exception;

    @Override
    public Integer call() throws Exception {

        Instant begin = Instant.now();

        List<T> models = modelsGenerator.get();
        insertModels(models);

        models.clear();
        return 0;
    }
}