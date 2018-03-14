//**********************************************************************************************
//                               AgronomicalObject.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 2017
// Subject: Represents the agronomical object
//***********************************************************************************************

package phis2ws.service.view.model.phis;

import java.util.ArrayList;

public class AgronomicalObject {
    private String uri;
    private String typeAgronomicalObject;
    private String geometry;
    private String experiment;
    
    //SILEX:INFO
    //Pour l'instant je l'ai mis en attribut pour aller plus vite et avancer le reste (dans le get ao)
    //Il faudra modifier le getAO (search) en récupérant toutes les propriétés, alias inclu
    private String alias;
    //\SILEX:INFO
    
    private ArrayList<Property> properties = new ArrayList<>();

    public AgronomicalObject(String uri) {
        this.uri = uri;
    }

    public AgronomicalObject() {
       
    }
    
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTypeAgronomicalObject() {
        return typeAgronomicalObject;
    }

    public void setTypeAgronomicalObject(String typeAgronomicalObject) {
        this.typeAgronomicalObject = typeAgronomicalObject;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getUriExperiment() {
        return experiment;
    }

    public void setUriExperiment(String uriExperiment) {
        this.experiment = uriExperiment;
    }
    
    public ArrayList<Property> getProperties() {
        return properties;
    }
    
    public void addProperty(Property property) {
        properties.add(property);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
