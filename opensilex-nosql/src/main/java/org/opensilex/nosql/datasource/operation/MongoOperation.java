package org.opensilex.nosql.datasource.operation;

import com.mongodb.client.ClientSession;
import org.opensilex.utils.ThrowingConsumer;

public interface MongoOperation extends DataSourceOperation, ThrowingConsumer<ClientSession,Exception> {

}
