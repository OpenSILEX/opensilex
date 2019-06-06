//******************************************************************************
//                                TraitDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 3 juin 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.variable;

import opensilex.service.model.Trait;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class TraitDTO extends RdfResourceDefinitionDTO {
    public TraitDTO(Trait trait) {
        super(trait);
    }
}
