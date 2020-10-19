/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core;

import com.mongodb.BasicDBList;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;

/**
 *
 * @author vmigot
 */
public class AbstractMongoIntegrationTest extends AbstractSecurityIntegrationTest {

    private static MongodExecutable mongoExec;
    private static MongodProcess mongod;

    @BeforeClass
    public static void initMongo() throws IOException {
        MongodStarter runtime = MongodStarter.getDefaultInstance();
        int nodePort = 28018;
        mongoExec = runtime.prepare(new MongodConfigBuilder().version(Version.Main.V4_0)
                .withLaunchArgument("--replSet", "rs0")
                .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
                .net(new Net("127.0.0.1", nodePort, false)).build());
        mongod = mongoExec.start();

        MongoClient mongo = new MongoClient(new ServerAddress("127.0.0.1", nodePort));

        MongoDatabase adminDatabase = mongo.getDatabase("admin");

        Document config = new Document("_id", "rs0");
        BasicDBList members = new BasicDBList();
        members.add(new Document("_id", 0)
                .append("host", "localhost:" + nodePort));
        config.put("members", members);

        adminDatabase.runCommand(new Document("replSetInitiate", config));

        mongo.close();
    }

    @AfterClass
    public static void stopMongo() {
        if (mongod != null) {
            try {
                mongod.stop();
            } catch (IllegalStateException e) {
                if (mongod != null) {
                    await().atMost(1, TimeUnit.MINUTES).until(() -> !mongod.isProcessRunning());
                } else {
                    throw new IllegalStateException(e);
                }
            }
        }
        if (mongoExec != null) {
            mongoExec.stop();
        }
    }

}
