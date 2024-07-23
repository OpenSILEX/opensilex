package org.opensilex.server.rest.serialization.uri;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;

public class UriJsonDeserializer extends JsonDeserializer<URI>{

    @Override
    public URI deserialize(JsonParser parser, DeserializationContext context) throws IOException {
       String value = parser.getText();
       if(StringUtils.isEmpty(value)){
           return null;
       }
       return UriFormater.formatURI(value);
    }
}
