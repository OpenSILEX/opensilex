/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupUserProfileModificationDTO {
    
    protected URI profileURI;
    
    protected URI userURI;
    
    @ValidURI
    @NotNull
    @ApiModelProperty(value = "User associated profile URI", required = true)
    public URI getProfileURI() {
        return this.profileURI;
    }
    
    public void setProfileURI(URI profileURI) {
        this.profileURI = profileURI;
    }
    
    @ValidURI
    @NotNull
    @ApiModelProperty(value = "User URI", required = true)
    public URI getUserURI() {
        return this.userURI;
    }
    
    public void setUserURI(URI userURI) {
        this.userURI = userURI;
    }
    
    public GroupUserProfileModel newModel() {
        GroupUserProfileModel userProfileModel = new GroupUserProfileModel();
        UserModel uModel = new UserModel();
        uModel.setUri(getUserURI());
        userProfileModel.setUser(uModel);
        ProfileModel pModel = new ProfileModel();
        pModel.setUri(getProfileURI());
        userProfileModel.setProfile(pModel);
        return userProfileModel;
    }
}
