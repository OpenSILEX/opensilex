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
import org.opensilex.rest.group.api.GroupUserProfileDTO;
import org.opensilex.rest.sparql.dto.NamedResourceGetDTO;

/**
 *
 * @author vince
 */
public class InfrastructureGetDTO extends NamedResourceGetDTO {

    protected URI parent;

    protected List<URI> children;

    protected List<GroupUserProfileDTO> userProfiles;

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public List<URI> getChildren() {
        return children;
    }

    public void setChildren(List<URI> children) {
        this.children = children;
    }

    public List<GroupUserProfileDTO> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileDTO> userProfiles) {
        this.userProfiles = userProfiles;
    }

    public static InfrastructureGetDTO fromModel(InfrastructureModel model) {
        InfrastructureGetDTO dto = new InfrastructureGetDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());

        if (model.getParent() != null) {
            URI parentURI = model.getParent().getUri();
            dto.setParent(parentURI);
        }

        List<URI> children = new ArrayList<>();
        if (model.getChildren() != null) {
            model.getChildren().forEach(child -> {
                children.add(child.getUri());
            });
        }
        dto.setChildren(children);

        List<GroupUserProfileDTO> userProfilesList = new ArrayList<>();
        if (model.getGroup() != null) {
            model.getGroup().getUserProfiles().forEach(userProfile -> {
                userProfilesList.add(GroupUserProfileDTO.fromModel(userProfile));
            });
        }
        dto.setUserProfiles(userProfilesList);

        return dto;
    }
}
