package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.CompoundOperation;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.nosql.datasource.operation.DataSourceOperation.OPERATION_STATE;
import org.opensilex.nosql.datasource.operation.LocalTransaction;
import org.opensilex.utils.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 *
 */
public abstract class AbstractDistributedCoordinator implements DistributedDataSourceCoordinator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractDistributedCoordinator.class);

    /**
     *
     */
    protected final Map<Object, LocalTransaction<?>> localDatabases;

    /**
     *
     */
    protected final Map<Object, List<DataSourceOperation<?>>> operationsByDatabase;

    /**
     *
     */
    protected final LinkedList<DataSourceOperation<?>> operationsExecutionOrder;

    protected AbstractDistributedCoordinator() {
        operationsByDatabase = new HashMap<>();
        localDatabases = new HashMap<>();
        operationsExecutionOrder = new LinkedList<>();
    }


    @Override
    public void addOperation(DataSourceOperation<?> operation) {
        Objects.requireNonNull(operation);

        Object dataSource = operation.getDataSource();
        Objects.requireNonNull(dataSource);

        if(operation instanceof CompoundOperation){
            this.addMixedOperation((CompoundOperation) operation);
            return;
        }

        if (!localDatabases.containsKey(dataSource)) {
            throw new IllegalArgumentException("Unregistered unit of work " + dataSource.getClass().getSimpleName() + " : " + dataSource);
        }
        operationsByDatabase.computeIfAbsent(dataSource, key -> new LinkedList<>()).add(operation);
        operationsExecutionOrder.add(operation);
    }

    @Override
    public void addMixedOperation(CompoundOperation operation) {
         Objects.requireNonNull(operation);
         operationsExecutionOrder.add(operation);
    }

    @Override
    public <T> void registerDataSource(T localDatabase,
                                       ThrowingConsumer<T, Exception> start,
                                       ThrowingConsumer<T, Exception> commit,
                                       ThrowingConsumer<T, Exception> rollback,
                                       ThrowingConsumer<T, Exception> close,
                                       String description
    ) {
        localDatabases.put(localDatabase, new LocalTransaction<>(localDatabase, start, commit, rollback, close, description));
        LOGGER.debug("Registering local database : {}", localDatabase);
    }

    /**
     *
     * @param action
     * @param state
     * @param actionMsg
     * @param <T>
     * @throws Exception
     */
    private <T> void performsActionsOnEachDatabase(
            Function<LocalTransaction<?>,
            ThrowingConsumer<T, Exception>> action,
            OPERATION_STATE state,
            String actionMsg) throws Exception {

        for (Map.Entry<Object, LocalTransaction<?>> entry : localDatabases.entrySet()) {
            LocalTransaction<T> localDatabase = (LocalTransaction<T>) entry.getValue();
            T datasource = localDatabase.getLocalDatabase();

            action.apply(localDatabase).accept(datasource);
            LOGGER.debug("\t[{}] on database {} [OK]", actionMsg, localDatabase.getDescription());

            if (state != null) {
                List<DataSourceOperation<?>> dataSourceOperations = operationsByDatabase.get(datasource);

                // some data sources can be registered without being effectively used
                // because it can depend on the execution flow of previous operations
                if( dataSourceOperations != null){
                    dataSourceOperations.forEach(operation -> operation.setState(state));
                }
            }
        }
    }

    /**
     *
     * @throws Exception
     */
    protected void start() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getStartAction, null,"TRANSACTION_START");
    }


    /**
     *
     * @throws Exception
     */
    protected void commit() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getCommitAction, OPERATION_STATE.COMMITTED,"TRANSACTION_COMMIT");

    }

    /**
     *
     * @throws Exception
     */
    protected void rollback() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getRollbackAction, OPERATION_STATE.ROLLBACK,"TRANSACTION_ROLLBACK");
    }

    /**
     *
     * @throws Exception
     */
    protected void close() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getCloseAction, null,"TRANSACTION_CLOSE");
    }


    /**
     *
     */
    abstract void resolveCommitFail();

    /**
     *
     * @throws Exception
     */
    protected void prepareForCommit() throws Exception {

        while(! operationsExecutionOrder.isEmpty()){
            DataSourceOperation<?> operation = operationsExecutionOrder.removeFirst();

            int oldSize = operationsExecutionOrder.size();
            operation.run();

            if(operation instanceof CompoundOperation){
                handleCompoundOperation(oldSize, (CompoundOperation) operation);
            }
        }
    }

    /**
     *
     * @param oldSize
     * @param operation
     */
    private void handleCompoundOperation(int oldSize, CompoundOperation operation){

        int newSize = operationsExecutionOrder.size();
        int newOperationNb = newSize - oldSize;

        if(newOperationNb < 0){
            // should never have happened, since operation which access to the coordinator only access to addOperation() method, not to the internal operations list
            throw new IllegalStateException("Fatal error, the previous operation has deleted incoming operation");
        }

        // no new operation were registered during the compound operation execution
        if(newOperationNb == 0){
            return;
        }

        // the operations can be evaluated after, so just continue the operation execution flow
        if(! operation.nestedOperationAreEvaluatedNow()){
            return;
        }

        //the compound operation has triggered the add of other simple operation at the end of the queue/list
        //in the case of nestedOperationAreEvaluatedNow() is true, then these operations must be evaluated just after, so we need to update the order of execution

        // Ex: if the current order is [ o1, o2, o3, o4 ] and considering that [o3,o4] were triggered by the CompoundOperation
        // then the desired order is [ o3, o4, o1, o2]

        // execution order is read from the end, and each operation are inserted in beginning,
        // so nested/triggered operation are well executed in FIFO order
        for (int i = 0; i < newOperationNb; i++) {
            DataSourceOperation<?> newRegisteredOperation = operationsExecutionOrder.removeLast();
            operationsExecutionOrder.addFirst(newRegisteredOperation);
        }
    }

    @Override
    public void run() throws Exception {
        start();

        try {
            prepareForCommit();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            rollback();
            throw e;
        }

        try {
            commit();
            close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            close();
            resolveCommitFail();
            throw e;
        }
    }

}
