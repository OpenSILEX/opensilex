//******************************************************************************
//                           InfrastructureDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 15 Oct. 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.infrastructure;

import opensilex.service.resource.dto.rdfResourceDefinition.PropertyDTO;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyLabelsDTO;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import opensilex.service.model.Infrastructure;
import opensilex.service.model.Property;

/**
 * Infrastructure DTO.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class InfrastructureDTO extends RdfResourceDefinitionDTO {
    
    /**
     * Infrastructure type.
     * @example http://www.opensilex.org/vocabulary/oeso#LocalInfrastructure
     */
    private String rdfType;

    /**
     * Infrastructure type label.
     * @example European Infrastructure
     */
    private String rdfTypeLabel;
    
    /**
     * URI of the parent infrastructure.
     * @example https://emphasis.plant-phenotyping.eu 
     */
    private String parent;
        
    /**
     * Constructor to create DTO from an Infrastructure Model.
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
