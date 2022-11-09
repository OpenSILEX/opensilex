/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.security.group.dal.GroupModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * DTO representing JSON for posting organisation
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "parents", "groups", "facilities"})
class OrganizationCreationDTO extends OrganizationDTO {
    protected List<URI> groups;
    protected List<URI> facilities;

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<URI> facilities) {
        this.facilities = facilities;
    }

    // Don't display this property as required for the creation
    @Override
    @JsonIgnore
    public String getTypeLabel() {
        return super.getTypeLabel();
    }

    @Override
    @JsonIgnore
    public List<URI> getChildren() {
        return super.getChildren();
    }

    @Override
    public void toModel(OrganizationModel model) {
        super.toModel(model);

        List<URI> groupUris = getGroups();
        if (groupUris != null) {
            List<GroupModel> groupModels = new ArrayList<>(groupUris.size());
            groupUris.forEach(groupUri -> {
                GroupModel groupModel = new GroupModel();
                groupModel.setUri(groupUri);
                groupModels.add(groupModel);
            });
            model.setGroups(groupModels);
        }

        List<URI> facilityUriList = getFacilities();
        if (facilityUriList != null) {
            List<FacilityModel> facilityModels = facilityUriList.stream().map(facilityUri -> {
                FacilityModel facilityModel = new FacilityModel();
                facilityModel.setUri(facilityUri);
                return facilityModel;
            }).collect(Collectors.toList());
            model.setFacilities(facilityModels);
        }
    }
}
