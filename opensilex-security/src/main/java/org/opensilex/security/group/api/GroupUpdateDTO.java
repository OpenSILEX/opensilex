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

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupUpdateDTO extends GroupCreationDTO {

    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager", required = true)
    @NotNull
    public URI getUri() {
        return super.getUri();
    }

}
