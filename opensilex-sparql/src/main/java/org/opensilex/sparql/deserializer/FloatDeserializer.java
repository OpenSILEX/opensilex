//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.sparql.exceptions.SPARQLException;

/**
 *
 * @author vincent
 */
public class FloatDeserializer implements SPARQLDeserializer<Float> {

    @Override
    public Float fromString(String value) throws SPARQLException {
        if (value.isEmpty()) {
            return null;
        }
        return Float.valueOf(value);
    }

    @Override
    public Node getNode(Object value) {
        return NodeFactory.createLiteralByValue(value, getDataType());
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDfloat;
    }
}
