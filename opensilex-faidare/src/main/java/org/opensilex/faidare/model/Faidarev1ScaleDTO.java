//******************************************************************************
//                          Faidarev1ScaleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import org.opensilex.core.variable.dal.UnitModel;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1ScaleDTO {
    private String dataType; // - Code - Duration - Nominal - Numerical - Ordinal - Text - Date
    private String decimalPlaces;
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

    public Faidarev1ScaleDTO extractFromModel(UnitModel unitModel){

        if (unitModel.getUri() != null){
            this.setScaleDbId(unitModel.getUri().toString());
        }

        if (unitModel.getName() != null){
            this.setScaleName(unitModel.getName());
        }

        return this;
    }

    public static Faidarev1ScaleDTO fromModel(UnitModel unitModel){
        Faidarev1ScaleDTO scale = new Faidarev1ScaleDTO();
        return scale.extractFromModel(unitModel);
    }
}
