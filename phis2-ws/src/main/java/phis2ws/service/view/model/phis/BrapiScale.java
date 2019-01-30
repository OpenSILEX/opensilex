//******************************************************************************
//                                       BrapiScale.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 27 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * Represents a scale according to brapi specifications
 * @See https://brapi.docs.apiary.io/#reference/observation-variables
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiScale {
    //The id of the unit in the triplestore e.g. http://www.phenome-fppn.fr/platform/id/units/u001
    private String scaleDbid;
    //The scaleName of the unit in the triplestore e.g. "m2.m2"
    private String scaleName;
    private String dataType;
    private String decimalPlaces;
    //SILEX:todo
    //class ontologyReference and get info
    private String ontologyReference;
    //\SILEX
    private String xref;
    private String validValues;

    public BrapiScale() {
    }

    public String getScaleDbid() {
        return scaleDbid;
    }

    public void setScaleDbid(String scaleDbid) {
        this.scaleDbid = scaleDbid;
    }

    public String getScaleName() {
        return scaleName;
    }

    public void setScaleName(String scaleName) {
        this.scaleName = scaleName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(String decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public String getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(String ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public String getXref() {
        return xref;
    }

    public void setXref(String xref) {
        this.xref = xref;
    }

    public String getValidValues() {
        return validValues;
    }

    public void setValidValues(String validValues) {
        this.validValues = validValues;
    }  
    
}
