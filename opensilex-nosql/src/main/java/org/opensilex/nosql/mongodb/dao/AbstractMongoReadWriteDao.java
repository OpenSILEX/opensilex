package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.Document;
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
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.opensilex.nosql.mongodb.service.v2.MongoLogType.*;
import static org.opensilex.utils.LogFilter.LOG_TYPE;

public abstract class AbstractMongoReadWriteDao<T extends MongoModel, F extends MongoSearchFilter> implements MongoWriteDao<T, F>, MongoReadDao<T, F> {

    protected final MongoDBServiceV2 mongodb;
    protected final MongoCollection<T> collection;
    protected static final Bson EMPTY_PROJECTION = Projections.fields(Projections.include(MongoModel.MONGO_ID_FIELD));

    protected final boolean checkUriGeneration;
    protected final boolean checkUriExistence;
    protected final String createPrefix;
    protected final Logger logger;
    protected final Class<T> modelClass;

    protected static final OrderBy DEFAULT_ORDER_BY = new OrderBy(MongoModel.URI_FIELD, Order.ASCENDING);

    protected AbstractMongoReadWriteDao(@NotNull MongoDBServiceV2 mongodb, @NotNull Class<T> modelClass, @NotNull @NotEmpty String collectionName, String createPrefix, boolean checkUriGeneration, boolean checkUriExistence) {
        Objects.requireNonNull(mongodb);
        this.mongodb = mongodb;
        collection = mongodb.getDatabase().getCollection(collectionName, modelClass);
        this.modelClass = modelClass;
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
        return get(null, uri);
    }

