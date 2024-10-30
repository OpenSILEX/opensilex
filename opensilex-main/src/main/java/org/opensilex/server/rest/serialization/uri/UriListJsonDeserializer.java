package org.opensilex.server.rest.serialization.uri;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom {@link JsonDeserializer} which format a List of URI during deserializing
 * @see UriFormater#formatURI(String)
 *
 * @author rcolin
 */
public class UriListJsonDeserializer extends JsonDeserializer<List<URI>> {

    @Override
    public List<URI> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode array = context.readTree(parser);
        if(array.isEmpty()){
            return null;
        }

        List<URI> uris = new ArrayList<>(array.size());
        array.forEach(node -> {
            String value = node.textValue();
            if (!StringUtils.isEmpty(value)) {
                uris.add(UriFormater.formatURI(value));
            }
        });
        return uris;
    }
}
