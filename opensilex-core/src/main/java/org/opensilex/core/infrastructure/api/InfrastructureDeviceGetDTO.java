/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import org.opensilex.core.infrastructure.dal.InfrastructureDeviceModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vince
 */
public class InfrastructureDeviceGetDTO extends NamedResourceDTO<InfrastructureDeviceModel> {

    protected URI infrastructure;

    public URI getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(URI infrastructure) {
        this.infrastructure = infrastructure;
    }

    @Override
    public void toModel(InfrastructureDeviceModel model) {
        super.toModel(model);
        InfrastructureModel infra = new InfrastructureModel();
        infra.setUri(getInfrastructure());
        model.setInfrastructure(infra);
    }

    @Override
    public void fromModel(InfrastructureDeviceModel model) {
        super.fromModel(model);
        setInfrastructure(model.getInfrastructure().getUri());
    }

    @Override
    public InfrastructureDeviceModel newModelInstance() {
        return new InfrastructureDeviceModel();
    }

    public static InfrastructureDeviceGetDTO getDTOFromModel(InfrastructureDeviceModel model) {
        InfrastructureDeviceGetDTO dto = new InfrastructureDeviceGetDTO();
        dto.fromModel(model);

        return dto;
    }
}
