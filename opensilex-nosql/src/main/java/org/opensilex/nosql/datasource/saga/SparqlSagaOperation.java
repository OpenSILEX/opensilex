package org.opensilex.nosql.datasource.saga;

import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

/**
 * Extension of {@link SparqlOperation} which is cancelable
 * @author rcolin
 */
public class SparqlSagaOperation extends SparqlOperation implements SagaDataSourceOperation<SPARQLService> {

    private final SPARQLService sparql;
    private final SparqlOperation compensation;

    public SparqlSagaOperation(SPARQLService sparql, ThrowingConsumer<SPARQLService, Exception> consumer, ThrowingConsumer<SPARQLService, Exception> compensationConsumer) {
        super(consumer);
        this.compensation = new SparqlOperation(compensationConsumer);
        this.sparql = sparql;
    }

    @Override
    public SPARQLService getCompensationContext() {
        return sparql;
    }

    @Override
    public SparqlOperation getCompensationAction() {
        return compensation;
    }
}
