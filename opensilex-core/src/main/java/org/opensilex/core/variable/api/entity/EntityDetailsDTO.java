//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import java.net.URI;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseMultiLabeledIdentifierDetailsDTO;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.api.MultiLabelDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.EntityMultiLabelModel;


/**
 *
 * @author vidalmor
 */

public class EntityDetailsDTO extends BaseMultiLabeledIdentifierDetailsDTO<EntityMultiLabelModel> {

    public EntityDetailsDTO(EntityMultiLabelModel model) {
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
    public MultiLabelDTO getMultiLabelDTO() {
        return multiLabelDTO;
    }
    @Override
    public void setMultiLabelDTO(MultiLabelDTO multiLabelDTO) {
        this.multiLabelDTO = multiLabelDTO;
    }



    @Override
    public EntityMultiLabelModel toModel() {
        EntityMultiLabelModel model = new EntityMultiLabelModel();
        setBasePropertiesToModel(model);
        return model;
    }
}
