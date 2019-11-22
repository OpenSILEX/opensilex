//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.types;

import java.util.*;

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
