package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.model.SPARQLResourceModel;

//TODO implement interface with correct stuff
public class SparqlSchema<T extends SPARQLResourceModel> {

    private final SparqlSchemaNode<T> root;

    public SparqlSchema(SparqlSchemaNode<T> root) {
        this.root = root;
    }

    public SparqlSchemaNode<T> getRoot() {
        return root;
    }

}
