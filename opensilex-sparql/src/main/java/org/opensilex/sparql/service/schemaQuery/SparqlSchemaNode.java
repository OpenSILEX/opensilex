package org.opensilex.sparql.service.schemaQuery;

import java.util.List;

public interface SparqlSchemaNode {
    /**
     *
     * @return the name of a class that is a descendent of SPARQLResourceModel,
     * this will tell the query that we need to fetch instances of this class.
     */
    String getNodeClassName();

    /**
     *
     * @return the child nodes, this will tell the query which classes need to be loaded into the instances of the class
     * handled by this node.
     */
    List<SparqlSchemaNode> getChildren();
}
