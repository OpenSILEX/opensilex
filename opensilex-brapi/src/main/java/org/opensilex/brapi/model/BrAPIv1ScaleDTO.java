//******************************************************************************
//                          BrAPIv1ScaleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1ScaleDTO {
    private String dataType; // - Code - Duration - Nominal - Numerical - Ordinal - Text - Date
    private String decimalPlaces;
    private BrAPIv1OntologyReferenceDTO ontologyReference;
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

    public BrAPIv1OntologyReferenceDTO getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(BrAPIv1OntologyReferenceDTO ontologyReference) {
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

    public BrAPIv1ScaleDTO extractFromModel(UnitModel unitModel){

        if (unitModel.getUri() != null){
            this.setScaleDbId(unitModel.getUri().toString());
        }

        if (unitModel.getName() != null){
            this.setScaleName(unitModel.getName());
        }

        return this;
    }

    public static BrAPIv1ScaleDTO fromModel(UnitModel unitModel){
        BrAPIv1ScaleDTO scale = new BrAPIv1ScaleDTO();
        return scale.extractFromModel(unitModel);
    }
}
