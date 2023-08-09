//******************************************************************************
//                         MongoDBService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import com.mongodb.client.result.DeleteResult;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
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
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.nosql.mongodb.codec.ObjectCodec;
import org.opensilex.nosql.mongodb.codec.URICodec;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;

import static com.mongodb.client.model.Filters.*;

@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBService extends BaseService {

    public static final String DEFAULT_SERVICE = "mongodb";

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBService.class);
    private static final String URI_FIELD = "uri";

    private final String dbName;
    private MongoClient mongoClient;
    private MongoDatabase db;
    private URI generationPrefixURI;
    private static String defaultTimezone;

    public MongoDBService(MongoDBConfig config) {
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
     * @param config MongoDB configuration
     * @throws MongoTimeoutException If the server is inaccessible after a timeout (in milliseconds) defined by {@link MongoDBConfig#serverSelectionTimeout()}
     * @throws MongoSecurityException If the execution of this command is unauthorized by the MongoDB server
     *
     * @see <a href="https://www.mongodb.com/docs/manual/reference/command/ping/">MongoDB ping</a>
     */
    private void checkConnection(MongoDBConfig config) throws MongoTimeoutException, MongoSecurityException {

        // check that network connection is OK by running ping command, throw MongoTimeoutException else
        final Bson pingCommand = new BsonDocument("ping", new BsonInt32(1));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[CHECK_MONGO_SERVER_CONNECTION] timeout: {}ms [START]", config.connectTimeoutMs());
        }
        Document checkConnectionResults = db.runCommand(pingCommand);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[CHECK_MONGO_SERVER_CONNECTION {} [OK]", checkConnectionResults);
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
        ClientSession session = mongoClient.startSession();
        session.startTransaction();
        LOGGER.debug("MONGO TRANSACTION START");
        return session;
    }

    public <R> R operationInTransaction(ThrowingFunction<ClientSession, R, Exception> preCommitOperation
    ) throws Exception {
        return operationInTransaction(mongoClient.startSession(), preCommitOperation, null, null);
    }

    public <R> R operationInTransaction(ClientSession session, ThrowingFunction<ClientSession, R, Exception> preCommitOperation
    ) throws Exception {
        return operationInTransaction(session, preCommitOperation, null, null);
    }

    public <R> R operationInTransaction(ThrowingFunction<ClientSession, R, Exception> preCommitOperation,
                                        Runnable rollbackOperation
    ) throws Exception {
        return operationInTransaction(mongoClient.startSession(), preCommitOperation, null, rollbackOperation);
    }

    public <R> R operationInTransaction(ClientSession session,
                                        ThrowingFunction<ClientSession,R, Exception> preCommitOperation,
                                        Runnable commitOperation,
                                        Runnable rollbackOperation

    ) throws Exception {

        Objects.requireNonNull(preCommitOperation);
        Objects.requireNonNull(session);
        session.startTransaction();
        try {
            R result = preCommitOperation.apply(session);
            if (session.hasActiveTransaction()) {
                session.commitTransaction();
            }
            if (commitOperation != null) {
                commitOperation.run();
            }
            return result;
        } catch (Exception e) {
            if (session.hasActiveTransaction()) {
                session.abortTransaction();
            }
            if (rollbackOperation != null) {
                rollbackOperation.run();
            }
            throw e;
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

    public <T extends MongoModel> InsertOneResult create(T instance, MongoCollection<T> collection, String uriGenerationPrefix, ClientSession session) throws Exception {
        LOGGER.debug("MONGO CREATE - Collection : {}", collection);
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

    public <T extends MongoModel> InsertManyResult createAll(List<T> instances, MongoCollection<T> collection, ClientSession session, String prefix, boolean checkUris, boolean checkUriExist) throws Exception {
        LOGGER.debug("[MONGO_CREATE] : { collection: {}}", collection.getNamespace().getCollectionName());

        if(checkUris){
            for (T instance : instances) {
                if (instance.getUri() == null) {
                    generateUniqueUriIfNullOrValidateCurrent(instance, checkUriExist, prefix, collection);
                }
            }
        }

        // if a session is provided, simply use it
        if(session != null){
            return collection.insertMany(session,instances);
        }

        // Transaction handling for data integrity since insertMany() can update several documents
        return operationInTransaction(
                createSession -> collection.insertMany(createSession, instances)
        );
    }

    public <T extends MongoModel> InsertManyResult createAll(List<T> instances, MongoCollection<T> collection, ClientSession session) throws Exception {
        return createAll(instances,collection,session,null,false,false);
    }

    public <T> T findByURI(MongoCollection<T> collection, URI uri) throws NoSQLInvalidURIException {
        return this.findByURI(collection, uri, MongoModel.URI_FIELD);
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
        T instance = collection.find(eq(uriField, uri)).first();
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            return instance;
        }
    }

    public <T extends MongoModel> List<T> findByURIs(MongoCollection<T> collection, Collection<URI> uris) {
        FindIterable<T> queryResult = findIterableByURIs(MongoModel.URI_FIELD, collection, uris.stream());

        List<T> instances = new ArrayList<>();
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
    public <T> boolean uriExists(MongoCollection<T> collection, URI uri) {
        LOGGER.debug("[MONGO_URI_EXISTS] : { collection: {}, uri: {}}", collection.getNamespace().getCollectionName(),uri);
        try {
            findByURI(collection, uri, MongoModel.URI_FIELD);
            return true;
        } catch (NoSQLInvalidURIException ex) {
            return false;
        }
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
        if (orderByList != null && !orderByList.isEmpty()) {
            for (OrderBy orderBy : orderByList) {
                if (orderBy.getOrder().equals(Order.ASCENDING)) {
                    sort.put(orderBy.getFieldName(), 1);
                } else if (orderBy.getOrder().equals(Order.DESCENDING)) {
                    sort.put(orderBy.getFieldName(), -1);
                }
            }
        }
        return sort;
    }

    public  <T extends MongoModel> Map.Entry<FindIterable<T>,Long> findWithPagination(MongoCollection<T> collection,
                                                                                      Document filter,
                                                                                      Bson projection,
                                                                                      List<OrderBy> orderByList,
                                                                                      int page,
                                                                                      int pageSize) {

        long resultsNumber = collection.countDocuments(filter);

        // call isDebugEnabled before displaying log, since LogOrderList is a function. We don't want to call this method, is DEBUG logging is not enabled
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("[MONGO_SEARCH_WITH_PAGINATION] : { collection: {}, order: {}, filter: {}, results_count: {}}", collection.getNamespace().getCollectionName(),LogOrderList(orderByList), filter, resultsNumber);
        }

        if (resultsNumber == 0) {
            return null;
        }

        FindIterable<T> queryResult = collection
                .find(filter)
                .sort(buildSort(orderByList))
                .skip(page * pageSize)
                .limit(pageSize)
                .projection(projection);

        return new AbstractMap.SimpleImmutableEntry<>(queryResult,resultsNumber);
    }

    public <T extends MongoModel> ListWithPagination<T> searchWithPagination(
            MongoCollection<T> collection,
            Document filter,
            Bson projection,
            List<OrderBy> orderByList,
            int page,
            int pageSize) {

        Map.Entry<FindIterable<T>,Long> resultAndCount = findWithPagination(collection,filter,projection,orderByList,page,pageSize);
        if(resultAndCount == null){
            return new ListWithPagination<>(Collections.emptyList());
        }

        // iterate over MongoDB result cursor and collect inside a List
        List<T> results = new ArrayList<>(pageSize);
        CollectionUtils.addAll(results,resultAndCount.getKey());

        return new ListWithPagination<>(results, page, pageSize, resultAndCount.getValue().intValue());
    }

    public <T> long count(
            MongoCollection<T> collection,
            Document filter) {

        LOGGER.debug("[MONGO_COUNT] : { collection: {},filter: {}}", collection.getNamespace().getCollectionName(), filter);
        return collection.countDocuments(filter);
    }

    public <T> List<T> search(
            MongoCollection<T> collection,
            Document filter,
            List<OrderBy> orderByList) {

        Document sort = buildSort(orderByList);
        FindIterable<T> queryResult = collection.find(filter).sort(sort);

        List<T> results = new ArrayList<>();
        CollectionUtils.addAll(results,queryResult);

        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("[MONGO_SEARCH_WITH_PAGINATION] : { collection: {}, order: {}, filter: {}, results_count: {}}", collection.getNamespace().getCollectionName(),LogOrderList(orderByList), filter, results.size());
        }

        return results;
    }

    public <T> Set<T> distinct(
            String field,
            Class<T> resultClass,
            String collectionName,
            Document filter) {

        LOGGER.debug("MONGO SEARCH - Collection : " + collectionName + " - Field : " + field + " - Filter : " + filter.toString());
        Set<T> results = new HashSet<>();
        MongoCollection<T> collection = db.getCollection(collectionName, resultClass);

        DistinctIterable<T> queryResult = collection.distinct(field, filter, resultClass);

        for (T res : queryResult) {
            results.add(res);
        }

        return results;
    }

    /**
     * @param collection        mongo collection
     * @param field             Name of the field for which we want to retrieve distinct value (can be an embedded field)
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
    public <T> Set<T> distinctWithPagination(
            MongoCollection<?> collection,
            String field,
            Function<Document, T> documentExtractor,
            Bson filter,
            List<OrderBy> orderByList,
            int page,
            int pageSize
    ) {

        LOGGER.debug("[MONGO_DISTINCT_WITH_PAGINATION] : { collection: {}, order: {}, filter: {}}", collection.getNamespace().getCollectionName(),LogOrderList(orderByList), filter);

        List<Bson> aggregatePipeline = new ArrayList<>();

        // filtering with match
        if (filter != null) {
            aggregatePipeline.add(Aggregates.match(filter));
        }

        // distinct field with match
        aggregatePipeline.add(Aggregates.group("$" + field));

        // sort.
        Document order = buildSort(orderByList);
        aggregatePipeline.add(Aggregates.sort(order.toBsonDocument()));

        // pagination : skip and limit
        aggregatePipeline.add(Aggregates.skip(page * pageSize));
        aggregatePipeline.add(Aggregates.limit(pageSize));

        // build a set which maintains natural order between each key
        Set<T> distinct = new HashSet<>();

        // map aggregation results a Document, since the aggregation will produce a different document schema
        collection.aggregate(aggregatePipeline, Document.class)
                .map(documentExtractor::apply)
                .forEach(distinct::add);

        return distinct;
    }

    public <T> Set<T> aggregate(
            String collectionName,
            List<Bson> aggregationArgs,
            Class<T> instanceClass) {
        LOGGER.debug("MONGO SEARCH - Collection : " + collectionName + " - Aggregation pipeline : " + aggregationArgs.toString());
        Set<T> results = new HashSet<>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);

        AggregateIterable<T> aggregate = collection.aggregate(aggregationArgs, instanceClass);

        for (T res : aggregate) {
            results.add(res);
        }

        return results;
    }

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
        T instance = findByURI(collection, uri, uriField);
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            if (session != null) {
                return collection.deleteOne(session, eq(URI_FIELD, uri));
            } else {
                return collection.deleteOne(eq(URI_FIELD, uri));
            }
        }
    }

    public <T extends MongoModel> DeleteResult delete(MongoCollection<T> collection, ClientSession session, List<URI> uris) throws Exception {
        LOGGER.debug("MONGO DELETE - Collection : {}, {} uris to delete", collection.getNamespace().getCollectionName(), uris.size());
        return deleteOnCriteria(collection, session, Filters.in(MongoModel.URI_FIELD, uris));
    }


    /**
     * Update the given instance
     *
     * @param collection  the collection on which the new instance is located
     * @param newInstance the instance to update
     * @param uriField    the name of the uri field
     * @param <T>         the instance class
     * @throws NoSQLInvalidURIException if no instance is found
     */
    public <T extends MongoModel> void update(T newInstance, MongoCollection<T> collection, String uriField) throws NoSQLInvalidURIException {
        T instance = findByURI(collection, newInstance.getUri(), uriField);
        if (instance == null) {
            throw new NoSQLInvalidURIException(newInstance.getUri());
        }
        collection.findOneAndReplace(eq(uriField, newInstance.getUri()), newInstance);
    }

    public <T extends MongoModel> void update(T newInstance, MongoCollection<T> collection) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO UPDATE - Collection : {}", collection.getNamespace().getCollectionName());
        update(newInstance, collection, MongoModel.URI_FIELD);
    }

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

    public final MongoClient getMongoClient() {
        return this.mongoClient;
    }

    public final MongoClient buildMongoDBClient() throws IOException {
        return buildMongoDBClient(getImplementedConfig());
    }

    /**
     * @see <a href="https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/mongoclientsettings/">MongoClient Settings</a>
     */
    public static MongoClient buildMongoDBClient(MongoDBConfig config) throws IOException {

        // Define custom codec registry for URI, Object and GeoJson
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(new URICodec(), new ObjectCodec()),
                CodecRegistries.fromProviders(
                        new GeoJsonCodecProvider(),
                        PojoCodecProvider.builder().register(URI.class).automatic(true).build()
                )
        );

        // Build client : set server, codec and socket settings
        MongoClientSettings.Builder clientBuilder = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder
                        .hosts(Collections.singletonList(new ServerAddress(config.host(), config.port())))
                        .serverSelectionTimeout(config.serverSelectionTimeout(), TimeUnit.MILLISECONDS)
                ).codecRegistry(codecRegistry)
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(config.connectTimeoutMs(), TimeUnit.MILLISECONDS)
                        .readTimeout(config.readTimeoutMs(), TimeUnit.MILLISECONDS)
                );

        // Handle authentication
        MongoAuthenticationService auth = config.authentication();
        if (auth != null) {
            MongoCredential mongoCredentials = auth.readCredentials();
            clientBuilder.credential(mongoCredentials);
        }
        return MongoClients.create(clientBuilder.build());

    }

    /**
     * Method to generate a valid URI for the instance
     *
     * @param <T>
     * @param instance            will be updated by a new generated URI
     * @param instanceClassPrefix
     * @param collection
     * @throws Exception
     */
    public <T extends MongoModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, boolean checkUriExist, String instanceClassPrefix, MongoCollection<T> collection) throws Exception {
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

    public <T extends MongoModel> DeleteResult deleteOnCriteria(MongoCollection<T> collection, ClientSession session, Bson filter) throws Exception {

        if(session != null){
            return collection.deleteMany(filter);
        }

        // if a session is provided use it, else generate a new one for transaction handling,
        // since deleteMany() can update several document inside a collection, transaction handling is always needed
        return operationInTransaction(deleteSession ->
                collection.deleteMany(filter)
        );
    }

    /**
     * Format order to string
     *
     * @param orders
     * @return
     */
    private String LogOrderList(List<OrderBy> orders) {
        if (orders == null || orders.isEmpty()) {
            return "No order";
        } else {
            return orders.toString();
        }
    }
}
