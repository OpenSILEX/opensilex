//******************************************************************************
//                         MongoDBService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import com.mongodb.client.result.DeleteResult;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.nosql.insert.MongoInsertOptions;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.insert.DefaultMongoInserter;
import org.opensilex.nosql.insert.MongoInserter;
import org.opensilex.nosql.mongodb.codec.ObjectCodec;
import org.opensilex.nosql.mongodb.codec.URICodec;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;
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
    private MongoDatabase db;
    private URI generationPrefixURI;
    private static String defaultTimezone;

    private final MongoInserter mongoInserter;

    public MongoDBService(MongoDBConfig config) {
        super(config);
        dbName = config.database();
        defaultTimezone = config.timezone();
        mongoInserter = new DefaultMongoInserter(config);
    }

    @Override
    public void startup() throws OpenSilexModuleNotFoundException {
        mongoClient = buildMongoClient();
        generationPrefixURI = getGenerationPrefixURI();
        db = mongoClient.getDatabase(dbName);
    }

    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        mongoInserter.shutdown();
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

    public ClientSession startSession(){
        return this.mongoClient.startSession();
    }

    public <T extends MongoModel> void create(T instance, Class<T> instanceClass, String collectionName, String uriGenerationPrefix) throws Exception {
       this.create(null,instance,instanceClass,collectionName,uriGenerationPrefix);
    }

    public <T extends MongoModel> void create(ClientSession session, T instance, Class<T> instanceClass, String collectionName, String uriGenerationPrefix) throws Exception {
        this.createAll(
                new MongoInsertOptions<>(
                        mongoClient,
                        db.getCollection(collectionName, instanceClass),
                        session,
                        Collections.singletonList(instance)
                ).setUriGenerationPrefix(uriGenerationPrefix)
        );
    }

    public <T extends MongoModel> void createAll(List<T> instances, Class<T> instanceClass, String collectionName, String prefix, boolean checkUriExist) throws Exception {

        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        createAll(new MongoInsertOptions<>(
                        mongoClient,
                        collection,
                        null,
                        instances
                ).setCheckUriExist(checkUriExist)
                        .setUriGenerationPrefix(prefix)
        );
    }

    /**
     *
     * @param insert insert options
     * @param <T> type of {@link MongoModel}
     * @throws Exception if some Exception if encountered during insertion.
     *
     * @apiNote The insertion is delegated to the inner {@link MongoInserter} implementation.
     */
    public <T extends MongoModel> void createAll(MongoInsertOptions<T> insert) throws Exception {

        for (T instance : insert.getModels()) {
            if (instance.getUri() == null) {
                generateUniqueUriIfNullOrValidateCurrent(
                        instance,
                        insert.isCheckUriExist(),
                        insert.getUriGenerationPrefix(),
                        insert.getCollectionName());
            }
        }

        mongoInserter.create(insert);
    }

    public <T> T operationWithTransaction(
            ThrowingFunction<ClientSession,T, Exception> mongoFunction) throws Exception {
        return operationWithTransaction(mongoFunction,null);
    }

    public <T> T operationWithTransaction(
            ThrowingFunction<ClientSession,T, Exception> mongoFunction,
            Consumer<Exception> customExceptionHandler) throws Exception {

        ClientSession session = startSession();
        session.startTransaction();

        try{
            return mongoFunction.apply(session);
        }catch (Exception e){
            if(session.hasActiveTransaction()){
                session.abortTransaction();
            }
            if(customExceptionHandler != null){
                customExceptionHandler.accept(e);
            }
            throw e;
        }finally {
            session.close();
        }
    }

    /**
     * Handle operations on {@link SPARQLService} and on {@link MongoDBService} by handling
     * transaction commit and rollback in case of error
     * @param sparqlService the {@link SPARQLService} used to performs operations on triplestore
     * @throws Exception if some Exception is encountered during mongoFunction or sparqlFunction applying, or with SPARQL or MongoDB driver.
     * When encountered, this method ensure that any data inserted is rollback()
     * @apiNote the mongo session is always closed ({@link ClientSession#close()}) at the end of method (with or without fail). So you should
     * not reuse this session after <br><br>
     *
     * The following method are used for SPARQL transaction handling :
     * <lu>
     *     <li>{@link SPARQLService#startTransaction()}</li>
     *     <li>{@link SPARQLService#hasActiveTransaction()}</li>
     *     <li>{@link SPARQLService#commitTransaction()}</li>
     *     <li>{@link SPARQLService#rollbackTransaction()}</li>
     * </lu>
     *
     * <br> The following method are used for MongoDB transaction handling : <br>
     * <lu>
     *     <li>{@link ClientSession#startTransaction()}</li>
     *     <li>{@link ClientSession#hasActiveTransaction()}</li>
     *     <li>{@link ClientSession#commitTransaction()}</li>
     *     <li>{@link ClientSession#abortTransaction()}</li>
     * </lu>
     */
    public void multipleOperationsWithTransaction(
            SPARQLService sparqlService,
            MultipleDataSourceOperation operation
    ) throws Exception {

        boolean useMongo = operation.useMongoDb();
        boolean useSparql = operation.useSparql();

        ClientSession mongoSession = useMongo ? startSession(): null;
        try {

            if(useMongo){
                mongoSession.startTransaction();
            }
            if(useSparql){
                sparqlService.startTransaction();
            }

            if(operation.isSparqlFirst()){
                if(useSparql){
                    for(ThrowingConsumer<SPARQLService, Exception> sparqlConsumer : operation.getSparqlConsumers()){
                        sparqlConsumer.accept(sparqlService);
                    }
                }
                if(useMongo){
                    for(ThrowingConsumer<ClientSession, Exception> mongoConsumer : operation.getMongoConsumers()){
                        mongoConsumer.accept(mongoSession);
                    }
                }
            }else{
                if(useMongo){
                    for(ThrowingConsumer<ClientSession, Exception> mongoConsumer : operation.getMongoConsumers()){
                        mongoConsumer.accept(mongoSession);
                    }
                }
                if(useSparql){
                    for(ThrowingConsumer<SPARQLService, Exception> sparqlConsumer : operation.getSparqlConsumers()){
                        sparqlConsumer.accept(sparqlService);
                    }
                }
            }

            if(useMongo && mongoSession.hasActiveTransaction()){
                mongoSession.commitTransaction();
            }
            if(useSparql && sparqlService.hasActiveTransaction()){
                sparqlService.commitTransaction();
            }
        }catch (Exception e){
            if(useMongo && mongoSession.hasActiveTransaction()){
                mongoSession.abortTransaction();
            }
            if(useSparql && sparqlService.hasActiveTransaction()){
                sparqlService.rollbackTransaction();
            }
            throw e;
        }finally {
            if(useMongo){
                mongoSession.close();
            }
        }
    }

    /**
     * Call {@link #multipleOperationsWithTransaction(SPARQLService, MultipleDataSourceOperation)} with sparql function evaluated first
     */
    public void multipleOperationsWithTransaction(
            SPARQLService sparqlService,
            ThrowingConsumer<ClientSession, Exception> mongoFunction,
            ThrowingConsumer<SPARQLService, Exception> sparqlFunction) throws Exception {

        multipleOperationsWithTransaction(sparqlService, new MultipleDataSourceOperation()
                .addMongoConsumer(mongoFunction)
                .addSparqlConsumer(sparqlFunction)
                .setSparqlFirst(true)
        );
    }

    /**
     *
     * @param instanceClass the instance class
     * @param collectionName the name of collection on which find the instance
     * @param uri URI of the instance to find
     * @param <T> the instance class
     * @return the instance which has the given uri
     * @throws NoSQLInvalidURIException
     */
    public <T> T findByURI(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO FIND BY URI - Collection : " + collectionName + " - uri : "  + uri);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        return this.findByURI(collection,uri,"uri");
    }

    /**
     *
     * @param collection the collection on which find an instance with the given uri
     * @param uri URI of the instance to find
     * @param uriField the name of the uri field
     * @param <T> the instance class
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
     *
     * @param fieldName name of the field on which apply {@link Filters#in(String, Object[])}
     * @param collection mongo collection
     * @param uris {@link Stream} of URIS to pass to {@link Filters#in(String, Object[])}
     * @param <T> {@link MongoModel} type
     * @return a {@link FindIterable} of T models from mongodb. Returned model must have the fieldName attribute and the value for this attribute must be in uris
     *
     */
    public <T extends MongoModel> FindIterable<T> findIterableByURIs(String fieldName, MongoCollection<T> collection, Stream<URI> uris){
        Bson filter = Filters.in(fieldName, uris::iterator);
        return collection.find(filter);
    }

    /**
     *
     * @param instanceClass the instance class
     * @param collectionName the name of collection on which find the instance
     * @param uri URI of the instance to check
     * @param <T> the instance class
     * @return if an instance with the given uri exists
     */
    public <T> boolean uriExists(Class<T> instanceClass, String collectionName, URI uri) {
        LOGGER.debug("MONGO URI EXISTS - Collection : " + collectionName + " - uri : "  + uri );
        try {
            findByURI(instanceClass, collectionName, uri);
            return true;
        } catch (NoSQLInvalidURIException ex) {
            return false;
        }
    }

    /**
     *
     * @param collection the collection on which find an instance with the given uri
     * @param uri URI of the instance to check
     * @param uriField the name of the uri field
     * @param <T> the instance class
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


    public Set<URI> getMissingUrisFromCollection(String collectionName, Set<URI> uris) {
        LOGGER.debug("MONGO MISSING URIS - Collection : " + collectionName);
        Document listFilter = new Document();
        listFilter.append("$in", uris);
        Document filter = new Document();
        filter.append(URI_FIELD, listFilter);

        Set<URI> foundedURIs = distinct(URI_FIELD, URI.class, collectionName, filter);

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

        List<T> results = new ArrayList<>();
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        long resultsNumber = collection.countDocuments(filter);
        int total = (int) resultsNumber;

        LOGGER.debug("MONGO SEARCH WITH PAGINATION - Collection : " + collectionName + " - Order : " + LogOrderList(orderByList) + " - Filter : " + filter);

        if (total > 0) {
            Document sort = buildSort(orderByList);

            FindIterable<T> queryResult = collection.find(filter).sort(sort).skip(page * pageSize).limit(pageSize);

            for (T res : queryResult) {
                results.add(res);
            }
        }

        return new ListWithPagination<>(results, page, pageSize, total);

    }
    
    public <T> int count( 
            Class<T> instanceClass,
            String collectionName,
            Document filter) {
        
        LOGGER.debug("MONGO COUNT - Collection : " + collectionName + " - Order : "  + " - Filter : " + filter.toString());

        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        long resultsNumber = collection.countDocuments(filter);
        return (int) resultsNumber;
        
    }
    
    public <T> List<T> search(
            Class<T> instanceClass,
            String collectionName,
            Document filter,
            List<OrderBy> orderByList) {

        LOGGER.debug("MONGO SEARCH - Collection : " + collectionName + " - Order : " + LogOrderList(orderByList) + " - Filter : " + filter.toString());

        List<T> results = new ArrayList<>();
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
    
    public <Document> Set<Document> aggregate( 
            String collectionName,
             List aggregationArgs) {
        LOGGER.debug("MONGO SEARCH - Collection : " + collectionName + " - Aggregation pipeline : " + aggregationArgs.toString());
        Set<Document> results = new HashSet<>();
        MongoCollection<?> collection = db.getCollection(collectionName);

        AggregateIterable<Document> aggregate = collection.aggregate(aggregationArgs);

        for (Document res : aggregate) {
            results.add(res);
        }

        return results;
    }

    public <T extends MongoModel> void delete(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO DELETE - Collection : " + collectionName + " - uri : "  + uri);
        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        this.delete(collection,uri,"uri");
    }


    /**
     * delete the instance with the given uri
     * @param collection the collection on which find an instance with the given uri
     * @param uri URI of the instance to find
     * @param uriField the name of the uri field
     * @param <T> the instance class
     * @throws NoSQLInvalidURIException if no instance is found
     */
    public <T extends MongoModel> void delete(MongoCollection<T> collection , URI uri, String uriField) throws NoSQLInvalidURIException {
        T instance = findByURI(collection,uri,uriField);
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            collection.deleteOne(eq(URI_FIELD, uri));
        }
    }

    public <T extends MongoModel> void delete(Class<T> instanceClass, String collectionName, List<URI> uris) throws NoSQLInvalidURIException, Exception {

        LOGGER.debug("MONGO DELETE - Collection : " + collectionName + " - uris : "  + uris);

        // #TODO cast will always fail !!!!!!!
        Set<URI> notFoundedURIs = checkUriListExists(instanceClass, collectionName, (Set<URI>) uris);

        if (!notFoundedURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException(uris);
        }
        operationWithTransaction((ClientSession session) -> {
            MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
            return collection.deleteMany(session,in(MongoModel.URI_FIELD,uris));
        });
    }


    /**
     * Update the given instance
     * @param collection the collection on which the new instance is located
     * @param newInstance  the instance to update
     * @param uriField the name of the uri field
     * @param <T> the instance class
     * @throws NoSQLInvalidURIException if no instance is found
     */
    public <T extends MongoModel> void update(T newInstance,MongoCollection<T> collection, String uriField) throws NoSQLInvalidURIException {
        T instance = findByURI(collection, newInstance.getUri(),uriField);
        if (instance == null) {
            throw new NoSQLInvalidURIException(newInstance.getUri());
        }
        collection.findOneAndReplace(eq(uriField, newInstance.getUri()), newInstance);
    }

    public <T extends MongoModel> void update(T newInstance, Class<T> instanceClass, String collectionName) throws NoSQLInvalidURIException {
        LOGGER.debug("MONGO UPDATE - Collection : " + collectionName);

        MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
        this.update(newInstance,collection,"uri");
    }

    private final MongoClient buildMongoClient() {
        return buildMongoClient(getImplementedConfig());
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public static MongoClient buildMongoClient(MongoDBConfig cfg) {
        String connectionString = "mongodb://";

        if (cfg.username() != null && cfg.password() != null && !cfg.username().isEmpty() && !cfg.password().isEmpty()) {
            connectionString += cfg.username() + ":" + cfg.password() + "@";
        }
        connectionString += cfg.host() + ":" + cfg.port();
        connectionString += "/" + cfg.database();

        Map<String, String> options = new HashMap<>();
        options.putAll(cfg.options());
        if (!StringUtils.isEmpty(cfg.authDB())) {
            options.put("authSource", cfg.authDB());
        }
        if (options.size() > 0) {
            connectionString += "?";
            Set<String> keys = options.keySet();
            int i = 0;
            for (String key : keys) {
                String value = options.get(key);
                connectionString += key + "=" + value;
                i++;

                if (i < options.size()) {
                    connectionString += "&";
                }
            }
        }
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(new URICodec(), new ObjectCodec()),
                CodecRegistries.fromProviders(
                        new GeoJsonCodecProvider(),
                        PojoCodecProvider.builder().register(URI.class).automatic(true).build()
                )
        );

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();
        return MongoClients.create(clientSettings);
    }

    /**
     * Method to generate a valid URI for the instance
     *
     * @param <T>
     * @param instance will be updated by a new generated URI
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
        Document filter = new Document();
        Document inFilter = new Document();
        inFilter.put("$in", uris);
        filter.put(URI_FIELD, inFilter);

        Set<URI> foundedURIs = distinct(URI_FIELD, URI.class, collectionName, filter);
        uris.removeAll(foundedURIs);
        return uris;
    }

    public <T extends MongoModel> DeleteResult deleteOnCriteria(Class<T> instanceClass, String collectionName, Document filter) throws Exception {

        return operationWithTransaction((ClientSession session) -> {
            MongoCollection<T> collection = db.getCollection(collectionName, instanceClass);
            return collection.deleteMany(session, filter);
        });
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
