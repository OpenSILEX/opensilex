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
public class CharDeserializer implements SPARQLDeserializer<Character> {

    @Override
    public Character fromString(String value) throws Exception {
        return value.charAt(0);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        return NodeFactory.createLiteral(value.toString().substring(0, 1));
    }
}
