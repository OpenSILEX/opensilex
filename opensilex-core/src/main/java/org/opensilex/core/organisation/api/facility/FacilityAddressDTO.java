package org.opensilex.core.organisation.api.facility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opensilex.core.address.api.AddressDTO;
import org.opensilex.core.organisation.dal.facility.FacilityAddressModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 * Address of a facility.
 *
 * @author Valentin RIGOLLE
 */
public class FacilityAddressDTO extends AddressDTO<FacilityAddressModel> {
    protected NamedResourceDTO<FacilityModel> facility;

    @JsonIgnore
    public NamedResourceDTO<FacilityModel> getFacility() {
        return facility;
    }

    public void setFacility(NamedResourceDTO<FacilityModel> facility) {
        this.facility = facility;
    }

    @Override
    public FacilityAddressModel newModelInstance() {
        return new FacilityAddressModel();
    }

    @Override
    public void fromModel(FacilityAddressModel model) {
        super.fromModel(model);

        FacilityModel facilityModel = model.getFacility();
        if (facilityModel != null) {
            setFacility(NamedResourceDTO.getDTOFromModel(facilityModel));
        }
    }

    @Override
    public void toModel(FacilityAddressModel model) {
        super.toModel(model);

        NamedResourceDTO<FacilityModel> facility = getFacility();
        if (facility != null) {
            FacilityModel facilityModel = new FacilityModel();
            facilityModel.setUri(facility.getUri());
            model.setFacility(facilityModel);
        }
    }
}
