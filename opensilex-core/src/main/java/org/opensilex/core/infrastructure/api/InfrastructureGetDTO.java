/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.rest.group.api.GroupUserProfileDTO;
import org.opensilex.rest.sparql.dto.NamedResourceGetDTO;
import org.opensilex.rest.user.api.UserGetDTO;

/**
 *
 * @author vince
 */
public class InfrastructureGetDTO extends NamedResourceGetDTO {

    protected List<GroupUserProfileDTO> userProfiles;

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
