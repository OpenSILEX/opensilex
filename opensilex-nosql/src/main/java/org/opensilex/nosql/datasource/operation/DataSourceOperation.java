package org.opensilex.nosql.datasource.operation;

public interface DataSourceOperation {

    enum OPERATION_STATE {
        READY_TO_COMMIT,
        COMMITTED
    }

    void setState(OPERATION_STATE state);

    OPERATION_STATE getState();
}
