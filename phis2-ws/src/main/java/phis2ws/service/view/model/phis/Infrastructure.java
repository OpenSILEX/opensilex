//******************************************************************************
//                                       Infrastructure.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 5 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
// Subject: Represents infrastructure model
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * Represents an infrastructure model
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class Infrastructure extends RdfResourceDefinition {
    
    //infrastructure type eg. http://www.opensilex.org/vocabulary/oeso#LocalInfrastructure
    private String rdfType;

    //infrastructure type label eg. European Infrastructure
    private String rdfTypeLabel;
    
    //uri of the parent infrastructure if exists eg. https://emphasis.plant-phenotyping.eu 
    private String parent;
    
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getRdfTypeLabel() {
        return rdfTypeLabel;
    }

    public void setRdfTypeLabel(String rdfTypeLabel) {
        this.rdfTypeLabel = rdfTypeLabel;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
    
}