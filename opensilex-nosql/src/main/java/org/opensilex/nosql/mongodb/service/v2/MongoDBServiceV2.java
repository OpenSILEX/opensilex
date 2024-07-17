//******************************************************************************
//                         MongoDBServiceV2.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2024
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.nosql.mongodb.service.v2;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
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
import org.opensilex.nosql.mongodb.codec.ZonedDateTimeCodec;
import org.opensilex.nosql.mongodb.logging.MongoLogger;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.opensilex.nosql.mongodb.logging.MongoLogger.*;
import static org.opensilex.utils.LogFilter.*;

/**
 * Definition of MongoDB based service. This class handle features concerning Mongo Client, session and transaction lifecycle
 * with the following features :
 * <ul>
 *     <li>Initialization of the {@link MongoClient} according the provided {@link MongoDBConfig}</li>
 *     <li>Mongo Index management with {@link #createIndex(MongoCollection, Bson, IndexOptions)} and {@link #dropIndex(MongoCollection, Bson)}</li>
 *     <li>Transaction management with {@link #computeTransaction(Function)}, {@link #runTransaction(Consumer)}, {@link #runThrowingTransaction(ThrowingConsumer)} and {@link #computeThrowingTransaction(ThrowingFunction)}</li>
 * </ul>
 *
 * See {@code opensilex-doc/src/main/resources/opensilex-nosql/mongodb/services/MongoDao.md} for mor details and example
 * @author rcolin
 */
