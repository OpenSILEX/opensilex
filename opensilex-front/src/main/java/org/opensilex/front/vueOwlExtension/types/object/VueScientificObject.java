package org.opensilex.front.vueOwlExtension.types.object;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rcolin
 */
public class VueScientificObject implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return Oeso.ScientificObject.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-ScientificObjectParentPropertySelector";
    }

    @Override
    public Map<String, String> getInputComponentsByProperty() {
        Map<String, String> inputMap = new HashMap<>();
        inputMap.put(Oeso.isPartOf.getURI(), "opensilex-ScientificObjectParentPropertySelector");
        return inputMap;
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDUriView";
    }
}
