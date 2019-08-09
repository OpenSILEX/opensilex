/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
        return NodeFactory.createLiteralByValue(date.format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate) ;
    }
}
