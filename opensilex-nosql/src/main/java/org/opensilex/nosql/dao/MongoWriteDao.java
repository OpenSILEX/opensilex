package org.opensilex.nosql.dao;

import com.mongodb.client.ClientSession;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.opensilex.nosql.dao.read.MongoSearchFilter;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.util.List;

public interface MongoWriteDao<T extends MongoModel,  F extends MongoSearchFilter> {

    InsertOneResult create(T instance) throws Exception;

    InsertOneResult create(T instance, ClientSession session) throws Exception;

    InsertManyResult create(List<T> instances) throws Exception;

    InsertManyResult create(List<T> instances, ClientSession session) throws Exception;

    default void update(T instance) throws Exception {
        update(instance,null);
    }

    default String idField(){
        return MongoModel.URI_FIELD;
    }

    void update(T instance, ClientSession session) throws Exception;

    DeleteResult delete(URI uri, ClientSession session) throws Exception;

    DeleteResult delete(List<URI> uris,  ClientSession session) throws Exception;

    default DeleteResult delete(F deleteFilter) throws Exception {
        return delete(deleteFilter,null);
    }

    DeleteResult delete(F deleteFilter, ClientSession session) throws Exception;
}
