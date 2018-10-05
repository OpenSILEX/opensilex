//******************************************************************************
//                                       Properties.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 sept. 2018
// Contact: vincent.migot@inra.fr morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 * Represents the view of the properties associated to an URI in the triplestore
 * @see Property
 * @author Migot Vincent <vincent.migot@inra.fr>
 */
public class Properties {
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
