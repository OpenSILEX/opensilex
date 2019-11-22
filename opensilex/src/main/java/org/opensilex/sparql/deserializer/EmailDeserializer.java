//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import javax.mail.internet.*;
import org.apache.jena.graph.*;


/**
 *
 * @author vincent
 */
public class EmailDeserializer 
        extends JsonDeserializer<InternetAddress>
        implements SPARQLDeserializer<InternetAddress> {
    
    @Override
    public InternetAddress deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        try {
            return fromString(node.asText());
        } catch (AddressException ex) {
            throw new EmailDeserializationException("Invalid email address: " + node.asText(), jp.getCurrentLocation(), ex);
        }
    }

    @Override
    public InternetAddress fromString(String value) throws AddressException {
        return new InternetAddress(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        return NodeFactory.createLiteral(value.toString());
    }

    private class EmailDeserializationException extends JsonProcessingException {

        private static final long serialVersionUID = 1L;
        private EmailDeserializationException(String message, JsonLocation location, Throwable cause) {
            super(message, location, cause);
        }
    }
    
}
