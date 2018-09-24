//******************************************************************************
//                                       PropertyDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 10 sept, 2018
// Contact: morgane.vidal@inra.fr, vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

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

    //property type (e.g. http://www.phenome-fppn.fr/vocabulary/2017#Variety)
    //null if it is a string (so not an uri)
    private String rdfType;
    //relation name (e.g. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
    private String relation;
    //the value (e.g. http://www.phenome-fppn.fr/id/species/maize)
    private String value;

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

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROPERTY_VALUE)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj != null && obj instanceof PropertyDTO) {
            PropertyDTO prop = (PropertyDTO) obj;

            return this.value.equals(prop.value)
                   && this.rdfType.equals(prop.rdfType)
                   && this.relation.equals(prop.relation);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (value + relation + rdfType).hashCode();
    }

}
