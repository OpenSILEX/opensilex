package org.opensilex.core.organisation.api.facility;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.sparql.response.ObjectNamedResourceDTO;

import java.net.URI;

public class FacilityNamedDTO extends ObjectNamedResourceDTO  {

    public FacilityNamedDTO() {
    }

    public FacilityNamedDTO(FacilityModel model) {
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
