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
public class CharDeserializer implements SPARQLDeserializer<Character> {

    @Override
    public Character fromString(String value) throws Exception {
        return value.charAt(0);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        if (value == null) {
            return NodeFactory.createLiteral("");
        }
        return NodeFactory.createLiteral(value.toString().substring(0, 1));
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDstring;
    }

    @Override
    public boolean validate(String value) {
        return value == null || value.length() >= 1;
    }
}
