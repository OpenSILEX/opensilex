//******************************************************************************
//                          BrAPIv1MethodDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1MethodDTO {
    @JsonProperty("class")
    private String brapiClass;
    private String description;
    private String formula;
    private String methodDbId;
    private String methodName;
    private BrAPIv1OntologyReferenceDTO ontologyReference;
    private String reference;

    public String getBrapiClass() {
        return brapiClass;
    }

    public void setBrapiClass(String brapiClass) {
        this.brapiClass = brapiClass;
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

    public BrAPIv1OntologyReferenceDTO getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(BrAPIv1OntologyReferenceDTO ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }    
    
}
