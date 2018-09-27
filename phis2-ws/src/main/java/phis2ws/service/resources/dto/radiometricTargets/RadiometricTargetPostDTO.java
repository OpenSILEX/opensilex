//******************************************************************************
//                                       RadiometricTargetDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 26 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.radiometricTargets;

import phis2ws.service.resources.dto.PropertiesPostDTO;
import phis2ws.service.view.model.phis.RadiometricTarget;

/**
 * Represents the JSON for the creation of a radiometric target.
 * @see PropertiesPostDTO
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class RadiometricTargetPostDTO extends PropertiesPostDTO {
    @Override
    public RadiometricTarget createObjectFromDTO() {
        RadiometricTarget radiometricTarget = new RadiometricTarget();
        radiometricTarget.setLabel(label);
        
        properties.forEach((property) -> {
            radiometricTarget.addProperty(property.createObjectFromDTO());
        });
        
        return radiometricTarget;
    }
}
