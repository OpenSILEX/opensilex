package org.opensilex.nosql.datasource.operation;

import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

/**
 * @author rcolin
 * Extension of {@link AbstractDataSourceOperation} which work with a {@link SPARQLService}
 */
public class SparqlOperation extends AbstractDataSourceOperation<SPARQLService> {

    public SparqlOperation(SPARQLService sparql, ThrowingConsumer<SPARQLService, Exception> consumer) {
        super(sparql,consumer);
    }


}
