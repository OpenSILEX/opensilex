/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupCreationDTO extends GroupDTO {

    @ApiModelProperty(value = "Group profiles")
    protected List<GroupUserProfileModificationDTO> userProfiles;

    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(value = "Group name", example = "Experiment manager", required = true)
    @Required
    public String getName() {
        return name;
    }

    @ApiModelProperty(value = "Group description", example = "Group for all experiments managers", required = true)
    @Required
    public String getDescription() {
        return description;
    }

    @Override
    public GroupUserProfileModificationDTO newUserProfileDtoInstance() {
        return new GroupUserProfileModificationDTO();
    }

}
