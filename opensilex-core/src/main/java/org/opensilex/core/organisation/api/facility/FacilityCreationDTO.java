/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO representing JSON for posting facility
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "name","organizations", "address"})
public
class FacilityCreationDTO extends FacilityDTO {
    @JsonProperty("organizations")
    protected List<URI> organizations;

    public List<URI> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
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
            model.setInfrastructures(organizationList);
        }
    }

    @Override
    public void fromModel(FacilityModel model) {
        super.fromModel(model);

        setOrganizations(model.getInfrastructureUris());
    }
}
