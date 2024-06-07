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
import org.opensilex.nosql.EmbedMongod;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ObjectUriResponse;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author vmigot
 */
public class AbstractMongoIntegrationTest extends AbstractSecurityIntegrationTest {
    /**
     * Type reference for reading results from "create" queries
     */
    protected static final TypeReference<ObjectUriResponse> objectUriResponseTypeReference = new TypeReference<ObjectUriResponse>() {};

    @BeforeClass
    public static void initMongo() {
        EmbedMongod.getInstance().start();
    }

    @AfterClass
    public static void stopMongo() {
        EmbedMongod.getInstance().stop();
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
