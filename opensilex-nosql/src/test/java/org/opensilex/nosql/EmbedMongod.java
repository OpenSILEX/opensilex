package org.opensilex.nosql;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class EmbedMongod {

    protected final static Logger LOGGER = LoggerFactory.getLogger(EmbedMongod.class);

    private final Mongod mongod;
    private TransitionWalker.ReachedState<RunningMongodProcess> runningMongod;
    public static final int MONGO_PORT = 28018;
    public static final String MONGO_DATABASE = "admin";
    public static final String MONGO_HOST = "localhost";
    public static final String REPLICA_NAME = "rs0";

    private static EmbedMongod INSTANCE;
    public static EmbedMongod getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EmbedMongod();
        }
        return INSTANCE;
    }

    private EmbedMongod() {
        mongod = Mongod.builder()
                .net(Start.to(Net.class).initializedWith(Net.defaults()
                        .withPort(MONGO_PORT)))
                .mongodArguments(Start.to(MongodArguments.class).initializedWith(MongodArguments.defaults()
                        .withArgs(Map.of("--replSet", REPLICA_NAME))
                        .withUseNoJournal(false)))
                .build();
    }

    public void start() {
        runningMongod = mongod.start(Version.V6_0_2);

        await().atMost(5, TimeUnit.SECONDS);

        try (MongoClient mongo = MongoClients.create("mongodb://" + MONGO_HOST + ":" + MONGO_PORT)) {
            MongoDatabase adminDatabase = mongo.getDatabase(MONGO_DATABASE);

            Document config = new Document("_id", REPLICA_NAME);
            BasicDBList members = new BasicDBList();
            members.add(new Document("_id", 0)
                    .append("host", MONGO_HOST + ":" + MONGO_PORT));
            config.put("members", members);

            adminDatabase.runCommand(new Document("replSetInitiate", config));

            var replicaStatus = mongo.getDatabase("admin").runCommand(new BasicDBObject("replSetGetStatus", "1"));
            while (replicaStatus.get("lastStableRecoveryTimestamp", BsonTimestamp.class).getTime() == 0) {
                replicaStatus = mongo.getDatabase("admin").runCommand(new BasicDBObject("replSetGetStatus", "1"));
                TimeUnit.MILLISECONDS.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (runningMongod != null) {
            try {
                runningMongod.close();
            } catch (IllegalStateException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

}
