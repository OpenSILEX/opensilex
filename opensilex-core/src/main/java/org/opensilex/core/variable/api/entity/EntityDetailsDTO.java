//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseMultiLabelResourceDetailsDTO;
import org.opensilex.core.variable.api.MultiLabelsDTO;
import org.opensilex.core.variable.dal.EntityModel;


/**wsq<
 *
 * @author vidalmor
 */

public class EntityDetailsDTO extends BaseMultiLabelResourceDetailsDTO<EntityModel> {

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
    public MultiLabelsDTO getMultiLabelsDTO() {
        return multiLabelsDTO;
    }
    @Override
    public void setMultiLabelsDTO(MultiLabelsDTO multiLabelsDTO) {

        this.multiLabelsDTO = multiLabelsDTO;
    }

    @Override
    public EntityModel toModel() {
        EntityModel model = new EntityModel();
        setBasePropertiesToModel(model);
        return model;
    }
}
