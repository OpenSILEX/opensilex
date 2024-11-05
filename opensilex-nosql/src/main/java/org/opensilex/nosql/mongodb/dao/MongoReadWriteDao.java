package org.opensilex.nosql.mongodb.dao;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.MongoDbUniqueIndexConstraintViolation;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.logging.MongoLogger;

import static org.opensilex.nosql.mongodb.logging.MongoLogger.*;

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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;
import static org.opensilex.utils.pagination.PaginatedSearchStrategy.COUNT_QUERY_BEFORE_SEARCH;
import static org.opensilex.utils.pagination.PaginatedSearchStrategy.HAS_NEXT_PAGE;

/**
 * Implementation of {@link MongoReadDao} and {@link MongoWriteDao} for a given T model inside a {@link MongoCollection} of T
 * This class implements all read/write methods from DAOs.
 *
 * @param <T> The type of the MongoDB model
 * @param <F> The kind of filter specific to this DAO
 * @author rcolin
 * @apiNote This class can be used by two-way :
 *
 * <ul>
 *     <li><b>By direct instanciation</b>. You just have to provide the specified {@code modelClass} and {@code collectionName} in the constructors</li>
 *     <li>See the example below which allow to handle a {@code MongoTestModel} in the collection {@code "mongo-dao-search-test"}</li>
 * </ul>
 * <p>
 * {@code MongoReadWriteDao<MongoTestModel, MongoSearchFilter> dao = new MongoReadWriteDao<>(mongoDBv2, MongoTestModel.class, "mongo-dao-search-test", "test")}
 *
 * <ul>
 *     <li><b>With an extension of the MongoReadWriteDao</b> with a specialized {@code MongoTestModel} and {@code MongoSearchFilter}</li>
 *     <li>See See {@code opensilex-doc/src/main/resources/opensilex-nosql/mongodb/services/MongoDaoTutorial.md} for an example of extension</li>
 * <ul>
 */
public class MongoReadWriteDao<T extends MongoModel, F extends MongoSearchFilter> implements MongoWriteDao<T, F>, MongoReadDao<T, F> {


    protected final MongoDBServiceV2 mongodb;
    protected final MongoDBConfig mongoDBConfig;

    protected final MongoCollection<T> collection;
    protected final Class<T> modelClass;
    protected final boolean checkUriGeneration;
    protected final boolean checkUriExistence;
    protected final String createPrefix;

    protected final MongoLogger mongoLogger;
    protected final Logger logger;

    protected static final OrderBy DEFAULT_ORDER_BY = new OrderBy(MongoModel.URI_FIELD, Order.ASCENDING);
    protected static final Bson EMPTY_PROJECTION = Projections.fields(Projections.include(MongoModel.MONGO_ID_FIELD));


    /**
     * @param mongodb            The MongoDB service object
     * @param modelClass         The MongoDB model class handled by this DAO instance
     * @param collectionName     The name of the MongoDB collection in which read/write operations are performed
     * @param createUriPath      The name of path which is used during URI generation. This path allow to generate a more humanly readable URI
     * @param checkUriGeneration Check during {@link #create(List)} if each model has a URI or not
     * @param checkUriExistence  if checkUriGeneration is true, check during {@link #create(List)} if each model URI already exists by performing a database request
     * @apiNote <b> checkUriExistence </b>
     * <ul>
     *     <li>The checking of the URI existence with checkUriExistence = true is very costly since it require to performs n request to the database. The only advantage is to provide a fine-grained error message </li>
     *     <li>Consider the catching of a {@link com.mongodb.MongoWriteException} with the {@link com.mongodb.ErrorCategory#DUPLICATE_KEY} error code and {@code checkUriExistence == false} if you want to avoid these requests</li>
     *     <li>If you known that you have a safe method to generate an unique URI (example with an UUID part), then you can reasonably consider that URI checking is useless </li>
     * </ul>
     *
     * <b> checkUriGeneration </b>
     * <ul>If you have another way to set the URI for a given model and that you want to skip the loop over instances during creation, then you can set {@code checkUriGeneration} to true</ul>
     */
    public MongoReadWriteDao(@NotNull MongoDBServiceV2 mongodb, @NotNull Class<T> modelClass, @NotNull @NotEmpty String collectionName, String createUriPath, boolean checkUriGeneration, boolean checkUriExistence) {
        Objects.requireNonNull(mongodb);
        this.mongodb = mongodb;
        this.mongoDBConfig = mongodb.getImplementedConfig();
        this.collection = mongodb.getDatabase().getCollection(collectionName, modelClass);
        this.modelClass = modelClass;
        this.checkUriGeneration = checkUriGeneration;
        this.checkUriExistence = checkUriExistence;

        UriBuilder uriBuilder = UriBuilder.fromUri(mongodb.getGenerationPrefixURI());
        this.createPrefix = StringUtils.isEmpty(createUriPath) ?
                uriBuilder.toString() :
                uriBuilder.path(createUriPath).toString();

        this.logger = LoggerFactory.getLogger(getClass());
        this.mongoLogger = new MongoLogger(collectionName, logger);
    }

