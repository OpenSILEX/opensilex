package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.nosql.datasource.operation.DataSourceOperation.OPERATION_STATE;
import org.opensilex.nosql.datasource.operation.UnitOfWork;
import org.opensilex.utils.ThrowingConsumer;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractDistributedCoordinator<O extends DataSourceOperation<?>>
        implements DistributedDataSourceCoordinator<O> {

    protected final  Map<Object, UnitOfWork<?>> unitOfWorks;

    protected final Map<Object, List<DataSourceOperation<?>>> operationsByDatabase;

    protected AbstractDistributedCoordinator() {
        operationsByDatabase = new HashMap<>();
        unitOfWorks = new HashMap<>();
    }


    @Override
    public <T> void addOperation(T unitOfWork, O operation) {
        if(! unitOfWorks.containsKey(unitOfWork)){
            throw new IllegalArgumentException("Unregistered unit of work " + unitOfWork.getClass().getSimpleName() + " : " + unitOfWork);
        }
        operationsByDatabase.computeIfAbsent(unitOfWork,key -> new LinkedList<>()).add(operation);
    }

    public <T> void registerDataSource(T dataSource,
                                       ThrowingConsumer<T, Exception> start,
                                       ThrowingConsumer<T, Exception> commit,
                                       ThrowingConsumer<T, Exception> rollback,
                                       ThrowingConsumer<T, Exception> close
    ) {
        unitOfWorks.put(dataSource, new UnitOfWork<>(dataSource, start, commit, rollback, close));
    }

    private <T> void performsActionsOnEachDatabase(Function<UnitOfWork<?>, ThrowingConsumer<T, Exception>> action, OPERATION_STATE state) throws Exception {

        for (Map.Entry<Object, UnitOfWork<?>> entry : unitOfWorks.entrySet()) {
            UnitOfWork<T> unitOfWork = (UnitOfWork<T>) entry.getValue();

            T datasource = unitOfWork.getDataSource();
            action.apply(unitOfWork).accept(datasource);

            if(state != null){
                operationsByDatabase.get(datasource).forEach(operation -> operation.setState(state));
            }
        }
    }

    protected void start() throws Exception {
      performsActionsOnEachDatabase(UnitOfWork::getStartAction,null);
    }

    protected void commit() throws Exception {
        performsActionsOnEachDatabase(UnitOfWork::getCommitAction, OPERATION_STATE.COMMITTED);

    }

    protected void rollback() throws Exception {
        performsActionsOnEachDatabase(UnitOfWork::getRollbackAction, OPERATION_STATE.ROLLBACK);
    }


    abstract void resolveCommitFail();

    <T> void prepareForCommit() throws Exception {
        for (Map.Entry<Object, List<DataSourceOperation<?>>> entry : operationsByDatabase.entrySet()) {
            T unitOfWork = (T) entry.getKey();
            List<DataSourceOperation<?>> operations = entry.getValue();

            for (DataSourceOperation<?> operation : operations) {
                DataSourceOperation<T> casted = (DataSourceOperation<T>) operation;
                casted.accept(unitOfWork);
            }
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
        } catch (Exception e) {
            resolveCommitFail();
            throw e;
        }
    }

}
