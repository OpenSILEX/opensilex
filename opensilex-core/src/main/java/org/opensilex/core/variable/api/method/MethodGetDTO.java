package org.opensilex.core.variable.api.method;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.MethodModel;

import java.net.URI;

public class MethodGetDTO extends BaseVariableGetDTO<MethodModel> {

    public MethodGetDTO(MethodModel model) {
        super(model);
    }

    public MethodGetDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/method/ImageAnalysis")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "ImageAnalysis")
    public String getName() {
        return name;
    }
}
