//******************************************************************************
//                                       Structure.java
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
public class Infrastructure {
    
    //infrastructure uri eg. http://www.phenome-fppn.fr/m3p
    private String uri;
    //infrastructure type eg. http://www.phenome-fppn.fr/vocabulary/2017#LocalInfrastructure
    private String rdfType;
    //infrastructure alias string eg. M3P
    private String label;

    public String getUri() {
        return uri;
    }

    public String getRdfType() {
        return rdfType;
    }

    public String getLabel() {
        return label;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}