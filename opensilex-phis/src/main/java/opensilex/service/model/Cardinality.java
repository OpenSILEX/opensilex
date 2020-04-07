//******************************************************************************
//                              Cardinality.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 8 June 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Concept cardinality model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Cardinality {
    
    /**
     * Type of the cardinality 
     * @example owl:cardinality, owl:minCardinality, owl:maxCardinality
     */ 
    private String rdfType;
    
    /**
     * Value of the cardinality.
     */
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
