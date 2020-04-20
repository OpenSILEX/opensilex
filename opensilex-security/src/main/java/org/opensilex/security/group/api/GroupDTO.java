/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vince
 */
public class GroupDTO extends NamedResourceDTO<GroupModel> {

    protected String description;

    protected List<GroupUserProfileDTO> userProfiles;

    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(value = "Group name", example = "Experiment manager")
    public String getName() {
        return name;
    }

    @ApiModelProperty(value = "Group description", example = "Group for all experiments managers")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Group user with profile")
    public List<GroupUserProfileDTO> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileDTO> userProfiles) {
        this.userProfiles = userProfiles;
    }

    @Override
    public GroupModel newModelInstance() {
        return new GroupModel();
    }

    @Override
    public void toModel(GroupModel group) {
        super.toModel(group);

        group.setDescription(getDescription());

        List<GroupUserProfileModel> userProfilesModel = new LinkedList<>();
        for (GroupUserProfileDTO userProfile : getUserProfiles()) {
            GroupUserProfileModel userProfileModel = userProfile.newModel();
            userProfilesModel.add(userProfileModel);
        }
        group.setUserProfiles(userProfilesModel);
    }

    @Override
    public void fromModel(GroupModel model) {
        super.fromModel(model);

        setDescription(model.getDescription());

        List<GroupUserProfileDTO> userProfilesDTO = new LinkedList<>();
        for (GroupUserProfileModel userProfile : model.getUserProfiles()) {
            GroupUserProfileDTO dto = newUserProfileDtoInstance();
            dto.fromModel(userProfile);
            userProfilesDTO.add(dto);
        }
        setUserProfiles(userProfilesDTO);
    }

    public static GroupDTO getDTOFromModel(GroupModel model) {
        GroupDTO dto = new GroupDTO();
        dto.fromModel(model);

        return dto;
    }

    public GroupUserProfileDTO newUserProfileDtoInstance() {
        return new GroupUserProfileDTO();
    }
}
