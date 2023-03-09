/*
 *  *************************************************************************************
 *  DistributedTransaction.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2022
 * Contact :  renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.nosql;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingBiConsumer;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;

public class DistributedTransaction {

    private final SPARQLService sparql;
    private final MongoDBService mongodb;

    public DistributedTransaction(SPARQLService sparql, MongoDBService mongodb) {
        this.sparql = sparql;
        this.mongodb = mongodb;
    }

    public <R> R execute(
            ThrowingFunction<ClientSession, R, Exception> operation
    ) throws Exception {
        return execute(mongodb.newSession(), true, operation);
    }

    protected <R> R execute(
            ClientSession mongoSession,
            boolean closeSession,
            ThrowingFunction<ClientSession, R, Exception> distributedOperation
    ) throws Exception {

        sparql.startTransaction();
        mongoSession.startTransaction();

        try {
            R result = distributedOperation.apply(mongoSession);
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
        } finally {
            if (closeSession) {
                mongoSession.close();
            }
        }
    }

    public <T, R> R operationInTransaction(ClientSession mongoSession,
                                           boolean closeSession,
                                           ThrowingBiConsumer<T, ClientSession, Exception> distributedOperation,
                                           T customTransaction,
                                           ThrowingConsumer<T, Exception> startTrxOperation,
                                           ThrowingFunction<ClientSession, R, Exception> commitOperation,
                                           ThrowingConsumer<T, Exception> rollbackOperation) throws Exception {

        mongoSession.startTransaction();
        if (startTrxOperation != null) {
            startTrxOperation.accept(customTransaction);
        }

        try {
            distributedOperation.accept(customTransaction, mongoSession);

            R result = commitOperation.apply(mongoSession);
            if (mongoSession.hasActiveTransaction()) {
                mongoSession.commitTransaction();
            }
            return result;
        } catch (Exception e) {
            if (mongoSession.hasActiveTransaction()) {
                mongoSession.abortTransaction();
            }
            if (rollbackOperation != null) {
                rollbackOperation.accept(customTransaction);
            }
            throw e;
        } finally {
            if (closeSession) {
                mongoSession.close();
            }
        }
    }


}
