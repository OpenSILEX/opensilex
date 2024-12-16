package org.opensilex.nosql;

import de.flapdoodle.embed.mongo.client.ClientActions;
import de.flapdoodle.embed.mongo.client.SyncClientAdapter;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class EmbedMongoClient {

    protected final static Logger LOGGER = LoggerFactory.getLogger(EmbedMongoClient.class);

    private TransitionWalker.ReachedState<RunningMongodProcess> runningMongoDb;

    public static final int MONGO_PORT = 28018;
    public static final String MONGO_HOST = "localhost";
    public static final String REPLICA_SET_NAME = "rs0";
    public static final Version MONGOD_VERSION = Version.V6_0_2;
    public static final Boolean SILENT_OUTPUT = true;

    private static EmbedMongoClient INSTANCE;

    public static EmbedMongoClient getInstance() throws IOException, InterruptedException {
        if (INSTANCE == null) {
            INSTANCE = new EmbedMongoClient();
        }
        return INSTANCE;
    }

    public void start() {
        var storage = Storage.of(REPLICA_SET_NAME, 5000);
        var mongod = Mongod.instance()
                .withNet(Start.to(Net.class)
                        .initializedWith(Net.defaults().withPort(MONGO_PORT)))
                .withMongodArguments(Start.to(MongodArguments.class)
                        .initializedWith(MongodArguments.defaults()
                                .withReplication(storage)
                                .withUseNoJournal(false)
                        ));

        if (SILENT_OUTPUT) {
            mongod = mongod.withProcessOutput(Start.to(ProcessOutput.class)
                    .initializedWith(ProcessOutput.silent()));
        }

        runningMongoDb = mongod.start(MONGOD_VERSION, ClientActions.initReplicaSet(new SyncClientAdapter(), MONGOD_VERSION, storage));

        await().atMost(5, TimeUnit.SECONDS);
    }

    public void stop() {
        if (runningMongoDb != null) {
            runningMongoDb.close();
            runningMongoDb = null;
        }
    }

}
