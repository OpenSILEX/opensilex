package org.opensilex.nosql.datasource.operation;

import org.opensilex.utils.ThrowingConsumer;

/**
 * Utility object used to define for a datasource, which operations apply for
 * <li>start a transaction : {@link #startAction}</li>
 * <li>commit a transaction : {@link #commitAction}</li>
 * <li>rollback a transaction : {@link #rollbackAction}</li>
 * <li>close a transaction : {@link #closeAction}</li>
 * @param <T> the type of datasource (depend on the driver used to access a database)
 *
 * @author rcolin
 */
public class LocalTransaction<T> {

    final T localDatabase;
    final ThrowingConsumer<T, Exception> commitAction;
    final ThrowingConsumer<T, Exception> startAction;
    final ThrowingConsumer<T, Exception> rollbackAction;
    final ThrowingConsumer<T, Exception> closeAction;

    final String description;

    public LocalTransaction(T localDatabase, ThrowingConsumer<T, Exception> commitAction, ThrowingConsumer<T, Exception> startAction, ThrowingConsumer<T, Exception> rollbackAction, ThrowingConsumer<T, Exception> closeAction, String description) {
        this.localDatabase = localDatabase;
        this.commitAction = commitAction;
        this.startAction = startAction;
        this.rollbackAction = rollbackAction;
        this.closeAction = closeAction;
        this.description = description;
    }

    public T getLocalDatabase() {
        return localDatabase;
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
