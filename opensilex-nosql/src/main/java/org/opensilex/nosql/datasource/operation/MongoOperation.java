package org.opensilex.nosql.datasource.operation;

import com.mongodb.client.ClientSession;
import org.opensilex.utils.ThrowingConsumer;

public class MongoOperation extends AbstractDataSourceOperation<ClientSession> {

    public MongoOperation(ThrowingConsumer<ClientSession, Exception> consumer) {
        super(consumer);
    }
}
