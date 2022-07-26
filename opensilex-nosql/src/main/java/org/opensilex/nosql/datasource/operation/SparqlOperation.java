package org.opensilex.nosql.datasource.operation;

import com.mongodb.client.ClientSession;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

/**
 * @author rcolin
 * Extension of {@link AbstractDataSourceOperation} which work with a {@link SPARQLService}
 */
public class SparqlOperation extends AbstractDataSourceOperation<SPARQLService> {

    public SparqlOperation(ThrowingConsumer<SPARQLService, Exception> consumer) {
        super(consumer);
    }


}
