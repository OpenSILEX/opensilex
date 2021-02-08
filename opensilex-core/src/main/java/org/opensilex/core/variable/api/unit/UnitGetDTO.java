package org.opensilex.core.variable.api.unit;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.sparql.response.ObjectNamedResourceDTO;

import java.net.URI;

public class UnitGetDTO extends ObjectNamedResourceDTO {

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
