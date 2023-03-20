//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.characteristic;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.dal.CharacteristicModel;


/**
 *
 * @author vidalmor
 */
public class CharacteristicDetailsDTO extends BaseVariableDetailsDTO<CharacteristicModel> {

    public CharacteristicDetailsDTO(CharacteristicModel model) {
        super(model);
    }

    public CharacteristicDetailsDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/characteristic/Height")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Height")
    public String getName() {
        return name;
    }


    @Override
    @ApiModelProperty(example = "Describe the height")
    public String getDescription() {
        return description;
    }

    @Override
    public CharacteristicModel toModel() {
        CharacteristicModel model = new CharacteristicModel();
        setBasePropertiesToModel(model);
        return model;
    }

}
