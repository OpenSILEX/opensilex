//**********************************************************************************************
//                                       DatasetSerializer.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: October, 23 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 23 2017
// Subject: Serialize a Dataset instance to JSON, 
//          used to have a different return from the model class for the GET 
//          dataset
//***********************************************************************************************
package phis2ws.service.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import phis2ws.service.view.model.phis.AgronomicalData;
import phis2ws.service.view.model.phis.Dataset;

/**
 * serialize a dataset instance to JSON, used to have a different return from 
 * the model class for the GET dataset
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DatasetsSerializer implements JsonSerializer<Dataset> {
    /* 
            {
                agronomicalObject: "http://.....",
                experiment: "http://....",
                data: [
                    {
                        date: "....",
                        value: "...",
                        variable: "http://...."
                    }
                ]
    }*/
    @Override
    public JsonElement serialize(Dataset src, Type typeOfSrc, JsonSerializationContext context) {
        Map<String, JsonArray> phenotypesDataToReturn = new HashMap<>();
        for (AgronomicalData d : src.getData()) {
            if (phenotypesDataToReturn.containsKey(d.getAgronomicalObject())) {
                JsonObject agronomicalObjectData = new JsonObject();
                 if (src.getVariableURI() != null) {
                    agronomicalObjectData.add("variable", new JsonPrimitive(src.getVariableURI()));
                } else {
                    agronomicalObjectData.add("variable", new JsonPrimitive(d.getVariable()));
                }
                agronomicalObjectData.add("date", new JsonPrimitive(d.getDate()));
                agronomicalObjectData.add("value", new JsonPrimitive(d.getValue()));
                if (d.getSensor() != null) {
                    agronomicalObjectData.add("sensor", new JsonPrimitive(d.getSensor()));
                }
                if (d.getIncertitude() != null) {
                    agronomicalObjectData.add("incertitude", new JsonPrimitive(d.getIncertitude()));
                }
                
                phenotypesDataToReturn.get(d.getAgronomicalObject()).add(agronomicalObjectData);
            } else {
                    JsonObject agronomicalObjectData = new JsonObject();
                    if (src.getVariableURI() != null) {
                        agronomicalObjectData.add("variable", new JsonPrimitive(src.getVariableURI()));
                    } else {
                        agronomicalObjectData.add("variable", new JsonPrimitive(d.getVariable()));
                    }
                    agronomicalObjectData.add("date", new JsonPrimitive(d.getDate()));
                    agronomicalObjectData.add("value", new JsonPrimitive(d.getValue()));
                    if (d.getSensor() != null) {
                        agronomicalObjectData.add("sensor", new JsonPrimitive(d.getSensor()));
                    }
                    if (d.getIncertitude() != null) {
                        agronomicalObjectData.add("incertitude", new JsonPrimitive(d.getIncertitude()));
                    }

                    JsonArray agronomicalObjectPhenotypes = new JsonArray();
                    agronomicalObjectPhenotypes.add(agronomicalObjectData);
                    phenotypesDataToReturn.put(d.getAgronomicalObject(), agronomicalObjectPhenotypes);
            }
        }
        
        JsonArray finalJson = new JsonArray();
        
        for (Entry<String, JsonArray> entry : phenotypesDataToReturn.entrySet()) {
            JsonObject agronomicalObjectData = new JsonObject();
            agronomicalObjectData.add("agronomicalObject", new JsonPrimitive(entry.getKey()));
            if (src.getExperiment() != null) {
                agronomicalObjectData.add("experiment", new JsonPrimitive(src.getExperiment()));
            }
            agronomicalObjectData.add("data", entry.getValue());
            finalJson.add(agronomicalObjectData);
        }
        return finalJson;
    }
}
