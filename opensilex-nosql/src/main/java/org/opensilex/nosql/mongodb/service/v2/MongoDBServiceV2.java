//******************************************************************************
//                         MongoDBService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb.service.v2;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
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
import org.opensilex.nosql.exceptions.MongoDBTransactionException;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.nosql.mongodb.codec.ObjectCodec;
import org.opensilex.nosql.mongodb.codec.URICodec;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.opensilex.nosql.mongodb.service.v2.MongoLogType.*;
import static org.opensilex.utils.LogFilter.LOG_TYPE;

@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBServiceV2 extends BaseService {

    public static final String DEFAULT_SERVICE = "mongodb2";

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBServiceV2.class);
    public static final String CHECK_MONGO_SERVER_CONNECTION_LOG_MSG = "MONGO_SERVER_CHECK_CONNECTION";

    public static final String MONGO_CREATE_INDEX_LOG_MSG = "createIndex";


    private final String dbName;
    private MongoClient mongoClient;
    private MongoDatabase db;
    private URI generationPrefixURI;
    private static String defaultTimezone;

    // Register each index which must be created by collection
    private static final Map<String, Map<Bson,IndexOptions>> INDEXES_BY_COLLECTION = new HashMap<>();

    public MongoDBServiceV2(MongoDBConfig config) {
        super(config);
        dbName = config.database();
        defaultTimezone = config.timezone();
    }

    public static void registerIndex(String collectionName, Bson indexKeys, IndexOptions indexOptions){
        INDEXES_BY_COLLECTION.computeIfAbsent(collectionName, newKey -> new HashMap<>()).put(indexKeys, indexOptions);
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

    @Override
    public void startup() throws OpenSilexModuleNotFoundException, IOException {
        mongoClient = buildMongoDBClient();
        generationPrefixURI = buildGenerationPrefixURI();
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
        createIndexes();
    }

    private void createIndexes(){
        INDEXES_BY_COLLECTION.forEach((collectionName, indexes) -> {
            MongoCollection<?> collection = db.getCollection(collectionName);
            indexes.forEach((indexKeys, indexOption) -> {

                Instant start = Instant.now();
                LOGGER.info("{}, {}, collection: {}, index: {}", kv(LOG_TYPE, MONGO_CREATE_INDEX_LOG_MSG), kv(MONGO_LOG_STATUS, MONGO_LOG_STATUS_START), collectionName, indexKeys);
                collection.createIndex(indexKeys, indexOption);

                long durationMs = Duration.between(start, Instant.now()).toMillis();
                LOGGER.info("{}, {}, collection: {}, index: {}, duration: {} ms", kv(LOG_TYPE, MONGO_CREATE_INDEX_LOG_MSG), kv(MONGO_LOG_STATUS, MONGO_LOG_STATUS_OK), collectionName, indexKeys, durationMs);
            });
        });

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
    public ClientSession startSession() {
        return mongoClient.startSession();
    }

    public void readOperationWithSession(ThrowingConsumer<ClientSession, Exception> operationInTrx) throws Exception {
        try (ClientSession session = startSession()) {
            operationInTrx.accept(session);
        }
    }

    /**
     * Execute the given operation with transaction management
     *
     * @param operationInTrx A Consumer which can execute database query by using a ClientSession
     * {@see @link #computeTransaction(ThrowingFunction)}
     */
    public void runTransaction(ThrowingConsumer<ClientSession,Exception> operationInTrx) throws MongoDBTransactionException {
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
     * @throws MongoDBTransactionException if some errors occurs during operation application but not related to the MongoDB driver.
     *
     * @apiNote
     * <ul>
     *     <li> Case were {@link MongoDBTransactionException} was throw because of the obligation with lambda to catch or rethrow a {@link RuntimeException}</li>
     *     <li> But this Exception has no logical meaning, so you have to handle the wrapped Exception {@link MongoDBTransactionException#getInnerException()}</li>
     * </ul>
     */
    public <R> R computeTransaction(ThrowingFunction<ClientSession, R, Exception> operationInTrx) throws MongoDBTransactionException {

        try (ClientSession session = mongoClient.startSession()) {
            LOGGER.info("Transaction start. serverSessionTransactionNumber: {}", session.getServerSession().getTransactionNumber());

            // Run operation within transaction handling
            R result = session.withTransaction(() -> {
                try {
                    return operationInTrx.apply(session);
                } catch (MongoException e) {
                    throw e; // Exception thrown by the MongoDB driver (client or server side)
                } catch (Exception e) {
                    // Other Exception thrown during operation, in this case the operation is  wrapped as a MongoDBTransactionException (A MongoException/RuntimeException)
                    //Since with lambda we have to catch or rethrow a RuntimeException
                    throw new MongoDBTransactionException(e.getMessage(), e);
                }
            });
            LOGGER.info("Transaction committed. serverSessionTransactionNumber: {}", session.getServerSession().getTransactionNumber());
            return result;
        }
    }

    public URI buildGenerationPrefixURI() throws OpenSilexModuleNotFoundException {
        return getOpenSilex().getModuleByClass(SPARQLModule.class).getGenerationPrefixURI();
    }

    public URI getGenerationPrefixURI() {
        return generationPrefixURI;
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
}
