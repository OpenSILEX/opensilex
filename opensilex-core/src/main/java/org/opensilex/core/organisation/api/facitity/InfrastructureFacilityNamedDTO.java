package org.opensilex.core.organisation.api.facitity;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.sparql.response.ObjectNamedResourceDTO;

import java.net.URI;

public class InfrastructureFacilityNamedDTO extends ObjectNamedResourceDTO  {

    public InfrastructureFacilityNamedDTO() {
    }

    public InfrastructureFacilityNamedDTO(InfrastructureFacilityModel model) {
        super(model);
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/greenHouseA")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "greenHouseA")
    public String getName() {
        return name;
    }
}
