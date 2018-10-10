//******************************************************************************
//                                       PropertiesPutDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 9 oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.Required;

/**
 * Represents the JSON for the update of an object with it uri, label and its properties.
 * @see PropertiesDTO
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertiesLabelDTO extends PropertiesDTO<PropertyDTO> {

    //Label of the object concerned by the properties. e.g. rt01
    protected String label;

    @Required
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
