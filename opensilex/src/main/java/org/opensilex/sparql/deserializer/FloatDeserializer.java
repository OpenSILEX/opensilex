/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.deserializer;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 *
 * @author vincent
 */
public class FloatDeserializer implements SPARQLDeserializer<Float> {

    @Override
    public Float fromString(String value) throws Exception {
        return Float.valueOf(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        return NodeFactory.createLiteralByValue(value, XSDDatatype.XSDfloat);
    }
}
