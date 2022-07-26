package org.opensilex.nosql.datasource.saga;

import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.utils.ThrowingConsumer;

/**
 * Extension of any {@link DataSourceOperation} which define which operation execute if we need to
 * abort an operation which has been already committed in distributed database context
 * @param <T>
 */
public interface SagaDataSourceOperation<T> extends DataSourceOperation<T>  {

    /**
     *
     * @return the action to execute if the operation has been committed but must be canceled
     */
    ThrowingConsumer<T,Exception> getCompensationAction();

    /**
     * @return the kind of object which allow to perform a compensation transaction
     */
    T getCompensationContext();
}
