package org.opensilex.nosql.datasource.operation;

import org.opensilex.utils.ThrowingConsumer;

public class UnitOfWork<T> {

    T localTransaction;
    ThrowingConsumer<T, Exception> commitAction;
    ThrowingConsumer<T, Exception> startAction;
    ThrowingConsumer<T, Exception> rollbackAction;
    ThrowingConsumer<T, Exception> closeAction;

    public UnitOfWork(T unitOfWork, ThrowingConsumer<T, Exception> commitAction, ThrowingConsumer<T, Exception> startAction, ThrowingConsumer<T, Exception> rollbackAction, ThrowingConsumer<T, Exception> closeAction) {
        this.localTransaction = unitOfWork;
        this.commitAction = commitAction;
        this.startAction = startAction;
        this.rollbackAction = rollbackAction;
        this.closeAction = closeAction;
    }

    public T getDataSource() {
        return localTransaction;
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
}
