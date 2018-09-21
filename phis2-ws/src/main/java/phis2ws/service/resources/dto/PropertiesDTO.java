//******************************************************************************
//                                       PropertiesDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import phis2ws.service.view.model.phis.Properties;

/**
 * Represents the JSON for an object protperties with its uri
 *
 * @see PropertyDTO
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class PropertiesDTO<T extends PropertyDTO> extends AbstractVerifiedClass {

    //uri of the object concerned by the properties
    private String uri;
    //list of the properties of the object
    private ArrayList<T> properties = new ArrayList<>();

    @Override
    public Properties createObjectFromDTO() {
        Properties newProperties = new Properties();
        newProperties.setUri(uri);

        for (T property : getProperties()) {
            newProperties.addProperty(property.createObjectFromDTO());
        }

        return newProperties;
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
    public ArrayList<T> getProperties() {
        return properties;
    }
   
    public void addProperty(T property) {
        properties.add(property);
    }
    
    public boolean hasProperty(T property) {
        return properties.contains(property);
    }
    
    public T getProperty(T property) {
        int index = properties.indexOf(property);
        
        if (index >= 0) {
            
            return properties.get(index);
        } else {
            
            return null;
        }
    }

}
