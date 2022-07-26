package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.utils.ThrowingConsumer;

public interface DistributedDataSourceCoordinator<O extends DataSourceOperation<?>>
{
    <T> void addOperation(T unitOfWork, O operation);

     <T> void registerDataSource(T dataSource,
                                 ThrowingConsumer<T, Exception> start,
                                 ThrowingConsumer<T, Exception> commit,
                                 ThrowingConsumer<T, Exception> rollback,
                                 ThrowingConsumer<T, Exception> close,
                                 String description);

    void run() throws Exception;

}
