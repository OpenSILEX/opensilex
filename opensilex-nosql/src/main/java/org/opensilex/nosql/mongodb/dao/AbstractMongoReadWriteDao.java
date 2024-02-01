package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.pagination.StreamWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractMongoReadWriteDao<T extends MongoModel, F extends MongoSearchFilter> implements MongoWriteDao<T, F>, MongoReadDao<T, F> {

    protected final MongoDBServiceV2 mongodb;
    protected final MongoCollection<T> collection;

    protected final boolean checkUriGeneration;
    protected final boolean checkUriExistence;
    protected final String createPrefix;
    protected final Logger logger;

    protected static final OrderBy DEFAULT_ORDER_BY = new OrderBy(MongoModel.URI_FIELD, Order.ASCENDING);

    protected AbstractMongoReadWriteDao(@NotNull MongoDBServiceV2 mongodb, @NotNull Class<T> modelClass, @NotNull @NotEmpty String collectionName, String createPrefix, boolean checkUriGeneration, boolean checkUriExistence) {
        Objects.requireNonNull(mongodb);
        this.mongodb = mongodb;
        collection = mongodb.getDatabase().getCollection(collectionName, modelClass);
        this.checkUriGeneration = checkUriGeneration;
        this.checkUriExistence = checkUriExistence;
        this.createPrefix = createPrefix;
        logger = LoggerFactory.getLogger(getClass());
    }

    protected void createIndexes() {
        // no index by default
    }

    @Override
    public String idField() {
        return MongoModel.URI_FIELD;
    }

    @Override
    public @NotNull T get(@NotNull URI uri) throws NoSQLInvalidURIException {
        Objects.requireNonNull(uri);
        return mongodb.findByURI(collection, uri, idField());
    }

    @Override
    public @NotNull T get(ClientSession session, @Valid @Required @NotNull URI uri) throws NoSQLInvalidURIException {
        Objects.requireNonNull(uri);
        return mongodb.findByURI(session, collection, uri, idField());
    }

    @Override
    public boolean exists(@NotNull URI uri) throws MongoException {
        return exists(null, uri);
    }

    @Override
    public boolean exists(ClientSession session, @NotNull URI uri) throws MongoException {
        Objects.requireNonNull(uri);
        return mongodb.uriExists(session, collection, uri);
    }

    /**
     * Generates a unique URI if null or validates the current URI's existence in the collection.
     *
     * @param instance            The instance of type T
     * @throws NoSQLAlreadyExistingUriException If the URI already exists in the collection
     * @throws URISyntaxException               If there's an issue with URI syntax
     */
    public void generateUniqueUriIfNullOrValidateCurrent(T instance) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            String prefix = UriBuilder.fromUri(mongodb.getGenerationPrefixURI()).path(createPrefix).toString();
            uri = instance.generateURI(prefix, instance, retry);
            while (checkUriExistence && mongodb.uriExists(collection, uri, MongoModel.URI_FIELD)) {
                uri = instance.generateURI(prefix, instance, retry++);
            }
            instance.setUri(uri);

        } else if (checkUriExistence && mongodb.uriExists(collection, uri, MongoModel.URI_FIELD)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }

    @Override
    public InsertOneResult create(@NotNull T instance, ClientSession session) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException {
        Objects.requireNonNull(instance);
        generateUniqueUriIfNullOrValidateCurrent(instance);
        return mongodb.create(instance, collection, session);
    }

    @Override
    public final @NotNull InsertOneResult create(@NotNull T instance) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException {
        return create(instance, null);
    }

    @Override
    public InsertManyResult create(List<T> instances) throws MongoException, NoSQLAlreadyExistingUriException, URISyntaxException {
        return create(instances, null);
    }

    @Override
    public InsertManyResult create(@NotNull @NotEmpty List<T> instances, ClientSession session) throws MongoException, NoSQLAlreadyExistingUriException, URISyntaxException {
        if (CollectionUtils.isEmpty(instances)) {
            throw new IllegalArgumentException("instances list must not be empty");
        }

        if (checkUriGeneration) {
            for (T instance : instances) {
                generateUniqueUriIfNullOrValidateCurrent(instance);
            }
        }

        return session != null ?
                mongodb.createAll(instances, collection, session):
                mongodb.computeTransaction(newSession -> mongodb.createAll(instances, collection, newSession));
    }

    public Bson getUpdateFilter(T instance) {
        // by default the filter for update is the id filter with the instance uri
        return getIdFilter(instance);
    }

    @Override
    public void update(@NotNull T instance, ClientSession session) throws MongoException, NoSQLInvalidURIException {
        Objects.requireNonNull(instance);
        mongodb.update(instance, collection, getUpdateFilter(instance), session);
    }

    @Override
    public void update(T instance) throws MongoException, NoSQLInvalidURIException {
        update(instance, null);
    }

    @Override
    public @NotNull DeleteResult delete(@NotNull URI uri, ClientSession session) throws MongoException {
        Objects.requireNonNull(uri);
        return mongodb.delete(collection, session, getIdFilter(uri));
    }

    @Override
    public @NotNull DeleteResult deleteMany(@NotNull @NotEmpty List<URI> uris, ClientSession session) throws MongoException {
        if (CollectionUtils.isEmpty(uris)) {
            throw new IllegalArgumentException("uris list must not be empty");
        }
        return mongodb.deleteMany(collection, session, Filters.in(idField(), uris));
    }

    public Bson deleteFilterToDocument(F deleteFilter) throws MongoException {
        return filterToBson(deleteFilter);
    }

    @Override
    public @NotNull DeleteResult deleteMany(@NotNull F deleteFilter, ClientSession session) throws MongoException {
        Objects.requireNonNull(deleteFilter);
        Bson deleteDocument = deleteFilterToDocument(deleteFilter);
        return session != null ?
                mongodb.deleteMany(collection, session, deleteDocument) :
                mongodb.computeTransaction(newSession -> mongodb.deleteMany(collection, newSession, deleteDocument));

    }

    @Override
    public @NotNull DeleteResult delete(@NotNull URI uri) throws MongoException {
        return delete(uri, null);
    }


    @Override
    public @NotNull DeleteResult deleteMany(@NotNull F filter) throws MongoException {
        return deleteMany(filter, null);
    }

    @Override
    public final long count(@NotNull F filter) throws MongoException {
        return count(null, filter);
    }

    @Override
    public long count(ClientSession session, @NotNull F filter) throws MongoException {
        Objects.requireNonNull(filter);
        return mongodb.count(session, collection, filterToBson(filter));
    }

    @Override
    public final ListWithPagination<T> search(@NotNull F filter) throws MongoException {
        return search(null, filter, null);
    }

    @Override
    public ListWithPagination<T> search(ClientSession session, @NotNull F filter, Bson projection) throws MongoException {
        Objects.requireNonNull(filter);

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
    public <T_CONVERTED> ListWithPagination<T_CONVERTED> search(ClientSession session, @NotNull F filter, Bson projection, @NotNull Function<T, T_CONVERTED> convertFunction) throws MongoException {

        Objects.requireNonNull(filter);
        Objects.requireNonNull(convertFunction);

        Map.Entry<FindIterable<T>, Long> resultAndCount = mongodb.findWithPagination(
                collection,
                filterToBson(filter),
                projection,
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize(),
                session
        );
        if (resultAndCount == null) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        // iterate over MongoDB result and convert result on the fly before collect them inside a List
        int resultCount = resultAndCount.getValue().intValue();
        List<T_CONVERTED> convertedResults = new ArrayList<>(resultCount);
        resultAndCount.getKey().forEach(
                mongoResult -> convertedResults.add(convertFunction.apply(mongoResult))
        );

        return new ListWithPagination<>(convertedResults, filter.getPage(), filter.getPageSize(), resultCount);
    }

    @Override
    public final <T_CONVERTED> ListWithPagination<T_CONVERTED> search(@NotNull F filter, Function<T, T_CONVERTED> convertFunction) throws MongoException {
        return search(null, filter, null, convertFunction);
    }

    @Override
    public StreamWithPagination<T> searchAsStream(@NotNull F filter) throws MongoException {
        return searchAsStream(null, filter, null);
    }

    @Override
    public @NotNull StreamWithPagination<T> searchAsStream(ClientSession session, @NotNull F filter, Bson projection) throws MongoException {

        Objects.requireNonNull(filter);

        Map.Entry<FindIterable<T>, Long> resultAndCount = mongodb.findWithPagination(
                collection,
                filterToBson(filter),
                projection,
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize(),
                session
        );

        // no result, return StreamWithPagination with an empty Stream
        if (resultAndCount == null) {
            return new StreamWithPagination<>();
        }

        // Build the StreamWithPagination with results Stream and results count
        int resultCount = resultAndCount.getValue().intValue();
        Stream<T> resultStream = StreamSupport.stream(resultAndCount.getKey().spliterator(), false);
        return new StreamWithPagination<>(resultStream, resultCount, filter.getPage(), filter.getPageSize());
    }

    @Override
    public Set<URI> distinctUris(ClientSession session, @NotNull F filter) throws MongoException {
        Objects.requireNonNull(filter);

        return mongodb.distinctWithPagination(
                collection,
                MongoModel.URI_FIELD,
                document -> document.get(idField(), URI.class),
                filterToBson(filter),
                CollectionUtils.isEmpty(filter.getOrderByList()) ? List.of(DEFAULT_ORDER_BY) : filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize(),
                session
        );
    }

    @Override
    public Set<URI> distinctUris(@NotNull F filter) throws MongoException {
        return distinctUris(null, filter);
    }

    protected Bson getIdFilter(URI id) {
        return Filters.eq(idField(), id);
    }

    protected Bson getIdFilter(T model) {
        return Filters.eq(idField(), model.getUri());
    }


    public List<Bson> getBsonFilters(F searchQuery) {
        List<Bson> filters = new ArrayList<>();

        if (searchQuery.getUri() != null) {
            filters.add(getIdFilter(searchQuery.getUri()));
        }
        if (!CollectionUtils.isEmpty(searchQuery.getIncludedUris())) {
            filters.add(Filters.in(idField(), searchQuery.getIncludedUris()));
        }
        if (!CollectionUtils.isEmpty(searchQuery.getRdfTypes())) {
            filters.add(Filters.in(MongoModel.TYPE_FIELD, searchQuery.getRdfTypes()));
        }

        return filters;
    }

    protected @NotNull Bson filterToBson(@NotNull F searchFilter) {

        List<Bson> bsonFilters = getBsonFilters(searchFilter);

        if (bsonFilters.isEmpty()) {
            return Filters.empty();
        } else if (bsonFilters.size() == 1) {
            return bsonFilters.get(0);
        }

        // append each filter with a logical AND or logical OR
        if (searchFilter.isLogicalAnd()) {
            return Filters.and(bsonFilters);
        } else {
            return Filters.or(bsonFilters);
        }
    }

    @Override
    public <T_RESULT> Set<T_RESULT> distinct(@NotNull String field, @NotNull Class<T_RESULT> resultClass, @NotNull F filter, ClientSession session) {
        return mongodb.distinct(field, resultClass, collection, filterToBson(filter), session);
    }

    @Override
    public <T_RESULT> Set<T_RESULT> distinctAggregation(@NotNull String field, @NotNull Class<T_RESULT> resultClass, F filter, ClientSession session) {
        return mongodb.distinctWithPagination(
                collection,
                field,
                document -> document.get("_id", resultClass), // the distinct field value is the value of each aggregated document with the key "_id"
                filterToBson(filter),
                CollectionUtils.isEmpty(filter.getOrderByList()) ? List.of(DEFAULT_ORDER_BY) : filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize(),
                session
        );
    }

    @Override
    public <T_RESULT, T_JOINED> List<T_RESULT> lookupAggregation(F filter,
                                                                 @NotNull String lookupField,
                                                                 @NotNull String lookupCollectionName,
                                                                 @NotNull Class<T_JOINED> lookupClass,
                                                                 @NotNull Function<T_JOINED, T_RESULT> convertFunction,
                                                                 ClientSession session) {
        return mongodb.lookupAggregation(
                collection,
                lookupCollectionName,
                lookupField,
                filterToBson(filter),
                filter == null ? Collections.emptyList() : filter.getOrderByList(),
                lookupClass,
                convertFunction,
                session
        );
    }

    public final MongoCollection<T> getCollection() {
        return collection;
    }

}
