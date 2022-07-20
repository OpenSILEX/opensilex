package org.opensilex.nosql.datasource.twoPhaseCommit;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.coordinator.AbstractDistributedCoordinator;
import org.opensilex.nosql.datasource.operation.DataSourceOperation.OPERATION_STATE;
import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;

public class TwoPhaseCommitCoordinator<M extends MongoOperation, S extends SparqlOperation> extends AbstractDistributedCoordinator<M, S> {

    public TwoPhaseCommitCoordinator(SPARQLService sparql, MongoDBService mongodb) {
        super(sparql, mongodb);
    }

    @Override
    public void run(boolean sparqlFirst) throws Exception {

        ClientSession mongoSession = mongodb.startSession();
        try {
            sparql.startTransaction();
            mongoSession.startTransaction();

            if (sparqlFirst) {
                for (SparqlOperation operation : getSparqlOperations()) {
                    operation.accept(sparql);
                }
                for (MongoOperation operation : getMongoOperations()) {
                    operation.accept(mongoSession);
                }
            } else {
                for (MongoOperation operation : getMongoOperations()) {
                    operation.accept(mongoSession);
                }
                for (SparqlOperation operation : getSparqlOperations()) {
                    operation.accept(sparql);
                }
            }

            sparql.commitTransaction();
            mongoSession.commitTransaction();

        } catch (Exception e) {
            if (sparql.hasActiveTransaction()) {
                sparql.rollbackTransaction();
            }
            if (mongoSession.hasActiveTransaction()) {
                mongoSession.abortTransaction();
            }
            throw e;
        } finally {
            mongoSession.close();
        }
    }
}
