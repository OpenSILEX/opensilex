package org.opensilex.core.organisation.api.site;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteAddressModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.security.group.dal.GroupModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creation DTO for a site. The name, address, organizations, facilities and groups can be specified. If an address is
 * specified, the geometry will be deduced by a geocoding service.
 *
 * @author Valentin RIGOLLE
 */
@JsonPropertyOrder({"uri", "rdf_type", "name", "address", "organizations", "facilities", "groups"})
public class SiteCreationDTO extends SiteDTO {
    protected SiteAddressDTO address;

    protected List<URI> organizations;

    protected List<URI> facilities;

    protected List<URI> groups;

    public SiteAddressDTO getAddress() {
        return address;
    }

    public void setAddress(SiteAddressDTO address) {
        this.address = address;
    }

    public List<URI> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<URI> facilities) {
        this.facilities = facilities;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

    @JsonIgnore
    @Override
    public String getTypeLabel() {
        return super.getTypeLabel();
    }

    @Override
    public void toModel(SiteModel model) {
        super.toModel(model);

        if (getAddress() != null) {
            model.setAddress(getAddress().newModel());
        }

        if (getOrganizations() != null) {
            List<OrganizationModel> organizationModels = getOrganizations().stream().map(organizationUri -> {
                OrganizationModel organizationModel = new OrganizationModel();
                organizationModel.setUri(organizationUri);
                return organizationModel;
            }).collect(Collectors.toList());
            model.setOrganizations(organizationModels);
        }

        if (getFacilities() != null) {
            List<FacilityModel> facilityModels = getFacilities().stream().map(facilityUri -> {
                FacilityModel facilityModel = new FacilityModel();
                facilityModel.setUri(facilityUri);
                return facilityModel;
            }).collect(Collectors.toList());
            model.setFacilities(facilityModels);
        }

        if (getGroups() != null) {
            List<GroupModel> groupModels = getGroups().stream().map(groupUri -> {
                GroupModel groupModel = new GroupModel();
                groupModel.setUri(groupUri);
                return groupModel;
            }).collect(Collectors.toList());
            model.setGroups(groupModels);
        }
    }

    @Override
    public void fromModel(SiteModel model) {
        super.fromModel(model);

        SiteAddressModel addressModel = model.getAddress();
        if (addressModel != null) {
            SiteAddressDTO addressDTO = new SiteAddressDTO();
            addressDTO.fromModel(addressModel);
            setAddress(addressDTO);
        }

        List<OrganizationModel> organizationModels = model.getOrganizations();
        if (organizationModels != null) {
            setOrganizations(organizationModels.stream()
                    .map(OrganizationModel::getUri)
                    .collect(Collectors.toList()));
        }

        List<FacilityModel> facilityModels = model.getFacilities();
        if (facilityModels != null) {
            setOrganizations(facilityModels.stream()
                    .map(FacilityModel::getUri)
                    .collect(Collectors.toList()));
        }

        List<GroupModel> groupModels = model.getGroups();
        if (groupModels != null) {
            setGroups(groupModels.stream()
                    .map(GroupModel::getUri)
                    .collect(Collectors.toList()));
        }
    }
}
