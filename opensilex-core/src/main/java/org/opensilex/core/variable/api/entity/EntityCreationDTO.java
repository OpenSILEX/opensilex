//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseMultiLabelResourceCreationDTO;
import org.opensilex.core.variable.dal.EntityModel;

import java.net.URI;

public class EntityCreationDTO extends BaseMultiLabelResourceCreationDTO<EntityModel> {

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }
    public void setUri(URI uri) {
        this.uri = uri;
    }
    public EntityCreationDTO() {}
    @Override
    public EntityModel newModel() {

        EntityModel model = super.newModel();

        model.getPrefLabels().addAllTranslations(this.multiLabelsDTO.getPrefLabels());
        model.getShortLabels().addAllTranslations(this.multiLabelsDTO.getShortLabels());
        model.getAltsLabels().addAllTranslations(this.multiLabelsDTO.getAltLabels());
        model.getDefinitions().addAllTranslations(this.multiLabelsDTO.getDefinitions());

        return model;
    }

    @Override
    protected EntityModel newModelInstance() {
        return new EntityModel();
    }

}


