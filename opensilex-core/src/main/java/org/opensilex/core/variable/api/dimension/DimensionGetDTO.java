package org.opensilex.core.variable.api.dimension;

import org.opensilex.core.variable.dal.DimensionModel;
import org.opensilex.sparql.response.ObjectNamedResourceDTO;

public class DimensionGetDTO extends ObjectNamedResourceDTO {

    public DimensionGetDTO() {
    }

    public DimensionGetDTO(DimensionModel model) {
        super(model);
    }

}