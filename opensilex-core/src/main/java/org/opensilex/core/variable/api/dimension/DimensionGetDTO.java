package org.opensilex.core.variable.api.dimension;

import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.DimensionModel;

public class DimensionGetDTO extends BaseVariableGetDTO<DimensionModel> {

    public DimensionGetDTO() {
    }

    public DimensionGetDTO(DimensionModel model) {
        super(model);
    }
}
