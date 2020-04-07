/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupUserProfileDTO {

    public static GroupUserProfileDTO fromModel(GroupUserProfileModel userProfile) {
            GroupUserProfileDTO userProfileDTO = new GroupUserProfileDTO();
            userProfileDTO.setProfileURI(userProfile.getProfile().getUri());
            userProfileDTO.setProfileName(userProfile.getProfile().getName());
            userProfileDTO.setUserURI(userProfile.getUser().getUri());
            userProfileDTO.setUserName(userProfile.getUser().getName());
            userProfileDTO.setUri(userProfile.getUri());
            return userProfileDTO;
    }

    protected URI uri;
    
    protected URI profileURI;

    protected String profileName;

    protected URI userURI;

    protected String userName;

    @ValidURI
    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    @ValidURI
    @ApiModelProperty(value = "User associated profile URI")
    public URI getProfileURI() {
        return profileURI;
    }

    public void setProfileURI(URI profileURI) {
        this.profileURI = profileURI;
    }

    @ApiModelProperty(value = "User associated profile name")
    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @ValidURI
    @ApiModelProperty(value = "User URI")
    public URI getUserURI() {
        return userURI;
    }

    public void setUserURI(URI userURI) {
        this.userURI = userURI;
    }

    @ApiModelProperty(value = "User name")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}
