//******************************************************************************
//                          JsonSchemaValidation.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldturner.medeia.api.InputSource;
import com.worldturner.medeia.api.SchemaSource;
import com.worldturner.medeia.api.StreamInputSource;
import com.worldturner.medeia.api.UrlSchemaSource;
import com.worldturner.medeia.api.ValidationFailedException;
import com.worldturner.medeia.api.jackson.MedeiaJacksonApi;
import com.worldturner.medeia.schema.validation.SchemaValidator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.JsonResponse;

/**
 *
 * @author boizetal
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Provider
public class JsonSchemaValidation implements MessageBodyReader<ProvenanceCreationDTO> {
    
    /**
     * Validation schema internal ressources directory
     */
    private static final String INTERNAL_VALIDATION_SCHEMAS_DIR = "provenancesSchemas";
    
    /**
     * JsonSchema validator
     */
    private static final MedeiaJacksonApi VALIDATION_API = new MedeiaJacksonApi();

    private static ObjectMapper objectMapper = new ObjectMapper();
    

    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return true;
    }

    @Override
    public ProvenanceCreationDTO readFrom(Class<ProvenanceCreationDTO> type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap<String, String> mm, InputStream in) throws IOException, WebApplicationException {

            // 1. read input            
            objectMapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
            InputStream bufferdInputStream = new BufferedInputStream(in);
            bufferdInputStream.mark(0);
            ProvenanceCreationDTO prov = objectMapper.readValue(bufferdInputStream, ProvenanceCreationDTO.class);
            String jsonSchema = prov.getJsonSchema();    
            bufferdInputStream.reset();            

            //String jsonSchema = "software";

            // 2. load validation schema if it exists
            SchemaValidator validator = this.loadSchema(jsonSchema);
            
            // 3. read input with or without schema
            if (validator != null) {
                InputSource source = new StreamInputSource(bufferdInputStream);
                JsonParser unValidatedParser = VALIDATION_API.createJsonParser(validator, source);
                JsonParser validatedParser = VALIDATION_API.decorateJsonParser(validator, unValidatedParser);
                try {
                    //VALIDATION_API.parseAll(validatedParser);
                    return objectMapper.readValue(validatedParser, ProvenanceCreationDTO.class);
                    
                } catch (ValidationFailedException e) {
                    final ErrorResponse postResponse = new ErrorResponse(Response.Status.BAD_REQUEST,"Unexpected JSON format", e);
                    throw new WebApplicationException(Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity(postResponse)
                            .build());
                        }
            } else {
                return objectMapper.readValue(bufferdInputStream, ProvenanceCreationDTO.class);
            }
            

    }
    
    /**
     * Load validation schema corresponding to Input entity type
     * @param type type of generic entity to test if it is a list or other collection
     * @param genericEntityTypetype of the entity can be a class type or an array of this class type
     * @return A java json schema validator
     * @see https://github.com/worldturner/medeia-validator
     */
    private SchemaValidator loadSchema(String provenanceType) {

        
        // 2. load corresponding schema or return null
        URL schemaResource = getClass().getResource("/" + INTERNAL_VALIDATION_SCHEMAS_DIR +"/" + provenanceType + ".json");

        if (schemaResource == null) {
            return null;
        } else {
            SchemaSource source = new UrlSchemaSource(schemaResource);
            return VALIDATION_API.loadSchema(source);
        }
    }
    
}
