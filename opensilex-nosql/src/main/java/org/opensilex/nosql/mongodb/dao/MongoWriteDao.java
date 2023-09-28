package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public interface MongoWriteDao<T extends MongoModel,  F extends MongoSearchFilter> {

    default InsertOneResult create(T instance) throws MongoException, URISyntaxException {
        return create(instance, null);
    }

    InsertOneResult create(T instance, ClientSession session) throws MongoException, URISyntaxException;

    default InsertManyResult create(List<T> instances) throws MongoException, URISyntaxException {
        return create(instances, null);

    }

    InsertManyResult create(List<T> instances, ClientSession session) throws MongoException, URISyntaxException;

    default String idField(){
        return MongoModel.URI_FIELD;
    }

    default void update(T instance) throws MongoException{
        update(instance,null);
    }
    void update(T instance, ClientSession session) throws MongoException;

    default DeleteResult delete(URI uri) throws MongoException {
        return delete(uri,null);
    }

    DeleteResult delete(URI uri, ClientSession session) throws MongoException;

    DeleteResult delete(List<URI> uris,  ClientSession session) throws MongoException;

    default DeleteResult delete(F deleteFilter) throws MongoException {
        return delete(deleteFilter,null);
    }

    DeleteResult delete(F deleteFilter, ClientSession session) throws MongoException;
}
