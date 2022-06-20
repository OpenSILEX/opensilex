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

/**
 * @author rcolin
 * Represent a {@link Callable} task for inserting MongoDB documents
 */
public abstract class InsertTask<T extends MongoModel> implements Callable<Integer> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(InsertTask.class);

    private final int taskId;
    protected final List<T> models;
    protected final MongoClient client;
    protected final MongoCollection<T> collection;

    protected InsertTask(int taskId, List<T> models, MongoClient client, MongoCollection<T> collection) {
        this.taskId = taskId;
        this.models = models;
        this.client = client;
        this.collection = collection;
    }

    abstract void insertModels() throws Exception;

    @Override
    public Integer call() throws Exception {

        LOGGER.info("Running mongo insert task {}, length: {}, [IN-PROGRESS]", taskId, models.size());
        Instant begin = Instant.now();

        insertModels();

        Duration duration = Duration.between(begin, Instant.now());
        LOGGER.info("Mongo insert task {} [OK] time: {} ms, length: {}", taskId, duration.toMillis(), models.size());

        return models.size();
    }
}