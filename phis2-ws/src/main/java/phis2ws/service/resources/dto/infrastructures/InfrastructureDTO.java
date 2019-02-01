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
    
    //infrastructure type eg. http://www.opensilex.org/vocabulary/oeso#LocalInfrastructure
    private String rdfType;

    //infrastructure type label eg. European Infrastructure
    private String rdfTypeLabel;
    
    //uri of the parent infrastructure if exists eg. https://emphasis.plant-phenotyping.eu 
    private String parent;
        
    /**
     * Constructor to create DTO from an Infrastructure Model
     * @param infrastructure 
     */
    public InfrastructureDTO(Infrastructure infrastructure) {
        super(infrastructure);
        this.setRdfType(infrastructure.getRdfType());
        this.setRdfTypeLabel(infrastructure.getRdfTypeLabel());
        this.setParent(infrastructure.getParent());
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

    public String getRdfTypeLabel() {
        return rdfTypeLabel;
    }

    public void setRdfTypeLabel(String rdfTypeLabel) {
        this.rdfTypeLabel = rdfTypeLabel;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
}
