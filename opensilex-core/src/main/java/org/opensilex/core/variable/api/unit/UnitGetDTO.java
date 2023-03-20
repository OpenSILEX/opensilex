package org.opensilex.core.variable.api.unit;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.UnitModel;

import java.net.URI;

public class UnitGetDTO extends BaseVariableGetDTO<UnitModel> {

    public UnitGetDTO(UnitModel model) {
        super(model);
    }

    public UnitGetDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/Centimeter")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Centimeter")
    public String getName() {
        return name;
    }
}
