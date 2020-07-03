/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import org.opensilex.core.infrastructure.dal.InfrastructureFacilityModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vince
 */
public class InfrastructureFacilityGetDTO extends NamedResourceDTO<InfrastructureFacilityModel> {

    protected URI infrastructure;

    public URI getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(URI infrastructure) {
        this.infrastructure = infrastructure;
    }

    @Override
    public void toModel(InfrastructureFacilityModel model) {
        super.toModel(model);
        InfrastructureModel infra = new InfrastructureModel();
        infra.setUri(getInfrastructure());
        model.setInfrastructure(infra);
    }

    @Override
    public void fromModel(InfrastructureFacilityModel model) {
        super.fromModel(model);
        setInfrastructure(model.getInfrastructure().getUri());
    }

    @Override
    public InfrastructureFacilityModel newModelInstance() {
        return new InfrastructureFacilityModel();
    }

    public static InfrastructureFacilityGetDTO getDTOFromModel(InfrastructureFacilityModel model) {
        InfrastructureFacilityGetDTO dto = new InfrastructureFacilityGetDTO();
        dto.fromModel(model);

        return dto;
    }
}
