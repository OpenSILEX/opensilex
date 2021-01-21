//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 *
 * @author vincent
 */
public class LongDeserializer implements SPARQLDeserializer<Long> {

    @Override
    public Long fromString(String value) throws Exception {
        if (value.isEmpty()) {
            return null;
        }
        return Long.valueOf(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        return NodeFactory.createLiteralByValue(value, getDataType());
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDlong;
    }

}
