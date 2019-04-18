//******************************************************************************
//                            ImageWaitingCheck.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 3 December 2015
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON Converter. Converts JSON objects into strings.
 * @author Samuël Chérimont
 */
@Singleton
public final class JsonConverter {

    /**
     * Get logger.
     */
    final static Logger LOGGER = LoggerFactory.getLogger(JsonConverter.class);

    /**
     * Converts an object to a pretty JSON format.
     * @param object
     * @return the JSON object
     */
    public static String ConvertToPrettyJson(Object object) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String jsonObject = gson.toJson(object);
        return jsonObject;
    }

    /**
     * Converts an object to a JSON format.
     * @param object
     * @return the JSON object
     */
    public static String ConvertToJson(Object object) {
        final Gson gson = new GsonBuilder().create();
        String jsonObject = null;
        try {
            jsonObject = gson.toJson(object);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return jsonObject;
    }

    /**
     * Converts from JSON.
     * @param stringToConvert
     * @param classToConvertIn
     * @return object converted
     *
     */
    public static Object ConvertFromJson(String stringToConvert, Class classToConvertIn) {
        final Gson gson = new GsonBuilder().create();
        Object jsonObject;
        try {
            jsonObject = classToConvertIn.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        jsonObject = gson.fromJson(stringToConvert, classToConvertIn);

        return jsonObject;
    }
}
