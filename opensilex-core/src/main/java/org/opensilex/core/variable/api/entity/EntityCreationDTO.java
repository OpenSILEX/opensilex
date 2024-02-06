//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.dal.EntityModel;

import java.net.URI;

public class EntityCreationDTO extends BaseVariableCreationDTO<EntityModel> {

    @Override
    protected EntityModel newModelInstance() {
        return new EntityModel();
    }

    @ApiModelProperty(example = "Plant", required = true)
    public String getName() {
        return name;
    }

    @ApiModelProperty(example = "The entity which describe a plant")
    public String getDescription() {
        return description;
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

}
