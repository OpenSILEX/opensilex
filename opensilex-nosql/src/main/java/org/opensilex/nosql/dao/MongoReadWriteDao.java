package org.opensilex.nosql.dao;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.dao.read.MongoReadDao;
import org.opensilex.nosql.dao.read.MongoSearchFilter;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MongoReadWriteDao<T extends MongoModel, F extends MongoSearchFilter> implements MongoWriteDao<T,F>, MongoReadDao<T,F> {

    protected final MongoDBService mongodb;
    protected final MongoCollection<T> collection;
    protected final String createPrefix;

    public MongoReadWriteDao(MongoDBService mongodb, Class<T> modelClass, String collectionName, String createPrefix) {
        Objects.requireNonNull(mongodb);
        this.mongodb = mongodb;
        this.collection = mongodb.getDatabase().getCollection(collectionName,modelClass);
        this.createPrefix = createPrefix;
    }

    protected void createIndexes(){
        // no index by default
    }


    @Override
    public T get(URI uri) throws NoSQLInvalidURIException {
        return mongodb.findByURI(collection,uri);
    }

    @Override
    public T get(F filter) throws Exception {
        return collection.find(filterToDocument(filter)).first();
    }

    @Override
    public InsertOneResult create(T instance) throws Exception {
        return create(instance,null);
    }

    @Override
    public InsertOneResult create(T instance, ClientSession session) throws Exception {
        return mongodb.create(instance, collection, createPrefix, session);
    }

    @Override
    public InsertManyResult create(List<T> instances) throws Exception {
        return create(instances,null);
    }

    @Override
    public InsertManyResult create(List<T> instances, ClientSession session) throws Exception {
        return mongodb.createAll(instances, collection, session, createPrefix,true);
    }

    public Bson getUpdateFilter(T instance) throws Exception {
        // by default the filter for update is the id filter with the instance uri
        return getIdFilter(instance.getUri());
    }

    @Override
    public void update(T instance, ClientSession session) throws Exception {

        // if document exist, replace it and return the old document
        // else return null
        T created = collection.findOneAndReplace(
                session,
                getUpdateFilter(instance),
                instance,
                new FindOneAndReplaceOptions().projection( // don't fetch the full old document
                        Projections.include(idField()) // only retrieve id
                )
        );
        if (created == null) {
            throw new NoSQLInvalidURIException(instance.getUri());
        }
    }

    @Override
    public DeleteResult delete(URI uri, ClientSession session) throws Exception {
        return mongodb.delete(collection, session, uri, idField());
    }

    @Override
    public DeleteResult delete(List<URI> uris, ClientSession session) throws Exception {
        return mongodb.deleteOnCriteria(collection, session, Filters.in(idField(), uris));
    }

    public Document deleteFilterToDocument(F deleteFilter) throws Exception {
        Document filter = filterToDocument(deleteFilter);
        if(filter.size() == 0){
            throw new IllegalArgumentException("["+collection.getNamespace().getCollectionName()+"] Empty delete filter : provide at least a filter");
        }
        return filter;
    }

    @Override
    public DeleteResult delete(F deleteFilter, ClientSession session) throws Exception {
       return mongodb.deleteOnCriteria(collection, session, deleteFilterToDocument(deleteFilter));
    }

    @Override
    public long count(F filter) throws Exception {
        return mongodb.count(collection, filterToDocument(filter));
    }

    @Override
    public ListWithPagination<T> search(F searchFilter) throws Exception {
       return search(searchFilter,null);
    }

    @Override
    public ListWithPagination<T> search(F searchFilter, Bson projection) throws Exception {
        return mongodb.searchWithPagination(
                collection,
                filterToDocument(searchFilter),
                null,
                searchFilter.getOrderByList(),
                searchFilter.getPage(),
                searchFilter.getPageSize()
        );
    }

    protected Bson getIdFilter(URI id){
        return Filters.eq(idField(), id);
    }

    public List<Bson> getBsonFilters(F searchFilter) throws Exception{
        List<Bson> filters = new ArrayList<>();
        if(searchFilter.getUri() != null){
            filters.add(getIdFilter(searchFilter.getUri()));
        }
        return filters;
    }

    public Document filterToDocument(F searchFilter) throws Exception {

        Objects.requireNonNull(searchFilter);

        Document document = new Document();

        List<Bson> bsonFilters = getBsonFilters(searchFilter);

        // append each filter with a logical AND or logical OR
        if(searchFilter.isLogicalAnd()){
            document.putAll(Filters.and(bsonFilters).toBsonDocument());
        }else{
            document.putAll(Filters.or(bsonFilters).toBsonDocument());
        }

        return document;
    }
}
