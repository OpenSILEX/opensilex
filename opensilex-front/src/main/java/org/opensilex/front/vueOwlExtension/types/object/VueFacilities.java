/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types.object;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;

/**
 *
 * @author vmigot
 */
public class VueFacilities implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return Oeso.Facility.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-InfrastructureFacilityPropertySelector";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-InfrastructureFacilityPropertyView";
    }

}
