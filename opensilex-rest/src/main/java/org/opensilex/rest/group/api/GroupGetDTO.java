/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.group.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.group.dal.GroupUserProfileModel;
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.rest.user.dal.UserModel;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupGetDTO {

    protected URI uri;
    
    protected String name;
    
    protected String description;
    
    protected List<GroupUserProfileDTO> userProfiles;
    
    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Group name", example = "Experiment manager")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    public static GroupGetDTO fromModel(GroupModel group) {
        GroupGetDTO dto = new GroupGetDTO();
        dto.setUri(group.getUri());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        
        List<GroupUserProfileDTO> userProfiles = new ArrayList<>();
        group.getUserProfiles().forEach((userProfile) -> {
            GroupUserProfileDTO userProfileDTO = new GroupUserProfileDTO();
            userProfileDTO.setProfileURI(userProfile.getProfile().getUri());
            userProfileDTO.setProfileName(userProfile.getProfile().getName());
            userProfileDTO.setUserURI(userProfile.getUser().getUri());
            userProfileDTO.setUserName(userProfile.getUser().getName());
            userProfileDTO.setUri(userProfile.getUri());
            userProfiles.add(userProfileDTO);
        });
        dto.setUserProfiles(userProfiles);
        
        return dto;
    }
}
