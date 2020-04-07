//******************************************************************************
//                          CustomJsonWriterReader.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.worldturner.medeia.api.SchemaSource;
import com.worldturner.medeia.api.UrlSchemaSource;
import com.worldturner.medeia.api.ValidationFailedException;
import com.worldturner.medeia.api.gson.MedeiaGsonApi;
import com.worldturner.medeia.schema.validation.SchemaValidator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.model.Dataset;

/**
 * Custom JSON handler.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 * @param <T>
 * For json validation schema
 * @see https://github.com/worldturner/medeia-validator
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Provider
public final class CustomJsonWriterReader<T> implements MessageBodyWriter<T>,
        MessageBodyReader<T> {

    /**
     * Logs
     */
    final static Logger LOGGER = LoggerFactory.getLogger(CustomJsonWriterReader.class);
    /**
     * JsonSchema validator
     */
    private static final MedeiaGsonApi VALIDATION_GSON_API = new MedeiaGsonApi();
    /**
     * Serializer/deserializer object
     */
    private static final Gson GSON = new Gson();
    
    /**
     * Extract class from List generic string type
     */
    private static final Pattern EXTRACT_CLASS_PATTERN = Pattern.compile(".*<(.*)>");
    
    /**
     * Validation schema internal ressources directory
     */
    private static final String INTERNAL_VALIDATION_SCHEMAS_DIR = "validationSchemas";
    
    /**
     * Permits to filter visible classes.
     * @param type
     * @param genericType
     * @param annotations
     * @param mediaType
     * @return
     */
    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            java.lang.annotation.Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    /**
     * Defines the way to read the elements.
     * @param type
     * @param genericType
     * @param annotations
     * @param mediaType
     * @param httpHeaders
     * @param entityStream
     * @return
     * @throws IOException
     * @throws WebApplicationException
     */
    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            // 1. read input
            final BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));

            // 2. load validation schema if it exists
            SchemaValidator entitySchemaValidator = this.loadSchema(type,genericType.getTypeName());
            
            // 3. read input with or without schema
            if(entitySchemaValidator != null){
                JsonReader validatedReader = VALIDATION_GSON_API.createJsonReader(entitySchemaValidator, reader);
                return GSON.fromJson(validatedReader, genericType);
            }else{
                return GSON.fromJson(reader, genericType);
            }
        } catch (JsonIOException | JsonSyntaxException | ValidationFailedException e) {
            LOGGER.warn(e.getMessage(), e);
            final ResponseFormPOST postResponse = new ResponseFormPOST(new Status(
                    "Unexpected JSON format", 
                    StatusCodeMsg.ERR, 
                    e.getMessage()));
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(postResponse)
                    .build());
        }
    }

    /**
     * Tells if a class is serializable. By default, they all are.
     * @param type
     * @param genericType
     * @param annotations
     * @param mediaType
     * @return true if serializable. False otherwise.
     */
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(T object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    /**
     * Defines how to serialize the classes.
     * @param t
     * @param type
     * @param genericType
     * @param annotations
     * @param mediaType
     * @param httpHeaders
     * @param entityStream
     * @throws IOException
     * @throws WebApplicationException
     * @see
     * https://github.com/plantbreeding/documentation/wiki/Best-Practices-and-Conventions
     */
    @Override
    public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            Gson g = new GsonBuilder()
                    .registerTypeAdapter(Dataset.class, new DatasetsSerializer())
                    // specific serializer for Annotation
                    .registerTypeAdapter(opensilex.service.model.Annotation.class, new AnnotationsSerializer())
                   
                    .serializeNulls() // To serialize null values in JSON null values
                    //@see https://github.com/plantbreeding/documentation/wiki/Best-Practices-and-Conventions
                    .create();
            try (OutputStreamWriter writer = new OutputStreamWriter(entityStream, UTF_8)) {
                g.toJson(t, genericType, writer);
            }
        } catch (JsonIOException | IOException gsonEx) {
            LOGGER.error(gsonEx.getMessage(), gsonEx);
            throw new ProcessingException(
                    "Error serializing a " + type + " to the output stream", gsonEx);
        }
    }
    
    /**
     * Load validation schema corresponding to Input entity type
     * @param type type of generic entity to test if it is a list or other collection
     * @param genericEntityTypetype of the entity can be a class type or an array of this class type
     * @return A java json schema validator
     * @see https://github.com/worldturner/medeia-validator
     */
    private SchemaValidator loadSchema(Class<T> type, String genericEntityType) {
        // 1. get class name from class type
        // E.g. java.util.ArrayList<opensilex.service.ressource.dto.ProvenancePostDTO>
        if(List.class.isAssignableFrom(type)){
            Matcher matcher = EXTRACT_CLASS_PATTERN.matcher(genericEntityType);
            if (matcher.find()) {
               genericEntityType = matcher.group(1);
            }
        }
        // E.g. opensilex.service.ressource.dto.ProvenancePostDTO
        String[] enstityDTOPath = genericEntityType.split("[.]");
        String entityType = enstityDTOPath[enstityDTOPath.length - 1]; 

        // E.g. ProvenancePostDTO
        if(entityType == null){
            return null;
        }
        
        // 2. load corresponding schema or return null
        URL schemaResource = getClass().getResource("/" + INTERNAL_VALIDATION_SCHEMAS_DIR +"/" + entityType + ".json");

        if (schemaResource == null) {
            return null;
        } else {
            SchemaSource source = new UrlSchemaSource(schemaResource);
            return VALIDATION_GSON_API.loadSchema(source);
        }
    }
}

