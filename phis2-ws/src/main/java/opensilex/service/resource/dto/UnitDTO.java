//******************************************************************************
//                               UnitDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 17 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.model.Unit;

/**
 * Unit DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class UnitDTO extends InstanceDefinitionDTO {
    final static Logger LOGGER = LoggerFactory.getLogger(UnitDTO.class);
    
    @Override
    public Unit createObjectFromDTO() {
        Unit trait = (Unit) super.createObjectFromDTO();
        return trait; 
    }
}
