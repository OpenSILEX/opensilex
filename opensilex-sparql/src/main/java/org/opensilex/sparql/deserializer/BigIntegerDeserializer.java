//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.math.BigInteger;
import org.apache.jena.datatypes.xsd.XSDDatatype;
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
        if (value.isEmpty()) {
            return null;
        }
        return new BigInteger(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        NodeValue v = new NodeValueInteger((BigInteger) value);
        return v.asNode();
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDinteger;
    }

}
