/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.team;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.organisation.dal.InfrastructureTeamModel;
import org.opensilex.security.group.api.GroupDTO;
import org.opensilex.security.group.api.GroupUserProfileDTO;


@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "description", "user_profiles", "organisation"})

public class InfrastructureTeamDTO extends GroupDTO {

    
    @JsonProperty("rdf_type")
    protected URI type;
    
    @JsonProperty("rdf_type_name")
    protected String typeLabel;
    
    
    @JsonProperty("user_profiles")
    protected List<GroupUserProfileDTO> userProfiles;
    
    @JsonProperty("organisation")
    protected URI infrastructure;

    
    @NotNull
    public URI getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(URI infrastructure) {
        this.infrastructure = infrastructure;
    }

    @Override
    public InfrastructureTeamModel newModelInstance() {
        return new InfrastructureTeamModel();
    }

    @Override
    public InfrastructureTeamModel newModel() {
        InfrastructureTeamModel instance = newModelInstance();
        toModel(instance);
        return instance;
    }

    public void fromModel(InfrastructureTeamModel model) {
        super.fromModel(model);
        setInfrastructure(model.getInfrastructure().getUri());
    }

    public void toModel(InfrastructureTeamModel model) {
        super.toModel(model);
        InfrastructureModel infrastructureModel = new InfrastructureModel();
        infrastructureModel.setUri(getInfrastructure());
        model.setInfrastructure(infrastructureModel);
    }

    public static InfrastructureTeamDTO getDTOFromModel(InfrastructureTeamModel model) {
        InfrastructureTeamDTO dto = new InfrastructureTeamDTO();
        dto.fromModel(model);

        return dto;
    }

}
