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
    private String scaleDbid;
    private String name;
    private String datatype;
    private String decimalPlaces;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(String decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
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
