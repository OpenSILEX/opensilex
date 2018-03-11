//**********************************************************************************************
//                                       CustomJsonWriterReader.java 
//
// Author(s): Arnaud CHARLEROY
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: Define the way to read incoming JSON and write output JSON
//***********************************************************************************************
package phis2ws.service.json;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import static java.nio.charset.StandardCharsets.UTF_8;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.Dataset;

/**
 * Classe permettant de surcharger la classe qui permet la sérialization et la
 * deserialisation du JSON dans Jersey
 *
 * @author Arnaud CHARLEROY
 * @date 05/16
 * @param <T>
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Provider
public final class CustomJsonWriterReader<T> implements MessageBodyWriter<T>,
        MessageBodyReader<T> {

    /**
     * Récupération des erreurs
     */
    final static Logger LOGGER = LoggerFactory.getLogger(CustomJsonWriterReader.class);

    /**
     * Permet de filtrer les classes qui sont lisibles
     *
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
     * Définit la façon de lire les éléments
     *
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
        final Gson g = new Gson();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
            return g.fromJson(reader, genericType);
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
            final ResponseFormPOST postResponse = new ResponseFormPOST(new Status("Unexpected JSON format", StatusCodeMsg.ERR, e.getMessage()));
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build());
        }
    }

    /**
     * Indique si une classe peut être sérialisee par défaut elles le sont
     * toutes !
     *
     * @param type
     * @param genericType
     * @param annotations
     * @param mediaType
     * @return
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
     * Définit comment sérialiser les classes
     *
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
                   
                    .serializeNulls() // Pour serialiser les variables null en nulll json
                    //                    see https://github.com/plantbreeding/documentation/wiki/Best-Practices-and-Conventions
                    .create();

            //                          //Suppression de champs non voulus
            // Put @Expose on field that you don't want serialize
            //                        .addSerializationExclusionStrategy(new ExclusionStrategy() {
            //                            @Override
            //                            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            //                                final Expose expose = fieldAttributes.getAnnotation(Expose.class);
            //                                return expose != null && !expose.serialize();
            //                            }
            //
            //                            @Override
            //                            public boolean shouldSkipClass(Class<?> aClass) {
            //                                return false;
            //                            }
            //                        })
            //                        .addDeserializationExclusionStrategy(new ExclusionStrategy() {
            //                            @Override
            //                            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            //                                final Expose expose = fieldAttributes.getAnnotation(Expose.class);
            //                                return expose != null && !expose.deserialize();
            //                            }
            //
            //                            @Override
            //                            public boolean shouldSkipClass(Class<?> aClass) {
            //                                return false;
            //                            }
            //                        })
            try (OutputStreamWriter writer = new OutputStreamWriter(entityStream, UTF_8)) {
                g.toJson(t, genericType, writer);
            }
        } catch (Exception gsonEx) {
            LOGGER.error(gsonEx.getMessage(), gsonEx);
            throw new ProcessingException(
                    "Error serializing a " + type + " to the output stream", gsonEx);
        }
    }

}
