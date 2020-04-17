/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import org.opensilex.core.infrastructure.dal.InfrastructureDeviceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vince
 */
public class InfrastructureDeviceGetDTO extends NamedResourceDTO {

    protected URI infrastructure;

    public URI getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(URI infrastructure) {
        this.infrastructure = infrastructure;
    }

    public static InfrastructureDeviceGetDTO fromModel(InfrastructureDeviceModel model) {
        InfrastructureDeviceGetDTO dto = new InfrastructureDeviceGetDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());

        if (model.getInfrastructure() != null) {
            URI parentURI = model.getInfrastructure().getUri();
            dto.setInfrastructure(parentURI);
        }

        return dto;
    }
}
