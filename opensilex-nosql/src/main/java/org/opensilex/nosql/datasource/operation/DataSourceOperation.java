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
public interface DataSourceOperation<T>{

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

    /**
     * Execute the operation on the data-source returned by {{@link #getDataSource()}}
     * @throws Exception if an error is encountered during operation execution
     */
    void run() throws Exception;

    /**
     *
     * @return the datasource on which the operation must be executed
     */
    T getDataSource();

    /**
     *
     * @return the effective operation which must be executed
     */
    ThrowingConsumer<T, Exception> getConsumer();

}
