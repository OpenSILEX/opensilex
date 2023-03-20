/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.ResourceDTO;

/**
 *
 * @author vidalmor
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "profile_uri", "profile_name", "user_uri", "user_name"})
public class GroupUserProfileDTO extends ResourceDTO<GroupUserProfileModel> {

    
  @ JsonProperty("rdf_type")
    protected URI type;
    
    @JsonProperty("rdf_type_name")
    protected String typeLabel;
    
    @JsonProperty("profile_uri")
    protected URI profileURI;

    @JsonProperty("profile_name")
    protected String profileName;

    @JsonProperty("user_uri")
    protected URI userURI;

    @JsonProperty("user_name")
    protected String userName;

    @ValidURI
    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
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
    @ApiModelProperty(value = "User URI",required = true)
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

    @Override
    public void toModel(GroupUserProfileModel model) {
        super.toModel(model);

        ProfileModel profile = new ProfileModel();
        profile.setUri(getProfileURI());
        profile.setName(getProfileName());
        model.setProfile(profile);

        AccountModel user = new AccountModel();
        user.setUri(userURI);
        model.setUser(user);
    }

    @Override
    public void fromModel(GroupUserProfileModel model) {
        super.fromModel(model);

        setProfileURI(model.getProfile().getUri());
        setProfileName(model.getProfile().getName());

        setUserURI(model.getUser().getUri());
        setUserName(model.getUser().getName());
    }

    @Override
    public GroupUserProfileModel newModelInstance() {
        return new GroupUserProfileModel();
    }
}
