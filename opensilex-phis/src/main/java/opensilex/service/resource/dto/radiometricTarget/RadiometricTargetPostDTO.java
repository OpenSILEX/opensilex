//******************************************************************************
//                        RadiometricTargetPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 26 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.radiometricTarget;

import opensilex.service.resource.dto.PropertiesPostDTO;
import opensilex.service.model.RadiometricTarget;

/**
 * Radiometric target POST DTO.
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
