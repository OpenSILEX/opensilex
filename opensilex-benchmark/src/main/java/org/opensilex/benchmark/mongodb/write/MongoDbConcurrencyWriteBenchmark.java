package org.opensilex.benchmark.mongodb.write;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.opensilex.benchmark.core.AbstractOpenSilexBenchmark;
import org.opensilex.benchmark.core.OpenSilexBenchmarkRunner;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * @author rcolin
 * This test is intented to test mongodb performances (success+write rate) when
 * performing concurrent write on the same collection
 */
@State(Scope.Benchmark)
public class MongoDbConcurrencyWriteBenchmark extends AbstractOpenSilexBenchmark {

    @Param({"1000"})
    public int transactionSize;

    @Param({"1"})
    public int concurrentWriteNb;

    @Param({"true"})
    public boolean usePrefixes;

    @Param({"10"})
    public int taskNb;

    private Random random;

    @Param({"10000"})
    public int nbTarget;

    private List<URI> targets;

    @Param({"10000"})
    public int nbVariable;

    @Param({"true"})
    public boolean submitTaskTogether;

    private List<URI> variables;

    @Param({"1000"})
    public int nbProvenance;

    @Param({"true"})
    public boolean withAllIndexes;

    private List<DataProvenanceModel> provenances;

    private static final String BASE_URI = "https://opensilex/benchmark/mongodb/";
    private static final String PREFIX = "benchmark";

    private MongoClient mongoClient;
    private MongoCollection<DataModel> collection;

    @Override
    @Setup(Level.Trial)
    public void setup() throws Exception {
        super.setup();
        random = new Random();

        targets = new ArrayList<>();
        for (int i = 0; i < nbTarget; i++) {
            targets.add(createURI("target/" + i));
        }

        variables = new ArrayList<>();
        for (int i = 0; i < nbVariable; i++) {
            variables.add(createURI("variable/" + i));
        }

        provenances = new ArrayList<>();
        for (int i = 0; i < nbProvenance; i++) {
            DataProvenanceModel provenance = new DataProvenanceModel();
            provenance.setUri(createURI("provenance/" + i));
            provenances.add(provenance);
        }

        mongoClient = mongodb.getMongoClient();
        collection = mongodb.getDatabase().getCollection(DataDAO.DATA_COLLECTION_NAME, DataModel.class);
        buildIndexes();
    }

    void buildIndexes(){
        if(! withAllIndexes){
            return;
        }
        collection.createIndex(Indexes.ascending("variable", "provenance", "target", "date"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("variable", "target", "date"));
        collection.createIndex(Indexes.compoundIndex(Arrays.asList(Indexes.ascending("variable"),Indexes.descending("date"))));
        collection.createIndex(Indexes.ascending("date"));
    }

    private URI createURI(String suffix) {
        return URI.create(PREFIX+":"+suffix);
    }

    private List<DataModel> getModels() {
        Instant batchInstant = Instant.now().plusNanos(random.nextLong());
        List<DataModel> models = new ArrayList<>(transactionSize);

        for (int i = 0; i < transactionSize; i++) {
            DataModel model = new DataModel();
            model.setUri(createURI("data/" + UUID.randomUUID()));
            model.setDate(batchInstant.plusNanos(i));
            model.setValue(random.nextInt());
            model.setConfidence(random.nextFloat());

            model.setTarget(targets.get(random.nextInt(targets.size())));
            model.setVariable(variables.get(random.nextInt(variables.size())));
            model.setProvenance(provenances.get(random.nextInt(provenances.size())));

            models.add(model);
        }

        return models;
    }

    private void testConcurrentWrite(IntFunction<InsertTask<?>> taskFunction) throws ExecutionException, InterruptedException {

        // create tasks
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentWriteNb);
        List<Callable<Integer>> tasks = new ArrayList<>(taskNb);
        for (int i = 1; i <= taskNb; i++) {
            Callable<Integer> task = taskFunction.apply(i);
            tasks.add(task);
        }

        // run tasks
        try {
            Instant begin = Instant.now();
            List<Future<Integer>> futures;
            if (submitTaskTogether) {
                futures = executorService.invokeAll(tasks);
            } else {
                futures = tasks.stream()
                        .map(executorService::submit)
                        .collect(Collectors.toList());
            }

            for (Future<Integer> future : futures) {
                future.get();
            }

            Duration totalDuration = Duration.between(begin, Instant.now());
            LOGGER.info("{} models inserted, time: {} ms", taskNb * transactionSize, totalDuration.toMillis());
            executorService.shutdownNow();

        } catch (Exception e) {
            e.printStackTrace();
            executorService.shutdownNow();
            throw e;
        }

    }

    @Benchmark
    public void testConcurrentWriteWithDao() throws ExecutionException, InterruptedException {

        IntFunction<InsertTask<?>> taskFunction = (int taskIndex) -> new InsertTask<DataModel>(taskIndex, this::getModels, mongoClient, collection) {
            @Override
            void insertModels(List<DataModel> models) throws Exception {
                DataDAO dao = new DataDAO(mongodb, sparql, fs);
                dao.createAll(models);
            }
        };

        testConcurrentWrite(taskFunction);
    }

    @Benchmark
    public void testConcurrentWriteWithSessionHandling() throws ExecutionException, InterruptedException {

        IntFunction<InsertTask<?>> taskFunction = (int taskIndex) -> new InsertTask<DataModel>(taskIndex, this::getModels, mongoClient, collection) {
            @Override
            void insertModels(List<DataModel> models) throws Exception {

                ClientSession session = mongoClient.startSession();
                session.startTransaction();

                try {
                    collection.insertMany(session, models);
                    session.commitTransaction();
                    session.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    session.abortTransaction();
                    session.close();
                    throw e;
                }
            }
        };

        testConcurrentWrite(taskFunction);
    }


    public static void main(String[] args) throws RunnerException {

        Options options = new OptionsBuilder()
                .param(CONFIG_PATH_JMH_PARAM, "/home/renaud/workspace/OpenSilex/opensilex-dev/opensilex-dev-tools/src/main/resources/config/opensilex.yml")
                .jvmArgs("-Xmx2G","-Xms1G")
                .build();

        OpenSilexBenchmarkRunner runner = new OpenSilexBenchmarkRunner(MongoDbConcurrencyWriteBenchmark.class, false, options);
        runner.run();
    }

}
