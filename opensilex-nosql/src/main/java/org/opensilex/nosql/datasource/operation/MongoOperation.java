package org.opensilex.nosql.datasource.operation;

import com.mongodb.client.ClientSession;
import org.opensilex.utils.ThrowingConsumer;
import org.simplejavamail.api.internal.clisupport.model.Cli;

/**
 * @author rcolin
 * Extension of {@link AbstractDataSourceOperation} which work with a {@link ClientSession}
 * This session define the level of transaction atomicty for MongoDB based operation
 */
public class MongoOperation extends AbstractDataSourceOperation<ClientSession> {

    public MongoOperation(ClientSession session, ThrowingConsumer<ClientSession, Exception> consumer) {
        super(session,consumer);
    }
}
