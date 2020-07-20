//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 *
 * @author vincent
 */
public class DateDeserializer implements SPARQLDeserializer<LocalDate> {

    @Override
    public LocalDate fromString(String value) throws Exception {
        return LocalDate.parse(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        LocalDate date = (LocalDate) value;
        return NodeFactory.createLiteralByValue(date.format(DateTimeFormatter.ISO_DATE), getDataType());
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDdate;
    }

}
