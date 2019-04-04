//******************************************************************************
//                          AnnotationSerializer.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 25 June 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import opensilex.service.configuration.DateFormats;
import opensilex.service.model.Annotation;

/**
 * Annotation JSON serializer.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationsSerializer implements JsonSerializer<Annotation> {

    /** 
     * Labels that replace each attribute corresponding name of an annotation 
     * instance when it is serialized into JSON
    */
    public final static String CREATED_LABEL = "creationDate";
    public final static String TARGETS_LABEL = "targets";
    public final static String URI_LABEL = "uri";
    public final static String CREATOR_LABEL = "creator";
    public final static String BODYVALUE_LABEL = "comments";
    public final static String MOTIVATION_LABEL = "motivatedBy";

    /**
     * This function convert an annotation into a JSON specific representation.
     * @param src
     * @param typeOfSrc
     * @return the JSON result
     * @example 
     * {
     *  "uri": "http://www.phenome-fppn.fr/platform/id/annotation/361ac1e4-dc5a-4fdb-95a3-3f65556b1d32",
     *  "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy",
     *  "motivatedBy": "http://www.w3.org/ns/oa#commenting",
     *  "creationDate": "2018-06-25T15:25:02+0200",
     *  "comments": [ "Ustilago maydis infection" ],
     *  "targets": [ "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy" ] 
     * }
     * SILEX:info 
     * The annotation json schema has been deciced according to the "Web Annotation Ontology".
     * @link https://www.w3.org/TR/annotation-model/#bodies-and-targets
     * \SILEX:info
     * @param context
     */
    @Override
    public JsonElement serialize(Annotation src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject annotationJson = new JsonObject();
        annotationJson.add(URI_LABEL, new JsonPrimitive(src.getUri()));
        annotationJson.add(CREATOR_LABEL, new JsonPrimitive(src.getCreator()));
        annotationJson.add(MOTIVATION_LABEL, new JsonPrimitive(src.getMotivatedBy()));

        DateTime created = src.getCreated();
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DateFormats.YMDTHMSZ_FORMAT);
        annotationJson.add(CREATED_LABEL, new JsonPrimitive(created.toString(formatter)));
        
        JsonArray bodyJsonArray = new JsonArray();
        if(src.getBodyValues() != null && !src.getBodyValues().isEmpty()){
            for (String body : src.getBodyValues()) {
                bodyJsonArray.add(body);
            }
        }
        annotationJson.add(BODYVALUE_LABEL, bodyJsonArray);
        
        JsonArray targetJsonArray = new JsonArray();
        src.getTargets().forEach((target) -> {
            targetJsonArray.add(target);
        });
        annotationJson.add(TARGETS_LABEL, targetJsonArray);

        return annotationJson;
    }
}
