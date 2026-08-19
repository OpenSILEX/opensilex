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
        return new InternetAddress(value.toLowerCase());
    }

    @Override
    public Node getNode(Object value) throws Exception {
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
