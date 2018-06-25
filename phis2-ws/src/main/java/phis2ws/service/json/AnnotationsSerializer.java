//**********************************************************************************************
//                                       AnnotationSerializer.java 
//
// Author(s): Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: June, 25 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  June, 25 2018
// Subject: Serialize a Annotation instance to JSON, 
//          used to have a different return from the model class for the GET 
//          annotation
//***********************************************************************************************
package phis2ws.service.json;

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
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.view.model.phis.Annotation;

/**
 * serialize a annotation instance to JSON, used to have a different return from
 * the model class for the GET dataset
 *
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationsSerializer implements JsonSerializer<Annotation> {

    public final static String CREATED_LABEL = "creationDate";
    public final static String TARGETS_LABEL = "targets";
    public final static String URI_LABEL = "uri";
    public final static String CREATOR_LABEL = "creator";
    public final static String BODYVALUE_LABEL = "comment";
    public final static String MOTIVATION_LABEL = "motivatedBy";

    /**
     *eg.{
     * "uri":
     * "http://www.phenome-fppn.fr/platform/id/annotation/361ac1e4-dc5a-4fdb-95a3-3f65556b1d32",
     * "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy",
     * "motivatedBy": "http://www.w3.org/ns/oa#commenting", "creationDate":
     * "2018-06-25T15:25:02+0200", "comment": "Ustilago maydis infection",
     * "targets": [ "http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy" ] }
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

        annotationJson.add(BODYVALUE_LABEL, new JsonPrimitive(src.getBodyValue()));
        JsonArray jsonArray = new JsonArray();
        for (String target : src.getTargets()) {
            jsonArray.add(target);
        }
        annotationJson.add(TARGETS_LABEL, jsonArray);

        return annotationJson;
    }
}
