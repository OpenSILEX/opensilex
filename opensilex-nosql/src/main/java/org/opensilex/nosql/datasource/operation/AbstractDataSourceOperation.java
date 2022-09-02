package org.opensilex.nosql.datasource.operation;

import org.opensilex.utils.ThrowingConsumer;

import java.util.Objects;
import java.util.Random;

/**
 * @author rcolin
 * Abstract implementation of {@link DataSourceOperation} which handle id generation, state handling and wrap
 * the ThrowingConsumer which is effectively executed
 *
 * @param <T> the type of object consumed by the operation, depend on the database on which the operation is executed.
 *           This type of object can be seen as the level of transaction atomicity for a database
 */
public abstract class AbstractDataSourceOperation<T> implements DataSourceOperation<T> {

    private final T dataSource;
    private final long id;
    private OPERATION_STATE state;
    private final ThrowingConsumer<T,Exception> consumer;

    private static final Random RANDOM = new Random(Double.doubleToLongBits(Math.random()));

    /**
     *
     * @param consumer the {@link ThrowingConsumer} which is effectively executed
     * @apiNote {@link #id} is generated with a random number generator
     */
    protected AbstractDataSourceOperation(T dataSource, ThrowingConsumer<T, Exception> consumer) {

        Objects.requireNonNull(consumer);
        Objects.requireNonNull(dataSource);

        this.dataSource = dataSource;
        this.consumer = consumer;
        this.id = RANDOM.nextLong();
    }

    @Override
    public void run() throws Exception {
        consumer.accept(dataSource);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public OPERATION_STATE getState() {
        return state;
    }

    @Override
    public void setState(OPERATION_STATE state) {
        this.state = state;
    }

    @Override
    public T getDataSource() {
        return dataSource;
    }

    @Override
    public ThrowingConsumer<T, Exception> getConsumer() {
        return consumer;
    }
}
