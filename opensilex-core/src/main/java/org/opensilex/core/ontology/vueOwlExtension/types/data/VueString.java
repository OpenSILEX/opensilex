/*
 * *****************************************************************************
 *                         VueString.java
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
 *
 * @author vmigot
 */
public class VueString implements VueOntologyDataType {

    @Override
    public String getTypeUri() {
        return XSDDatatype.XSDstring.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-XSDStringInput";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDRawView";
    }

    @Override
    public String getLabelKey() {
        return "datatypes.string";
    }

}
