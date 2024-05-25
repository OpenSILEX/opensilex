//******************************************************************************
//                          Faidarev1ScaleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1ScaleDTO {
    private String dataType; // - Code - Duration - Nominal - Numerical - Ordinal - Text - Date
    private String decimalPlaces;
    private String scaleDbId;
    private String name;
    private String validValues;
    private String xref;

    public String getDataType() {
        return dataType;
    }

    public Faidarev1ScaleDTO setDataType(String dataType) {
        this.dataType = dataType;
        return this;
    }

    public String getDecimalPlaces() {
        return decimalPlaces;
    }

    public Faidarev1ScaleDTO setDecimalPlaces(String decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    public String getScaleDbId() {
        return scaleDbId;
    }

    public Faidarev1ScaleDTO setScaleDbId(String scaleDbId) {
        this.scaleDbId = scaleDbId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Faidarev1ScaleDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getValidValues() {
        return validValues;
    }

    public Faidarev1ScaleDTO setValidValues(String validValues) {
        this.validValues = validValues;
        return this;
    }

    public String getXref() {
        return xref;
    }

    public Faidarev1ScaleDTO setXref(String xref) {
        this.xref = xref;
        return this;
    }
}
