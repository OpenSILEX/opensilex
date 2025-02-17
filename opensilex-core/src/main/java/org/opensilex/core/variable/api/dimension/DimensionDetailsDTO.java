package org.opensilex.core.variable.api.dimension;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.api.unit.UnitDetailsDTO;
import org.opensilex.core.variable.dal.DimensionModel;
import org.opensilex.core.variable.dal.UnitModel;

import java.net.URI;
import java.util.Objects;

public class DimensionDetailsDTO extends BaseVariableDetailsDTO<DimensionModel> {

    @JsonProperty("unit")
    private UnitDetailsDTO unit;

    @JsonProperty("datatype")
    private URI dataType;

    public DimensionDetailsDTO(DimensionModel model) {
        super(model);
        this.unit = Objects.nonNull(model.getUnit()) ? new UnitDetailsDTO(model.getUnit()) : null;
        this.dataType = model.getDataType();
    }

    public UnitDetailsDTO getUnit() {
        return unit;
    }

    public void setUnit(UnitDetailsDTO unit) {
        this.unit = unit;
    }

    public URI getDataType() {
        return dataType;
    }

    public void setDataType(URI dataType) {
        this.dataType = dataType;
    }

    @Override
    public DimensionModel toModel() {
        DimensionModel model = new DimensionModel();
        model.setDataType(this.getDataType());
        model.setUnit(new UnitModel(this.getUnit().getUri()));
        setBasePropertiesToModel(model);
        return model;
    }

}
