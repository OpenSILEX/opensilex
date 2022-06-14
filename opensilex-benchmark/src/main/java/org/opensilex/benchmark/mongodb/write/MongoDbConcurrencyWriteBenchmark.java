package org.opensilex.benchmark.mongodb.write;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.apache.jena.shared.PrefixMapping;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.opensilex.benchmark.core.AbstractOpenSilexBenchmark;
import org.opensilex.benchmark.core.OpenSilexBenchmarkRunner;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * @author rcolin
 * This test is intented to test mongodb performances (success+write rate) when
 * performing concurrent write on the same collection
 */
@State(Scope.Benchmark)
public class MongoDbConcurrencyWriteBenchmark extends AbstractOpenSilexBenchmark {

    //    @Param({"1000","2000","5000","10000"})
    @Param({"1000"})
    public int length;
    public static final String LENGTH_JMH_PARAM = "length";

    //    @Param({"1","2","4","8"})
    @Param({"1"})
    public int concurrentWriteNb;
    public static final String CONCURRENT_WRITE_NB_JMH_PARAM = "concurrentWriteNb";

    @Param({"true"})
    public boolean usePrefixes;
    public static final String USE_PREFIXES_JMH_PARAM = "usePrefixes";

    private DataDAO dataDAO;

    private Random random;

    @Param({"1000"})
    public int nbTarget;
    public static final String NB_TARGET_JMH_PARAM = "nbTarget";

    private List<URI> targets;

    @Param({"100"})
    public int nbVariable;
    public static final String NB_VARIABLE_JMH_PARAM = "nbVariable";

    private List<URI> variables;

    @Param({"100"})
    public int nbProvenance;
    public static final String NB_PROVENANCE_JMH_PARAM = "nbProvenance";

    private List<DataProvenanceModel> provenances;

    private static final String BASE_URI = "https://opensilex/benchmark/mongodb/";
    private static final String PREFIX = "benchmark";

    private MongoClient mongoClient;
    private MongoCollection<DataModel> collection;

    @Override
    @Setup(Level.Trial)
    public void setup() throws Exception {
        super.setup();
        dataDAO = new DataDAO(mongodb, sparql, fs);
        random = new Random();

        PrefixMapping prefixMapping = SPARQLService.getPrefixMapping();
        prefixMapping.setNsPrefix(PREFIX, BASE_URI);
        URIDeserializer.setPrefixes(prefixMapping, usePrefixes);

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
        collection = mongodb.getDatabase().getCollection(DataDAO.DATA_COLLECTION_NAME,DataModel.class);
    }

    private URI createURI(String suffix) {
        return usePrefixes ?
                URI.create(URIDeserializer.getShortURI(BASE_URI + suffix)) :
                URI.create(URIDeserializer.getExpandedURI(BASE_URI + suffix));
    }

    private List<DataModel> getModels() {
        Instant batchInstant = Instant.now();
        List<DataModel> models = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
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

    private void testConcurrentWrite(Function<Integer, Callable<Integer>> taskFunction) throws InterruptedException, ExecutionException {

        // create tasks
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentWriteNb);
        List<Callable<Integer>> tasks = new ArrayList<>(concurrentWriteNb);
        for (int i = 0; i < concurrentWriteNb; i++) {
            Callable<Integer> task = taskFunction.apply(i);
            tasks.add(task);
        }

        // run tasks
        try {
            List<Future<Integer>> futures = executorService.invokeAll(tasks);
            for (Future<Integer> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            LOGGER.info("Error on task : {}", e.getMessage());
        } finally {
            executorService.shutdownNow();
        }

    }

//    @Benchmark
    public void testConcurrentWriteWithDao() throws InterruptedException, ExecutionException {
        Function<Integer, Callable<Integer>> taskFunction = (taskIndex) ->
                () -> {
                    LOGGER.info("Running mongo insert task {} [IN-PROGRESS]", taskIndex);
                    Instant begin = Instant.now();

                    List<DataModel> models = getModels();
                    DataDAO dao = new DataDAO(mongodb, sparql, fs);
                    dao.createAll(models);

                    Duration duration = Duration.between(begin, Instant.now());
                    LOGGER.info("Mongo insert task {} [OK] time: {} ms", taskIndex, duration.toMillis());

                    return models.size();
                };

        testConcurrentWrite(taskFunction);
    }

    @Benchmark
    public void testConcurrentWriteWithSessionHandling() throws InterruptedException, ExecutionException {
        Function<Integer, Callable<Integer>> taskFunction = (taskIndex) ->
                () -> {
                    LOGGER.info("Running mongo insert task {} [IN-PROGRESS]", taskIndex);
                    Instant begin = Instant.now();

                    ClientSession session = mongoClient.startSession();
                    List<DataModel> models = getModels();
                    session.startTransaction();

                    try{
                        collection.insertMany(session,models);
                        session.commitTransaction();
                    }catch (Exception e){
                        session.abortTransaction();
                    }

                    Duration duration = Duration.between(begin, Instant.now());
                    LOGGER.info("Mongo insert task {} [OK] time: {} ms", taskIndex, duration.toMillis());

                    return models.size();
                };

        testConcurrentWrite(taskFunction);
    }


    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .param(LENGTH_JMH_PARAM, String.valueOf(1000))
                .param(CONCURRENT_WRITE_NB_JMH_PARAM, String.valueOf(4))
                .param(CONFIG_PATH_JMH_PARAM, "/home/renaud/workspace/OpenSilex/opensilex-dev/opensilex-dev-tools/src/main/resources/config/opensilex.yml")
                .build();

        OpenSilexBenchmarkRunner runner = new OpenSilexBenchmarkRunner(MongoDbConcurrencyWriteBenchmark.class, true, options);
        runner.run();
    }

}
