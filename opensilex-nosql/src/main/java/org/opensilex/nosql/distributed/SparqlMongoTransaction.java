package org.opensilex.nosql.distributed;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.functionnal.ThrowingBiFunction;

/**
 *
 */
public class SparqlMongoTransaction {

    private final SPARQLService sparql;
    private final MongoDBService mongodb;

    public SparqlMongoTransaction(SPARQLService sparql, MongoDBService mongodb) {
        this.sparql = sparql;
        this.mongodb = mongodb;
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
            if (sparql.hasActiveTransaction()) {
                sparql.rollbackTransaction();
            }
            if (mongoSession.hasActiveTransaction()) {
                mongoSession.abortTransaction();
            }
            throw e;
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
            if (sparql.hasActiveTransaction()) {
                sparql.rollbackTransaction();
            }
            if (mongoSession.hasActiveTransaction()) {
                mongoSession.abortTransaction();
            }
            throw e;
        }
    }
}
