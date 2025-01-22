/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DTO representing JSON for posting facility
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "name", "organizations", "sites", "address", "variableGroups", "description"})
public
class FacilityCreationDTO extends FacilityDTO {
    @JsonProperty("organizations")
    protected List<URI> organizations;

    @JsonProperty("sites")
    protected List<URI> sites;

    @JsonProperty("variableGroups")
    protected List<URI> variableGroups;

    protected List<LocationObservationDTO> locations;

    public List<URI> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
    }

    public List<URI> getSites() {
        return sites;
    }

    public void setSites(List<URI> sites) {
        this.sites = sites;
    }

    public List<URI> getVariableGroups() {
        return variableGroups;
    }

    public void setVariableGroups(List<URI> variableGroups) {
        this.variableGroups = variableGroups;
    }

    public List<LocationObservationDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationObservationDTO> locations) {
        this.locations = locations;
    }

    @Override
    public void toModel(FacilityModel model) {
        super.toModel(model);

        if (getOrganizations() != null) {
            List<OrganizationModel> organizationList = getOrganizations().stream().map(organizationUri -> {
                OrganizationModel organization = new OrganizationModel();
                organization.setUri(organizationUri);
                return organization;
            }).collect(Collectors.toList());
            model.setOrganizations(organizationList);
        }

        if (Objects.nonNull(getSites())) {
            model.setSites(getSites().stream().map(siteURI -> {
                SiteModel site = new SiteModel();
                site.setUri(siteURI);
                return site;
            }).collect(Collectors.toList()));
        }

        if (Objects.nonNull(getVariableGroups())) {
            model.setVariableGroups(getVariableGroups().stream().map(groupURI -> {
                VariablesGroupModel group = new VariablesGroupModel();
                group.setUri(groupURI);
                return group;
            }).collect(Collectors.toList()));
        }
    }

    @Override
    public void fromModel(FacilityModel model) {
        super.fromModel(model);

        setOrganizations(model.getOrganizationUris());
    }
}
