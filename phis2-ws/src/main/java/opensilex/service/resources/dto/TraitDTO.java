//******************************************************************************
//                                TraitDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 17 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.model.Trait;

/**
 * Trait DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class TraitDTO extends InstanceDefinitionDTO {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitDTO.class);
    
    @Override
    public Trait createObjectFromDTO() {
        Trait trait = (Trait) super.createObjectFromDTO();
        return trait; 
    }
}
