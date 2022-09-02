package org.opensilex.nosql.datasource.coordinator;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

/**
 * Default implementation of {@link DistributedDataSourceCoordinator} which can handle
 * distributed transaction on an RDF and a MongoDB database
 *
 * @author rcolin
 */
public class DefaultDataSourceCoordinator extends AbstractDistributedCoordinator {

    private final SPARQLService sparql;
    private final ClientSession session;

    public DefaultDataSourceCoordinator(SPARQLService sparql, ClientSession session) {
        super();

        this.sparql = sparql;
        this.session = session;

        // register RDF database by using the SPARQLService
        registerDataSource(
                sparql,
                SPARQLService::startTransaction,
                (SPARQLService unitOfWork) -> {
                    if (unitOfWork.hasActiveTransaction()) {
                        unitOfWork.commitTransaction();
                    }
                },
                (SPARQLService unitOfWork) -> {
                    if (unitOfWork.hasActiveTransaction()) {
                        unitOfWork.rollbackTransaction();
                    }
                },
                (SPARQLService unitOfWork) -> {
                },
                SPARQLService.class.getSimpleName() + System.identityHashCode(sparql)
        );

        // register a MongoDB based database operation
        registerDataSource(
                session,
                ClientSession::startTransaction,
                (ClientSession unitOfWork) -> {
                    if (unitOfWork.hasActiveTransaction()) {
                        unitOfWork.commitTransaction();
                    }
                },
                (ClientSession unitOfWork) -> {
                    if (unitOfWork.hasActiveTransaction()) {
                        unitOfWork.abortTransaction();
                    }
                },
                (ClientSession::close),
                ClientSession.class.getSimpleName() + System.identityHashCode(session)
        );
    }

    @Override
    protected void resolveCommitFail() {
        LOGGER.warn("this implementation has no mechanism to cancel a committed transaction");
    }

    /**
     * Utility method for executing operation on a TripleStore
     *
     * @param consumer an operation which use a {@link SPARQLService} in order to perform write operation on a TripleStore with transaction handling
     * @return the current coordinator
     * @see #addOperation(DataSourceOperation)
     * @see SparqlOperation
     */
    public DefaultDataSourceCoordinator addSparqlOperation(ThrowingConsumer<SPARQLService, Exception> consumer) {
        this.addOperation(new SparqlOperation(sparql, consumer));
        return this;
    }

    /**
     * Utility method for executing operation on MongoDB
     *
     * @param consumer an operation which use a {@link ClientSession} in order to perform write operation on MongoDB with transaction handling
     * @return the current coordinator
     * @see #addOperation(DataSourceOperation)
     * @see MongoOperation
     */
    public DefaultDataSourceCoordinator addMongoOperation(ThrowingConsumer<ClientSession, Exception> consumer) {
        this.addOperation(new MongoOperation(session, consumer));
        return this;
    }

}
