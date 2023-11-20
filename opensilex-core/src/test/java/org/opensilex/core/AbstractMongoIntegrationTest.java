/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.BasicDBList;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;

import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.process.ProcessOutput;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ObjectUriResponse;

/**
 *
 * @author vmigot
 */
public class AbstractMongoIntegrationTest extends AbstractSecurityIntegrationTest {

    private static MongodExecutable mongoExec;
    private static MongodProcess mongod;
    private static int replicaCount = 0;

    public static final int MONGO_PORT = 28018;
    public static final String MONGO_DATABASE = "admin";
    public static final String MONGO_HOST = "localhost";

    /**
     * Type reference for reading results from "create" queries
     */
    protected static final TypeReference<ObjectUriResponse> objectUriResponseTypeReference = new TypeReference<ObjectUriResponse>() {};

    @BeforeClass
    public static void initMongo() throws IOException {

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
                    .append("host", MONGO_HOST+":" + nodePort));
            config.put("members", members);

            adminDatabase.runCommand(new Document("replSetInitiate", config));

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ex) {
            }
        }
    }

    @AfterClass
    public static void stopMongo() {
        if (mongod != null) {
            try {
                mongod.stopInternal();
            } catch (IllegalStateException e) {

            } finally {
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
        }

        if (mongoExec != null) {
            mongoExec.stop();
        }
    }

    @Override
    public void afterEach() throws Exception {
        this.clearCollections();
    }

    /**
     *
     * @return
     */
    protected List<String> getCollectionsToClearNames() {
        return Collections.emptyList();
    }

    private void clearCollections(){

        MongoDBService mongoDBService = getOpensilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        MongoDatabase mongoDb = mongoDBService.getDatabase();

        try{
            for(String collectionName : getCollectionsToClearNames()){
                MongoCollection<?> collection = mongoDb.getCollection(collectionName);
                collection.drop();
            }
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }

    }

    protected static MongoDBService getMongoDBService(){
        return getOpensilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
    }


}
