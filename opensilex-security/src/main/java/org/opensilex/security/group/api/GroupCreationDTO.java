/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
@Schema
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "description", "user_profiles"})
public class GroupCreationDTO extends GroupDTO {

    @Schema(description = "Group profiles")
    @JsonProperty("user_profiles")
    protected List<GroupUserProfileModificationDTO> userProfiles;

    @Schema(description = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
    }

    @Schema(description = "Group name", example = "Experiment manager", requiredMode = Schema.RequiredMode.REQUIRED)
    @Required
    public String getName() {
        return name;
    }

    @Schema(description = "Group description", example = "Group for all experiments managers", requiredMode = Schema.RequiredMode.REQUIRED)
    @Required
    public String getDescription() {
        return description;
    }

    @Override
    public GroupUserProfileModificationDTO newUserProfileDtoInstance() {
        return new GroupUserProfileModificationDTO();
    }

}
