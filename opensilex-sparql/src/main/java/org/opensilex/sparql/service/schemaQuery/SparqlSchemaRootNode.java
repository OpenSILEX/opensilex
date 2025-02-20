package org.opensilex.sparql.service.schemaQuery;

import org.apache.jena.graph.Node;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.util.List;

/**
 * A class for readability enhancing purposes only, calls SparqlSchemaNode's constructor with some values set to null,
 * because they won't be used. No field name and no isListField, isListField is set to any value as a boolean can't be null
 */
public class SparqlSchemaRootNode<T extends SPARQLResourceModel> extends SparqlSchemaNode<T> {

    //Constructor with no passed graph
    public SparqlSchemaRootNode(
            Class<T> objectClass,
            List<SparqlSchemaNode<?>> childNodes,
            boolean fetchDynamicRelations
    ) {
        super(
                objectClass,
                null,
                childNodes,
                false,
                fetchDynamicRelations
        );
    }

    //Constructor to make this node use a specific graph
    public SparqlSchemaRootNode(
            Class<T> objectClass,
            Node graph,
            List<SparqlSchemaNode<?>> childNodes,
            boolean fetchDynamicRelations
    ) {
        super(
                objectClass,
                null,
                graph,
                childNodes,
                false,
                fetchDynamicRelations
        );
    }
}
