package org.opensilex.nosql.datasource.saga;

import org.opensilex.nosql.datasource.operation.DataSourceOperation;

public interface SagaDataSourceOperation<T extends DataSourceOperation> {

    T getCompensationAction();
}
