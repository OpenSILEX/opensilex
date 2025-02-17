package org.opensilex.core.variable.api.dimension;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.DimensionModel;

import java.net.URI;

public class DimensionGetDTO extends BaseVariableGetDTO<DimensionModel> {

    public DimensionGetDTO() {
    }

    public DimensionGetDTO(DimensionModel model) {
        super(model);
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/id/variable/dimension.name")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Dimension name")
    public String getName() {
        return name;
    }
}
