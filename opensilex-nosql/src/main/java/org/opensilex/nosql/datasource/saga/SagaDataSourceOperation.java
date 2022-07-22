package org.opensilex.nosql.datasource.saga;

import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.utils.ThrowingConsumer;

public interface SagaDataSourceOperation<T> extends DataSourceOperation<T>  {

    ThrowingConsumer<T,Exception> getCompensationAction();
    T getCompensationContext();
}
