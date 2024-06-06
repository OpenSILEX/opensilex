package org.opensilex.nosql;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class EmbedMongoClient {

    protected final static Logger LOGGER = LoggerFactory.getLogger(EmbedMongoClient.class);

    private final TransitionWalker.ReachedState<RunningMongodProcess> runningMongod;
//    private final MongodExecutable mongoExec;
//    private final MongodProcess mongod;
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
        var mongod = Mongod.builder()
                .net(Start.to(Net.class).initializedWith(Net.defaults().withPort(MONGO_PORT)))
                .mongodArguments(Start.to(MongodArguments.class).initializedWith(MongodArguments.defaults().withArgs(Map.of("--replSet", "rs0")).withUseNoJournal(false)))
                .build();
        runningMongod = mongod.start(Version.V6_0_2);

        await().atMost(5, TimeUnit.SECONDS);
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
