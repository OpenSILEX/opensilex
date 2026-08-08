package org.opensilex.core.organisation.api.facility;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "http://opensilex.dev/greenHouseA")
    public URI getUri() {
        return uri;
    }

    @Override
    @Schema(example = "greenHouseA")
    public String getName() {
        return name;
    }
}
