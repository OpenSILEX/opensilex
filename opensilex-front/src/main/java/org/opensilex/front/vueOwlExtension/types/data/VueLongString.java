/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types.data;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;

/**
 *
 * @author vmigot
 */
public class VueLongString implements VueOntologyDataType {

    @Override
    public String getTypeUri() {
        return Oeso.longString.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-XSDLongStringInput";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDLongStringView";
    }

    @Override
    public String getLabelKey() {
        return "datatypes.longstring";
    }

}
