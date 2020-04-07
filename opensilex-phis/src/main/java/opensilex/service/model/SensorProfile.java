//******************************************************************************
//                            SensorProfile.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 May 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Sensor profile model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorProfile {
    
    /**
     * URI of the sensor.
     */
    private String uri;
    
    /**
     * Properties.
     */
    private ArrayList<Property> properties = new ArrayList<>();

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public ArrayList<Property> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<Property> properties) {
        this.properties = properties;
    }
    
    public void addProperty(Property property) {
        properties.add(property);
    }
}
