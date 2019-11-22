//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.math.*;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.*;


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
