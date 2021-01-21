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
public class VueGermplasm implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return Oeso.Germplasm.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-GermplasmPropertySelector";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-GermplasmPropertyView";
    }

}
