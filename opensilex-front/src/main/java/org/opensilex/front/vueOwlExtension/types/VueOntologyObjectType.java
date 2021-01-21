/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types;

import java.util.HashMap;
import java.util.Map;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
public interface VueOntologyObjectType extends VueOntologyType {

    public default Map<String, String> getInputComponentsByProperty() {
        return new HashMap<>();
    }

    public default Map<String, String> getInputComponentsMap() {
        Map<String, String> map = getInputComponentsByProperty();
        Map<String, String> shortMap = new HashMap<>();
        map.forEach((key, value) -> {
            shortMap.put(SPARQLDeserializers.getShortURI(key), value);
        });
        map.putAll(shortMap);
        return map;
    }
}
