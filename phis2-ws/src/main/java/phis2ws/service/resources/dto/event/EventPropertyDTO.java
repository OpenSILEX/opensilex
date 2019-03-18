//******************************************************************************
//                              EventPropertyDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 18 Mar 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.event;

import io.swagger.annotations.ApiModelProperty;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyDTO;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.Property;

/**
 * Property DTO with event-specific examples
 * @author andreas
 */
public class EventPropertyDTO extends PropertyDTO {

    EventPropertyDTO(Property property) {
        super(property);
    }

    @Override
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_PROPERTY_RDF_TYPE)
    public String getRdfType() {
        return super.getRdfType();
    }

    @Override
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_PROPERTY_RELATION)
    public String getRelation() {
        return super.getRelation();
    }

    @Override
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_PROPERTY_VALUE)
    public String getValue() {
        return super.getValue();
    }
}
