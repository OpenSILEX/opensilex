package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.model.SPARQLResourceModel;

import java.util.List;

public interface SparqlSchemaNodeInterface<T extends SPARQLResourceModel> {

    /**
     *
     * @return the objectClass that is a descendent of SPARQLResourceModel,
     * this will tell the query that we need to fetch instances of this class.
     */
    Class<T> getNodeObjectClass();

    /**
     *
     * @return the child nodes, this will tell the query which classes need to be loaded into the instances of the class
     * handled by this node.
     */
    List<SparqlSchemaNodeInterface> getChildren();
}
