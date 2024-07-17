//******************************************************************************
//                          Faidarev1MethodDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1MethodDTO {
    @JsonProperty("class")
    private String faidareClass;
    private String description;
    private String formula;
    private String methodDbId;
    private String name;
    private String reference;

    public String getFaidareClass() {
        return faidareClass;
    }

    public Faidarev1MethodDTO setFaidareClass(String faidareClass) {
        this.faidareClass = faidareClass;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Faidarev1MethodDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getFormula() {
        return formula;
    }

    public Faidarev1MethodDTO setFormula(String formula) {
        this.formula = formula;
        return this;
    }

    public String getMethodDbId() {
        return methodDbId;
    }

    public Faidarev1MethodDTO setMethodDbId(String methodDbId) {
        this.methodDbId = methodDbId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Faidarev1MethodDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public Faidarev1MethodDTO setReference(String reference) {
        this.reference = reference;
        return this;
    }
}
