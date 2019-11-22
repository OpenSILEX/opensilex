//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.time.*;
import java.time.format.*;
import org.apache.jena.datatypes.xsd.*;
import org.apache.jena.graph.*;



/**
 *
 * @author vincent
 */
public class DateTimeDeserializer implements SPARQLDeserializer<OffsetDateTime> {

    @Override
    public OffsetDateTime fromString(String value) throws Exception {
        return  OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        OffsetDateTime date = (OffsetDateTime) value;
        return NodeFactory.createLiteralByValue(date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), XSDDatatype.XSDdateTime) ;
    }
}
