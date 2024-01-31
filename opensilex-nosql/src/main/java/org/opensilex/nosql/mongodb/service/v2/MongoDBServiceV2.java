//******************************************************************************
//                         MongoDBService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb.service.v2;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.nosql.mongodb.codec.ObjectCodec;
import org.opensilex.nosql.mongodb.codec.URICodec;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.opensilex.utils.LogFilter.LOG_TYPE;

@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBServiceV2 extends BaseService {

    public static final String DEFAULT_SERVICE = "mongodb2";

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBServiceV2.class);
    public static final String CHECK_MONGO_SERVER_CONNECTION_LOG_MSG = "MONGO_SERVER_CHECK_CONNECTION";
    public static final String MONGO_CREATE_LOG_MSG = "MONGO_CREATE";
    public static final String MONGO_CHECK_URI_LOG_MSG = "MONGO_CHECK_URI";

    private static final Bson EMPTY_PROJECTION = Projections.fields(Projections.include(MongoModel.MONGO_ID_FIELD));


    private final String dbName;
    private MongoClient mongoClient;
    private MongoDatabase db;
    private URI generationPrefixURI;
    private static String defaultTimezone;

    public MongoDBServiceV2(MongoDBConfig config) {
        super(config);
        dbName = config.database();
        defaultTimezone = config.timezone();
    }

    @Override
    public void startup() throws OpenSilexModuleNotFoundException, IOException {
        mongoClient = buildMongoDBClient();
        generationPrefixURI = getGenerationPrefixURI();
        db = mongoClient.getDatabase(dbName);

        // check connection and close connection in case of connection/authentication error
        try {
            if (!getOpenSilex().isTest() && !getOpenSilex().isReservedProfile()) {
                checkConnection(getImplementedConfig());
            }
        } catch (MongoTimeoutException | MongoSecurityException e) {
            mongoClient.close();
            throw e;
        }
    }

    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Test if the connection to the MongoDB server is OK
     *
     * @param config MongoDB configuration
     * @throws MongoTimeoutException  If the server is inaccessible after a timeout (in milliseconds) defined by {@link MongoDBConfig#serverSelectionTimeout()}
     * @throws MongoSecurityException If the execution of this command is unauthorized by the MongoDB server
     * @see <a href="https://www.mongodb.com/docs/manual/reference/command/ping/">MongoDB ping</a>
     */
    private void checkConnection(MongoDBConfig config) throws MongoTimeoutException, MongoSecurityException {

        // check that network connection is OK by running ping command, throw MongoTimeoutException else
        final Bson pingCommand = new BsonDocument("ping", new BsonInt32(1));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Check MongoDB connection, timeout: {} {}", config.connectTimeoutMs(), kv(LOG_TYPE, CHECK_MONGO_SERVER_CONNECTION_LOG_MSG));
        }

        db.runCommand(pingCommand);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("MongoDB connection OK {}", kv(LOG_TYPE, CHECK_MONGO_SERVER_CONNECTION_LOG_MSG));
        }
    }

    /**
     * @return a new {@link ClientSession}. A transaction is started in the context of this session
     * @apiNote <ul>
     * <li>You must use a new session for each operation which involve a transaction, and NOT share this session across
     * concurrent operation. Indeed {@link ClientSession} is not thread safe ({@link MongoClient#startSession()} method is thread-safe)
     * </li>
     * <li>This session must be closed by calling {@link ClientSession#close()}</li>
     * </ul>
     * @see MongoClient#startSession()
     * @see ClientSession#startTransaction()
     */
    public ClientSession newSession() {
        return mongoClient.startSession();
    }

    public void readOperationWithSession(ThrowingConsumer<ClientSession, Exception> operationInTrx) throws Exception {
        try (ClientSession session = newSession()) {
            operationInTrx.accept(session);
        }
    }

    /**
     * Execute the given operation with transaction management
     *
     * @param operationInTrx A Consumer which can execute database query by using a ClientSession
     * @throws MongoException If an error occurs during transaction management or inside operationInTrx execution
     */
    public void runTransaction(ThrowingConsumer<ClientSession, Exception> operationInTrx) throws MongoException {
        computeTransaction(session -> {
            operationInTrx.accept(session);
            return null;
        });
    }

    /**
     * Execute the given operation with transaction management and return results
     *
     * @param operationInTrx A Consumer which can execute database query by using a ClientSession
     * @param <R>            : The type of result returned by the operation
     * @throws MongoException If an error occurs during transaction management or inside operationInTrx execution
     */
    public <R> R computeTransaction(ThrowingFunction<ClientSession, R, Exception> operationInTrx) throws MongoException {

        ClientSession session = newSession();
        session.startTransaction();
        try {
            R result = operationInTrx.apply(session);
            if (session.hasActiveTransaction()) {
                session.commitTransaction();
            }
            return result;
        } catch (Exception e) {
            if (session.hasActiveTransaction()) {
                session.abortTransaction();
            }
            throw MongoException.fromThrowableNonNull(e);
        } finally {
            session.close();
        }
    }


    public URI getGenerationPrefixURI() throws OpenSilexModuleNotFoundException {
        return getOpenSilex().getModuleByClass(SPARQLModule.class).getGenerationPrefixURI();
    }

    public static String getDefaultTimeZone() {
        return defaultTimezone;
    }

    public MongoDBConfig getImplementedConfig() {
        return (MongoDBConfig) this.getConfig();
    }

    public MongoDatabase getDatabase() {
        return this.db;
    }

    /**
     * Creates and inserts a single document into the specified MongoCollection.
     *
     * @param <T>                 The type extending MongoModel
     * @param instance            The instance of type T to be inserted
     * @param collection          The MongoCollection where the document will be inserted
     * @param uriGenerationPrefix The prefix for generating a unique URI
     * @param session             The ClientSession for handling the transaction (can be null)
     * @return InsertOneResult    The result of the insertion operation
     * @throws URISyntaxException               If there's an issue with URI syntax
     * @throws NoSQLAlreadyExistingUriException If the URI already exists in the collection
     */
    public <T extends MongoModel> InsertOneResult create(T instance, MongoCollection<T> collection, String uriGenerationPrefix, ClientSession session) throws URISyntaxException, NoSQLAlreadyExistingUriException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MongoDB create, collection: {} {}", collection.getNamespace().getCollectionName(), kv(LOG_TYPE, MONGO_CREATE_LOG_MSG));
        }
        if (instance.getUri() == null) {
            generateUniqueUriIfNullOrValidateCurrent(instance, true, uriGenerationPrefix, collection);
        }

        // custom logic which imply to let caller handle the session and transaction
        if (session != null) {
            return collection.insertOne(session, instance);
        } else {
            // no need to handle transaction in case of only one document insert, if the insert fail, then the collection is unchanged
            return collection.insertOne(instance);
        }
    }

    /**
     * Creates and inserts multiple documents into the specified MongoCollection.
     *
     * @param <T>           The type extending MongoModel
     * @param instances     The list of instances to be inserted
     * @param collection    The MongoCollection where the documents will be inserted
     * @param session       The ClientSession for handling the transaction (can be null)
     * @param prefix        The prefix for generating a unique URI
     * @param checkUris     Boolean indicating whether to check URIs before insertion
     * @param checkUriExist Boolean indicating whether to check the existence of URIs before insertion
     * @return InsertManyResult The result of the insertion operation
     * @throws MongoException                   If a MongoDB-related error occurs
     * @throws URISyntaxException               If there's an issue with URI syntax
     * @throws NoSQLAlreadyExistingUriException If the URI already exists in the collection
     */
    public <T extends MongoModel> InsertManyResult createAll(List<T> instances, MongoCollection<T> collection, ClientSession session, String prefix, boolean checkUris, boolean checkUriExist) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MongoDB create, collection: {} {}", collection.getNamespace().getCollectionName(), kv(LOG_TYPE, MONGO_CREATE_LOG_MSG));
        }

        if (checkUris) {
            for (T instance : instances) {
                if (instance.getUri() == null) {
                    generateUniqueUriIfNullOrValidateCurrent(instance, checkUriExist, prefix, collection);
                }
            }
        }

        // if a session is provided, simply use it
        if (session != null) {
            return collection.insertMany(session, instances);
        }

        // Transaction handling for data integrity since insertMany() can update several documents
        return computeTransaction(createSession -> collection.insertMany(createSession, instances));
    }

    /**
     * Overloaded method for creating and inserting multiple documents into the specified MongoCollection.
     * This method omits URI checking.
     *
     * @param <T>        The type extending MongoModel
     * @param instances  The list of instances to be inserted
     * @param collection The MongoCollection where the documents will be inserted
     * @param session    The ClientSession for handling the transaction (can be null)
     * @return InsertManyResult The result of the insertion operation
     * @throws MongoException                   If a MongoDB-related error occurs
     * @throws URISyntaxException               If there's an issue with URI syntax
     * @throws NoSQLAlreadyExistingUriException If the URI already exists in the collection
     */
    public <T extends MongoModel> InsertManyResult createAll(List<T> instances, MongoCollection<T> collection, ClientSession session) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException {
        return createAll(instances, collection, session, null, false, false);
    }

    /**
     * Finds a document in the specified MongoCollection based on the given URI field.
     *
     * @param <T>        The type of the document
     * @param collection The MongoCollection to search in
     * @param uri        The URI used to find the document
     * @return T           The document found (if exists)
     * @throws NoSQLInvalidURIException If the provided URI is invalid
     */
    public <T> T findByURI(MongoCollection<T> collection, URI uri) throws NoSQLInvalidURIException {
        return findByURI(collection, uri, MongoModel.URI_FIELD);
    }

    /**
     * @param collection the collection on which find an instance with the given uri
     * @param uri        URI of the instance to find
     * @param uriField   the name of the uri field
     * @param <T>        the instance class
     * @return the instance which has the given uri
     * @throws NoSQLInvalidURIException if no instance is found
     */
    public <T> T findByURI(MongoCollection<T> collection, URI uri, String uriField) throws NoSQLInvalidURIException {
        return findByURI(null, collection, uri, uriField);
    }

    /**
     * Finds a document in the specified MongoCollection based on the provided URI field.
     *
     * @param <T>        The type of the document
     * @param session    The ClientSession for handling the transaction (can be null)
     * @param collection The MongoCollection to search in
     * @param uri        The URI used to find the document
     * @param uriField   The field in the document containing the URI
     * @return T          The document found (if exists)
     * @throws NoSQLInvalidURIException If the provided URI is invalid
     */
    public <T> T findByURI(ClientSession session, MongoCollection<T> collection, URI uri, String uriField) throws NoSQLInvalidURIException {

        T instance = session == null ?
                collection.find(eq(uriField, uri)).first() :
                collection.find(session, eq(uriField, uri)).first();

        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        }
        return instance;
    }

    /**
     * Finds multiple documents in the specified MongoCollection based on a collection of URIs.
     *
     * @param <T>        The type extending MongoModel
     * @param collection The MongoCollection to search in
     * @param uris       The collection of URIs to search for documents
     * @return List<T>     The list of documents found (if any)
     */
    public <T extends MongoModel> List<T> findByURIs(MongoCollection<T> collection, Collection<URI> uris) {
        FindIterable<T> queryResult = findIterableByURIs(MongoModel.URI_FIELD, collection, uris.stream());

        List<T> instances = new ArrayList<>(uris.size());
        for (T res : queryResult) {
            instances.add(res);
        }
        return instances;
    }

    /**
     * @param fieldName  name of the field on which apply {@link Filters#in(String, Object[])}
     * @param collection mongo collection
     * @param uris       {@link Stream} of URIS to pass to {@link Filters#in(String, Object[])}
     * @param <T>        {@link MongoModel} type
     * @return a {@link FindIterable} of T models from mongodb. Returned model must have the fieldName attribute and the value for this attribute must be in uris
     */
    public <T extends MongoModel> FindIterable<T> findIterableByURIs(String fieldName, MongoCollection<T> collection, Stream<URI> uris) {
        Bson filter = Filters.in(fieldName, uris::iterator);
        return collection.find(filter);
    }

    /**
     * @param collection the Collection on which find the instance
     * @param uri        URI of the instance to check
     * @param <T>        the instance class
     * @return if an instance with the given uri exists
     */
    public <T> boolean uriExists(ClientSession session, MongoCollection<T> collection, URI uri) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MongoDB uriExists {} {} {}", kv(LOG_TYPE, MONGO_CHECK_URI_LOG_MSG), kv("collection", collection.getNamespace().getCollectionName()), kv("uri", uri));
        }
        FindIterable<T> findIt = session == null ?
                collection.find(eq(MongoModel.URI_FIELD, uri)) :
                collection.find(session, eq(MongoModel.URI_FIELD, uri));

        // Use a minimal projection in order to avoid the fetching of fields which are useless here
        return findIt.projection(EMPTY_PROJECTION).first() != null;
    }

    /**
     * @param collection the collection on which find an instance with the given uri
     * @param uri        URI of the instance to check
     * @param uriField   the name of the uri field
     * @param <T>        the instance class
     * @return if an instance with the given uri exists
     */
    public <T> boolean uriExists(MongoCollection<T> collection, URI uri, String uriField) {
        try {
            findByURI(collection, uri, uriField);
            return true;
        } catch (NoSQLInvalidURIException ex) {
            return false;
        }
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

    /**
     * Finds documents in the specified MongoCollection with pagination support.
     *
     * @param <T>         The type extending MongoModel
     * @param collection  The MongoCollection to search in
     * @param filter      The filter to apply during the search
     * @param projection  The projection to apply (can be null)
     * @param orderByList The list of fields to order by
     * @param page        The page number for pagination
     * @param pageSize    The size of each page for pagination
     * @param session     The ClientSession for handling the transaction (can be null)
     * @return Map.Entry<FindIterable < T>, Long>    The query results and the total count of results
     */
    public <T extends MongoModel> Map.Entry<FindIterable<T>, Long> findWithPagination(MongoCollection<T> collection, Bson filter, Bson projection, List<OrderBy> orderByList, int page, int pageSize, ClientSession session) {

        long resultsNumber = collection.countDocuments(filter);

        // call isDebugEnabled before displaying log, since LogOrderList is a function. We don't want to call this method, is DEBUG logging is not enabled
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[MONGO_SEARCH_WITH_PAGINATION] : { collection: {}, order: {}, filter: {}, results_count: {}}", collection.getNamespace().getCollectionName(), logOrderList(orderByList), filter, resultsNumber);
        }

        if (resultsNumber == 0) {
            return null;
        }
        FindIterable<T> queryResult = session == null ?
                collection.find(filter) :
                collection.find(session, filter);

        queryResult.sort(buildSort(orderByList))
                .skip(page * pageSize)
                .limit(pageSize);

        if (projection != null) {
            queryResult.projection(projection);
        }

        return new AbstractMap.SimpleImmutableEntry<>(queryResult, resultsNumber);
    }

    /**
     * Searches documents in the specified MongoCollection with pagination support.
     *
     * @param <T>         The type extending MongoModel
     * @param collection  The MongoCollection to search in
     * @param filter      The filter to apply during the search
     * @param projection  The projection to apply (can be null)
     * @param orderByList The list of fields to order by
     * @param page        The page number for pagination
     * @param pageSize    The size of each page for pagination
     * @param session     The ClientSession for handling the transaction (can be null)
     * @return ListWithPagination<T>    The paginated list of search results
     */
    public <T extends MongoModel> @NotNull ListWithPagination<T> searchWithPagination(MongoCollection<T> collection, Bson filter, Bson projection, List<OrderBy> orderByList, int page, int pageSize, ClientSession session) {

        Map.Entry<FindIterable<T>, Long> resultAndCount = findWithPagination(collection, filter, projection, orderByList, page, pageSize, session);
        if (resultAndCount == null) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        // iterate over MongoDB result cursor and collect inside a List
        List<T> results = new ArrayList<>(pageSize);
        CollectionUtils.addAll(results, resultAndCount.getKey());

        return new ListWithPagination<>(results, page, pageSize, resultAndCount.getValue().intValue());
    }

    /**
     * Counts the number of documents in the specified MongoCollection based on the provided filter.
     *
     * @param <T>        The type of the document
     * @param session    The ClientSession for handling the transaction (can be null)
     * @param collection The MongoCollection to count documents in
     * @param filter     The filter to apply during counting
     * @return long        The count of documents based on the filter
     */
    public <T> long count(ClientSession session, MongoCollection<T> collection, Bson filter) {

        LOGGER.debug("[MONGO_COUNT] : { collection: {},filter: {}}", collection.getNamespace().getCollectionName(), filter);
        return session == null ?
                collection.countDocuments(filter) :
                collection.countDocuments(session, filter);
    }

    /**
     * Searches documents in the specified MongoCollection based on the provided filter and ordering.
     *
     * @param <T>         The type of the document
     * @param collection  The MongoCollection to search in
     * @param filter      The filter to apply during the search
     * @param orderByList The list of fields to order by
     * @return List<T>       The list of documents found
     */
    public <T> List<T> search(MongoCollection<T> collection, Document filter, List<OrderBy> orderByList) {

        Document sort = buildSort(orderByList);
        FindIterable<T> queryResult = collection.find(filter).sort(sort);

        List<T> results = new ArrayList<>();
        CollectionUtils.addAll(results, queryResult);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[MONGO_SEARCH_WITH_PAGINATION] : { collection: {}, order: {}, filter: {}, results_count: {}}", collection.getNamespace().getCollectionName(), logOrderList(orderByList), filter, results.size());
        }

        return results;
    }

    /**
     * Retrieves distinct values for a specific field from documents in the specified MongoCollection.
     *
     * @param <T>            The type of the distinct field
     * @param field          The field for which distinct values are sought
     * @param resultClass    The class type of the resulting distinct values
     * @param collectionName The name of the collection
     * @param filter         The filter to apply during the distinct operation
     * @return Set<T>          The set of distinct values for the specified field
     */
    public <T> Set<T> distinct(String field, Class<T> resultClass, String collectionName, Bson filter) {

        MongoCollection<?> collection = db.getCollection(collectionName);
        return distinct(field, resultClass, collection, filter, null);
    }

    /**
     * Retrieves distinct values for a specific field from documents in the specified MongoCollection with session support.
     *
     * @param <T>         The type of the distinct field
     * @param field       The field for which distinct values are sought
     * @param resultClass The class type of the resulting distinct values
     * @param collection  The MongoCollection to perform the distinct operation
     * @param filter      The filter to apply during the distinct operation
     * @param session     The ClientSession for handling the transaction (can be null)
     * @return Set<T>          The set of distinct values for the specified field
     */
    public <T> Set<T> distinct(String field, Class<T> resultClass, MongoCollection<?> collection, Bson filter, ClientSession session) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[MONGO_DISTINCT] : { collection: {}, filter: {}, field: {}, resultClass: {} }", collection.getNamespace().getCollectionName(), filter, field, resultClass.getSimpleName());
        }

        Set<T> results = new HashSet<>();
        DistinctIterable<T> queryResult = session == null ?
                collection.distinct(field, filter, resultClass) :
                collection.distinct(session, field, filter, resultClass);

        for (T res : queryResult) {
            results.add(res);
        }

        return results;
    }

    /**
     * @param collection        mongo collection
     * @param distinctField     Name of the field for which we want to retrieve distinct value (can be an embedded field)
     * @param documentExtractor a function which return the value to extract from any {@link Document} which are returned
     *                          by the aggregation
     * @param filter            an optional pre-filter on documents
     * @param orderByList       list of sort
     * @param page              page
     * @param pageSize          page size
     * @param <T>               the type of unique values to retrieve
     * @return a Set of unique value for a given field from a collection
     * @see <a href="https://www.mongodb.com/docs/manual/reference/operator/aggregation/group/#std-label-aggregation-group-distinct-values">
     * Distinct with aggregation pipeline
     */
    public <T> Set<T> distinctWithPagination(MongoCollection<?> collection, String distinctField, Function<Document, T> documentExtractor, Bson filter, List<OrderBy> orderByList, int page, int pageSize, ClientSession session) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[MONGO_DISTINCT_WITH_PAGINATION] : { collection: {}, order: {}, filter: {}}", collection.getNamespace().getCollectionName(), logOrderList(orderByList), filter);
        }

        List<Bson> aggregatePipeline = new ArrayList<>();

        // filtering with match
        if (filter != null) {
            aggregatePipeline.add(Aggregates.match(filter));
        }

        // distinct field with match
        aggregatePipeline.add(Aggregates.group("$" + distinctField));

        // sort
        Document order = buildSort(orderByList);
        aggregatePipeline.add(Aggregates.sort(order.toBsonDocument()));

        // pagination : skip and limit
        if (pageSize > 0) {
            aggregatePipeline.add(Aggregates.limit(pageSize));
            if (page > 0) {
                aggregatePipeline.add(Aggregates.skip(page * pageSize));
            }
        }

        // Use a set implementation which maintains the same order as the insertion order
        // The order is computed by MongoDB and the results are returned in this order
        // The LinkedHashSet allow to keep the same order from database, when iterating the set
        Set<T> distinct = new LinkedHashSet<>();

        // map aggregation results a Document, since the aggregation will produce a different document schema
        AggregateIterable<Document> aggregateIt = session == null ?
                collection.aggregate(aggregatePipeline, Document.class) :
                collection.aggregate(session, aggregatePipeline, Document.class);

        // Transform document on the fly and add inside set
        aggregateIt.map(documentExtractor::apply).forEach(distinct::add);

        return distinct;
    }

    /**
     * Performs a lookup aggregation operation on the source collection, joining with a destination collection.
     *
     * @param <T_RESULT>        The type of the result after conversion
     * @param <T_JOINED>        The type of the joined collection
     * @param srcCollection     The source MongoCollection to perform aggregation on
     * @param dstCollectionName The name of the destination collection to join with
     * @param lookupField       The field used for lookup operation
     * @param filter            The filter to apply during the aggregation
     * @param orderByList       The list of fields to order by
     * @param lookupClass       The class type of the joined collection
     * @param convertFunction   The function to convert the joined collection to the result
     * @param session           The ClientSession for handling the transaction (can be null)
     * @return List<T_RESULT>  The list of results after the lookup aggregation
     */
    public <T_RESULT, T_JOINED> List<T_RESULT> lookupAggregation(MongoCollection<?> srcCollection, String dstCollectionName, String lookupField, Bson filter, List<OrderBy> orderByList, Class<T_JOINED> lookupClass, Function<T_JOINED, T_RESULT> convertFunction, ClientSession session) {

        List<Bson> pipeline = new ArrayList<>();

        // filtering with match
        if (filter != null) {
            pipeline.add(Aggregates.match(filter));
        }

        // First projection, only restrict to joined fields
        pipeline.add(Aggregates.project(Projections.include(lookupField)));

        // group by foreign key
        String outputFieldName = lookupField + "_join";
        String outputFieldJoinKey = lookupField + "." + MongoModel.URI_FIELD;
        String groupKey = "group";

        pipeline.add(Aggregates.group("$" + outputFieldJoinKey, Accumulators.first(groupKey, "$$ROOT")));

        // replace root
        pipeline.add(Aggregates.replaceRoot("$" + groupKey + "." + lookupField));

        // lookup with destination collection
        pipeline.add(Aggregates.lookup(dstCollectionName, MongoModel.URI_FIELD, MongoModel.URI_FIELD, outputFieldName));

        // unwind
        pipeline.add(Aggregates.unwind("$" + outputFieldName));

        // replace root
        pipeline.add(Aggregates.replaceRoot("$" + lookupField));

        // sort
        if (!CollectionUtils.isEmpty(orderByList)) {
            Document order = buildSort(orderByList);
            pipeline.add(Aggregates.sort(order.toBsonDocument()));
        }

        List<T_RESULT> results = new ArrayList<>();
        AggregateIterable<T_JOINED> aggregateIt = session == null ?
                srcCollection.aggregate(pipeline, lookupClass) :
                srcCollection.aggregate(session, pipeline, lookupClass);

        aggregateIt.map(convertFunction::apply).forEach(results::add);
        return results;
    }

    /**
     * Performs an aggregation operation on the specified collection using the provided aggregation arguments.
     *
     * @param <T>             The type of the instance
     * @param collection      The collection to perform aggregation on
     * @param aggregationArgs The list of aggregation arguments
     * @param instanceClass   The class type of the instance
     * @return Set<T>           The set of results after aggregation
     */
    public <T> Set<T> aggregate(MongoCollection<T> collection, List<Bson> aggregationArgs, Class<T> instanceClass) {
        LOGGER.debug("MONGO SEARCH - Collection : {}, Aggregation pipeline: {}", collection.getNamespace().getCollectionName(), aggregationArgs);

        Set<T> results = new HashSet<>();

        AggregateIterable<T> aggregate = collection.aggregate(aggregationArgs, instanceClass);
        for (T res : aggregate) {
            results.add(res);
        }

        return results;
    }

    /**
     * Deletes a document in the specified collection based on the provided URI.
     *
     * @param <T>        The type extending MongoModel
     * @param collection The MongoCollection to delete from
     * @param session    The ClientSession for handling the transaction (can be null)
     * @param uri        The URI used to identify the document for deletion
     * @return DeleteResult The result of the deletion operation
     * @throws NoSQLInvalidURIException If the provided URI is invalid
     */
    public <T extends MongoModel> DeleteResult delete(MongoCollection<T> collection, ClientSession session, URI uri) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO DELETE - Collection : {}, uri: {}", collection.getNamespace().getCollectionName(), uri);
        return delete(collection, session, uri, MongoModel.URI_FIELD);
    }


    /**
     * delete the instance with the given uri
     *
     * @param collection the collection on which find an instance with the given uri
     * @param uri        URI of the instance to find
     * @param uriField   the name of the uri field
     * @param <T>        the instance class
     * @throws NoSQLInvalidURIException if no instance is found
     */
    public <T extends MongoModel> DeleteResult delete(MongoCollection<T> collection, ClientSession session, URI uri, String uriField) throws NoSQLInvalidURIException {
        DeleteResult deleteResult =  delete(collection, session, eq(uriField, uri));

        if(deleteResult.getDeletedCount() == 0) {
            throw new NoSQLInvalidURIException(uri);
        }
        return deleteResult;
    }

    /**
     * Deletes documents in the specified collection based on the provided filter.
     *
     * @param <T>        The type extending MongoModel
     * @param collection The MongoCollection to delete from
     * @param session    The ClientSession for handling the transaction (can be null)
     * @param filter     The filter to apply during deletion
     * @return DeleteResult The result of the deletion operation
     */
    public <T extends MongoModel> DeleteResult delete(MongoCollection<T> collection, ClientSession session, Bson filter) {

        return session == null ?
                collection.deleteOne(filter) :
                collection.deleteOne(session, filter);
    }


    /**
     * Deletes documents in the specified collection based on a list of URIs.
     *
     * @param <T>        The type extending MongoModel
     * @param collection The MongoCollection to delete from
     * @param session    The ClientSession for handling the transaction (can be null)
     * @param uris       The list of URIs used to identify documents for deletion
     * @return DeleteResult The result of the deletion operation
     * @throws MongoException If an error occurs during the deletion process
     */
    public <T extends MongoModel> DeleteResult delete(MongoCollection<T> collection, ClientSession session, List<URI> uris) throws MongoException {
        LOGGER.debug("MONGO DELETE - Collection : {}, {} uris to delete", collection.getNamespace().getCollectionName(), uris.size());
        return deleteMany(collection, session, Filters.in(MongoModel.URI_FIELD, uris));
    }


    /**
     * @param uriField the name of the ID/URI field of the model
     * @see #update(MongoModel, MongoCollection, Bson, ClientSession)
     */
    public <T extends MongoModel> void update(T newModel, MongoCollection<T> collection, String uriField, ClientSession session) throws NoSQLInvalidURIException {
        update(newModel, collection, Filters.eq(uriField, newModel.getUri()), session);
    }

    /**
     * Update the new model in the given collection according a filter
     *
     * @param newModel   new model to write in database
     * @param collection collection of the current model in database
     * @param filter     additional BSON filter
     * @param session    current session
     * @throws NoSQLInvalidURIException if no previous corresponding model was found in the collection
     */
    public <T extends MongoModel> void update(T newModel, MongoCollection<T> collection, Bson filter, ClientSession session) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO UPDATE - Collection : {}", collection.getNamespace().getCollectionName());

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

    /**
     * @see <a href="https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/crud/write-operations/upsert/">Insert or Update in a Single Operation</a>
     */
    public <T extends MongoModel> UpdateResult upsert(MongoCollection<T> collection, ClientSession session, T newInstance, Bson idFilter) {
        return session == null ?
                collection.replaceOne(idFilter, newInstance, new ReplaceOptions().upsert(true)) :
                collection.replaceOne(session, idFilter, newInstance, new ReplaceOptions().upsert(true));

    }

    /**
     * Updates or deletes a document in the specified collection based on the ID field and URI.
     *
     * @param <T>        The type extending MongoModel
     * @param idField    The field used as an ID in the collection
     * @param uri        The URI used to identify the document
     * @param instance   The instance of type T to update or delete
     * @param collection The MongoCollection to perform the update or delete operation
     * @param session    The ClientSession for handling the transaction (can be null)
     */
    public <T extends MongoModel> void updateOrDelete(String idField, URI uri, T instance, MongoCollection<T> collection, ClientSession session) {
        Bson idFilter = eq(idField, uri);

        if (instance == null) {
            // delete old model if exists
            collection.deleteOne(session, idFilter);
        } else {
            // find the old document which match the id filter
            // if old document found, then it's replaced, else a new document is created by setting upsert(true)
            collection.replaceOne(session, idFilter, instance, new ReplaceOptions().upsert(true));
        }
    }

    public final MongoClient buildMongoDBClient() throws IOException {
        return buildMongoDBClient(getImplementedConfig());
    }

    /**
     * @see <a href="https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/mongoclientsettings/">MongoClient Settings</a>
     */
    public static MongoClient buildMongoDBClient(MongoDBConfig config) throws IOException {

        // Define custom codec registry for URI, Object and GeoJson
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromCodecs(new URICodec(), new ObjectCodec()), CodecRegistries.fromProviders(new GeoJsonCodecProvider(), PojoCodecProvider.builder().register(URI.class).automatic(true).build()));

        // Build client : set server, codec and socket settings
        MongoClientSettings.Builder clientBuilder = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(new ServerAddress(config.host(), config.port())))
                        .serverSelectionTimeout(config.serverSelectionTimeout(), TimeUnit.MILLISECONDS))
                .codecRegistry(codecRegistry)
                .applyToSocketSettings(builder -> builder.connectTimeout(config.connectTimeoutMs(), TimeUnit.MILLISECONDS)
                        .readTimeout(config.readTimeoutMs(), TimeUnit.MILLISECONDS));

        // Handle authentication
        MongoAuthenticationService auth = config.authentication();
        if (auth != null) {
            MongoCredential mongoCredentials = auth.readCredentials();
            clientBuilder.credential(mongoCredentials);
        }
        return MongoClients.create(clientBuilder.build());

    }


    /**
     * Generates a unique URI if null or validates the current URI's existence in the collection.
     *
     * @param <T>                 The type extending MongoModel
     * @param instance            The instance of type T
     * @param checkUriExist       Boolean indicating whether to check URI existence
     * @param instanceClassPrefix The prefix for instance class
     * @param collection          The MongoCollection to check URI existence in
     * @throws NoSQLAlreadyExistingUriException If the URI already exists in the collection
     * @throws URISyntaxException               If there's an issue with URI syntax
     */
    public <T extends MongoModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, boolean checkUriExist, String instanceClassPrefix, MongoCollection<T> collection) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            String prefix = UriBuilder.fromUri(generationPrefixURI).path(instanceClassPrefix).toString();
            uri = instance.generateURI(prefix, instance, retry);
            while (checkUriExist && uriExists(collection, uri, MongoModel.URI_FIELD)) {
                uri = instance.generateURI(prefix, instance, retry++);
            }
            instance.setUri(uri);

        } else if (checkUriExist && uriExists(collection, uri, MongoModel.URI_FIELD)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }

    /**
     * Deletes documents in the specified collection based on a filter with transaction support.
     *
     * @param <T>        The type extending MongoModel
     * @param collection The MongoCollection to delete from
     * @param session    The ClientSession for handling the transaction (can be null)
     * @param filter     The filter to apply during deletion
     * @return DeleteResult The result of the deletion operation
     * @throws MongoException If an exception related to MongoDB occurs
     */
    public <T extends MongoModel> DeleteResult deleteMany(MongoCollection<T> collection, ClientSession session, Bson filter) throws MongoException {

        if (session != null) {
            return collection.deleteMany(filter);
        }

        // if a session is provided use it, else generate a new one for transaction handling,
        // since deleteMany() can update several document inside a collection, transaction handling is always needed
        return computeTransaction(deleteSession -> collection.deleteMany(filter));
    }

    private String logOrderList(List<OrderBy> orders) {
        if (orders == null || orders.isEmpty()) {
            return "No order";
        } else {
            return orders.toString();
        }
    }
}
