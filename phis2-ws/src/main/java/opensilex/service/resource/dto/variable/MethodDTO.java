//******************************************************************************
//                                MethodDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 3 juin 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.variable;

import opensilex.service.model.Method;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class MethodDTO extends RdfResourceDefinitionDTO {
    public MethodDTO(Method method) {
        super(method);
    }
}
