//******************************************************************************
//                                       SensorProfile.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 28 mai 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  28 mai 2018
// Subject: Represents the view of the profiles of sensors.
//******************************************************************************
package opensilex.service.view.model.phis;

import java.util.ArrayList;

/**
 * Represents the view of the profiles of sensors.
 * @see Property
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorProfile {
    //uri of the sensor
    private String uri;
    //properties of the sensor
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
