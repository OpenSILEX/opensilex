//******************************************************************************
//                            PropertyPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 23 Oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.rdfResourceDefinition;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;

/**
 * Property POST DTO.
 * It adds the @Required to the value param.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyPostDTO extends PropertyDTO {
    
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROPERTY_VALUE)
    @Override
    public String getValue() {
        return value;
    }
}
