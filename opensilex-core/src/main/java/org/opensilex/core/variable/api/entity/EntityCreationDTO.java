//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseIdentifierCreationDTO;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.api.LabelDTO;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.security.group.api.GroupUserProfileDTO;

import java.net.URI;
import java.util.List;

public class EntityCreationDTO extends BaseIdentifierCreationDTO<EntityModel> {

    @Override
    protected EntityModel newModelInstance() {
        return new EntityModel();
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(required = true)
    public List<LabelDTO> getLabelDTOs() {
        return labelDTOs;
    }



}
