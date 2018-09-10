//******************************************************************************
//                                       PropertiesDTO.java
//
// Author(s): Vincent Migot <vincent.migot@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 10 septembre 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  10 septembre 2018
// Subject: Represents the JSON for an object protperties with its uri
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.SensorProfile;

/**
 * Represents the JSON for an object protperties with its uri
 *
 * @see PropertyDTO
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class PropertiesDTO extends AbstractVerifiedClass {

    //uri of the object concerned by the properties
    private String uri;
    //list of the properties of the object
    private ArrayList<PropertyDTO> properties = new ArrayList<>();

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
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_INFRASTRUCTURE_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @NotEmpty
    @NotNull
    @Valid
    public ArrayList<PropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyDTO> properties) {
        this.properties = properties;
    }
    
    public void addProperty(PropertyDTO property) {
        properties.add(property);
    }
}
