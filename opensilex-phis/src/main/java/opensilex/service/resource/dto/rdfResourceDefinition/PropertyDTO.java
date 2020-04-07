//******************************************************************************
//                                PropertyDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 10 Sept, 2018
// Contact: morgane.vidal@inra.fr, vincent.migot@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.rdfResourceDefinition;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Property;

/**
 * Property DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyDTO extends AbstractVerifiedClass {

    /**
     * Property type.
     * null if it is a string (so not an URI).
     * @example http://www.opensilex.org/vocabulary/oeso#Variety
     */
    protected String rdfType; 

    /**
     * Relation name.
     * @example http://www.opensilex.org/vocabulary/oeso#hasVariety)
     */
    protected String relation;

    /**
     * Value.
     * @example http://www.phenome-fppn.fr/id/species/maize
     */
    protected String value;

    public PropertyDTO() {
    }
        
    public PropertyDTO(Property property) {
        this();
        
        this.rdfType = property.getRdfType();
        this.relation = property.getRelation();
        this.value = property.getValue();
    }

    @Override
    public Property createObjectFromDTO() {
        Property property = new Property();
        property.setRdfType(rdfType);
        property.setRelation(relation);
        property.setValue(value);

        return property;
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROPERTY_RDF_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROPERTY_RELATION)
    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROPERTY_VALUE)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
