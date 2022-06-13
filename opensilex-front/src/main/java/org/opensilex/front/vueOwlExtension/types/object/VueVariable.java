/*******************************************************************************
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 ******************************************************************************/

package org.opensilex.front.vueOwlExtension.types.object;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;

public class VueVariable implements VueOntologyObjectType {

    @Override
    public String getTypeUri() {
        return Oeso.Variable.getURI();
    }

    @Override
    public String getInputComponent(){
         return "opensilex-VariablePropertySelector";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDUriView";
    }
}
