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
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.rest.user.dal.UserModel;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupGetDTO {

    /**
     * Group URI
     */
    protected URI uri;
    
    protected String name;
    
    protected String description;
    
    protected List<URI> profiles;
    
    protected List<URI> users;

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

    @ApiModelProperty(value = "Group profiles")
    public List<URI> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<URI> profiles) {
        this.profiles = profiles;
    }

    @ApiModelProperty(value = "Group users")
    public List<URI> getUsers() {
        return users;
    }

    public void setUsers(List<URI> users) {
        this.users = users;
    }
    
    public static GroupGetDTO fromModel(GroupModel group) {
        GroupGetDTO dto = new GroupGetDTO();
        dto.setUri(group.getUri());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        
        List<URI> profiles = new ArrayList<>();
        group.getProfiles().forEach((profile) -> {
            profiles.add(profile.getUri());
        });
        dto.setProfiles(profiles);
        
        List<URI> users = new ArrayList<>();
        group.getUsers().forEach((user) -> {
            profiles.add(user.getUri());
        });
        dto.setUsers(users);
        
        return dto;
    }
    
    public GroupModel getModel() {
        GroupModel group = new GroupModel();
        group.setUri(uri);
        group.setName(name);
        group.setDescription(description);
        List<UserModel> usersModel = new ArrayList<>();
        users.forEach((userURI) -> {
            UserModel user = new UserModel();
            user.setUri(userURI);
            usersModel.add(user);
        });
        group.setUsers(usersModel);

        List<ProfileModel> profilesModel = new ArrayList<>();
        profiles.forEach((profileURI) -> {
            ProfileModel profile = new ProfileModel();
            profile.setUri(profileURI);
            profilesModel.add(profile);
        });
        group.setProfiles(profilesModel);

        return group;
    }
}
