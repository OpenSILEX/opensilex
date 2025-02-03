package org.opensilex.core.variable.api.dimension;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.dal.DimensionModel;
import org.opensilex.core.variable.dal.UnitModel;

import java.net.URI;

public class DimensionCreationDTO extends BaseVariableCreationDTO<DimensionModel> {

    @JsonProperty("dataType")
    private URI dataType;

    @JsonProperty("unit")
    private URI unit;

    @Override
    protected DimensionModel newModelInstance() {
        DimensionModel model = new DimensionModel();
        model.setDataType(this.dataType);
        model.setUnit(new UnitModel(this.unit));
        return model;
    }

    public URI getDataType() {
        return dataType;
    }

    public void setDataType(URI dataType) {
        this.dataType = dataType;
    }

    public URI getUnit() {
        return unit;
    }

    public void setUnit(URI unit) {
        this.unit = unit;
    }
}
