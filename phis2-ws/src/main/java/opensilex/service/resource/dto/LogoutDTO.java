//******************************************************************************
//                               LayerDTO.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: August 2017
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;

/**
 * Logout DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@ApiModel
public class LogoutDTO extends AbstractVerifiedClass {

    @Required
    @ApiModelProperty(example = "2107aa78b05410a0dbb8f1d8b2d1b54b")
    public String access_token;

    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
