//******************************************************************************
//                          Scale.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class Scale {
    private String dataType; // - Code - Duration - Nominal - Numerical - Ordinal - Text - Date
    private String decimalPlaces;
    private String name;
    private OntologyReference ontologyReference;
    private String scaleDbId;
    private String scaleName;
    private String validValues;
    private String xref;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OntologyReference getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(OntologyReference ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public String getScaleDbId() {
        return scaleDbId;
    }

    public void setScaleDbId(String scaleDbId) {
        this.scaleDbId = scaleDbId;
    }

    public String getScaleName() {
        return scaleName;
    }

    public void setScaleName(String scaleName) {
        this.scaleName = scaleName;
    }

    public String getValidValues() {
        return validValues;
    }

    public void setValidValues(String validValues) {
        this.validValues = validValues;
    }

    public String getXref() {
        return xref;
    }

    public void setXref(String xref) {
        this.xref = xref;
    }
    
}
