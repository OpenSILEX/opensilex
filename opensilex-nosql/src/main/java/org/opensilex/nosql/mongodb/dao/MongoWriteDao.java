package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public interface MongoWriteDao<T extends MongoModel,  F extends MongoSearchFilter> {

    InsertOneResult create(T instance) throws MongoException, URISyntaxException;

    InsertOneResult create(T instance, ClientSession session) throws MongoException, URISyntaxException;

    InsertManyResult create(List<T> instances) throws MongoException, URISyntaxException;

    InsertManyResult create(List<T> instances, ClientSession session) throws MongoException, URISyntaxException;

    String idField();

    void update(T instance) throws MongoException;

    void update(T instance, ClientSession session) throws MongoException;

    DeleteResult delete(URI uri) throws MongoException;

    DeleteResult delete(URI uri, ClientSession session) throws MongoException;

    DeleteResult delete(List<URI> uris,  ClientSession session) throws MongoException;

    DeleteResult delete(F deleteFilter) throws MongoException;

    DeleteResult delete(F deleteFilter, ClientSession session) throws MongoException;
}
