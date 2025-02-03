package org.opensilex.core.variable.api.dimension;

import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.dal.DimensionModel;

public class DimensionDetailsDTO extends BaseVariableDetailsDTO<DimensionModel> {

    @Override
    public DimensionModel toModel() {
        DimensionModel model = new DimensionModel();
        setBasePropertiesToModel(model);
        return model;
    }

}
