/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.deserializer;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 *
 * @author vincent
 */
public class StringDeserializer implements SPARQLDeserializer<String> {

    @Override
    public String fromString(String value) throws Exception {
        return value;
    }

    @Override
    public Node getNode(Object value) throws Exception {
        return NodeFactory.createLiteral(value.toString());
    }
}