    @Override
    public @NotNull T get(ClientSession session, @Valid @Required @NotNull URI uri) throws NoSQLInvalidURIException {
        Objects.requireNonNull(uri);

        T instance = session == null ?
                collection.find(eq(idField(), uri)).first() :
                collection.find(session, eq(idField(), uri)).first();

        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        }
        return instance;
    }

    @Override
    public List<T> findByUris(@NotNull Stream<URI> uris, int size) {
        Objects.requireNonNull(uris);
        if(size <= 0){
            throw new IllegalArgumentException("streamSize must be strictly positive");
        }

        // Create filter from URIS and run query with pagination
        Bson filter = Filters.in(idField(), uris.limit(size).iterator());
        FindIterable<T> queryResult = collection.find(filter).limit(size);

        List<T> instances = new ArrayList<>(size);
        for (T res : queryResult) {
            instances.add(res);
        }
        return instances;
    }

    @Override
    public final boolean exists(@NotNull URI uri) throws MongoException {
        return exists(null, uri);
    }

    @Override
    public boolean exists(ClientSession session, @NotNull URI uri) throws MongoException {
        Objects.requireNonNull(uri);

        FindIterable<T> findIt = session == null ?
                collection.find(eq(idField(), uri)) :
                collection.find(session, eq(idField(), uri));

        // Use a minimal projection in order to avoid the fetching of fields which are useless here
        return findIt.projection(EMPTY_PROJECTION).first() != null;
    }

    /**
     * Generates a unique URI if null or validates the current URI's existence in the collection.
     *
     * @param instance The instance of type T
     * @throws NoSQLAlreadyExistingUriException If the URI already exists in the collection
     * @throws URISyntaxException               If there's an issue with URI syntax
     */
    public void generateUniqueUriIfNullOrValidateCurrent(T instance) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            String prefix = UriBuilder.fromUri(mongodb.getGenerationPrefixURI()).path(createPrefix).toString();
            uri = instance.generateURI(prefix, instance, retry);
            while (checkUriExistence && exists(uri)) {
                uri = instance.generateURI(prefix, instance, retry++);
            }
            instance.setUri(uri);

        } else if (checkUriExistence && exists(uri)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }

    @Override
    public InsertOneResult create(@NotNull T instance, ClientSession session) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException {
        Objects.requireNonNull(instance);
        generateUniqueUriIfNullOrValidateCurrent(instance);

        if (logger.isDebugEnabled()) {
            logger.debug("MongoDB create, collection: {} {}", collection.getNamespace().getCollectionName(), kv(LOG_TYPE, MONGO_CREATE_LOG_MSG));
        }
        return session != null ?
                collection.insertOne(session, instance) :
                collection.insertOne(instance);
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

        logger.info("{} {} collection: {}", kv(LOG_TYPE, MONGO_CREATE_LOG_MSG), kv(MONGO_LOG_STATUS, MONGO_LOG_STATUS_START), collection.getNamespace().getCollectionName());
        Instant start = Instant.now();

        if (checkUriGeneration) {
            for (T instance : instances) {
                generateUniqueUriIfNullOrValidateCurrent(instance);
            }
        }

        // Transaction handling for data integrity since insertMany() can update several documents
        InsertManyResult result = session != null ?
                collection.insertMany(session, instances) :
                mongodb.computeTransaction(newSession -> collection.insertMany(newSession, instances));

        long durationMs = Duration.between(start, Instant.now()).toMillis();
        logger.info("{}, {}, collection: {}, insertCount: {}, duration: {} ms", kv(LOG_TYPE, MONGO_CREATE_LOG_MSG),  kv(MONGO_LOG_STATUS, MONGO_LOG_STATUS_OK), collection.getNamespace().getCollectionName(),  result.getInsertedIds().size(), durationMs);
        return result;
    }

    public Bson getUpdateFilter(T instance) {
        // by default the filter for update is the id filter with the instance uri
        return getIdFilter(instance);
    }

    /**
     * Update the new model in the given collection according a filter
     *
     * @param newModel new model to write in database
     * @param filter   additional BSON filter
     * @param session  current session
     * @throws NoSQLInvalidURIException if no previous corresponding model was found in the collection
     */
    protected void update(T newModel, Bson filter, ClientSession session) throws NoSQLInvalidURIException {
        logger.debug("MONGO UPDATE - Collection : {}", collection.getNamespace().getCollectionName());

        FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().projection( // don't fetch the full old document
                Projections.include(MongoModel.URI_FIELD) // only retrieve id
        );
        T updatedModel = session == null ?
                collection.findOneAndReplace(filter, newModel, options) :
                collection.findOneAndReplace(session, filter, newModel, options);

        // Directly check the results of the findOneAndReplace() instead of performing a find() query before
        // If the returned model is null, it implies that the previous model was not found.
        if (updatedModel == null) {
            throw new NoSQLInvalidURIException(newModel.getUri());
        }
    }

    @Override
    public void update(@NotNull T model, ClientSession session) throws MongoException, NoSQLInvalidURIException {
        Objects.requireNonNull(model);
        update(model, getUpdateFilter(model), session);
    }

    @Override
    public void update(T instance) throws MongoException, NoSQLInvalidURIException {
        update(instance, null);
    }

    @Override
    public final @NotNull DeleteResult delete(@NotNull URI uri) throws MongoException, NoSQLInvalidURIException {
        return delete(uri, null);
    }


    @Override
    public @NotNull DeleteResult delete(@NotNull URI uri, ClientSession session) throws MongoException, NoSQLInvalidURIException {
        Objects.requireNonNull(uri);
        Bson bsonFilter = getIdFilter(uri);

        DeleteResult deleteResult = session == null ?
                collection.deleteOne(bsonFilter) :
                collection.deleteOne(session, bsonFilter);

        if (deleteResult.getDeletedCount() == 0) {
            throw new NoSQLInvalidURIException(uri);
        }
        return deleteResult;
    }

    protected @NotNull DeleteResult deleteMany(ClientSession session, Bson deleteFilterBson) {
        logger.info("{}, {}, collection: {}", kv(LOG_TYPE, MONGO_DELETE_MANY_LOG_MSG), kv(MONGO_LOG_STATUS, MONGO_LOG_STATUS_START), collection.getNamespace().getCollectionName());
        Instant start = Instant.now();

        // deleteMany() can update several document inside a collection, transaction handling is always needed
        DeleteResult result = session != null ?
                collection.deleteMany(session, deleteFilterBson) :
                mongodb.computeTransaction(newSession -> collection.deleteMany(newSession, deleteFilterBson));

        long durationMs = Duration.between(start, Instant.now()).toMillis();
        logger.info("{} {} collection: {}, deleteCount: {}, duration: {} ms", kv(LOG_TYPE, MONGO_DELETE_MANY_LOG_MSG), kv(MONGO_LOG_STATUS, MONGO_LOG_STATUS_OK), collection.getNamespace().getCollectionName(), result.getDeletedCount(), durationMs);
        return result;
    }

    @Override
    public final @NotNull DeleteResult deleteMany(@NotNull @NotEmpty List<URI> uris, ClientSession session) throws MongoException {
        if (CollectionUtils.isEmpty(uris)) {
            throw new IllegalArgumentException("uris list must not be empty");
        }
        return deleteMany(session, Filters.in(idField(), uris));
    }

    public Bson deleteFilterToDocument(F deleteFilter) throws MongoException {
        return filterToBson(deleteFilter);
    }

    @Override
    public final @NotNull DeleteResult deleteMany(@NotNull F deleteFilter, ClientSession session) throws MongoException {
        Objects.requireNonNull(deleteFilter);
        return deleteMany(session, deleteFilterToDocument(deleteFilter));
    }

    @Override
    public final @NotNull DeleteResult deleteMany(@NotNull F filter) throws MongoException {
        return deleteMany(filter, null);
    }

    @Override
    public final long count(@NotNull F filter) throws MongoException {
        return count(null, filter);
    }

    @Override
    public long count(ClientSession session, @NotNull F filter) throws MongoException {
        Objects.requireNonNull(filter);

        logger.debug("[MONGO_COUNT] : { collection: {},filter: {}}", collection.getNamespace().getCollectionName(), filter);

        Bson bsonFilter = filterToBson(filter);
        return session == null ?
                collection.countDocuments(bsonFilter) :
                collection.countDocuments(session, bsonFilter);
    }

    /**
     * Finds documents in the specified MongoCollection with pagination support.
     *
     * @param filter      The filter to apply during the search
     * @param projection  The projection to apply (can be null)
     * @param session     The ClientSession for handling the transaction (can be null)
     * @return Map.Entry<FindIterable < T>, Long>    The query results and the total count of results
     */
    protected Map.Entry<FindIterable<T>, Long> findWithPagination(Bson projection, F filter, ClientSession session) {

        Bson bsonFilter = filterToBson(filter);
        long resultsNumber = collection.countDocuments(bsonFilter);

        // call isDebugEnabled before displaying log, since LogOrderList is a function. We don't want to call this method, is DEBUG logging is not enabled
        if (logger.isDebugEnabled()) {
            logger.debug("[MONGO_SEARCH_WITH_PAGINATION] : { collection: {}, filter: {}, results_count: {}}", collection.getNamespace().getCollectionName(), filter, resultsNumber);
        }

        if (resultsNumber == 0) {
            return null;
        }
        FindIterable<T> queryResult = session == null ?
                collection.find(bsonFilter) :
                collection.find(session, bsonFilter);

        queryResult.sort(buildSort(filter.getOrderByList()))
                .skip(filter.getPage() * filter.getPageSize())
                .limit(filter.getPageSize());

        if (projection != null) {
            queryResult.projection(projection);
        }

        return new AbstractMap.SimpleImmutableEntry<>(queryResult, resultsNumber);
    }

    @Override
    public final ListWithPagination<T> search(@NotNull F filter) throws MongoException {
        return search(null, filter, null);
    }

    @Override
    public ListWithPagination<T> search(ClientSession session, @NotNull F filter, Bson projection) throws MongoException {
        Objects.requireNonNull(filter);

        Map.Entry<FindIterable<T>, Long> resultAndCount = findWithPagination(projection, filter, session);
        if (resultAndCount == null) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        // iterate over MongoDB result cursor and collect inside a List
        List<T> results = new ArrayList<>(filter.getPageSize());
        CollectionUtils.addAll(results, resultAndCount.getKey());

        return new ListWithPagination<>(results,  filter.getPage(), filter.getPageSize(), resultAndCount.getValue().intValue());
    }

    @Override
    public <T_CONVERTED> ListWithPagination<T_CONVERTED> search(ClientSession session, @NotNull F filter, Bson projection, @NotNull Function<T, T_CONVERTED> convertFunction) throws MongoException {

        Objects.requireNonNull(filter);
        Objects.requireNonNull(convertFunction);

        Map.Entry<FindIterable<T>, Long> resultAndCount = findWithPagination(projection, filter, session);
        if (resultAndCount == null) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        // iterate over MongoDB result and convert result on the fly before collect them inside a List
        int resultCount = resultAndCount.getValue().intValue();
        List<T_CONVERTED> convertedResults = new ArrayList<>(filter.getPageSize());
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

        Map.Entry<FindIterable<T>, Long> resultAndCount = findWithPagination(projection, filter, session);

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
        return distinct(idField(), URI.class, filter, session);
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

        if (logger.isDebugEnabled()) {
            logger.debug("[MONGO_DISTINCT] : { collection: {}, filter: {}, field: {}, resultClass: {} }", collection.getNamespace().getCollectionName(), filter, field, resultClass.getSimpleName());
        }

        Set<T_RESULT> results = new HashSet<>();
        DistinctIterable<T_RESULT> queryResult = session == null ?
                collection.distinct(field, filterToBson(filter), resultClass) :
                collection.distinct(session, field, filterToBson(filter), resultClass);

        for (T_RESULT res : queryResult) {
            results.add(res);
        }

        return results;
    }

    protected <T_RESULT> Set<T_RESULT> distinctWithPagination(ClientSession session, @NotNull String distinctField, @NotNull F filter, Function<Document, T_RESULT> documentExtractor) {
        Objects.requireNonNull(filter);
        if (logger.isDebugEnabled()) {
            logger.debug("[MONGO_DISTINCT_WITH_PAGINATION] : { collection: {}, filter: {}}", collection.getNamespace().getCollectionName(), filter);
        }

        List<Bson> aggregatePipeline = new ArrayList<>();

        // filtering with match
        aggregatePipeline.add(Aggregates.match(filterToBson(filter)));

        // distinct field with match
        aggregatePipeline.add(Aggregates.group("$" + distinctField));

        // sort
        List<OrderBy> orderByList = CollectionUtils.isEmpty(filter.getOrderByList()) ? List.of(DEFAULT_ORDER_BY) : filter.getOrderByList();
        Document order = buildSort(orderByList);
        aggregatePipeline.add(Aggregates.sort(order.toBsonDocument()));

        // pagination : skip and limit
        if (filter.getPageSize() > 0) {
            aggregatePipeline.add(Aggregates.limit(filter.getPageSize()));
            if (filter.getPage() > 0) {
                aggregatePipeline.add(Aggregates.skip(filter.getPage() * filter.getPageSize()));
            }
        }

        // Use a set implementation which maintains the same order as the insertion order
        // The order is computed by MongoDB and the results are returned in this order
        // The LinkedHashSet allow to keep the same order from database, when iterating the set
        Set<T_RESULT> distinctValues = new LinkedHashSet<>();

        // map aggregation results a Document, since the aggregation will produce a different document schema
        AggregateIterable<Document> aggregateIt = session == null ?
                collection.aggregate(aggregatePipeline, Document.class) :
                collection.aggregate(session, aggregatePipeline, Document.class);

        // Transform document on the fly and add inside set
        aggregateIt.map(documentExtractor::apply)
                .forEach(extracted -> {
                    if (extracted != null) {
                        distinctValues.add(extracted);
                    }
                });

        return distinctValues;
    }

    @Override
    public Set<T> aggregate(List<Bson> aggregationPipeline) {
        Set<T> results = new HashSet<>();

        AggregateIterable<T> aggregate = collection.aggregate(aggregationPipeline, modelClass);
        for (T res : aggregate) {
            results.add(res);
        }

        return results;
    }

    @Override
    public <T_RESULT> Set<T_RESULT> distinctAggregation(@NotNull String distinctField, @NotNull Class<T_RESULT> resultClass, @NotNull F filter, ClientSession session) {

        return distinctWithPagination(
                session,
                distinctField,
                filter,
                document -> document.get("_id", resultClass) // the distinct field value is the value of each aggregated document with the key "_id"
        );
    }

    @Override
    public <T_RESULT, T_JOINED> List<T_RESULT> lookupAggregation(F filter,
                                                                 @NotNull String lookupField,
                                                                 @NotNull String lookupCollectionName,
                                                                 @NotNull Class<T_JOINED> lookupClass,
                                                                 @NotNull Function<T_JOINED, T_RESULT> convertFunction,
                                                                 ClientSession session) {

        String outputFieldName = lookupField + "_join";
        String outputFieldJoinKey = lookupField + "." + MongoModel.URI_FIELD;
        String groupKey = "group";

        List<Bson> pipeline = new ArrayList<>();
        // filtering with match
        // First projection, only restrict to joined fields
        // group by foreign key
        // lookup with destination collection
        // unwind
        // replace root
        if (filter != null) {
            pipeline.add(Aggregates.match(filterToBson(filter)));
        }
        pipeline.add(Aggregates.project(Projections.include(lookupField)));
        pipeline.add(Aggregates.group("$" + outputFieldJoinKey, Accumulators.first(groupKey, "$$ROOT")));
        pipeline.add(Aggregates.replaceRoot("$" + groupKey + "." + lookupField));
        pipeline.add(Aggregates.lookup(lookupCollectionName, MongoModel.URI_FIELD, MongoModel.URI_FIELD, outputFieldName));
        pipeline.add(Aggregates.unwind("$" + outputFieldName));
        pipeline.add(Aggregates.replaceRoot("$" + lookupField));

        // sort
        if (filter != null && !CollectionUtils.isEmpty(filter.getOrderByList())) {
            Document order = buildSort(filter.getOrderByList());
            pipeline.add(Aggregates.sort(order.toBsonDocument()));
        }

        // run aggregation and collect results
        List<T_RESULT> results = new ArrayList<>();
        AggregateIterable<T_JOINED> aggregateIt = session == null ?
                collection.aggregate(pipeline, lookupClass) :
                collection.aggregate(session, pipeline, lookupClass);

        aggregateIt.map(convertFunction::apply).forEach(results::add);
        return results;
    }

    public final MongoCollection<T> getCollection() {
        return collection;
    }

    public Document buildSort(List<OrderBy> orderByList) {
        Document sort = new Document();
        if (CollectionUtils.isEmpty(orderByList)) {
            return sort;
        }
        for (OrderBy orderBy : orderByList) {
            if (orderBy.getOrder().equals(Order.ASCENDING)) {
                sort.put(orderBy.getFieldName(), 1);
            } else if (orderBy.getOrder().equals(Order.DESCENDING)) {
                sort.put(orderBy.getFieldName(), -1);
            }
        }
        return sort;
    }

}
