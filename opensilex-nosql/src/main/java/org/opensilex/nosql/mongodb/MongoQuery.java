package org.opensilex.nosql.mongodb;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;

import java.util.Objects;

public class MongoQuery<T extends MongoModel> {

    final MongoDBService mongodb;

    public MongoQuery(MongoDBService mongodb) {
        this.mongodb = mongodb;
    }

    private ClientSession session;
    private MongoCollection<T> collection;


    public ClientSession getSession() {
        return session;
    }

    public MongoQuery<T> setSession(ClientSession session) {
        this.session = session;
        return this;
    }

    public MongoCollection<T> getCollection() {
        return collection;
    }

    public MongoQuery<T> setCollection(MongoCollection<T> collection) {
        this.collection = collection;
        return this;
    }
}
