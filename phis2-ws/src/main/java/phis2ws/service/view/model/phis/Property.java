//**********************************************************************************************
//                                       Property.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  May, 29 2018 - add the domain attribute 
// Subject: Represents the view of Properties (used in Agronomical Object, SensorProfile). 
//          Corresponds to data which will be saved in the triplestore
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Represents the view of the properties in the triplestore. 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Property {
    //type of the range (if needed)
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#Variety)
    private String rdfType;
    private LinkedList<String> rdfTypeLabel;
    //relation name 
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
    private String relation;
    private LinkedList<String> relationLabel;
    //value
    //(e.g. plot alias, or the uri of the variety)
    private String value;
    private LinkedList<String> valueLabel;
    //the domain of the property. For the first version, it is only a single string.
    //then, the union and others will be added
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#MultispectralCamera)
    private String domain;
    //the list of the labels of the property. Hash Map with the languages if needed
    //it is a hash map with the language and the label
    //if there is no language for a label, the key is equals to none (?)
    private HashMap<String, String> labels = new HashMap<>();
    
    public Property() {
    }
    
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
    public HashMap<String, String> getLabels() {
        return labels;
    }

    public void setLabels(HashMap<String, String> labels) {
        this.labels = labels;
    }
    
    public void addLabel(String language, String label) {
        labels.put(language, label);
    }
    
    public LinkedList<String> getRdfTypeLabel() {
        return rdfTypeLabel;
    }

    public void setRdfTypeLabel(LinkedList<String> rdfTypeLabel) {
        this.rdfTypeLabel = rdfTypeLabel;
    }

    public LinkedList<String> getRelationLabel() {
        return relationLabel;
    }

    public void setRelationLabel(LinkedList<String> relationLabel) {
        this.relationLabel = relationLabel;
    }

    public LinkedList<String> getValueLabel() {
        return valueLabel;
    }

    public void setValueLabel(LinkedList<String> valueLabel) {
        this.valueLabel = valueLabel;
    }
}
