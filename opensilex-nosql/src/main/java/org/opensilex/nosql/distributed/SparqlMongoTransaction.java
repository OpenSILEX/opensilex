package org.opensilex.nosql.distributed;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.functionnal.ThrowingBiFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 *
 */
public class SparqlMongoTransaction {

    private final SPARQLService sparql;
    private final MongoDBService mongodb;
    private final Map<Class<? extends Exception>, Function<Exception, Exception>> exceptionMapping;

    public SparqlMongoTransaction(SPARQLService sparql, MongoDBService mongodb) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        exceptionMapping = new HashMap<>();
    }

    public SparqlMongoTransaction handleCustomException(Class<? extends Exception> inputExceptionClass, Function<Exception, Exception> customExceptionLogic) {
        exceptionMapping.put(inputExceptionClass, customExceptionLogic);
        return this;
    }

    public <R, E extends Exception> R execute(ThrowingBiFunction<SPARQLService, ClientSession, R, E> operation
    ) throws Exception {

        try(ClientSession session = mongodb.newSession()){
            return this.execute(sparql, session, operation);
        }
    }


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
        if (sparql.hasActiveTransaction()) {
            sparql.rollbackTransaction();
        }
        if (mongoSession.hasActiveTransaction()) {
            mongoSession.abortTransaction();
        }
        Class<? extends Exception> exceptionClass = e.getClass();
        if (exceptionMapping.containsKey(exceptionClass)) {
            return exceptionMapping.get(exceptionClass).apply(e);
        } else {
            return e;
        }
    }
}
