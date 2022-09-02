package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.utils.ThrowingConsumer;

/**
 * <pre>
 * Define how to perform transaction on multiple database
 * The coordinator take the responsibility to register a data source, to add one or multiple operation
 * for a data source and to run these operations by properly handling data coherence.
 *
 * I.e. if some error occurs on one database, the coordinator take the responsibility
 * to rollback changes on the data-source and to ensure that no changes were made on
 * other datasource by rollback changes on these databases.
 *
 * The use of this class helps to not handle manually the transaction start/rollback and commit, since
 * these operations are handled by this class.
 *
 * The cycle of use of this coordinator is :
 *
 * - <b>Register a datasource </b> by defining how to start, rollback, commit and close a transaction
 *      - {@link #registerDataSource(Object, ThrowingConsumer, ThrowingConsumer, ThrowingConsumer, ThrowingConsumer, String)}
 *
 * - <b>Define the execution pipeline</b> by adding operation which use a registered data-source
 *      - {@link #addOperation(DataSourceOperation)}, {@link #addMixedOperation(ThrowingConsumer, boolean)}
 *
 * - <b>Run the pipeline</b>
 *      - {@link #run()}
 * </pre>
 *
 * @author rcolin
 */
public interface DistributedDataSourceCoordinator
{
    /**
     * Add an operation to execute
     * @param operation the operation to execute
     * This object depends of the driver/API used to perform operation on a database.
     * Ex: Connection for a JDBC database, session for a MongoDB database
     *
     * @apiNote The order or operation registering must be handled
     * @throws IllegalArgumentException if dataSource has not been already registered by calling {@link #registerDataSource(Object, ThrowingConsumer, ThrowingConsumer, ThrowingConsumer, ThrowingConsumer, String)}
     */
   void addOperation(DataSourceOperation<?> operation) throws IllegalArgumentException;


    /**
     * Add a compound operation (an operation which performs it-self on multiple data-source)
     * @param consumer define which sub-operation performs with the coordinator
     * @param nestedOperationAreEvaluatedNow @{@link CompoundOperation#nestedOperationAreEvaluatedNow()}
     */
    void addMixedOperation(ThrowingConsumer<DistributedDataSourceCoordinator, Exception> consumer, boolean nestedOperationAreEvaluatedNow);

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
     * Run all registered operation by handling data coherence.
     * This method ensure that all operation are committed if no errors were encountered on any data-source,
     * else the changes are rollback
     *
     * @throws Exception if an Exception has been throw during the execution of an operation.
     * The implementation must take the responsibility to properly handle data state on each started/committed transaction before
     * rethrowing the inner Exception
     *
     * @apiNote operations are executed in FIFO order considering the {@link #addOperation(DataSourceOperation)} method call order
     */
    void run() throws Exception;

}