package org.opensilex.core.organisation.api.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteAddressModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Get DTO for the site. The geometry is deduced from the address, if specified.
 *
 * @author Valentin RIGOLLE
 */
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "address", "organizations", "facilities", "groups", "geometry"})
public class SiteGetDTO extends SiteDTO {
    protected SiteAddressDTO address;

    protected List<NamedResourceDTO<OrganizationModel>> organizations;

    protected List<NamedResourceDTO<GroupModel>> groups;

    protected List<NamedResourceDTO<FacilityModel>> facilities;

    @JsonProperty("geometry")
    protected GeoJsonObject geometry;

    public SiteAddressDTO getAddress() {
        return address;
    }

    public void setAddress(SiteAddressDTO address) {
        this.address = address;
    }

    public List<NamedResourceDTO<OrganizationModel>> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<NamedResourceDTO<OrganizationModel>> organizations) {
        this.organizations = organizations;
    }

    public List<NamedResourceDTO<FacilityModel>> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<NamedResourceDTO<FacilityModel>> facilities) {
        this.facilities = facilities;
    }

    public List<NamedResourceDTO<GroupModel>> getGroups() {
        return groups;
    }

    public void setGroups(List<NamedResourceDTO<GroupModel>> groups) {
        this.groups = groups;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    @Override
    public void toModel(SiteModel model) {
        super.toModel(model);

        if (getAddress() != null) {
            model.setAddress(getAddress().newModel());
        }

        if (getOrganizations() != null) {
            List<OrganizationModel> organizationModels = getOrganizations().stream().map(organizationDto -> {
                OrganizationModel organizationModel = new OrganizationModel();
                organizationModel.setUri(organizationDto.getUri());
                return organizationModel;
            }).collect(Collectors.toList());
            model.setOrganizations(organizationModels);
        }

        if (getFacilities() != null) {
            List<FacilityModel> facilityModels = getFacilities().stream().map(facilityDto -> {
                FacilityModel facilityModel = new FacilityModel();
                facilityModel.setUri(facilityDto.getUri());
                return facilityModel;
            }).collect(Collectors.toList());
            model.setFacilities(facilityModels);
        }

        if (getGroups() != null) {
            List<GroupModel> groupModels = getGroups().stream().map(groupDto -> {
                GroupModel groupModel = new GroupModel();
                groupModel.setUri(groupDto.getUri());
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
            List<NamedResourceDTO<OrganizationModel>> organizationDtos = organizationModels.stream()
                    .map(organizationModel -> (NamedResourceDTO<OrganizationModel>) NamedResourceDTO.getDTOFromModel(organizationModel))
                    .collect(Collectors.toList());
            setOrganizations(organizationDtos);
        }

        List<FacilityModel> facilityModels = model.getFacilities();
        if (facilityModels != null) {
            List<NamedResourceDTO<FacilityModel>> facilityDtos = facilityModels.stream()
                    .map(facilityModel -> (NamedResourceDTO<FacilityModel>) NamedResourceDTO.getDTOFromModel(facilityModel))
                    .collect(Collectors.toList());
            setFacilities(facilityDtos);
        }

        List<GroupModel> groupModels = model.getGroups();
        if (groupModels != null) {
            List<NamedResourceDTO<GroupModel>> groupDtos = groupModels.stream()
                    .map(groupModel -> (NamedResourceDTO<GroupModel>) NamedResourceDTO.getDTOFromModel(groupModel))
                    .collect(Collectors.toList());
            setGroups(groupDtos);
        }
    }

    public void fromModelWithGeospatialInfo(SiteModel siteModel, GeospatialModel geospatialModel) throws JsonProcessingException {
        fromModel(siteModel);
        if (geospatialModel != null) {
            setGeometry(GeospatialDAO.geometryToGeoJson(geospatialModel.getGeometry()));
        }
    }
}
