/*
 * *****************************************************************************
 *                         VueUser.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 23/06/2025 13:13
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.ontology.vueOwlExtension.types.object;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.ontology.vueOwlExtension.types.VueOntologyObjectType;

/**
 * @author rcolin
 */
public class VueUser implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return FOAF.Agent.getURI();
    }

    @Override
    public String getInputComponent() {
        // #TODO append input component
        return null;
    }

    @Override
    public String getViewComponent() {
        // #TODO append view component
        return null;
    }

}
