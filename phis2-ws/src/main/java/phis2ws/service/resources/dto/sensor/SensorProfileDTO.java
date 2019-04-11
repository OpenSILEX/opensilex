//******************************************************************************
//                                       SensorProfileDTO.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 28 mai 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  28 mai 2018
// Subject: Represents the submitted JSON for the sensor's profile
//******************************************************************************
package phis2ws.service.resources.dto.sensor;

import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyDTO;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyPostDTO;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.SensorProfile;

/**
 * Represents the submitted JSON for the sensor's profile
 *
 * @see PropertyDTO
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorProfileDTO extends AbstractVerifiedClass {

    //uri of the sensor concerned by the properties
    private String uri;
    //list of the properties of the sensor
    private ArrayList<PropertyPostDTO> properties = new ArrayList<>();

    @Override
    public SensorProfile createObjectFromDTO() {
        SensorProfile sensorProfile = new SensorProfile();
        sensorProfile.setUri(uri);

        properties.forEach((property) -> {
            sensorProfile.addProperty(property.createObjectFromDTO());
        });

        return sensorProfile;
    }
    
    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @NotEmpty
    @NotNull
    @Valid
    public ArrayList<PropertyPostDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyPostDTO> properties) {
        this.properties = properties;
    }
    
    public void addProperty(PropertyPostDTO property) {
        properties.add(property);
    }
}
