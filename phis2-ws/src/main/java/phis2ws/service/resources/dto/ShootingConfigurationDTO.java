//**********************************************************************************************
//                                       ShootingConfigurationDTO.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 8 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 8 2017
// Subject: Represents the JSON submitted for the images shooting configuration 
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.dto.validation.interfaces.Date;
import phis2ws.service.resources.dto.validation.interfaces.URL;
import phis2ws.service.view.model.phis.ShootingConfiguration;

/**
 * corresponds to the submitted JSON for an image shooting configuration
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ShootingConfigurationDTO extends AbstractVerifiedClass {
    
    //date of the shooting
    private String date;
    //position of the sensor
    private String position;
    //uri of the sensor used to take the images
    private String sensor;
    
    @Override
    public ShootingConfiguration createObjectFromDTO() {
        ShootingConfiguration shootingConfiguration = new ShootingConfiguration();
        shootingConfiguration.setDate(date);
        shootingConfiguration.setSensor(sensor);
        shootingConfiguration.setPosition(position);
        
        return shootingConfiguration;
    }
    
    @Date(DateFormat.YMDHMSZ)
    @Required
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

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_URI)
    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }
}
