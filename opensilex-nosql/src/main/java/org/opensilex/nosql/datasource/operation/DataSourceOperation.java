package org.opensilex.nosql.datasource.operation;

import org.opensilex.utils.ThrowingConsumer;

public interface DataSourceOperation<T> extends ThrowingConsumer<T,Exception> {

    long getId();

    void setState(OPERATION_STATE state);

    OPERATION_STATE getState();

    enum OPERATION_STATE{
        STARTED,
        READY_FOR_COMMIT,
        ROLLBACK,
        COMMITTED
    }

}
