package org.opensilex.nosql;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.process.ProcessOutput;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class EmbedMongoClient {

    protected final static Logger LOGGER = LoggerFactory.getLogger(EmbedMongoClient.class);

    private final MongodExecutable mongoExec;
    private final MongodProcess mongod;
    public static final int MONGO_PORT = 28018;
    public static final String MONGO_DATABASE = "admin";
    public static final String MONGO_HOST = "localhost";

    private static EmbedMongoClient INSTANCE;
    public static EmbedMongoClient getInstance() throws IOException, InterruptedException {
        if (INSTANCE == null) {
            INSTANCE = new EmbedMongoClient();
        }
        return INSTANCE;
    }

    private EmbedMongoClient() throws IOException {

        RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD)
                .processOutput(ProcessOutput.silent())
                .build();

        MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
        int nodePort = MONGO_PORT;
        Map<String, String> args = new HashMap<>();
        String replicaName = "rs0";
        args.put("--replSet", replicaName);
        mongoExec = runtime.prepare(MongodConfig.builder().version(Version.V6_0_2)
                .args(args)
                .cmdOptions(MongoCmdOptions.builder().useNoJournal(false).build())
                .net(new Net("127.0.0.1", nodePort, false)).build());
        mongod = mongoExec.start();

        try (MongoClient mongo = MongoClients.create("mongodb://127.0.0.1:" + nodePort)) {
            MongoDatabase adminDatabase = mongo.getDatabase(MONGO_DATABASE);

            Document config = new Document("_id", replicaName);
            BasicDBList members = new BasicDBList();
            members.add(new Document("_id", 0)
                    .append("host", MONGO_HOST + ":" + nodePort));
            config.put("members", members);

            adminDatabase.runCommand(new Document("replSetInitiate", config));
        }

        await().atMost(5, TimeUnit.SECONDS);
    }

    public void stop() {
        if (mongod != null) {
            try {
                mongod.stopInternal();
            } catch (IllegalStateException e) {
                LOGGER.error(e.getMessage());
            } finally {
                try {
                    mongod.stop();
                } catch (IllegalStateException e) {
                    await().atMost(1, TimeUnit.MINUTES).until(() -> !mongod.isProcessRunning());
                }
            }
        }

        if (mongoExec != null) {
            mongoExec.stop();
        }
    }

}
