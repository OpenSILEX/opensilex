package org.opensilex.nosql.dao.read;

import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.function.Function;

/**
 *
 * @param <T>
 * @param <F> The kind of filter which is specific to this dao
 */
public interface MongoReadDao<T extends MongoModel, F extends MongoSearchFilter> {

    T get(URI uri) throws NoSQLInvalidURIException;

    T get(F filter) throws Exception;

    long count(F filter) throws Exception;

    ListWithPagination<T> search(F filter) throws Exception;

    ListWithPagination<T> search(F filter, Bson projection) throws Exception;

    <T_CONVERTED> ListWithPagination<T_CONVERTED> search(F filter, Bson projection, Function<T,T_CONVERTED> convertFunction) throws Exception;
}
