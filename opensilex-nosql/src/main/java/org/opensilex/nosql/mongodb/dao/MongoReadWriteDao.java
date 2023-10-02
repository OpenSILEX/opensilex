package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;

public abstract class MongoReadWriteDao<T extends MongoModel, F extends MongoSearchFilter> implements MongoWriteDao<T, F>, MongoReadDao<T, F> {

    protected final MongoDBService mongodb;
    protected final MongoCollection<T> collection;
    protected final String createPrefix;

    protected MongoReadWriteDao(MongoDBService mongodb, Class<T> modelClass, String collectionName, String createPrefix) {
        Objects.requireNonNull(mongodb);
        this.mongodb = mongodb;
        this.collection = mongodb.getDatabase().getCollection(collectionName, modelClass);
        this.createPrefix = createPrefix;
    }

    protected void createIndexes() {
        // no index by default
    }

    @Override
    public T get(URI uri) throws NoSQLInvalidURIException {
        return mongodb.findByURI(collection, uri, MongoModel.URI_FIELD);
    }

    @Override
    public T get(ClientSession session, URI uri) throws NoSQLInvalidURIException {
        return mongodb.findByURI(session, collection, uri, MongoModel.URI_FIELD);
    }

    @Override
    public boolean exists(URI uri) throws MongoException{
        return exists(null, uri);
    }

    @Override
    public boolean exists(ClientSession session, URI uri) throws MongoException {
        return mongodb.uriExists(session, collection, uri);
    }

    @Override
    public InsertOneResult create(T instance, ClientSession session) throws MongoException, URISyntaxException {
        return mongodb.create(instance, collection, createPrefix, session);
    }

    @Override
    public InsertManyResult create(List<T> instances) throws MongoException, URISyntaxException {
        return create(instances, null);
    }

    @Override
    public InsertManyResult create(List<T> instances, ClientSession session) throws MongoException, URISyntaxException {
        return mongodb.createAll(instances, collection, session, createPrefix, true, true);
    }

    public Bson getUpdateFilter(T instance) {
        // by default the filter for update is the id filter with the instance uri
        return getIdFilter(instance);
    }

    @Override
    public void update(T instance, ClientSession session) throws MongoException, NoSQLInvalidURIException {
        mongodb.update(instance, collection, getUpdateFilter(instance), session);
    }

    @Override
    public DeleteResult delete(URI uri, ClientSession session) throws MongoException {
        return mongodb.delete(collection, session, getIdFilter(uri));
    }

    @Override
    public DeleteResult delete(List<URI> uris, ClientSession session) throws MongoException {
        return mongodb.deleteOnCriteria(collection, session, Filters.in(idField(), uris));
    }

    public Bson deleteFilterToDocument(F deleteFilter) throws MongoException {
        Bson filter = filterToBson(deleteFilter);
        if (filter == null) {
            throw new IllegalArgumentException("[" + collection.getNamespace().getCollectionName() + "] Empty delete filter : provide at least a filter");
        }
        return filter;
    }

    @Override
    public DeleteResult delete(F deleteFilter, ClientSession session) throws MongoException {
        return mongodb.deleteOnCriteria(collection, session, deleteFilterToDocument(deleteFilter));
    }

    @Override
    public long count(F filter) throws MongoException {
        return count(null, filter);
    }

    @Override
    public long count(ClientSession session, F filter) throws MongoException {
        return mongodb.count(session, collection, filterToBson(filter));
    }

    public ListWithPagination<T> search(F filter) throws MongoException {
        return search(null, filter, null);
    }

    @Override
    public ListWithPagination<T> search(ClientSession session, F filter, Bson projection) throws MongoException {
        return mongodb.searchWithPagination(
                collection,
                filterToBson(filter),
                projection,
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize(),
                session
        );
    }

    @Override
    public <T_CONVERTED> ListWithPagination<T_CONVERTED> search(ClientSession session, F filter, Bson projection, Function<T, T_CONVERTED> convertFunction) throws MongoException {

        Objects.requireNonNull(convertFunction);

        Map.Entry<FindIterable<T>, Long> resultAndCount = mongodb.findWithPagination(
                collection,
                filterToBson(filter),
                null,
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize(),
                session
        );

        if (resultAndCount != null) {
            int resultCount = resultAndCount.getValue().intValue();

            // iterate over MongoDB result and convert result on the fly before collect them inside a List
            List<T_CONVERTED> convertedResults = new ArrayList<>(resultCount);
            resultAndCount.getKey().forEach(mongoResult ->
                convertedResults.add(convertFunction.apply(mongoResult)
            ));

            return new ListWithPagination<>(convertedResults, filter.getPage(), filter.getPageSize(), resultCount);
        }
        return new ListWithPagination<>(Collections.emptyList());
    }

    public <T_CONVERTED> ListWithPagination<T_CONVERTED> search(F filter, Function<T, T_CONVERTED> convertFunction) throws MongoException {
        return search(null, filter, null, convertFunction);
    }

    @Override
    public  Set<URI> distinctUris(ClientSession session, F filter) throws MongoException {
        return mongodb.distinctWithPagination(
                collection,
                MongoModel.URI_FIELD,
                document -> document.get(MongoModel.URI_FIELD, URI.class),
                filterToBson(filter),
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize(),
                session
        );
    }

    protected Bson getIdFilter(URI id) {
        return Filters.eq(idField(), id);
    }

    protected Bson getIdFilter(T model) {
        return Filters.eq(idField(), model.getUri());
    }


    public List<Bson> getBsonFilters(F searchFilter) {
        List<Bson> filters = new ArrayList<>();
        if (searchFilter.getUri() != null) {
            filters.add(getIdFilter(searchFilter.getUri()));
        }
        return filters;
    }

    protected Bson filterToBson(F searchFilter) {

        Objects.requireNonNull(searchFilter);
        List<Bson> bsonFilters = getBsonFilters(searchFilter);

        if(bsonFilters.isEmpty()){
            return null;
        }else if(bsonFilters.size() == 1){
            return bsonFilters.get(0);
        }

        // append each filter with a logical AND or logical OR
        if (searchFilter.isLogicalAnd()) {
            return Filters.and(bsonFilters);
        } else {
            return Filters.or(bsonFilters);
        }

    }
}
