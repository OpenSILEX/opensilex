/*
 * *****************************************************************************
 *                         VueFactorLevel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 23/06/2025 13:13
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.vueOwlExtension.types.object;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.vueOwlExtension.types.VueOntologyObjectType;

/**
 *
 * @author vmigot
 */
public class VueFactorLevel implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return Oeso.FactorLevel.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-FactorLevelPropertySelector";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-FactorLevelPropertyView";
    }

}
