package org.opensilex.nosql.datasource.operation;

import org.opensilex.utils.ThrowingConsumer;

import java.util.Objects;
import java.util.Random;

public abstract class AbstractDataSourceOperation<T> implements DataSourceOperation<T> {

    private final long id;
    private OPERATION_STATE state;
    private final ThrowingConsumer<T,Exception> consumer;

    private static final Random RANDOM = new Random(Double.doubleToLongBits(Math.random()));

    public AbstractDataSourceOperation(ThrowingConsumer<T, Exception> consumer) {

        Objects.requireNonNull(consumer);
        this.consumer = consumer;
        this.id = RANDOM.nextLong();
        state = OPERATION_STATE.STARTED;

    }

    @Override
    public void accept(T t) throws Exception {
        consumer.accept(t);
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
}
