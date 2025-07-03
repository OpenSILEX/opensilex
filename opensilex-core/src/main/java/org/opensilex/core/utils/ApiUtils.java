package org.opensilex.core.utils;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingFunction;

public class ApiUtils {
    /**
     * Runs function either in a transaction or nay
     *
     * @param function to be run
     * @param clientSession if not null then we know we are already in a transaction
     * @param sparql the sparql service
     * @param mongodb the mango service
     * @return the result of function
     * @param <R> return type of function
     * @param <E> Some Exception type
     * @throws Exception
     */
    public static <R, E extends Exception> R wrapWithTransaction(
            ThrowingFunction<ClientSession, R, E> function,
            ClientSession clientSession,
            SPARQLService sparql,
            MongoDBService mongodb
    ) throws Exception {
        if(clientSession == null){
            return new SparqlMongoTransaction(sparql, mongodb.getServiceV2()).execute(function);
        }
        return function.apply(clientSession);
    }
}
