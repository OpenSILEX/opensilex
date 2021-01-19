/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import java.net.URI;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.organisation.dal.InfrastructureTeamModel;
import org.opensilex.security.group.api.GroupDTO;

/**
 *
 * @author vince
 */
public class InfrastructureTeamDTO extends GroupDTO {

    protected URI infrastructure;

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
