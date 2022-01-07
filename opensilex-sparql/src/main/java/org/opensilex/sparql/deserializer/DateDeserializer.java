//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        if (value == null) {
            return null;
        }
        DateTimeFormatter slashFormater = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter slashRevertFormater = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        DateTimeFormatter hyphenFormater = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter hyphenRevertFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate localDate = null;
        try {
            localDate = LocalDate.parse(value, hyphenRevertFormater);
        } catch (DateTimeParseException ex1) {
            try {
                localDate = LocalDate.parse(value, slashRevertFormater);
            } catch (DateTimeParseException ex2) {
                try {
                    localDate = LocalDate.parse(value, hyphenFormater);
                } catch (DateTimeParseException ex3) {
                    try {
                        localDate = LocalDate.parse(value, slashFormater);
                    } catch (DateTimeParseException ex4) {
                        LocalDate.parse(value, hyphenRevertFormater);
                    }
                }
            }
        }

        return localDate;
    }

    @Override
    public Node getNode(Object value) throws Exception {
        LocalDate date = (LocalDate) value;
        if (value instanceof String) {
            date = fromString(value.toString());
        }

        return NodeFactory.createLiteralByValue(date.format(DateTimeFormatter.ISO_DATE), getDataType());
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDdate;
    }

    @Override
    public boolean validate(String value) {
        try {
            fromString(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
