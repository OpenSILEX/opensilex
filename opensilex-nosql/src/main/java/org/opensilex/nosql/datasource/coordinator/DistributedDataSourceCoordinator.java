package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.CompoundOperation;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.utils.ThrowingConsumer;

/**
 * Define how to coordinate operation which applies on multiple database <br>
 * The coordinator take the responsibility to register a data source, to add one or multiple operation
 * for a data source and to run these operations by properly handling data coherence
 *
 * @author rcolin
 */
public interface DistributedDataSourceCoordinator
{
    /**
     * Add an operation to execute
     * @param operation the operation to execute
     * This object depends of the driver/API used to perform operation on a database.
     * Ex:  Connection for a JDBC database, {@link com.mongodb.client.ClientSession} for a MongoDB database
     *
     * @apiNote The order or operation registering must be handled
     * @throws IllegalArgumentException if dataSource has not been already registered by calling {@link #registerDataSource(Object, ThrowingConsumer, ThrowingConsumer, ThrowingConsumer, ThrowingConsumer, String)}
     */
   void addOperation(DataSourceOperation<?> operation) throws IllegalArgumentException;


    /**
     *
     * @param operation
     * @throws Exception
     */
    void addMixedOperation(CompoundOperation operation) throws Exception;

    /**
     * Register a data source
     * @param <T> the kind of datasource on which applying a local transaction.
     * This object depends of the driver/API used to perform operation on a database.
     * Ex:  Connection for a JDBC database, {@link com.mongodb.client.ClientSession} for a MongoDB database
     *
     * @param dataSource the datasource on which apply the operation
     * @param start an action which define how to start transaction on the datasource
     * @param commit an action which define how to commit transaction on the datasource
     * @param rollback an action which define how to rollback transaction on the datasource
     * @param close an action which define how to close transaction on the datasource
     * @param description a textual description of the data source (optional)
     */
     <T> void registerDataSource(T dataSource,
                                 ThrowingConsumer<T, Exception> start,
                                 ThrowingConsumer<T, Exception> commit,
                                 ThrowingConsumer<T, Exception> rollback,
                                 ThrowingConsumer<T, Exception> close,
                                 String description);

    /**
     * Run all registered operation by handling data coherence
     * @throws Exception if an Exception has been throw during the execution of an operation.
     * The implementation must take the responsibility to properly handle data state on each started/committed transaction before
     * rethrowing the inner Exception
     */
    void run() throws Exception;


}