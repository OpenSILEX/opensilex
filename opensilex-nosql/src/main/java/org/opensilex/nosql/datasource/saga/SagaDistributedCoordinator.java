package org.opensilex.nosql.datasource.saga;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.coordinator.DefaultDataSourceCoordinator;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SagaDistributedCoordinator extends DefaultDataSourceCoordinator<SagaDataSourceOperation<?>> {

    private static final int MAX_COMPENSATION_RETRY_NB = 3;
    private static final int RETRY_TIMEOUT_IN_SECONDS = 60;
    public SagaDistributedCoordinator(SPARQLService sparql, ClientSession session) {
        super(sparql, session);
    }

    @Override
    public <T> void addOperation(T dataSource, SagaDataSourceOperation<?> operation) {
        super.addOperation(dataSource, operation);
    }

    @Override
    protected void resolveCommitFail() {

        // loop over database and find all operation which have already been committed
        for (Map.Entry<Object, List<DataSourceOperation<?>>> entry : operationsByDatabase.entrySet()) {
            List<DataSourceOperation<?>> operations = entry.getValue();

            for(DataSourceOperation<?> operation : operations){

                // for all these operations, performs a compensation
                if(operation.getState().equals(DataSourceOperation.OPERATION_STATE.COMMITTED)){
                    SagaDataSourceOperation<?> sagaOperation = (SagaDataSourceOperation<?>) operation;
                    compensate(sagaOperation);
                }
            }
        }
    }

    /**
     *
     * @param sagaOperation
     */
    protected void compensate(SagaDataSourceOperation sagaOperation){
        ThrowingConsumer compensateAction = sagaOperation.getCompensationAction();
        Object compensationContext = sagaOperation.getCompensationContext();

        boolean compensationExecuted = false;
        int i= 0;
        while (! compensationExecuted){
            i++;
            try {
                compensateAction.accept(compensationContext);
                compensationExecuted = true;
            } catch (Exception e) {
                if(i == MAX_COMPENSATION_RETRY_NB){
                    throw new RuntimeException(e);
                }
            }
        }

        // #TODO define compensation context closing

    }
}
