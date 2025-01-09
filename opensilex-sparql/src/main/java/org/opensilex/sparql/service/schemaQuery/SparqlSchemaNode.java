package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.model.SPARQLResourceModel;

import java.util.List;

//TODO implement interface with correct stuff
public class SparqlSchemaNode<T extends SPARQLResourceModel>{

    Class<T> objectClass;

    //@Override
    public Class<T> getNodeObjectClass() {
        return objectClass;
    }

    //@Override
    public List<SparqlSchemaNode> getChildren() {
        return List.of();
    }
}
