//**********************************************************************************************
//                                       ShootingConfigurationDTO.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 8 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 8 2017
// Subject: Represents the JSON submitted for the images shooting configuration 
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.ShootingConfiguration;

public class ShootingConfigurationDTO extends AbstractVerifiedClass {

    private String date;
    private String position;
    
    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("date", Boolean.TRUE);
        rules.put("position", Boolean.FALSE);
        return rules;
    }

    @Override
    public ShootingConfiguration createObjectFromDTO() {
        ShootingConfiguration shootingConfiguration = new ShootingConfiguration();
        shootingConfiguration.setDate(date);
        shootingConfiguration.setPosition(position);
        
        return shootingConfiguration;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SHOOTING_CONFIGURATION_DATE)
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SHOOTING_CONFIGURATION_POSITION)
    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
