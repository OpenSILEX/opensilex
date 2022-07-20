package org.opensilex.nosql.datasource.saga;

import org.opensilex.nosql.datasource.operation.SparqlOperation;

public interface SparqlSagaOperation extends SparqlOperation, SagaDataSourceOperation<SparqlOperation> {

}