    /**
     * @apiNote use {@link #MongoReadWriteDao(MongoDBServiceV2, Class, String, String, boolean, boolean)} with {@code checkUriGeneration = true} and {@code checkUriExistence = false}
     */
    public MongoReadWriteDao(@NotNull MongoDBServiceV2 mongodb, @NotNull Class<T> modelClass, @NotNull @NotEmpty String collectionName, String createUriPath) {
        this(mongodb, modelClass, collectionName, createUriPath, true, false);
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
    public T get(ClientSession session, URI uri, Bson projection) throws NoSQLInvalidURIException {
        Objects.requireNonNull(uri);

        T instance = session == null ?
                collection.find(eq(idField(), uri)).projection(projection).first() :
                collection.find(session, eq(idField(), uri)).projection(projection).first();

        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        }
        return instance;
    }

    @Override
    public List<T> findByUris(@NotNull Stream<URI> uris, int size) {
        Objects.requireNonNull(uris);
        if (size <= 0) {
            throw new IllegalArgumentException("streamSize must be strictly positive");
        }

        // Create filter from URIS and run query with pagination
        // Filters.in's second parameter is an iterable. An Iterable has a single iterator.
        Bson filter = Filters.in(idField(), () -> uris.limit(size).iterator());
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
     * @throws NoSQLAlreadyExistingUriException If the instance URI is not null and already exists in the collection
     * @throws URISyntaxException               If there's an issue with URI syntax
     */
    protected void generateUniqueUriIfNullOrValidateCurrent(T instance) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            uri = instance.generateURI(createPrefix, instance, retry);
            while (checkUriExistence && exists(uri)) {
                uri = instance.generateURI(createPrefix, instance, retry++);
            }
            instance.setUri(uri);

        } else if (checkUriExistence && exists(uri)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }

