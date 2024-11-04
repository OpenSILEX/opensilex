package org.opensilex.server.rest.serialization.uri;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;

/**
 *
 * Custom {@link JsonDeserializer} which format the URI during deserializing

 * This deserializer can be used in two-way :
 * <ul>
 *     <li>
 *         For all {@link URI} deserializing by adding a module and an associated deserializer inside the {@link org.opensilex.server.rest.serialization.ObjectMapperContextResolver}
 *         <pre>{@code
 *         SimpleModule uriJsonModule = new SimpleModule();
 *         uriJsonModule.addDeserializer(URI.class, new UriJsonDeserializer());
 *         mapper.registerModule(uriJsonModule);
 *         }</pre>
 *     </li>
 *     <li>At the field level by just adding {@code @JsonDeserialize(using = UriJsonDeserializer.class)} annotation on your field </li>
 *
 * </ul>
 *
 * @see UriFormater#formatURI(String)
 *
 * @author rcolin
 */
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
