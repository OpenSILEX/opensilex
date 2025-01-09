package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingFunction;

//TODO implement interface with correct stuff
public class SparqlSchema<T extends SPARQLResourceModel> {
    //@Override
    public SparqlSchemaNode<T> getRootNode() {
        return null;
    }

    //@Override
    //Function that we will pass as the throwing result handler fucntion in sparql service
    public T resolveSchema(SPARQLResult sparqlResult, SPARQLService sparql) throws Exception{
        SparqlNoProxyFetcher<T> customFetcher = new SparqlNoProxyFetcher<>(getRootNode().getNodeObjectClass(), sparql);
        customFetcher.getInstance(result, lang)
        return null;
    }
}
