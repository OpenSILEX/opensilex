/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.deserializer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 *
 * @author vincent
 */
public class DateTimeDeserializer implements Deserializer<OffsetDateTime> {

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
