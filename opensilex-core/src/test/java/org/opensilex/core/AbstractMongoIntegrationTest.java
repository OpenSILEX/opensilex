/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.nosql.EmbedMongoClient;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ObjectUriResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author vmigot
 * Abstract class used in OpenSILEX for Secure API testing with Mongodb support.
 * This is used for testing services with direct Mongodb setup and teardown (specific Before and After tests methods, etc...)
 */
public abstract class AbstractMongoIntegrationTest extends AbstractSecurityIntegrationTest {

    private static EmbedMongoClient embedMongoClient;
    private static final int replicaCount = 0;

    public static final int MONGO_PORT = 28018;
    public static final String MONGO_DATABASE = "admin";
    public static final String MONGO_HOST = "localhost";

    /**
     * Type reference for reading results from "create" queries
     */
    protected static final TypeReference<ObjectUriResponse> objectUriResponseTypeReference = new TypeReference<ObjectUriResponse>() {
    };

    @BeforeClass
    public static void initMongo() throws IOException, InterruptedException {
        if (embedMongoClient == null) {
            embedMongoClient = EmbedMongoClient.getInstance();
        }
        embedMongoClient.start();
    }

    @AfterClass
    public static void stopMongo() {
        embedMongoClient.stop();
    }

    @Override
    public void afterEach() throws Exception {
        this.clearCollections();
    }

    /**
     * @return
     */
    protected List<String> getCollectionsToClearNames() {
        return Collections.emptyList();
    }

    private void clearCollections() {

        MongoDBService mongoDBService = getOpensilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        MongoDatabase mongoDb = mongoDBService.getDatabase();

        try {
            for (String collectionName : getCollectionsToClearNames()) {
                MongoCollection<?> collection = mongoDb.getCollection(collectionName);
                collection.drop();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    protected static MongoDBService getMongoDBService() {
        return getOpensilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
    }


}
