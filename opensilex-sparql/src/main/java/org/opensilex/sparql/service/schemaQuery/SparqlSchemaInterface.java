package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

public interface SparqlSchemaInterface<T extends SPARQLResourceModel> {
    /**
     *
     * @return the starting point of the query
     */
    SparqlSchemaNodeInterface<T> getRootNode();

    //TODO javadoc
    T resolveSchema(SPARQLResult sparqlResult, SPARQLService sparql);
}
