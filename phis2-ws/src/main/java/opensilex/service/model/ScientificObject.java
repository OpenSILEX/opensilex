//******************************************************************************
//                               ScientificObject.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: August 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package opensilex.service.model;

import java.util.ArrayList;

/**
 * Scientific object model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ScientificObject {
    
    /**
     * Scientific object URI.
     */
    private String uri;
    
    /**
     * //SILEX:todo
     * For the moment it is an attribute.
     * Modify getAO (search) getting all properties, alias included
     */
    private String alias;
       //\SILEX:todo
    
    /**
     * Type.
     */
    private String rdfType;
    
    /**
     * Geometry of the scientific object.
     */
    private String geometry;
    
    /**
     * Experiment of the scientific object.
     */
    private String experiment;
    
    /**
     * Object which has part the scientific object
     */
    private String isPartOf;
    
    //year of the scientific object
    private String year;
    
    //label of the scientific object
    private String label;
    
    //germplasm
    private String germplasmURI;
    
    //The properties of the scientific object
    
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

    public void setProperties(ArrayList<Property> newProperties) {
        properties = newProperties;
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }   

    public String getGermplasmURI() {
        return germplasmURI;
    }

    public void setGermplasmURI(String germplasmURI) {
        this.germplasmURI = germplasmURI;
    }
    
    
}
