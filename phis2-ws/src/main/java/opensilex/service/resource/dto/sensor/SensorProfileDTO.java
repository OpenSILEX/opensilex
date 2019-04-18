//******************************************************************************
//                             SensorProfileDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 May 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.sensor;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyPostDTO;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.SensorProfile;

/**
 * Sensor profile DTO.
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
