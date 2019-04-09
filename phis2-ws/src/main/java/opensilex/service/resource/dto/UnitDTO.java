//******************************************************************************
//                               UnitDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 17 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
<<<<<<< HEAD:phis2-ws/src/main/java/opensilex/service/resources/dto/UnitDTO.java
//******************************************************************************
package opensilex.service.resources.dto;
=======
// Last modification date:  November, 17 2017
// Subject: A class which contains methods to automatically check the attributes 
//          of a class, from rules defined by user.
//          Contains the list of the elements which might be send by the client 
//          to save the database
//***********************************************************************************************
package opensilex.service.resource.dto;
>>>>>>> reorganize-packages:phis2-ws/src/main/java/opensilex/service/resource/dto/UnitDTO.java

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
