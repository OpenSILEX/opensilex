/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.types;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author vincent
 */
public class Label {
    
    private final static String DEFAULT_LANG_KEY = "_default";
    
    private Map<String, String> labelByLanguages = new HashMap<>();
    
    public String getLabel(String language) {
        if (labelByLanguages.containsKey(language)) {
            return labelByLanguages.get(DEFAULT_LANG_KEY);
        } else {
            return getDefaultLabel();
        }
    }
    
    public String getDefaultLabel() {
        if (labelByLanguages.containsKey(DEFAULT_LANG_KEY)) {
            return labelByLanguages.get(DEFAULT_LANG_KEY);
        } else {
            return "";
        }
    }
}
