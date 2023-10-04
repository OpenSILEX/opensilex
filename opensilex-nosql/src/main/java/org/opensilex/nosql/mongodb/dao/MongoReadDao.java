package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 *
 * @param <T>
 * @param <F> The kind of filter which is specific to this dao
 */
public interface MongoReadDao<T extends MongoModel, F extends MongoSearchFilter> {

    T get(URI uri) throws NoSQLInvalidURIException;

    T get(ClientSession session, URI uri) throws NoSQLInvalidURIException;

    boolean exists(URI uri) throws MongoException;

    boolean exists(ClientSession session, URI uri) throws MongoException;

    long count(F filter) throws MongoException;

    long count(ClientSession session, F filter) throws MongoException;

    ListWithPagination<T> search(F filter) throws MongoException;

    ListWithPagination<T> search(ClientSession session, F filter, Bson projection) throws MongoException;

    <T_RESULT> ListWithPagination<T_RESULT> search(F filter, Function<T, T_RESULT> convertFunction) throws MongoException;

    <T_RESULT> ListWithPagination<T_RESULT> search(ClientSession session, F filter, Bson projection, Function<T, T_RESULT> convertFunction) throws MongoException;

    Set<URI> distinctUris(ClientSession session, F filter) throws MongoException;

    <T_RESULT> Set<T_RESULT> distinct(String field, Class<T_RESULT> resultClass, F filter, ClientSession session);

    <T_RESULT> Set<T_RESULT> distinctAggregation(String field, Class<T_RESULT> resultClass, F filter, ClientSession session);

    <T_RESULT, T_JOINED> List<T_RESULT> lookupAggregation(
            F filter,
            String lookupCollectionName,
            String lookupField,
            Class<T_JOINED> lookupClass,
            Function<T_JOINED, T_RESULT> convertFunction,
            ClientSession session
    );
}
