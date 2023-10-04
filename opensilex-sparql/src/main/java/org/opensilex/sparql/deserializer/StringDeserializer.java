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
public class StringDeserializer implements SPARQLDeserializer<String> {

    @Override
    public String fromString(String value) {
        return value;
    }

    @Override
    public Node getNode(Object value) {
        return NodeFactory.createLiteral(value.toString());
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDstring;
    }
    
}
