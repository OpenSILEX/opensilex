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
    
    //infrastructure type eg. http://www.phenome-fppn.fr/vocabulary/2017#LocalInfrastructure
    private String rdfType;

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }
}