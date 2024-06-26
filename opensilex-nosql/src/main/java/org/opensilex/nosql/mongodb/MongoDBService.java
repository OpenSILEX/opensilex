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

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import com.mongodb.client.result.DeleteResult;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

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
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.nosql.mongodb.codec.*;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;

@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBService extends BaseService {

    public final static String DEFAULT_SERVICE = "mongodb";

    private final static Logger LOGGER = LoggerFactory.getLogger(MongoDBService.class);
    private final String URI_FIELD = "uri";

    private final String dbName;
    private MongoClient mongoClient;
    private ClientSession session = null;
    private MongoDatabase db;
    private URI generationPrefixURI;
    private static String defaultTimezone;

    // V2 service : used for an easier transition from this one to the new V2
    private MongoDBServiceV2 serviceV2;

    public MongoDBService(MongoDBConfig config) {
        super(config);
        dbName = config.database();
        defaultTimezone = config.timezone();
    }

    /**
     * Test if the connection to the MongoDB server is OK
     * @param config MongoDB configuration
     * @throws MongoTimeoutException If the server is inaccessible after a timeout (in milliseconds) defined by {@link MongoDBConfig#serverSelectionTimeoutMs()}
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
                CodecRegistries.fromCodecs(
                        new URICodec(),
                        new ObjectCodec(),
                        new ZonedDateTimeCodec()
                ),
                CodecRegistries.fromProviders(
                        new GeoJsonCodecProvider(),
                        PojoCodecProvider.builder().register(URI.class).automatic(true).build()
                )
        );

        // Build client : set server, codec and socket settings
        MongoClientSettings.Builder clientBuilder = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder
                        .hosts(Collections.singletonList(new ServerAddress(config.host(), config.port())))
                        .serverSelectionTimeout(config.serverSelectionTimeoutMs(),TimeUnit.MILLISECONDS)
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

    public ClientSession getSession() {
        return this.session;
    }

    private int transactionLevel = 0;

    public void startTransaction() {
        if (transactionLevel == 0) {
            LOGGER.debug("MONGO TRANSACTION START");
            session = mongoClient.startSession();
            session.startTransaction();
        }
        transactionLevel++;
    }

    public void commitTransaction() {
        transactionLevel--;
        if (transactionLevel == 0) {
            LOGGER.debug("MONGO TRANSACTION COMMIT");
            session.commitTransaction();
            session.close();
            session = null;
        }
    }

    public void rollbackTransaction() throws Exception {
        if (transactionLevel != 0) {
            LOGGER.error("MONGO TRANSACTION ROLLBACK");
            transactionLevel = 0;
            session.abortTransaction();
            session.close();
            session = null;
        }

    }

    public <T extends MongoModel> void create(T instance, Class<T> instanceClass, String collectionName, String uriGenerationPrefix) throws Exception {
        LOGGER.debug("MONGO CREATE - Collection : " + collectionName);
        if (instance.getUri() == null) {
            generateUniqueUriIfNullOrValidateCurrent(instance, true, uriGenerationPrefix, collectionName);
        }

        if (Objects.isNull(instance.getPublicationDate())) {
            instance.setPublicationDate(Instant.now());
        }

        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        try {
            if (session != null) {
                collection.insertOne(session, instance);
            } else {
                collection.insertOne(instance);
            }
        } catch (Exception error) {
            throw error;
        }
    }

    public <T extends MongoModel> void createAll(List<T> instances, Class<T> instanceClass, String collectionName, String prefix, boolean checkUriExist) throws Exception {
        LOGGER.debug("MONGO CREATE - Collection : " + collectionName);
        for (T instance : instances) {
            if (instance.getUri() == null) {
                generateUniqueUriIfNullOrValidateCurrent(instance, checkUriExist, prefix, collectionName);
            }

            if (Objects.isNull(instance.getPublicationDate())) {
                instance.setPublicationDate(Instant.now());
            }
        }

        startTransaction();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);

        try {
            collection.insertMany(session, instances);
            commitTransaction();
        } catch (Exception exception) {
            rollbackTransaction();
            throw exception;
        }

    }

    /**
     * @param instanceClass  the instance class
     * @param collectionName the name of collection on which find the instance
     * @param uri            URI of the instance to find
     * @param <T>            the instance class
     * @return the instance which has the given uri
     * @throws NoSQLInvalidURIException
     */
    public <T> T findByURI(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO FIND BY URI - Collection : " + collectionName + " - uri : " + uri);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        return this.findByURI(collection, uri, "uri");
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
        T instance = (T) collection.find(eq(uriField, uri)).first();
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            return instance;
        }
    }

    public <T> List<T> findByURIs(Class<T> instanceClass, String collectionName, List<URI> uris) {
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        Document listFilter = new Document();
        listFilter.append("$in", uris);
        Document filter = new Document();
        filter.append("uri", listFilter);
        FindIterable<T> queryResult = collection.find(filter);
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
     * @param instanceClass  the instance class
     * @param collectionName the name of collection on which find the instance
     * @param uri            URI of the instance to check
     * @param <T>            the instance class
     * @return if an instance with the given uri exists
     */
    public <T> boolean uriExists(Class<T> instanceClass, String collectionName, URI uri) {
        LOGGER.debug("MONGO URI EXISTS - Collection : " + collectionName + " - uri : " + uri);
        try {
            T instance = findByURI(instanceClass, collectionName, uri);
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
            T instance = findByURI(collection, uri, uriField);
            return true;
        } catch (NoSQLInvalidURIException ex) {
            return false;
        }
    }


    public Set<URI> getMissingUrisFromCollection(String collectionName, Set<URI> uris) {
        LOGGER.debug("MONGO MISSING URIS - Collection : " + collectionName);
        Document listFilter = new Document();
        listFilter.append("$in", uris);
        Document filter = new Document();
        filter.append(URI_FIELD, listFilter);

        Set foundedURIs = distinct(URI_FIELD, URI.class, collectionName, filter);

        uris.removeAll(foundedURIs);
        return uris;
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

    public <T> ListWithPagination<T> searchWithPagination(
            Class<T> instanceClass,
            String collectionName,
            Document filter,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) {

        List<T> results = new ArrayList<T>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        long resultsNumber = collection.countDocuments(filter);
        int total = (int) resultsNumber;

        LOGGER.debug("MONGO SEARCH WITH PAGINATION - Collection : " + collectionName + " - Order : " + LogOrderList(orderByList) + " - Filter : " + filter.toString());

        if (total > 0) {
            Document sort = buildSort(orderByList);

            FindIterable<T> queryResult = collection.find(filter).sort(sort).skip(page * pageSize).limit(pageSize);

            for (T res : queryResult) {
                results.add(res);
            }
        }

        return new ListWithPagination(results, page, pageSize, total);

    }

    public <T> int count(
            Class<T> instanceClass,
            String collectionName,
            Document filter) {

        LOGGER.debug("MONGO COUNT - Collection : " + collectionName + " - Order : " + " - Filter : " + filter.toString());

        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        long resultsNumber = collection.countDocuments(filter);
        int total = (int) resultsNumber;

        return total;

    }

    public <T> List<T> search(
            Class<T> instanceClass,
            String collectionName,
            Document filter,
            List<OrderBy> orderByList) {

        LOGGER.debug("MONGO SEARCH - Collection : " + collectionName + " - Order : " + LogOrderList(orderByList) + " - Filter : " + filter.toString());

        List<T> results = new ArrayList<T>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        Document sort = buildSort(orderByList);
        FindIterable<T> queryResult = collection.find(filter).sort(sort);

        for (T res : queryResult) {
            results.add(res);
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

        LOGGER.debug("MONGO DISTINCT (paginated) - Collection : {}, Field: {}", collection.getNamespace().getCollectionName(), field);

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
        LOGGER.debug("MONGO AGGREGATE - Collection : " + collectionName + " - Aggregation pipeline : " + aggregationArgs.toString());
        Set<T> results = new HashSet<>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);

        AggregateIterable<T> aggregate = collection.aggregate(aggregationArgs, instanceClass);

        for (T res : aggregate) {
            results.add(res);
        }
        return results;
    }

    public <T extends MongoModel> void delete(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO DELETE - Collection : " + collectionName + " - uri : " + uri);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        this.delete(collection, uri, "uri");
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
    public <T extends MongoModel> void delete(MongoCollection<T> collection, URI uri, String uriField) throws NoSQLInvalidURIException {
        T instance = findByURI(collection, uri, uriField);
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            if (session != null) {
                collection.deleteOne(session, eq(URI_FIELD, uri));
            } else {
                collection.deleteOne(eq(URI_FIELD, uri));
            }
        }
    }

    public <T extends MongoModel> void delete(Class<T> instanceClass, String collectionName, List<URI> uris) throws NoSQLInvalidURIException, Exception {
        LOGGER.debug("MONGO DELETE - Collection : " + collectionName + " - uris : " + uris);
        Set<URI> notFoundedURIs = checkUriListExists(instanceClass, collectionName, (Set<URI>) uris);

        if (notFoundedURIs.isEmpty()) {
            startTransaction();
            MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
            for (URI uri : uris) {
                try {
                    collection.deleteOne(session, eq(URI_FIELD, uri));
                    commitTransaction();
                } catch (Exception exception) {
                    rollbackTransaction();
                    throw exception;
                } finally {
                    session.close();
                    session = null;
                }

            }
        } else {
            throw new NoSQLInvalidUriListException(uris);
        }
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

        if (Objects.isNull(newInstance.getPublicationDate())) {
            newInstance.setPublicationDate(instance.getPublicationDate());
        }

        newInstance.setLastUpdateDate(Instant.now());
        collection.findOneAndReplace(eq(uriField, newInstance.getUri()), newInstance);
    }

    public <T extends MongoModel> void update(T newInstance, Class<T> instanceClass, String collectionName) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO UPDATE - Collection : " + collectionName);

        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        this.update(newInstance, collection, "uri");
    }

    /**
     * Method to generate a valid URI for the instance
     *
     * @param <T>
     * @param instance            will be updated by a new generated URI
     * @param instanceClassPrefix
     * @param collectionName
     * @throws Exception
     */
    public <T extends MongoModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, boolean checkUriExist, String instanceClassPrefix, String collectionName) throws Exception {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            String prefix = UriBuilder.fromUri(generationPrefixURI).path(instanceClassPrefix).toString();
            uri = instance.generateURI(prefix, instance, retry);
            while (checkUriExist && uriExists(instance.getClass(), collectionName, uri)) {
                uri = instance.generateURI(prefix, instance, retry++);
            }
            instance.setUri(uri);

        } else if (checkUriExist && uriExists(instance.getClass(), collectionName, uri)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }

    private <T extends MongoModel> Set<URI> checkUriListExists(Class<T> instanceClass, String collectionName, Set<URI> uris) {
        Set foundedURIs = new HashSet<>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        Document filter = new Document();
        Document inFilter = new Document();
        inFilter.put("$in", uris);
        filter.put(URI_FIELD, inFilter);
        foundedURIs = distinct(URI_FIELD, instanceClass, collectionName, filter);
        uris.removeAll(foundedURIs);
        return uris;
    }

    public <T extends MongoModel> DeleteResult deleteOnCriteria(Class<T> instanceClass, String collectionName, Document filter) throws Exception {
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        startTransaction();
        try {
            DeleteResult result = collection.deleteMany(session, filter);
            commitTransaction();
            return result;
        } catch (Exception exception) {
            rollbackTransaction();
            throw exception;
        }
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

    public MongoDBServiceV2 getServiceV2() {
        if(serviceV2 == null){
            serviceV2 = getOpenSilex().getServiceInstance(MongoDBServiceV2.DEFAULT_SERVICE, MongoDBServiceV2.class);
            Objects.requireNonNull(serviceV2);
        }
        return serviceV2;
    }
}
