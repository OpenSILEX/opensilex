/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import phis2ws.service.resources.dto.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;

/**
 * Represente le JSON soumis pour les objets de type token
 *
 * @author A. Charleroy
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