@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBServiceV2 extends BaseService {

    public static final String DEFAULT_SERVICE = "mongodb2";

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBServiceV2.class);
    private final MongoLogger mongoLogger;

    private final String dbName;
    private MongoClient mongoClient;
    private MongoDatabase db;
    private URI generationPrefixURI;

    /**
     * Register for each collection name, the Set of (index name, index options)
     */
    private final Map<String,Map<Bson,IndexOptions> > indexRegister;

    public MongoDBServiceV2(MongoDBConfig config) {
        super(config);
        dbName = config.database();
        mongoLogger = new MongoLogger(null, LOGGER);
        indexRegister = new HashMap<>();
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
     * @throws MongoTimeoutException  If the server is inaccessible after a timeout (in milliseconds) defined by {@link MongoDBConfig#serverSelectionTimeoutMs()}
     * @throws MongoSecurityException If the execution of this command is unauthorized by the MongoDB server
     * @see <a href="https://www.mongodb.com/docs/manual/reference/command/ping/">MongoDB ping</a>
     */
    private void checkConnection(MongoDBConfig config) throws MongoTimeoutException, MongoSecurityException {

        // check that network connection is OK by running ping command, throw MongoTimeoutException else
        final Bson pingCommand = new BsonDocument("ping", new BsonInt32(1));
        Instant operationStart = mongoLogger.logOperationStart(CHECK_MONGO_SERVER_CONNECTION, TIMEOUT_MS, config.connectTimeoutMs());

        db.runCommand(pingCommand);
        mongoLogger.logOperationOk(CHECK_MONGO_SERVER_CONNECTION, operationStart);
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
                createIndexes();
            }
        } catch (MongoTimeoutException | MongoSecurityException e) {
            mongoClient.close();
            throw e;
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
    public ClientSession startSession() {
        return mongoClient.startSession();
    }

    public void withSession(ThrowingConsumer<ClientSession, Exception> operationInTrx) throws Exception {
        try (ClientSession session = startSession()) {
            operationInTrx.accept(session);
        }
    }

    /**
     * Execute the given operation with transaction management
     *
     * @param operationInTrx A Consumer which can execute database query by using a ClientSession
     * @see #computeTransaction(Function)
     */
    public void runTransaction(Consumer<ClientSession> operationInTrx) throws MongoDBTransactionException {
        computeTransaction(session -> {
            operationInTrx.accept(session);
            return null;
        });
    }

    /**
     * Execute the given operation with transaction management
     *
     * @param operationInTrx A Consumer which can execute database query by using a ClientSession
     * @see #computeThrowingTransaction(ThrowingFunction)
     */
    public void runThrowingTransaction(ThrowingConsumer<ClientSession,Exception> operationInTrx) throws Exception {
        computeThrowingTransaction(session -> {
            operationInTrx.accept(session);
            return null;
        });
    }

    /**
     * Execute the given operation with transaction management and return results
     *
     * @param operationInTrx A Consumer which can execute database query by using a ClientSession
     * @param <R>            : The type of result returned by the operation
     *
     */
    public <R> R computeTransaction(Function<ClientSession, R> operationInTrx){

        try (ClientSession session = mongoClient.startSession()) {
            Instant operationStart = mongoLogger.logOperationStart(TRANSACTION);

            // Run operation within transaction handling
            R result = session.withTransaction(() -> operationInTrx.apply(session));
            mongoLogger.logOperationOk(TRANSACTION, operationStart);
            return result;
        }
    }

    /**
     * Execute the given operation with transaction management and return results
     *
     * @param operationInTrx A Consumer which can execute database query by using a ClientSession
     * @param <R>            : The type of result returned by the operation
     * @throws MongoDBTransactionException if some errors occurs during operation application but not related to the MongoDB driver.
     */
    public <R> R computeThrowingTransaction(ThrowingFunction<ClientSession, R, Exception> operationInTrx) throws Exception {

        try (ClientSession session = mongoClient.startSession()) {
            Instant operationStart = mongoLogger.logOperationStart(TRANSACTION);

            // Run operation within transaction handling
            R result = session.withTransaction(() -> {
                try {
                    return operationInTrx.apply(session);
                } catch (MongoException e) {
                    throw e; // Exception thrown by the MongoDB driver (client or server side)
                } catch (Exception e) {
                    // Other Exception thrown during operation, in this case the operation is wrapped as a MongoDBTransactionException (A MongoException/RuntimeException)
                    //Since with lambda we have to catch or rethrow a RuntimeException
                    throw new MongoDBTransactionException(e.getMessage(), e);
                }
            });
            mongoLogger.logOperationOk(TRANSACTION, operationStart);
            return result;
        }catch (MongoDBTransactionException e){
            mongoLogger.logOperationError(TRANSACTION, LOG_STATUS_ROLLBACK, LOG_ERROR_MESSAGE_KEY, e.getMessage());
            throw e.getInnerException();
        }
    }

    public URI buildGenerationPrefixURI() throws OpenSilexModuleNotFoundException {
        return getOpenSilex().getModuleByClass(SPARQLModule.class).getGenerationPrefixURI();
    }

    public URI getGenerationPrefixURI() {
        return generationPrefixURI;
    }

    public MongoDBConfig getImplementedConfig() {
        return (MongoDBConfig) this.getConfig();
    }

    public MongoDatabase getDatabase() {
        return this.db;
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
                        new ZonedDateTimeCodec()),
                CodecRegistries.fromProviders(
                        new GeoJsonCodecProvider(),
                        PojoCodecProvider.builder().register(URI.class).automatic(true).build()
                )
        );

        // Build client : set server, codec and socket settings
        MongoClientSettings.Builder clientBuilder = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(new ServerAddress(config.host(), config.port())))
                        .serverSelectionTimeout(config.serverSelectionTimeoutMs(), TimeUnit.MILLISECONDS))
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

    public void registerIndexes(String collectionName, Map<Bson,IndexOptions> indexes){
        Objects.requireNonNull(indexes);
        if(StringUtils.isEmpty(collectionName)){
            throw new IllegalArgumentException("collectionName is null or empty");
        }
        // Register index
        indexes.forEach((indexKeys, indexOptions) -> indexRegister.computeIfAbsent(collectionName, (newKey) -> new HashMap<>()).putIfAbsent(indexKeys, indexOptions));
    }

    public void createIndexes(){

        indexRegister.forEach((collectionName, indexes) -> {
            MongoCollection<?> collection = getDatabase().getCollection(collectionName);
            indexes.forEach((indexKeys, indexOptions) -> createIndex(collection, indexKeys, indexOptions));
        });
    }

    /**
     * Register the creation of some MongoDB index
     * @param collection MongoDB's collection on which create index (required)
     * @param indexKeys The index key (created with {@link com.mongodb.client.model.Indexes} (required)
     * @param indexOptions Specific Index option (Can be null)
     * @see com.mongodb.client.model.Indexes
     * @see IndexOptions
     * @throws MongoCommandException if an error occurs during index creation. If the index already exists
     */
    public void createIndex(MongoCollection<?> collection, Bson indexKeys, IndexOptions indexOptions) throws MongoCommandException{

        Objects.requireNonNull(collection);
        Objects.requireNonNull(indexKeys);

        Instant operationStart = mongoLogger.logOperationStart(MONGO_CREATE_INDEX, COLLECTION, collection.getNamespace().getCollectionName(), INDEX, indexKeys.toBsonDocument().toJson());
        try {
            // Ensure index is build on background in order to preserve server availability
            indexOptions = Objects.requireNonNullElseGet(indexOptions, () -> new IndexOptions().background(true));
            collection.createIndex(indexKeys, indexOptions);
            mongoLogger.logOperationOk(MONGO_CREATE_INDEX, operationStart, COLLECTION, collection.getNamespace().getCollectionName(), INDEX, indexKeys.toBsonDocument().toJson());

        }catch (MongoCommandException e){

            // It's OK if index already exists -> https://www.mongodb.com/docs/manual/reference/error-codes/ : 86 IndexKeySpecsConflict
            if(e.getErrorCode() != 86){
                mongoLogger.logOperationError(MONGO_CREATE_INDEX, COLLECTION, collection, INDEX, indexKeys);
                throw e;
            }
        }
    }

    public void dropIndex(MongoCollection<?> collection, Bson indexKeys) throws MongoCommandException{

        Objects.requireNonNull(collection);
        Objects.requireNonNull(indexKeys);

        Instant operationStart = mongoLogger.logOperationStart(MONGO_DELETE_INDEX, COLLECTION, collection.getNamespace().getCollectionName(), INDEX, indexKeys.toBsonDocument().toJson());
        collection.dropIndex(indexKeys);
        mongoLogger.logOperationOk(MONGO_DELETE_INDEX, operationStart, COLLECTION, collection.getNamespace().getCollectionName(), INDEX, indexKeys.toBsonDocument().toJson());
    }

    public Map<String, Map<Bson, IndexOptions>> getIndexRegister() {
        return Collections.unmodifiableMap(indexRegister);
    }
}
