package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * A simple class containing only field name and the class, can be used for more readable way to create child nodes.
 * If you make a node this way, the isListfield attribute will be calculated automatically and fetchDynamicRelations will be set to false.
 */
public class SparqlSchemaSimpleNode<T extends SPARQLResourceModel> {
    private final Class<T> objectClass;
    private final String fieldName;

    public SparqlSchemaSimpleNode(Class<T> objectClass, String fieldName) {
        this.objectClass = objectClass;
        this.fieldName = fieldName;
    }

    public Class<T> getObjectClass() {
        return objectClass;
    }

    public String getFieldName() {
        return fieldName;
    }
}
