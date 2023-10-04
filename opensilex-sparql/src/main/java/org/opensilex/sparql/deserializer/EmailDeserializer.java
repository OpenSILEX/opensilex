//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidModelException;

/**
 *
 * @author vincent
 */
public class EmailDeserializer
        extends JsonDeserializer<InternetAddress>
        implements SPARQLDeserializer<InternetAddress> {

    @Override
    public InternetAddress deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        try {
            return new InternetAddress(node.asText().toLowerCase());
        } catch (AddressException ex) {
            throw new EmailDeserializationException("Invalid email address: " + node.asText(), jp.getCurrentLocation(), ex);
        }
    }

    @Override
    public InternetAddress fromString(String value) {
        try{
            return new InternetAddress(value.toLowerCase());
        }catch (AddressException e){
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Node getNode(Object value) {
        return NodeFactory.createLiteral(value.toString().toLowerCase());
    }

    private class EmailDeserializationException extends JsonProcessingException {

        private static final long serialVersionUID = 1L;

        private EmailDeserializationException(String message, JsonLocation location, Throwable cause) {
            super(message, location, cause);
        }
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDstring;
    }

}
