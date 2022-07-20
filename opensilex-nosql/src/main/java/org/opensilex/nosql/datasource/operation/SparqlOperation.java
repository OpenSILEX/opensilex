package org.opensilex.nosql.datasource.operation;

import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

public interface SparqlOperation extends DataSourceOperation, ThrowingConsumer<SPARQLService,Exception> {


}
