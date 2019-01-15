//**********************************************************************************************
//                               ScientificObject.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 2017
// Subject: Represents the scientific object
//***********************************************************************************************

package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 * Represents an scientific object view
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ScientificObject {
    
    //scientific object uri
    private String uri;
    //type of the scientific object
    private String rdfType;
    //geometry of the scientific object
    private String geometry;
    //experiment of the scientific object
    private String experiment;
    //object which has part the scientific object
    private String isPartOf;
    
    //SILEX:INFO
    //Pour l'instant je l'ai mis en attribut pour aller plus vite et avancer le reste (dans le get ao)
    //Il faudra modifier le getAO (search) en récupérant toutes les propriétés, alias inclu
    private String alias;
    //\SILEX:INFO
    
    private ArrayList<Property> properties = new ArrayList<>();

    public ScientificObject(String uri) {
        this.uri = uri;
    }

    public ScientificObject() {
       
    }
    
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
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

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(String isPartOf) {
        this.isPartOf = isPartOf;
    }
}
