/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.security.group.dal.GroupModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * DTO representing JSON for posting organisation
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "name", "parent", "children", "groups"})
class InfrastructureCreationDTO extends InfrastructureDTO {
    public List<URI> groups;

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

    // Don't display this property as required for the creation
    @Override
    @JsonIgnore
    public String getTypeLabel() {
        return super.getTypeLabel();
    }

    @Override
    public void toModel(InfrastructureModel model) {
        super.toModel(model);

        List<URI> groupUriList = getGroups();
        if (groupUriList != null) {
            List<GroupModel> groups = new ArrayList<>(groupUriList.size());
            groupUriList.forEach(groupUri -> {
                GroupModel groupModel = new GroupModel();
                groupModel.setUri(groupUri);
                groups.add(groupModel);
            });
            model.setGroups(groups);
        }
    }
}
