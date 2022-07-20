package org.opensilex.nosql.datasource.saga;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.coordinator.AbstractDistributedCoordinator;
import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;

public abstract class SagaDistributedCoordinator extends AbstractDistributedCoordinator<MongoSagaOperation, SparqlSagaOperation> {

    public SagaDistributedCoordinator(SPARQLService sparql, MongoDBService mongodb) {
        super(sparql, mongodb);
    }

    private void prepareForCommit(boolean sparqlFirst, ClientSession mongoSession) throws Exception {
        try{
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
        }catch (Exception e){
            // TODO property handle transaction rollback failure
            if(sparql.hasActiveTransaction()){
                sparql.rollbackTransaction();
            }
            if(mongoSession.hasActiveTransaction()){
                mongoSession.abortTransaction();
            }
            mongoSession.close();
            throw e;
        }
    }

    private void commitOrCompensate(ClientSession mongoSession) throws Exception {
        boolean sparqlCommitted = false;

        try{
            sparql.commitTransaction();
            sparqlCommitted = true;

            mongoSession.commitTransaction();
        }catch (Exception e){

            // TODO : property handled compensation retry
            if(sparqlCommitted){
                for(SparqlSagaOperation sparqlSagaOperation : getSparqlOperations()){
                    sparqlSagaOperation.getCompensationAction().accept(sparql);
                }
            }
            else{
                sparql.rollbackTransaction();
            }

            // TODO property handle transaction rollback failure
            mongoSession.abortTransaction();

        }finally {
            mongoSession.close();
        }
    }

    @Override
    public void run(boolean sparqlFirst) throws Exception {
        ClientSession mongoSession = mongodb.startSession();

        sparql.startTransaction();
        mongoSession.startTransaction();

        prepareForCommit(sparqlFirst,mongoSession);
        commitOrCompensate(mongoSession);
    }
}
