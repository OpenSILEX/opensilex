package org.opensilex.nosql.distributed;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected final Logger logger;
    private final SPARQLService sparql;
    private final MongoDBServiceV2 mongodb;

    private boolean customExceptionMappingFlag;
    private Map<Class<? extends Exception>, Function<Exception, Exception>> exceptionMapping;

    public SparqlMongoTransaction(SPARQLService sparql, MongoDBServiceV2 mongodb) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        customExceptionMappingFlag = false;
        logger = LoggerFactory.getLogger(getClass());
    }

    /**
     * Define custom handling of exception. This method is required when for some business logic,
     * a specific Exception is expected, and a custom action must be applied.
     * During {@link #execute(ThrowingFunction)}call,
     * if an {@link Exception} is catch, then database transactions are rollback, this method allow to define custom management
     * of the exception, after database rollback.
     *
     * @param inputExceptionClass A specific Exception class, which when is thrown, requires a specific action
     * @param customExceptionLogic The custom exception to throw when the targeted exception is caught
     *
     * @return this {@link SparqlMongoTransaction}
     * @apiNote This method must be called BEFORE the call to {@link #execute(ThrowingFunction)} method.
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
     * Execute a {@link java.util.function.BiFunction} which requires transaction management on RDF and MongoDB database.
     * This method ensure that transaction is started and (committed or rollback)
     *
     * @param operation the operation to execute (not null)
     * @return the function results
     * @param <R> the function results type
     * @param <E> the type of Exception thrown during Function application
     * @throws Exception if any Exception is catch during Function application
     *
     * @apiNote
     * <ul>
     *     <li>The method works in two step : first the operation is applied, then transaction are committed on both database </li>
     * </ul>
     */
    public <R, E extends Exception> R execute(ThrowingFunction<ClientSession, R, E> operation
    ) throws Exception {

        Objects.requireNonNull(operation);

        try {
            sparql.startTransaction();
            R result = mongodb.computeThrowingTransaction(operation::apply);

            if (sparql.hasActiveTransaction()) {
                sparql.commitTransaction();
            }
            return result;
        } catch (Exception e) {
            if (sparql.hasActiveTransaction()) {
                sparql.rollbackTransaction(e);
            }
            throw handleTransaction(e);
        }
        /* Phase 1 : Check if the database is ready to commit the transaction
         - Start transactions
         - Performs operation on each data-source
         */

    }

    private Exception handleTransaction(Exception e) {

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