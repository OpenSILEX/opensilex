/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.rest.group.api.GroupUserProfileModificationDTO;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.group.dal.GroupUserProfileModel;

/**
 *
 * @author vince
 */
class InfrastructureCreationDTO {

    protected URI uri;

    protected URI type;

    protected String name;

    protected URI parent;

    protected List<GroupUserProfileModificationDTO> userProfiles;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public List<GroupUserProfileModificationDTO> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileModificationDTO> userProfiles) {
        this.userProfiles = userProfiles;
    }

    public InfrastructureModel newModel() {
        InfrastructureModel model = new InfrastructureModel();
        model.setUri(getUri());
        model.setName(getName());
        if (getType() != null) {
            model.setType(getType());
        }
        if (getParent() != null) {
            InfrastructureModel parentModel = new InfrastructureModel();
            parentModel.setUri(getParent());
            model.setParent(parentModel);
        }

        GroupModel group = new GroupModel();
        group.setName(getName() + " (auto)");
        group.setDescription(group.getName());
        List<GroupUserProfileModel> userProfiles = new ArrayList<>();
        if (getUserProfiles() != null) {
            getUserProfiles().forEach(userProfile -> {
                userProfiles.add(userProfile.newModel());
            });
        }
        group.setUserProfiles(userProfiles);
        model.setGroup(group);

        return model;
    }
}
