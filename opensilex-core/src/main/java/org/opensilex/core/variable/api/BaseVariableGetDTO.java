/*******************************************************************************
 *                         BaseVariableGetDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 30/01/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.variable.api;

import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.response.ObjectNamedResourceDTO;

/**
 * @author Valentin Rigolle
 */
public abstract class BaseVariableGetDTO<T extends BaseVariableModel<T>> extends ObjectNamedResourceDTO {

    public BaseVariableGetDTO() {
    }

    public BaseVariableGetDTO(SPARQLNamedResourceModel<?> model) {
        super(model);
    }
}
