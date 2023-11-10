//******************************************************************************
//                          Faidarev1MethodDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.variable.dal.MethodModel;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1MethodDTO {
    @JsonProperty("class")
    private String faidareClass;
    private String description;
    private String formula;
    private String methodDbId;
    private String methodName;
    private String reference;

    public String getfaidareClass() {
        return faidareClass;
    }

    public void setfaidareClass(String faidareClass) {
        this.faidareClass = faidareClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getMethodDbId() {
        return methodDbId;
    }

    public void setMethodDbId(String methodDbId) {
        this.methodDbId = methodDbId;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }    

    public Faidarev1MethodDTO extractFromModel(MethodModel methodModel){

        if (methodModel.getUri() != null){
            this.setMethodDbId(methodModel.getUri().toString());
        }

        if (methodModel.getName() != null){
            this.setMethodName(methodModel.getName());
        }

        if (methodModel.getDescription() != null){
            this.setDescription(methodModel.getDescription());
        }

        return this;
    }

    public static Faidarev1MethodDTO fromModel(MethodModel methodModel){
        Faidarev1MethodDTO method = new Faidarev1MethodDTO();
        return method.extractFromModel(methodModel);
    }
}
