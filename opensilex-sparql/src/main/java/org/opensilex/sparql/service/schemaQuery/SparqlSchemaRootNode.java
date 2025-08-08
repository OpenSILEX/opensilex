/*
 * *****************************************************************************
 *                         SparqlSchemaRootNode.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *****************************************************************************
 */
package org.opensilex.sparql.service.schemaQuery;

import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.mapping.SPARQLClassAnalyzer;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class for readability enhancing purposes only, calls SparqlSchemaNode's constructor with some values set to null,
 * because they won't be used. No field name and no isListField, isListField is set to any value as a boolean can't be null.
 * Currently, a root node will always use global graph. Add a Node graph in the constructor if you need this.
 *
 * @author mhart
 */
public class SparqlSchemaRootNode<T extends SPARQLResourceModel> extends SparqlSchemaNode<T> {

    /**
     * Constructor to use when there is more than one layer (children of children, example Organisation -> Site -> Facility)
     */
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

    /**
     * Constructor for when there is only one layer below the root node (no children of children).
     * You just have to pass a list of SparqlSchemaSimpleNode.
     * The isListField attribute is calculated automatically.
     * WARNING : Using this constructor will set the 'fetchDynamicRelations' attribute to false in all child nodes.
     */
    public SparqlSchemaRootNode(
            SPARQLService sparql,
            Class<T> objectClass,
            List<SparqlSchemaSimpleNode<?>> uncompletedChildNodes,
            boolean fetchDynamicRelations
    ) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        super(
                objectClass,
                null,
                getChildNodes(sparql.getMapperIndex().getForClass(objectClass).getClassAnalyzer(), uncompletedChildNodes),
                false,
                fetchDynamicRelations
        );
    }

    /**
     * A function to translate a list of SparqlSchemaSimpleNode into a list of completed SparqlSchemaNodes
     *
     */
    private static <T extends SPARQLResourceModel> List<SparqlSchemaNode<?>> getChildNodes(
            SPARQLClassAnalyzer analyzer,
            List<SparqlSchemaSimpleNode<?>> uncompletedChildNodes
    ) {
        List<SparqlSchemaNode<?>> childNodes = new ArrayList<>();

        for (SparqlSchemaSimpleNode<?> uncompletedNode : uncompletedChildNodes) {
            Field field = analyzer.getFieldFromName(uncompletedNode.getFieldName());

            // field
            if (analyzer.isObjectListField(field)) {
                childNodes.add(
                        new SparqlSchemaNode<>(
                                uncompletedNode.getObjectClass(),
                                uncompletedNode.getFieldName(),
                                new ArrayList<>(),
                                true,
                                false
                        ));
            }else if(analyzer.isObjectPropertyField(field)){
                childNodes.add(new SparqlSchemaNode<>(
                        uncompletedNode.getObjectClass(),
                        uncompletedNode.getFieldName(),
                        new ArrayList<>(),
                        false,
                        false
                ));
            }
        }

        return childNodes;
    }


}
