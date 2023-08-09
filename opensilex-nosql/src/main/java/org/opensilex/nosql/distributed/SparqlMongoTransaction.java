package org.opensilex.nosql.distributed;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.functionnal.ThrowingBiFunction;


public class SparqlMongoTransaction implements DistributedOperation {

    private final SPARQLService sparql;
    private final MongoDBService mongodb;

    public SparqlMongoTransaction(SPARQLService sparql, MongoDBService mongodb) {
        this.sparql = sparql;
        this.mongodb = mongodb;
    }

    public <R, E extends Exception> R execute(ThrowingBiFunction<SPARQLService, ClientSession, R, E> operation
    ) throws Exception {
        return this.execute(sparql, mongodb.newSession(), operation);
    }


    public <R, E extends Exception> R execute(SPARQLService sparqlConnection, ClientSession mongoSession, ThrowingBiFunction<SPARQLService, ClientSession, R, E> operation
    ) throws Exception {

        try {
            sparql.startTransaction();
            mongoSession.startTransaction();

            R result = operation.apply(sparqlConnection, mongoSession);
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
        finally {
            mongoSession.close();
            sparql.close();
        }
    }
}
