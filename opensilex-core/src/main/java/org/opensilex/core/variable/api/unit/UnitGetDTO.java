package org.opensilex.core.variable.api.unit;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "http://opensilex.dev/set/variables/unit/Centimeter")
    public URI getUri() {
        return uri;
    }

    @Override
    @Schema(example = "Centimeter")
    public String getName() {
        return name;
    }
}
