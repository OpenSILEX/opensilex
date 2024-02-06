package org.opensilex.nosql.distributed;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.functionnal.ThrowingBiFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Operation which requires transaction management of RDF TripleStore AND of MongoDB
 * @author rcolin
 */
public class SparqlMongoTransaction {

    private final SPARQLService sparql;
    private final MongoDBServiceV2 mongodb;

    private boolean customExceptionMappingFlag;
    private Map<Class<? extends Exception>, Function<Exception, Exception>> exceptionMapping;

    public SparqlMongoTransaction(SPARQLService sparql, MongoDBServiceV2 mongodb) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        customExceptionMappingFlag = false;
    }

    /**
     * Define custom handling of exception. This method is required when for some business logic,
     * a specific Exception is expected, and a custom action must be applied.
     * During {@link #execute(ThrowingBiFunction)} or {@link #execute(SPARQLService, ClientSession, ThrowingBiFunction)} call,
     * if an {@link Exception} is catch, then database transactions are rollback, this method allow to define custom management
     * of the exception, after database rollback.
     *
     * @param inputExceptionClass A specific Exception class, which when is thrown, requires a specific action
     * @param customExceptionLogic The custom exception to throw when the targeted exception is caught
     *
     * @return this {@link SparqlMongoTransaction}
     * @apiNote This method must be called BEFORE the call to {@link #execute(ThrowingBiFunction)} or {@link #execute(SPARQLService, ClientSession, ThrowingBiFunction)} methods.
     * Else, there are no way to handle the custom exception class, since this one has not been registered
     */
    public SparqlMongoTransaction customException(Class<? extends Exception> inputExceptionClass, UnaryOperator<Exception> customExceptionLogic) {
        Objects.requireNonNull(inputExceptionClass);
        Objects.requireNonNull(customExceptionLogic);

        // Lazy initialization of Map, since custom exception handling is not always required
        if(exceptionMapping == null){
            exceptionMapping = new HashMap<>();
            customExceptionMappingFlag = true;
        }
        exceptionMapping.put(inputExceptionClass, customExceptionLogic);
        return this;
    }

    /**
     * Execute a {@link java.util.function.BiFunction} which requires transaction management on RDF and MongoDB database
     * @param operation the operation to execute (not null)
     * @return the function results
     * @param <R> the function results type
     * @param <E> the type of Exception thrown during Function application
     *
     * @throws Exception if any Exception is catch during Function application
     */
    public <R, E extends Exception> R execute(ThrowingBiFunction<SPARQLService, ClientSession, R, E> operation
    ) throws Exception {

        Objects.requireNonNull(operation);
        try(ClientSession session = mongodb.startSession()){
            return this.execute(sparql, session, operation);
        }
    }


    /**
     * Execute a {@link java.util.function.BiFunction} which requires transaction management on RDF and MongoDB database.
     * This method ensure that transaction is started and (committed or rollback)
     *
     * @param operation the operation to execute (not null)
     * @param sparqlConnection The {@link SPARQLService} used for RDF database transaction
     * @param mongoSession The {@link ClientSession} used for MongoDB database transaction
     * @return the function results
     * @param <R> the function results type
     * @param <E> the type of Exception thrown during Function application
     * @throws Exception if any Exception is catch during Function application
     *
     * @apiNote
     * <ul>
     *     <li>The method works in two step : first the operation is applied, then transaction are committed on both database </li>
     *     <li>When mongoSession is provided, the lifecycle of the session is not managed.
     *      * You can use this method if you reuse the session for different transaction, else you rather use {@link #execute(ThrowingBiFunction)}
     *      which start and close the session
     *      </li>
     * </ul>
     */
    public <R, E extends Exception> R execute(SPARQLService sparqlConnection, ClientSession mongoSession, ThrowingBiFunction<SPARQLService, ClientSession, R, E> operation
    ) throws Exception {

        R result;

        /* Phase 1 : Check if the database is ready to commit the transaction
         - Start transactions
         - Performs operation on each data-source
         */
        sparql.startTransaction();
        mongoSession.startTransaction();

        try {
            // Performs business logic and write inside transaction
            result = operation.apply(sparqlConnection, mongoSession);
        } catch (Exception e) {
            throw handleTransaction(e, sparql, mongoSession);
        }

        // Phase 2 :Commit transaction
        try {
            if (sparql.hasActiveTransaction()) {
                sparql.commitTransaction();
            }
            if (mongoSession.hasActiveTransaction()) {
                mongoSession.commitTransaction();
            }
            return result;

        } catch (Exception e) {
            throw handleTransaction(e, sparql, mongoSession);
        }
    }

    private Exception handleTransaction(Exception e, SPARQLService sparql, ClientSession mongoSession) throws Exception {
        // Try to rollback
        if (sparql.hasActiveTransaction()) {
            sparql.rollbackTransaction();
        }
        if (mongoSession.hasActiveTransaction()) {
            mongoSession.abortTransaction();
        }

        // Custom application of
        Class<? extends Exception> exceptionClass = e.getClass();

        // Check if a custom exception handling is defined and the caught exception is registered inside custom Exception
        if (customExceptionMappingFlag && exceptionMapping.containsKey(exceptionClass)) {
            return exceptionMapping.get(exceptionClass).apply(e);
        }
        // Else just thrown the exception
        return e;
    }
}