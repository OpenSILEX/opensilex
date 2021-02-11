//******************************************************************************
//                          Method.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class Method {
    @JsonProperty("class")
    private String brapiClass;
    private String description;
    private String formula;
    private String methodDbId;
    private String methodName;
    private OntologyReference ontologyReference;    
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

    public OntologyReference getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(OntologyReference ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }    
    
}
