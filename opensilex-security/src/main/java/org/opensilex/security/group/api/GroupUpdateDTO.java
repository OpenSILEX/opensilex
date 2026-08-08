/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 *
 * @author vidalmor
 */
@Schema
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "description", "user_profiles"})
public class GroupUpdateDTO extends GroupCreationDTO {

    @Schema(description = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    public URI getUri() {
        return super.getUri();
    }

}
