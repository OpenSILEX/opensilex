package org.opensilex.nosql.datasource.saga;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.coordinator.DefaultDataSourceCoordinator;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

import java.util.List;
import java.util.Map;

public class SagaDistributedCoordinator extends DefaultDataSourceCoordinator<SagaDataSourceOperation<?>> {

    public SagaDistributedCoordinator(SPARQLService sparql, ClientSession session) {
        super(sparql, session);
    }

    @Override
    public <T> void addOperation(T unitOfWork, SagaDataSourceOperation<?> operation) {
        super.addOperation(unitOfWork, operation);
    }

    @Override
    protected void resolveCommitFail() {

        // loop over database

        for (Map.Entry<Object, List<DataSourceOperation<?>>> entry : operationsByDatabase.entrySet()) {
            List<DataSourceOperation<?>> operations = entry.getValue();

            for(DataSourceOperation<?> operation : operations){
                if(operation.getState().equals(DataSourceOperation.OPERATION_STATE.COMMITTED)){
                    SagaDataSourceOperation<?> sagaOperation = (SagaDataSourceOperation<?>) operation;
                    compensate(sagaOperation);
                }
            }
        }
        // for each ALREADY COMMITTED operation -> compensate them
        super.resolveCommitFail();
    }

    protected void compensate(SagaDataSourceOperation sagaOperation){
        ThrowingConsumer compensateAction = sagaOperation.getCompensationAction();
        Object compensationContext = sagaOperation.getCompensationContext();

        // #TODO try multiple times to resolve
        try {
            compensateAction.accept(compensationContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
