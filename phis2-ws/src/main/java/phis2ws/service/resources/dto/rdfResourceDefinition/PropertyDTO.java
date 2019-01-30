//******************************************************************************
//                                       PropertyDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 10 sept, 2018
// Contact: morgane.vidal@inra.fr, vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.rdfResourceDefinition;

import io.swagger.annotations.ApiModelProperty;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.Property;

/**
 * Represents the submitted JSON for a property
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyDTO extends AbstractVerifiedClass {

    //property type (e.g. http://www.opensilex.org/vocabulary/oeso#Variety)
    //null if it is a string (so not an uri)
    protected String rdfType;
    //relation name (e.g. http://www.opensilex.org/vocabulary/oeso#fromVariety)
    protected String relation;
    //the value (e.g. http://www.phenome-fppn.fr/id/species/maize)
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
