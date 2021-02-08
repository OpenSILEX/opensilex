//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.EntityModel;


/**
 *
 * @author vidalmor
 */

public class EntityDetailsDTO extends BaseVariableGetDTO<EntityModel> {

    public EntityDetailsDTO(EntityModel model) {
        super(model);
    }

    public EntityDetailsDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Plant")
    public String getName() {
        return name;
    }

    @Override
    @ApiModelProperty(example = "The entity which describe a plant")
    public String getDescription() {
        return description;
    }


}
