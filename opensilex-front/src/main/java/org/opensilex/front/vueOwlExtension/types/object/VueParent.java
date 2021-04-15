/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types.object;

import java.util.HashMap;
import java.util.Map;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;

/**
 *
 * @author vince
 */
public class VueParent implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return Oeso.ScientificObject.getURI();
    }

    @Override
    public String getInputComponent() {
        return null;
    }

    @Override
    public Map<String, String> getInputComponentsByProperty() {
        Map<String, String> inputMap = new HashMap<>();
        inputMap.put(Oeso.isPartOf.getURI(), "opensilex-ScientificObjectParentPropertySelector");
        return inputMap;
    }

    @Override
    public String getViewComponent() {
        return "opensilex-ScientificObjectUriView";
    }

}
