package org.opensilex.nosql.datasource.saga;

import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

public class SparqlSagaOperation extends SparqlOperation implements SagaDataSourceOperation<SPARQLService> {

    private SparqlOperation compensation;

    public SparqlSagaOperation(ThrowingConsumer<SPARQLService, Exception> consumer, ThrowingConsumer<SPARQLService, Exception> compensationConsumer) {
        super(consumer);
        this.compensation = new SparqlOperation(compensationConsumer);
    }

    @Override
    public SparqlOperation getCompensationAction() {
        return compensation;
    }
}