    @Override
    public InsertOneResult create(ClientSession session, @NotNull T instance) throws MongoException, MongoDbUniqueIndexConstraintViolation, URISyntaxException, NoSQLAlreadyExistingUriException {
        Objects.requireNonNull(instance);

        Instant operationStart = mongoLogger.logOperationStart(INSERT_ONE);
        generateUniqueUriIfNullOrValidateCurrent(instance);

        try {
            InsertOneResult result = session != null ?
                    collection.insertOne(session, instance) :
                    collection.insertOne(instance);

            mongoLogger.logOperationOk(INSERT_ONE, operationStart, URI_KEY, instance.getUri());
            return result;

        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new MongoDbUniqueIndexConstraintViolation(e);
            }
            throw e;
        }
    }

    @Override
    public final @NotNull InsertOneResult create(@NotNull T instance) throws MongoException, MongoDbUniqueIndexConstraintViolation, URISyntaxException, NoSQLAlreadyExistingUriException {
        return create(null, instance);
    }

    @Override
    public InsertManyResult create(List<T> instances) throws MongoException, MongoDbUniqueIndexConstraintViolation, NoSQLAlreadyExistingUriException, URISyntaxException {
        return create(null, instances);
    }

    @Override
    public InsertManyResult create(ClientSession session, @NotNull @NotEmpty List<T> instances) throws MongoException, MongoDbUniqueIndexConstraintViolation, NoSQLAlreadyExistingUriException, URISyntaxException {
        if (CollectionUtils.isEmpty(instances)) {
            throw new IllegalArgumentException("instances list can't be null or empty");
        }

        Instant operationStart = mongoLogger.logOperationStart(INSERT_MANY, INSERT_MANY_COUNT, instances.size());

        // check if each model has a URI
        if (checkUriGeneration) {
            for (T instance : instances) {
                generateUniqueUriIfNullOrValidateCurrent(instance);
            }
        }

        // Transaction handling for data integrity since insertMany() can update several documents
        try {
            InsertManyResult result = session != null ?
                    collection.insertMany(session, instances) :
                    mongodb.computeTransaction(newSession -> collection.insertMany(newSession, instances));

            mongoLogger.logOperationOk(INSERT_MANY, operationStart, INSERT_MANY_COUNT, result.getInsertedIds().size());
            return result;

        } catch (MongoBulkWriteException e) {
            // Check a DUPLICATE_KEY error inside write errors
            if (e.getWriteErrors().isEmpty() || ErrorCategory.fromErrorCode(e.getWriteErrors().get(0).getCode()) != ErrorCategory.DUPLICATE_KEY) {
                throw e;
            }
            throw new MongoDbUniqueIndexConstraintViolation(e);
        }
    }

    public Bson getUpdateFilter(T instance) {
        // by default the filter for update is the id filter with the instance uri
        return getIdFilter(instance.getUri());
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

        FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().projection( // don't fetch the full old document
                Projections.include(MongoModel.URI_FIELD) // only retrieve id
        );

        Instant operationStart = mongoLogger.logOperationStart(UPDATE_ONE, URI_KEY, newModel.getUri());
        T updatedModel = session == null ?
                collection.findOneAndReplace(filter, newModel, options) :
                collection.findOneAndReplace(session, filter, newModel, options);

        // Directly check the results of the findOneAndReplace() instead of performing a find() query before
        // If the returned model is null, it implies that the previous model was not found.
        if (updatedModel == null) {
            throw new NoSQLInvalidURIException(newModel.getUri());
        }
        mongoLogger.logOperationOk(UPDATE_ONE, operationStart, URI_KEY, newModel.getUri());
    }

    @Override
    public void update(ClientSession session, @NotNull T model) throws MongoException, NoSQLInvalidURIException {
        Objects.requireNonNull(model);
        update(model, getUpdateFilter(model), session);
    }

    @Override
    public void update(T instance) throws MongoException, NoSQLInvalidURIException {
        update(null, instance);
    }

    /**
     * Update an existing model instance in the database if it exists,
     * otherwise insert it.
     *
     * @param model new model to upsert
     * @param filter   additional BSON filter
     * @param session  current session
     * @throws NoSQLInvalidURIException if no previous corresponding model was found in the collection
     */
    protected void upsert(T model, Bson filter, ClientSession session) {
        FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().upsert(true).projection( // don't fetch the full old document
                Projections.include(MongoModel.URI_FIELD) // only retrieve id
        );

        Instant operationStart = mongoLogger.logOperationStart(UPSERT_ONE, URI_KEY, model.getUri());
        T updatedModel = session == null ?
                collection.findOneAndReplace(filter, model, options) :
                collection.findOneAndReplace(session, filter, model, options);

        mongoLogger.logOperationOk(UPSERT_ONE, operationStart, URI_KEY, model.getUri());
    }

    @Override
    public void upsert(ClientSession session, @NotNull T instance) throws MongoException{
        upsert(instance, getUpdateFilter(instance), session);
    }

    @Override
    public void upsert(@NotNull T instance) throws MongoException{
        upsert(instance, getUpdateFilter(instance), null);
    }

    @Override
    public final @NotNull DeleteResult delete(@NotNull URI uri) throws MongoException, NoSQLInvalidURIException {
        return delete(null, uri);
    }

    @Override
    public @NotNull DeleteResult delete(ClientSession session, @NotNull URI uri) throws MongoException, NoSQLInvalidURIException {
        Objects.requireNonNull(uri);
        Bson bsonFilter = getIdFilter(uri);

        Instant operationStart = mongoLogger.logOperationStart(DELETE_ONE, URI_KEY, uri);
        DeleteResult deleteResult = session == null ?
                collection.deleteOne(bsonFilter) :
                collection.deleteOne(session, bsonFilter);

        if (deleteResult.getDeletedCount() == 0) {
            throw new NoSQLInvalidURIException(uri);
        }

        mongoLogger.logOperationOk(DELETE_ONE, operationStart, URI_KEY, uri);
        return deleteResult;
    }

    protected final @NotNull DeleteResult deleteMany(ClientSession session, Bson deleteFilterBson) {

        String deleteFilterString = deleteFilterBson.toString();
        Instant operationStart = mongoLogger.logOperationStart(DELETE_MANY, FILTER, deleteFilterString);

        // deleteMany() can update several document inside a collection, transaction handling is always needed
        DeleteResult result = session != null ?
                collection.deleteMany(session, deleteFilterBson) :
                mongodb.computeTransaction(newSession -> collection.deleteMany(newSession, deleteFilterBson));

        mongoLogger.logOperationOk(DELETE_MANY, operationStart, FILTER, deleteFilterString, DELETE_MANY_COUNT, result.getDeletedCount());
        return result;
    }

    @Override
    public final @NotNull DeleteResult deleteMany(ClientSession session, @NotNull @NotEmpty List<URI> uris) throws MongoException {
        if (CollectionUtils.isEmpty(uris)) {
            throw new IllegalArgumentException("uris list must not be empty");
        }
        return deleteMany(session, Filters.in(idField(), uris));
    }

    /**
     * @param deleteFilter the delete filter
     * @return the {@link Bson} document passed to mongodb for finding which document delete
     * @throws IllegalArgumentException if the provided deleteFilter contains no filter (it prevent the deletion of the whole database)
     * @apiNote This method return the {@link Bson} returned by {@link #filterToBson(MongoSearchFilter)}, which means
     * that delete and search use the same filter.
     */
    private Bson deleteFilterToDocument(F deleteFilter) throws MongoException {
        List<Bson> bsonFilters = getBsonFilters(deleteFilter);
        if (CollectionUtils.isEmpty(bsonFilters)) {
            throw new IllegalArgumentException("You can't provide an empty filter when deleting documents inside a collection");
        }
        return getBsonFilter(bsonFilters, deleteFilter.isLogicalAnd());
    }

    @Override
    public final @NotNull DeleteResult deleteMany(ClientSession session, @NotNull F deleteFilter) throws MongoException {
        Objects.requireNonNull(deleteFilter);
        return deleteMany(session, deleteFilterToDocument(deleteFilter));
    }

    @Override
    public final @NotNull DeleteResult deleteMany(@NotNull F filter) throws MongoException {
        return deleteMany(null, filter);
    }

    /**
     * @see #count(ClientSession, MongoSearchFilter, CountOptions)
     */
    @Override
    public final long count(@NotNull F filter) throws MongoException {
        // by default, use an empty count option with allow an exact count without limit
        return count(null, filter, new CountOptions());
    }

    /**
     * @param filter the search filter
     * @return the {@link CountOptions} to use when calling {@link MongoCollection#countDocuments(ClientSession, Bson, CountOptions)}
     * @apiNote The following  settings are set inside the default implementation :
     * <ul>
     *      <li>{@link CountOptions#maxTime(long, TimeUnit)}} : Set according {@link MongoDBConfig#readTimeoutMs()} value </li>
     *      <li>{@link CountOptions#hintString(String)} : No index hint is provided. It could be improved into specialized implementation according search filter options</li>
     * </ul>
     */
    protected CountOptions getDefaultCountOptions(F filter) {
        return new CountOptions()
                .maxTime(mongoDBConfig.readTimeoutMs(), TimeUnit.MILLISECONDS);

    }


    /**
     * @param filter the search filter
     * @return the {@link CountOptions} to use when calling {@link MongoCollection#countDocuments(ClientSession, Bson, CountOptions)}
     * @apiNote The following  settings are set inside the default implementation :
     * <ul>
     *     <li>{@link CountOptions#limit(int)} : A maximum limit equals to {@link MongoDBConfig#maxCountLimit()}, for better count performance.
     *      The greater this limit is, the more MongoDB has to read document from collection or index, which can affect performance,
     *      especially if the collection has more millions of document to read.
     *      </li>
     *      <li>{@link CountOptions#maxTime(long, TimeUnit)}} : Set according {@link MongoDBConfig#readTimeoutMs()} value </li>
     *      <li>{@link CountOptions#hintString(String)} : No index hint is provided. It could be improved into specialized implementation according search filter options</li>
     * </ul>
     */
    protected CountOptions getDefaultSearchCountOptions(F filter) {

        CountOptions countOptions = new CountOptions()
                .maxTime(mongoDBConfig.readTimeoutMs(), TimeUnit.MILLISECONDS);

        // No pageSize specified, limit by default
        if (filter.getPageSize() == 0) {
            return countOptions.limit(mongoDBConfig.maxCountLimit());
        }

        // case 1 : Need to display several pages
        // case 2 : Query asked for explicit large page : allow it. Ideally, should be only allowed for internal query (not from user directly)
        return filter.getPageSize() < mongoDBConfig.maxCountLimit() ?
                countOptions.limit(filter.getPageSize() * mongoDBConfig.maxPageCountLimit()) :
                countOptions.limit(filter.getPageSize());
    }

    /**
     * Count models based on the provided filter within a client session.
     *
     * @param session The MongoDB client session.
     * @param query   The search query
     * @return The number of models matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     */
    private long countDocuments(ClientSession session, F query, CountOptions countOptions) throws MongoException {

        List<Bson> filters = query.getFilterList();

        // no filter : rely on server stats document count
        // if a specific count limit was specified, then don't rely on estimatedDocumentCount which will return the full collection length
        if (filters.isEmpty() && countOptions.getLimit() == 0) {
            return collection.estimatedDocumentCount(
                    new EstimatedDocumentCountOptions().maxTime(mongoDBConfig.readTimeoutMs(), TimeUnit.MILLISECONDS)
            );
        }

        Instant operationStart = mongoLogger.logOperationStart(COUNT, FILTER, query.getFilterBsonStr());
        long count = session == null ?
                collection.countDocuments(query.getFilterBson(), countOptions) :
                collection.countDocuments(session, query.getFilterBson(), countOptions);

        mongoLogger.logOperationOk(COUNT, operationStart, FILTER, query.getFilterBsonStr(), COUNT_RESULT, count);
        return count;
    }

    /**
     * @apiNote This implementation use {@link CountOptions} returned by the {@link #getDefaultCountOptions(F)} method if {@code countOptions == null}
     * @see #countDocuments(ClientSession, MongoSearchFilter, CountOptions)
     */
    @Override
    public final long count(ClientSession session, @NotNull F filter, CountOptions countOptions) throws MongoException {
        Objects.requireNonNull(filter);
        initializeSearchFilter(filter);
        return countDocuments(session, filter, countOptions == null ? getDefaultCountOptions(filter) : countOptions);
    }

    private void initializeSearchFilter(F filter){
        List<Bson> bsonFilters = getBsonFilters(filter);
        filter.setFilterList(bsonFilters);
        Bson bsonFilter = getBsonFilter(bsonFilters, filter.isLogicalAnd());
        filter.setFilterBson(bsonFilter);
        filter.setFilterBsonStr(bsonFilter.toString());
    }

    private <T_CONVERTED> FindIterable<T> getFindIterable(MongoSearchQuery<T, F, T_CONVERTED> query) {

        F filter = query.getFilter();
        int limit = filter.getPageSize();

        // just ask the database for one more element in order to check if there are other result after the current page
        if (query.getPaginationStrategy() == HAS_NEXT_PAGE) {
            limit++;
        }

        // search with filter and session
        FindIterable<T> queryResult = query.getSession() == null ?
                collection.find(query.getFilter().getFilterBson()) :
                collection.find(query.getSession(), query.getFilter().getFilterBson());

        // apply sort and pagination
        queryResult.sort(buildSort(filter.getOrderByList()))
                .skip(filter.getPage() * filter.getPageSize())
                .limit(limit)
                .maxTime(mongoDBConfig.readTimeoutMs(), TimeUnit.MILLISECONDS);

        // apply projection
        if (query.getProjection() != null) {
            queryResult.projection(query.getProjection());
        }

        return queryResult;
    }

    @Override
    public final ListWithPagination<T> searchWithPagination(@NotNull F filter) throws MongoException {
        return searchWithPagination(new MongoSearchQuery<T, F, T>()
                .setFilter(filter)
                .setConvertFunction(Function.identity())
        );
    }

    @Override
    public final <T_CONVERTED> ListWithPagination<T_CONVERTED> searchWithPagination(MongoSearchQuery<T, F, T_CONVERTED> query) throws MongoException {

        // compute Bson filter filter
        F filter = query.getFilter();
        initializeSearchFilter(filter);

        Instant operationStart = mongoLogger.logOperationStart(SEARCH, FILTER, filter.getFilterBsonStr());

        long totalCount = 0;
        int countLimit = 0;
        List<T_CONVERTED> results;

        if (query.getPaginationStrategy() == HAS_NEXT_PAGE) {
            results = new ArrayList<>(filter.getPageSize());
        } else {
            // run count query
            CountOptions countOptions = query.getCountOptions() == null ? getDefaultSearchCountOptions(filter) : query.getCountOptions();
            totalCount = countDocuments(query.getSession(), filter, countOptions);

            // return empty list, keep information about the pagination used
            if (totalCount == 0) {
                return new ListWithPagination<>(filter.getPage(), filter.getPageSize());
            }
            results = new ArrayList<>((int) (Math.min(totalCount, filter.getPageSize())));
            countLimit = countOptions.getLimit();
        }

        // Run the search query and get cursor/iterable over database results
        ListWithPagination<T_CONVERTED> paginatedIt;
        FindIterable<T> dbResults = getFindIterable(query);

        try (MongoCursor<T> dbResultsIt = dbResults.iterator()) {

            // Iterate over MongoDB result and convert result on the fly before collect them inside a List
            // if !dbResultsIt.hasNext() -> less result that the specified page size
            for (int i = 0; i < filter.getPageSize() && dbResultsIt.hasNext(); i++) {
                results.add(query.getConvertFunction().apply(dbResultsIt.next()));
            }

            // Return the list and just indicate if there exists one document on the next page
            if (query.getPaginationStrategy() == HAS_NEXT_PAGE) {
                paginatedIt = new ListWithPagination<>(results, filter.getPage(), filter.getPageSize(), dbResultsIt.hasNext());
            } else {
                // return the list with information about counted element and the used limit
                paginatedIt = new ListWithPagination<>(results, filter.getPage(), filter.getPageSize(), totalCount, countLimit);
            }
        }
        paginatedIt.setPaginationStrategy(query.getPaginationStrategy());

        mongoLogger.logOperationOk(SEARCH, operationStart, FILTER, filter.getFilterBsonStr(), RESULT_COUNT, results.size());
        return paginatedIt;
    }

    @Override
    public final StreamWithPagination<T> searchAsStreamWithPagination(F filter) throws MongoException {
        return searchAsStreamWithPagination(new MongoSearchQuery<T, F, T>()
                .setFilter(filter)
                .setConvertFunction(Function.identity())
        );
    }

    @Override
    public final <T_RESULT> StreamWithPagination<T_RESULT> searchAsStreamWithPagination(MongoSearchQuery<T, F, T_RESULT> query) throws MongoException {

        // compute Bson filter filter+
        F filter = query.getFilter();
        initializeSearchFilter(filter);

        Instant operationStart = mongoLogger.logOperationStart(SEARCH_STREAM, FILTER, filter.getFilterBsonStr());

        long totalCount = 0;
        int countLimit = 0;
        if (query.getPaginationStrategy() == COUNT_QUERY_BEFORE_SEARCH) {
            // run count query
            CountOptions countOptions = query.getCountOptions() == null ? getDefaultSearchCountOptions(filter) : query.getCountOptions();
            totalCount = countDocuments(query.getSession(), filter, countOptions);

            // return empty Stream, keep information about the pagination used
            if (totalCount == 0) {
                return new StreamWithPagination<>(filter.getPage(), filter.getPageSize());
            }
            countLimit = countOptions.getLimit();
        }

        // Run the search query and get cursor/iterable over database results
        StreamWithPagination<T_RESULT> paginatedIt;
        FindIterable<T> dbResults = getFindIterable(query);

        Stream<T_RESULT> results = StreamSupport
                .stream(dbResults.spliterator(), false)
                .map(result -> query.getConvertFunction().apply(result));

        if (query.getPaginationStrategy() == HAS_NEXT_PAGE) {
            paginatedIt = new StreamWithPagination<>(results, filter.getPage(), filter.getPageSize(), 0);
        } else {
            // return the list with information about counted element and the used limit
            paginatedIt = new StreamWithPagination<>(results, filter.getPage(), filter.getPageSize(), totalCount, countLimit);
        }
        paginatedIt.setPaginationStrategy(query.getPaginationStrategy());

        mongoLogger.logOperationOk(SEARCH_STREAM, operationStart, FILTER, filter.getFilterBsonStr(), RESULT_COUNT, totalCount);
        return paginatedIt;
    }

    @Override
    public List<URI> distinctUris(ClientSession session, @NotNull F filter) throws MongoException {
        Objects.requireNonNull(filter);
        return distinct(session, idField(), URI.class, filter);
    }

    @Override
    public List<URI> distinctUris(@NotNull F filter) throws MongoException {
        return distinctUris(null, filter);
    }

    /**
     * @param id the id used for unique filter
     * @return an equality filter between the given id and the {{@link #idField()}}
     */
    protected Bson getIdFilter(URI id) {
        return Filters.eq(idField(), id);
    }

    /**
     * @param searchQuery the Search filter
     * @return the List of {@link Bson} document according the given {@code searchQuery}
     * @apiNote By default, this method build filters according each default {@link MongoSearchFilter} fields (if not null or empty) :
     * <ul>
     *     <li>{@link MongoSearchFilter#getUri()} : use the {@link #getIdFilter(URI)}</li>
     *     <li>{@link MongoSearchFilter#getIncludedUris()} : use a {@link Filters#in(String, Object[])} filter with the {@link #idField()}</li>
     *     <li>{@link MongoSearchFilter#getRdfTypes()} : use a {@link Filters#in(String, Object[])} filter with the {@link MongoModel#TYPE_FIELD}</li>
     * </ul>
     * <p>
     * In order to allow search with new fields inside specialized class, you have to @override this method
     * by defined which MongoDB filters you want to build according {@link MongoSearchFilter} specializations fields.
     * If you want to conserve the default semantics described just below, you just have to call {@code super.getBsonFilters(searchQuery)}
     * and add your new filters inside the returned list, and to return this list.
     * If you don't want to use these filters, just build and return a new list
     * </p>
     */
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

    private @NotNull Bson filterToBson(@NotNull F searchFilter) {
        List<Bson> bsonFilters = getBsonFilters(searchFilter);
        return getBsonFilter(bsonFilters, searchFilter.isLogicalAnd());
    }

    /**
     * @param bsonFilters the list of {@link Bson} filter
     * @param logicalAnd  if true, apply a logical AND between each  {@link Bson} filter, else a logical OR
     * @return <ul>
     * <li>if {@code bsonFilters} is empty, an empty {@link Bson}</li>
     * <li>If {@code bsonFilters} has only one filter, this unique filter </li>
     * <li>Else a logical AND or a logical OR between each filters</li>
     * </ul>
     */
    private Bson getBsonFilter(List<Bson> bsonFilters, boolean logicalAnd) {
        if (bsonFilters.isEmpty()) {
            return Filters.empty();
        } else if (bsonFilters.size() == 1) {
            return bsonFilters.get(0);
        }

        // append each filter with a logical AND or logical OR
        if (logicalAnd) {
            return Filters.and(bsonFilters);
        } else {
            return Filters.or(bsonFilters);
        }
    }

    @Override
    public <T_RESULT> List<T_RESULT> distinct(ClientSession session, @NotNull String field, @NotNull Class<T_RESULT> resultClass, @NotNull F filter) {

        if (StringUtils.isEmpty(field)) {
            throw new IllegalArgumentException("distinct field can't be null or empty");
        }

        // Compute DistinctIterable
        Bson bsonFilter = filterToBson(filter);
        String bsonFilterStr = bsonFilter.toString();

        Instant operationStart = mongoLogger.logOperationStart(DISTINCT, DISTINCT_FIELD, field, FILTER, bsonFilterStr);

        DistinctIterable<T_RESULT> queryResult = session == null ?
                collection.distinct(field, bsonFilter, resultClass) :
                collection.distinct(session, field, bsonFilter, resultClass);

        // Fetch results from database
        List<T_RESULT> results = new ArrayList<>();
        for (T_RESULT res : queryResult) {
            results.add(res);
        }

        mongoLogger.logOperationOk(DISTINCT, operationStart, DISTINCT_FIELD, field, FILTER, bsonFilterStr);
        return results;
    }

    @Override
    public List<T> aggregate(List<Bson> pipeline) {
        return aggregate(pipeline, modelClass);
    }

    @Override
    public Stream<T> aggregateAsStream(List<Bson> pipeline) {
        return aggregateAsStream(pipeline, modelClass);
    }

    @Override
    public <T_RESULT> List<T_RESULT> aggregate(List<Bson> aggregationPipeline, Class<T_RESULT> tResultClass) {
        String pipelineStr = aggregationPipeline.toString();
        Instant operationStart = mongoLogger.logOperationStart(AGGREGATE, AGGREGATION_PIPELINE, pipelineStr);

        AggregateIterable<T_RESULT> aggregate = collection.aggregate(aggregationPipeline, tResultClass);
        mongoLogger.logOperationOk(AGGREGATE, operationStart, AGGREGATION_PIPELINE, aggregationPipeline);

        List<T_RESULT> results = new ArrayList<>();
        for (T_RESULT res : aggregate) {
            results.add(res);
        }
        mongoLogger.logOperationOk(AGGREGATE, operationStart, AGGREGATION_PIPELINE, pipelineStr);

        return results;
    }

    @Override
    public <T_RESULT> Stream<T_RESULT> aggregateAsStream(List<Bson> aggregationPipeline, Class<T_RESULT> tResultClass) {
        String pipelineStr = aggregationPipeline.toString();
        Instant operationStart = mongoLogger.logOperationStart(AGGREGATE, AGGREGATION_PIPELINE, pipelineStr);
        AggregateIterable<T_RESULT> aggregate = collection.aggregate(aggregationPipeline, tResultClass);
        mongoLogger.logOperationOk(AGGREGATE, operationStart, AGGREGATION_PIPELINE, pipelineStr);

        return StreamSupport.stream(aggregate.spliterator(), false);
    }

    @Override
    public <T_RESULT> ListWithPagination<T_RESULT> distinctWithPagination(ClientSession session, @NotNull String distinctField, @NotNull Class<T_RESULT> resultClass, @NotNull F filter) {

        Objects.requireNonNull(filter);

        Bson bsonFilter = filterToBson(filter);

        List<Bson> aggregatePipeline = new ArrayList<>();

        // filtering with match
        aggregatePipeline.add(Aggregates.match(bsonFilter));

        // distinct field with match
        aggregatePipeline.add(Aggregates.group("$" + distinctField));

        // sort
        List<OrderBy> orderByList = CollectionUtils.isEmpty(filter.getOrderByList()) ? List.of(DEFAULT_ORDER_BY) : filter.getOrderByList();
        Document order = buildSort(orderByList);
        aggregatePipeline.add(Aggregates.sort(order.toBsonDocument()));

        // pagination : skip and limit, check if > 0 else error is thrown by the aggregate builder
        if (filter.getPageSize() > 0) {
            aggregatePipeline.add(Aggregates.limit(filter.getPageSize()));
            if (filter.getPage() > 0) {
                aggregatePipeline.add(Aggregates.skip(filter.getPage() * filter.getPageSize()));
            }
        }

        // Use a list which maintains the same order as the insertion order
        // The order is computed by MongoDB and the results are returned in this order
        String bsonFilterStr = bsonFilter.toString();

        Instant operationStart = mongoLogger.logOperationStart(DISTINCT, FILTER, bsonFilterStr, DISTINCT_FIELD, distinctField);
        List<T_RESULT> distinctValues = new ArrayList<>();

        // map aggregation results a Document, since the aggregation will produce a different document schema
        AggregateIterable<Document> aggregateIt = session == null ?
                collection.aggregate(aggregatePipeline, Document.class) :
                collection.aggregate(session, aggregatePipeline, Document.class);

        // Transform document on the fly and add inside list
        // the distinct field value is the value of each aggregated document with the key "_id"
        aggregateIt.map(document -> document.get("_id", resultClass))
                .forEach(extracted -> {
                    if (extracted != null) {
                        distinctValues.add(extracted);
                    }
                });
        mongoLogger.logOperationOk(DISTINCT, operationStart, FILTER, bsonFilterStr, RESULT_COUNT, distinctValues.size());

        return new ListWithPagination<>(distinctValues, filter.getPage(), filter.getPageSize(), distinctValues.size());
    }

    @Override
    public <T_RESULT, T_JOINED> List<T_RESULT> lookupAggregation(ClientSession session, F filter,
                                                                 @NotNull String lookupField,
                                                                 @NotNull String lookupCollectionName,
                                                                 @NotNull Class<T_JOINED> lookupClass,
                                                                 @NotNull Function<T_JOINED, T_RESULT> convertFunction) {

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

        // run aggregation

        String pipelineStr = pipeline.toString();

        Instant operationStart = mongoLogger.logOperationStart(AGGREGATE, AGGREGATION_PIPELINE, pipelineStr);
        List<T_RESULT> results = new ArrayList<>();
        AggregateIterable<T_JOINED> aggregateIt = session == null ?
                collection.aggregate(pipeline, lookupClass) :
                collection.aggregate(session, pipeline, lookupClass);

        // Collect results from DB
        aggregateIt.map(convertFunction::apply).forEach(results::add);
        mongoLogger.logOperationOk(AGGREGATE, operationStart, AGGREGATION_PIPELINE, pipelineStr);

        return results;
    }

    /**
     * @return the {@link MongoCollection} used by this DAO
     */
    public final MongoCollection<T> getCollection() {
        return collection;
    }

    /**
     * @param orderByList the List of {@link OrderBy} to convert
     * @return a {@link Document} which define sorts (field and order) to pass to a mongodb find query
     */
    public Document buildSort(List<OrderBy> orderByList) {
        Document sort = new Document();
        if (CollectionUtils.isEmpty(orderByList)) {
            addDefaultSort(sort);
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

    /**
     * Append a Default Sort in case where the List of {@link OrderBy} passed to {@link #buildSort(List)} is empty
     */
    protected void addDefaultSort(Document sort){
        // no default sort
    }

}

