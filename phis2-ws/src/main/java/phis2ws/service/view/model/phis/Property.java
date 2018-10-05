//******************************************************************************
//                                       Property.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 sept. 2018
// Contact: vincent.migot@inra.fr morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Represents the view of the properties in the triplestore. 
 * @author Migot Vincent <vincent.migot@inra.fr>
 */
public class Property {
    //type of the range (if needed)
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#Variety)
    private String rdfType;
    private LinkedList<String> rdfTypeLabels;
    //relation name 
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
    private String relation;
    private LinkedList<String> relationLabels;
    //value
    //(e.g. plot alias, or the uri of the variety)
    private String value;
    private LinkedList<String> valueLabels;
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
    
    public LinkedList<String> getRdfTypeLabels() {
        return rdfTypeLabels;
    }

    public void setRdfTypeLabels(LinkedList<String> rdfTypeLabels) {
        this.rdfTypeLabels = rdfTypeLabels;
    }

    public LinkedList<String> getRelationLabels() {
        return relationLabels;
    }

    public void setRelationLabels(LinkedList<String> relationLabels) {
        this.relationLabels = relationLabels;
    }

    public LinkedList<String> getValueLabels() {
        return valueLabels;
    }

    public void setValueLabels(LinkedList<String> valueLabels) {
        this.valueLabels = valueLabels;
    }
}
