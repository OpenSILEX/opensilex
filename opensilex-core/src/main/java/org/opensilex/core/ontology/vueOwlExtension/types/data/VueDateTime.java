/*
 * *****************************************************************************
 *                         VueDateTime.java
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
package org.opensilex.core.ontology.vueOwlExtension.types.data;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.core.ontology.vueOwlExtension.types.VueOntologyDataType;

/**
 * TODO: Activer cette classe une fois que les composant de datetime seront opérationnels dans vue
 *  Il suffit simplement de supprimer la méthode isDisabled
 *
 * @author vmigot
 */
public class VueDateTime implements VueOntologyDataType {

    // TODO method a supprimer pour réactiver le type datetime
    @Override
    public boolean isDisabled() {
        return true;
    }
    
    @Override
    public String getTypeUri() {
        return XSDDatatype.XSDdateTime.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-XSDDateTimeInput";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDDateTimeView";
    }

    @Override
    public String getLabelKey() {
        return "datatypes.datetime";
    }
}
