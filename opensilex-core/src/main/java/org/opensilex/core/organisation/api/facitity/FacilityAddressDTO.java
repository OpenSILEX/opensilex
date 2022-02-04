package org.opensilex.core.organisation.api.facitity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opensilex.core.address.api.AddressDTO;
import org.opensilex.core.organisation.dal.FacilityAddressModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 * Address of a facility.
 *
 * @author Valentin RIGOLLE
 */
public class FacilityAddressDTO extends AddressDTO<FacilityAddressModel> {
    protected NamedResourceDTO<InfrastructureFacilityModel> facility;

    @JsonIgnore
    public NamedResourceDTO<InfrastructureFacilityModel> getFacility() {
        return facility;
    }

    public void setFacility(NamedResourceDTO<InfrastructureFacilityModel> facility) {
        this.facility = facility;
    }

    @Override
    public FacilityAddressModel newModelInstance() {
        return new FacilityAddressModel();
    }

    @Override
    public void fromModel(FacilityAddressModel model) {
        super.fromModel(model);

        InfrastructureFacilityModel facilityModel = model.getFacility();
        if (facilityModel != null) {
            setFacility(NamedResourceDTO.getDTOFromModel(facilityModel));
        }
    }

    @Override
    public void toModel(FacilityAddressModel model) {
        super.toModel(model);

        NamedResourceDTO<InfrastructureFacilityModel> facility = getFacility();
        if (facility != null) {
            InfrastructureFacilityModel facilityModel = new InfrastructureFacilityModel();
            facilityModel.setUri(facility.getUri());
            model.setFacility(facilityModel);
        }
    }
}
