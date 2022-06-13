package org.opensilex.front.vueOwlExtension.types.object;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;

/**
 * @author rcolin
 */
public class VueDevice implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return Oeso.Device.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-DevicePropertySelector";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDUriView";
    }
}
