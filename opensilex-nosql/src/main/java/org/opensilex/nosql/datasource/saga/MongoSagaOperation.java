package org.opensilex.nosql.datasource.saga;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.utils.ThrowingConsumer;

public class MongoSagaOperation extends MongoOperation implements SagaDataSourceOperation<ClientSession> {

    private final MongoOperation compensation;
    private final MongoDBService mongo;

    public MongoSagaOperation(MongoDBService mongo, ThrowingConsumer<ClientSession, Exception> consumer, ThrowingConsumer<ClientSession, Exception> compensationConsumer) {
        super(consumer);
        this.compensation = new MongoOperation(compensationConsumer);
        this.mongo = mongo;
    }

    @Override
    public MongoOperation getCompensationAction() {
        return compensation;
    }

    @Override
    public ClientSession getCompensationContext() {
        return mongo.startSession();
    }
}
