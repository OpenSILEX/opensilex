package org.opensilex.nosql.datasource.coordinator;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.sparql.service.SPARQLService;

public class DefaultDataSourceCoordinator<O extends DataSourceOperation<?>> extends AbstractDistributedCoordinator<O> {

    public DefaultDataSourceCoordinator(SPARQLService sparql, ClientSession session) {
        super();

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
                }
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
                (ClientSession::close)
        );
    }

    @Override
    protected void resolveCommitFail() {

    }

}
