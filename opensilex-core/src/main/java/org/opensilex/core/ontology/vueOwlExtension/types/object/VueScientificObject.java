/*
 * *****************************************************************************
 *                         VueScientificObject.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 23/06/2025 13:13
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.ontology.vueOwlExtension.types.object;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.vueOwlExtension.types.VueOntologyObjectType;

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
