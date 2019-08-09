/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.deserializer;

import java.math.BigInteger;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;

/**
 *
 * @author vincent
 */
public class BigIntegerDeserializer implements SPARQLDeserializer<BigInteger> {

    @Override
    public BigInteger fromString(String value) throws Exception {
        return new BigInteger(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        NodeValue v = new NodeValueInteger((BigInteger) value);
        return v.asNode();
    }
}
