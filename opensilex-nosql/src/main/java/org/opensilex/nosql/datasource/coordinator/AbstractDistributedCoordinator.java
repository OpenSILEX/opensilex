package org.opensilex.nosql.datasource.coordinator;

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

        if (!localDatabases.containsKey(dataSource)) {
            throw new IllegalArgumentException("Unregistered unit of work " + dataSource.getClass().getSimpleName() + " : " + dataSource);
        }
        operationsByDatabase.computeIfAbsent(dataSource, key -> new LinkedList<>()).add(operation);
        operationsExecutionOrder.add(operation);
    }

    @Override
    public void addMixedOperation(ThrowingConsumer<DistributedDataSourceCoordinator, Exception> consumer, boolean nestedOperationAreEvaluatedNow) {
        Objects.requireNonNull(consumer);
        operationsExecutionOrder.add(new CompoundOperation(this, consumer, nestedOperationAreEvaluatedNow));
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
    }

    /**
     * Utility method used for updating state on each registered database
     * @param transactionAction the function to execution of a data-source for updating its transaction state
     * @param state the state of operation after data-source state change
     * @param actionMsg a message which describe the operation (for the purpose of logging)
     * @param <T> the type of datasource
     *
     */
    private <T> void performsActionsOnEachDatabase(
            Function<LocalTransaction<?>, ThrowingConsumer<T, Exception>> transactionAction,
            OPERATION_STATE state,
            String actionMsg) throws Exception {

        for (Map.Entry<Object, LocalTransaction<?>> entry : localDatabases.entrySet()) {
            LocalTransaction<T> localDatabase = (LocalTransaction<T>) entry.getValue();
            T datasource = localDatabase.getLocalDatabase();

            transactionAction.apply(localDatabase).accept(datasource);
            LOGGER.debug("\t[{}] on database {} [OK]", actionMsg, localDatabase.getDescription());

            if (state != null) {
                List<DataSourceOperation<?>> dataSourceOperations = operationsByDatabase.get(datasource);

                // some data sources can be registered without being effectively used
                // because it can depend on the execution flow of previous operations'
                // ex : the execution of the operation B depends on the result of A
                if (dataSourceOperations != null) {
                    dataSourceOperations.forEach(operation -> operation.setState(state));
                }
            }
        }
    }

    /**
     * Start transaction of each registered data-source
     * @throws Exception if some error where encountered during some transactions start
     */
    protected void startTransactions() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getStartAction, OPERATION_STATE.STARTED, "TRANSACTION_START");
    }


    /**
     * Commit transaction of each registered data-source
     * @throws Exception if some error where encountered during some transactions commit
     */
    protected void commitTransactions() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getCommitAction, OPERATION_STATE.COMMITTED, "TRANSACTION_COMMIT");

    }

    /**
     * Rollback transaction of each registered data-source
     * @throws Exception if some error where encountered during some transactions rollback
     */
    protected void rollbackTransactions() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getRollbackAction, OPERATION_STATE.ROLLBACK, "TRANSACTION_ROLLBACK");
    }

    /**
     * Close transaction of each registered data-source
     * @throws Exception if some error where encountered during some transactions close
     */
    protected void closeTransactions() throws Exception {
        performsActionsOnEachDatabase(LocalTransaction::getCloseAction, null, "TRANSACTION_CLOSE");
    }


    /**
     * Define how to cancel changes after that some database have already committed changes
     *
     */
    abstract void resolveCommitFail();

    /**
     * Execute each registered operation on theirs corresponding datasource
     * @throws Exception if some error is encountered during datasource write operation
     */
    protected void prepareForCommit() throws Exception {

        // read operations as a queue
        while (!operationsExecutionOrder.isEmpty()) {
            DataSourceOperation<?> operation = operationsExecutionOrder.removeFirst();

            // determine old size in order to be able to known if the operation
            // has triggered the further execution of other operation
            int oldSize = operationsExecutionOrder.size();

            operation.run();

            // if the operation is Compound, then it's susceptible to have
            // triggered the further execution of other operation
            if (operation instanceof CompoundOperation) {
                handleCompoundOperation(oldSize, (CompoundOperation) operation);
            }
        }
    }

    /**
     * <pre>
     * Re-order execution flow if <b>operation</b> has triggered other operation and if
     * it's specified that these operations must be evaluated just now.
     * This information depends on {@link CompoundOperation#nestedOperationAreEvaluatedNow()}
     * </pre>
     *
     * @param oldSize number of pending operation before execution of <b>operation</b>
     * @param operation the compound operation which may have triggered other operation executions
     */
    private void handleCompoundOperation(int oldSize, CompoundOperation operation) {

        int newSize = operationsExecutionOrder.size();
        int newOperationNb = newSize - oldSize;

        if (newOperationNb < 0) {
            // should never have happened, since operation which access to the coordinator only access to addOperation() method, not to the internal operations list
            throw new IllegalStateException("Fatal error, the previous operation has deleted incoming operation");
        }

        // no new operation were registered during the compound operation execution
        if (newOperationNb == 0) {
            return;
        }

        // the operations can be evaluated after, so just continue the operation execution flow
        if (!operation.nestedOperationAreEvaluatedNow()) {
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

        try {
            startTransactions();
        }catch (Exception e){
            closeTransactions();
            throw e;
        }

        try {
            prepareForCommit();
        } catch (Exception e) {
            rollbackTransactions();
            closeTransactions();
            throw e;
        }

        try {
            commitTransactions();
            closeTransactions();
        } catch (Exception e) {
            closeTransactions();
            resolveCommitFail();
            throw e;
        }
    }

}
