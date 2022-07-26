package org.opensilex.nosql.datasource.operation;

import org.opensilex.utils.ThrowingConsumer;

public class LocalDataBase<T> {

    final T dataSource;
    final ThrowingConsumer<T, Exception> commitAction;
    final ThrowingConsumer<T, Exception> startAction;
    final ThrowingConsumer<T, Exception> rollbackAction;
    final ThrowingConsumer<T, Exception> closeAction;

    final String description;

    public LocalDataBase(T unitOfWork, ThrowingConsumer<T, Exception> commitAction, ThrowingConsumer<T, Exception> startAction, ThrowingConsumer<T, Exception> rollbackAction, ThrowingConsumer<T, Exception> closeAction, String description) {
        this.dataSource = unitOfWork;
        this.commitAction = commitAction;
        this.startAction = startAction;
        this.rollbackAction = rollbackAction;
        this.closeAction = closeAction;
        this.description = description;
    }

    public T getDataSource() {
        return dataSource;
    }

    public ThrowingConsumer<T, Exception> getCommitAction() {
        return commitAction;
    }

    public ThrowingConsumer<T, Exception> getStartAction() {
        return startAction;
    }

    public ThrowingConsumer<T, Exception> getRollbackAction() {
        return rollbackAction;
    }

    public ThrowingConsumer<T, Exception> getCloseAction() {
        return closeAction;
    }

    public String getDescription() {
        return description;
    }
}
