/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.group.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupCreationDTO {
    protected URI uri;
    
    protected String name;

    protected String description;

    protected List<GroupUserProfileModificationDTO> userProfiles;

    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    @ApiModelProperty(value = "Group name", example = "Experiment manager", required = true)
    @Required
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Group description", example = "Group for all experiments managers", required = true)
    @Required
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Group user with profile")
    @NotNull
    public List<GroupUserProfileModificationDTO> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileModificationDTO> userProfiles) {
        this.userProfiles = userProfiles;
    }

}
