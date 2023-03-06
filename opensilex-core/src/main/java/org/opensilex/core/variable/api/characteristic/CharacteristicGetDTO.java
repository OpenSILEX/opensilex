package org.opensilex.core.variable.api.characteristic;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.CharacteristicModel;

import java.net.URI;

public class CharacteristicGetDTO extends BaseVariableGetDTO<CharacteristicModel> {

    public CharacteristicGetDTO(CharacteristicModel model) {
        super(model);
    }

    public CharacteristicGetDTO() {
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
}
