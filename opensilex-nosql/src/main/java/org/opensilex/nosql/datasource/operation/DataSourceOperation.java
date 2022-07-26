package org.opensilex.nosql.datasource.operation;

import org.opensilex.utils.ThrowingConsumer;

/**
 * @author rcolin
 *
 * Represent an operation which can be executed of any database in the case of a distributed transaction
 *
 * Extends the {@link ThrowingConsumer} interface by adding concepts of identifier and state
 * @param <T> the type of object consumed by the operation, depend on the database on which the operation is executed
 */
public interface DataSourceOperation<T> extends ThrowingConsumer<T,Exception> {

    /**
     * @return the identifier of the operation
     */
    long getId();

    void setState(OPERATION_STATE state);

    OPERATION_STATE getState();

    /**
     * Define common states for any database operation
     */
    enum OPERATION_STATE{

        STARTED,
        READY_FOR_COMMIT,
        ROLLBACK,
        COMMITTED
    }

}
