//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseMultiLabelIdentifierCreationDTO;
import org.opensilex.core.variable.api.MultiLabelDTO;
import org.opensilex.core.variable.dal.EntityMultiLabelModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EntityCreationDTO extends BaseMultiLabelIdentifierCreationDTO<EntityMultiLabelModel> {


    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }


    public void setUri(URI uri) {
        this.uri = uri;
    }


    public EntityCreationDTO() {

    }
    @Override
    public EntityMultiLabelModel newModel() {
        EntityMultiLabelModel model = super.newModel();

        model.setPrefLabels(this.multiLabelDTO.getPrefLabels());
        model.setAltsLabels(this.multiLabelDTO.getAltLabels());
        model.setDefinitions(this.multiLabelDTO.getDefinitions());

        return model;
}
    @Override
    protected EntityMultiLabelModel newModelInstance() {
        return new EntityMultiLabelModel();
    }

}


