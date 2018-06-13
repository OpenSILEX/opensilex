//******************************************************************************
//                                       Cardinality.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 8 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  8 juin 2018
// Subject: represents the cardinality owl of a concept (for the triplestore)
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * represents the cardinality of a concept
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Cardinality {
    
    //type of the cardinality (owl:cardinality, owl:minCardinality, owl:maxCardinality)
    private String rdfType;
    //value of the cardinality (integer)
    private int cardinaity;

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public int getCardinaity() {
        return cardinaity;
    }

    public void setCardinaity(int cardinaity) {
        this.cardinaity = cardinaity;
    }
}
