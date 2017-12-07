//**********************************************************************************************
//                                       TraitDTO.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 17 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 17 2017
// Subject: A class which contains methods to automatically check the attributes 
//          of a class, from rules defined by user.
//          Contains the list of the elements which might be send by the client 
//          to save the database
//***********************************************************************************************
package phis2ws.service.resources.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.view.model.phis.Trait;

public class TraitDTO extends InstanceDefinitionDTO {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitDTO.class);
    
    public Trait createObjectFromDTO() {
        Trait trait = (Trait) super.createObjectFromDTO();
        return trait; 
    }
}
