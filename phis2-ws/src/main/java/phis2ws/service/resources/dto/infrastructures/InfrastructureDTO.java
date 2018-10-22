//******************************************************************************
//                                       InfrastructureDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 15 oct. 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.infrastructures;

import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyDTO;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyLabelsDTO;
import phis2ws.service.resources.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import phis2ws.service.view.model.phis.Infrastructure;
import phis2ws.service.view.model.phis.Property;

/**
 * Represent an infrastructure
 * 
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class InfrastructureDTO extends RdfResourceDefinitionDTO {
    
    String rdfType;
    
    /**
     * Constructor to create DTO from a Infrastructure Model
     * @param infrastructure 
     */
    public InfrastructureDTO(Infrastructure infrastructure) {
        super(infrastructure);
        this.setRdfType(infrastructure.getRdfType());
    }
    
    @Override
    protected PropertyDTO getDTOInstance(Property property) {
        return new PropertyLabelsDTO(property);
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }
}
