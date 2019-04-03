//******************************************************************************
//                              DatasetSerializer.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 23 October 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.json;

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
import opensilex.service.model.AgronomicalData;
import opensilex.service.model.Dataset;

/**
 * Dataset JSON serializer.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DatasetsSerializer implements JsonSerializer<Dataset> {
    /**
     * @param src
     * @param typeOfSrc
     * @param context
     * @return the JSON created
     * @example
     * {
     *   scientificObject: "http://.....",
     *   experiment: "http://....",
     *   data: [
     *     {
     *       date: "....",
     *       value: "...",
     *       variable: "http://...."
     *     }
     *   ]
    }*/
    @Override
    public JsonElement serialize(Dataset src, Type typeOfSrc, JsonSerializationContext context) {
        Map<String, JsonArray> phenotypesDataToReturn = new HashMap<>();
        src.getData().forEach((data) -> {
            if (phenotypesDataToReturn.containsKey(data.getAgronomicalObject())) {
                JsonObject agronomicalObjectData = new JsonObject();
                if (src.getVariableURI() != null) {
                    agronomicalObjectData.add("variable", new JsonPrimitive(src.getVariableURI()));
                } else {
                    agronomicalObjectData.add("variable", new JsonPrimitive(data.getVariable()));
                }
                agronomicalObjectData.add("date", new JsonPrimitive(data.getDate()));
                agronomicalObjectData.add("value", new JsonPrimitive(data.getValue()));
                if (data.getSensor() != null) {
                    agronomicalObjectData.add("sensor", new JsonPrimitive(data.getSensor()));
                }
                if (data.getIncertitude() != null) {
                    agronomicalObjectData.add("incertitude", new JsonPrimitive(data.getIncertitude()));
                }
                
                phenotypesDataToReturn.get(data.getAgronomicalObject()).add(agronomicalObjectData);
            } else {
                JsonObject agronomicalObjectData = new JsonObject();
                if (src.getVariableURI() != null) {
                    agronomicalObjectData.add("variable", new JsonPrimitive(src.getVariableURI()));
                } else {
                    agronomicalObjectData.add("variable", new JsonPrimitive(data.getVariable()));
                }
                agronomicalObjectData.add("date", new JsonPrimitive(data.getDate()));
                agronomicalObjectData.add("value", new JsonPrimitive(data.getValue()));
                if (data.getSensor() != null) {
                    agronomicalObjectData.add("sensor", new JsonPrimitive(data.getSensor()));
                }
                if (data.getIncertitude() != null) {
                    agronomicalObjectData.add("incertitude", new JsonPrimitive(data.getIncertitude()));
                }
                
                JsonArray agronomicalObjectPhenotypes = new JsonArray();
                agronomicalObjectPhenotypes.add(agronomicalObjectData);
                phenotypesDataToReturn.put(data.getAgronomicalObject(), agronomicalObjectPhenotypes);
            }
        });
        
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
