package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.nosql.datasource.operation.DataSourceOperation.OPERATION_STATE;
import org.opensilex.nosql.datasource.operation.LocalDataBase;
import org.opensilex.utils.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractDistributedCoordinator<O extends DataSourceOperation<?>>
        implements DistributedDataSourceCoordinator<O> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDistributedCoordinator.class);

    protected final Map<Object, LocalDataBase<?>> localDatabases;

    protected final Map<Object, List<DataSourceOperation<?>>> operationsByDatabase;

    protected AbstractDistributedCoordinator() {
        operationsByDatabase = new HashMap<>();
        localDatabases = new HashMap<>();
    }


    @Override
    public <T> void addOperation(T dataSource, O operation) {
        if (!localDatabases.containsKey(dataSource)) {
            throw new IllegalArgumentException("Unregistered unit of work " + dataSource.getClass().getSimpleName() + " : " + dataSource);
        }
        operationsByDatabase.computeIfAbsent(dataSource, key -> new LinkedList<>()).add(operation);
    }

    @Override
    public <T> void registerDataSource(T localDatabase,
                                       ThrowingConsumer<T, Exception> start,
                                       ThrowingConsumer<T, Exception> commit,
                                       ThrowingConsumer<T, Exception> rollback,
                                       ThrowingConsumer<T, Exception> close,
                                       String description
    ) {
        localDatabases.put(localDatabase, new LocalDataBase<>(localDatabase, start, commit, rollback, close, description));
        LOGGER.debug("Registering local database : {}", localDatabase);
    }

    private <T> void performsActionsOnEachDatabase(Function<LocalDataBase<?>, ThrowingConsumer<T, Exception>> action, OPERATION_STATE state, String actionMsg) throws Exception {

        for (Map.Entry<Object, LocalDataBase<?>> entry : localDatabases.entrySet()) {
            LocalDataBase<T> localDatabase = (LocalDataBase<T>) entry.getValue();
            T datasource = localDatabase.getDataSource();

            LOGGER.debug("\t[{}] on database {} [START]", actionMsg, localDatabase.getDescription());
            action.apply(localDatabase).accept(datasource);
            LOGGER.debug("\t[{}] on database {} [OK]", actionMsg, localDatabase.getDescription());

            if (state != null) {
                operationsByDatabase.get(datasource).forEach(operation -> operation.setState(state));
            }
        }
    }

    protected void start() throws Exception {
        performsActionsOnEachDatabase(LocalDataBase::getStartAction, null,"TRANSACTION_START");

    }

    protected void commit() throws Exception {
        performsActionsOnEachDatabase(LocalDataBase::getCommitAction, OPERATION_STATE.COMMITTED,"TRANSACTION_COMMIT");

    }

    protected void rollback() throws Exception {
        performsActionsOnEachDatabase(LocalDataBase::getRollbackAction, OPERATION_STATE.ROLLBACK,"TRANSACTION_ROLLBACK");
    }

    protected void close() throws Exception {
        performsActionsOnEachDatabase(LocalDataBase::getCloseAction, null,"TRANSACTION_CLOSE");
    }


    abstract void resolveCommitFail();

    protected <T> void prepareForCommit() throws Exception {

        // loop over registered local databases
        for (Map.Entry<Object, List<DataSourceOperation<?>>> entry : operationsByDatabase.entrySet()) {
            T dataSource = (T) entry.getKey();
            List<DataSourceOperation<?>> operations = entry.getValue();

            LocalDataBase<T> localDataBase = (LocalDataBase<T>) this.localDatabases.get(dataSource);

            LOGGER.debug("\t\t{} operation(s) on database {} [START]", operations.size(), localDataBase.getDescription());
            // execute each operation on local database
            for (DataSourceOperation<?> operation : operations) {
                DataSourceOperation<T> castedOperation = (DataSourceOperation<T>) operation;
                castedOperation.accept(dataSource);
            }
            LOGGER.debug("\t\t{} operation(s) on database {} [OK]", operations.size(), localDataBase.getDescription());
        }
    }

    @Override
    public void run() throws Exception {
        start();

        try {
            prepareForCommit();
        } catch (Exception e) {
            rollback();
            throw e;
        }

        try {
            commit();
            close();
        } catch (Exception e) {
            close();
            try{
                resolveCommitFail();
            }catch (Exception e2){
                LOGGER.error("Error during commit fail resolving: {}", e2.getMessage());
                throw e2;
            }
            throw e;
        }
    }

}
