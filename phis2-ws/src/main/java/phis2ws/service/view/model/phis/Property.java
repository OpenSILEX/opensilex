//******************************************************************************
//                                       Property.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 sept. 2018
// Contact: vincent.migot@inra.fr morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyDTO;

/**
 * Represents the view of the properties in the triplestore. 
 * @author Migot Vincent <vincent.migot@inra.fr>
 */
public class Property {
    //type of the range (if needed)
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#Variety)
    private String rdfType;
    private LinkedList<String> rdfTypeLabels = new LinkedList<>();
    //relation name 
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
    private String relation;
    private LinkedList<String> relationLabels = new LinkedList<>();
    //value
    //(e.g. plot alias, or the uri of the variety)
    private String value;
    private LinkedList<String> valueLabels = new LinkedList<>();
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
    
    public Property(String rdfType, LinkedList<String> rdfTypeLabels
            , String relation, LinkedList<String> relationLabels
            , String value, LinkedList<String> valueLabels
            , String domain, HashMap<String, String> labels) {
        this.rdfType = rdfType;
        this.rdfTypeLabels = rdfTypeLabels;
        this.relation = relation;
        this.relationLabels = relationLabels;
        this.value = value;
        this.valueLabels = valueLabels;
        this.domain = domain;
        this.labels = labels;
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

    public void addFirstRdfTypeLabel(String label) {
        rdfTypeLabels.addFirst(label);
    }
            
    public void addLastRdfTypeLabel(String label) {
        rdfTypeLabels.addLast(label);
    }
            
    public void addRdfTypeLabels(Collection<String> labels) {
        rdfTypeLabels.addAll(labels);
    }
    
    public LinkedList<String> getRelationLabels() {
        return relationLabels;
    }

    public void setRelationLabels(LinkedList<String> relationLabels) {
        this.relationLabels = relationLabels;
    }

    public void addFirstRelationLabel(String label) {
        relationLabels.addFirst(label);
    }
    
    public void addLastRelationLabel(String label) {
        relationLabels.addLast(label);
    }
        
    public void addRelationLabels(Collection<String> labels) {
        relationLabels.addAll(labels);
    }
    
    public LinkedList<String> getValueLabels() {
        return valueLabels;
    }

    public void setValueLabels(LinkedList<String> valueLabels) {
        this.valueLabels = valueLabels;
    }
    
    public void addFirstValueLabel(String label) {
        valueLabels.addFirst(label);
    }
    
    public void addLastValueLabel(String label) {
        valueLabels.addLast(label);
    }
    
    public void addValueLabels(Collection<String> labels) {
        valueLabels.addAll(labels);
    }
    
    @Override
    public boolean equals(Object obj) {
        // this is equal to obj if value, rdfType and relation are equals
        if (obj != null && obj instanceof Property) {
            Property prop = (Property) obj;

            boolean isEqual = true;
            
            // Check if this.value == obj.value with null protection
            if (this.value == null) {
                isEqual = isEqual && (prop.value == null);
            } else {
                 isEqual = isEqual && this.value.equals(prop.value);
            }
            
            // Check if this.rdfType == obj.rdfType with null protection
            if (this.rdfType == null) {
                isEqual = isEqual && (prop.rdfType == null);
            } else {
                 isEqual = isEqual && this.rdfType.equals(prop.rdfType);
            }
            
            // Check if this.relation == obj.relation with null protection
            if (this.relation == null) {
                isEqual = isEqual && (prop.relation == null);
            } else {
                 isEqual = isEqual && this.relation.equals(prop.relation);
            }
            
            return isEqual;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Unique hascode for property is based on string concatenation of
        // value, relation and rdfType on which the String.hascode method is applied
        // @see String.hashCode
        return (value + relation + rdfType).hashCode();
    }
}
