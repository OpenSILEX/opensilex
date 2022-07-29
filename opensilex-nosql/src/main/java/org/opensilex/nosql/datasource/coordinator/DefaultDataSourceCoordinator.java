package org.opensilex.nosql.datasource.coordinator;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.sparql.service.SPARQLService;

/**
 * Default implementation of {@link DistributedDataSourceCoordinator} which can handle
 * distributed transaction on an RDF and a MongoDB database
 * @param <O>
 */
public class DefaultDataSourceCoordinator<O extends DataSourceOperation<?>> extends AbstractDistributedCoordinator<O> {

    public DefaultDataSourceCoordinator(SPARQLService sparql, ClientSession session) {
        super();

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
                SPARQLService.class.getSimpleName()+System.identityHashCode(sparql)
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
                ClientSession.class.getSimpleName()+System.identityHashCode(session)
        );
    }

    @Override
    protected void resolveCommitFail() {
        throw new UnsupportedOperationException("this implementation has no mechanism to cancel a committed transaction");
    }

}
