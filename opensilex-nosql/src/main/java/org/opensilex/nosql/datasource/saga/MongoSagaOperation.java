package org.opensilex.nosql.datasource.saga;

import org.opensilex.nosql.datasource.operation.MongoOperation;

public interface MongoSagaOperation extends MongoOperation, SagaDataSourceOperation<MongoOperation>{

    @Override
    MongoOperation getCompensationAction();
}
