package org.opensilex.nosql.datasource.operation;

import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

public class SparqlOperation extends AbstractDataSourceOperation<SPARQLService> {

    public SparqlOperation(ThrowingConsumer<SPARQLService, Exception> consumer) {
        super(consumer);
    }


}
