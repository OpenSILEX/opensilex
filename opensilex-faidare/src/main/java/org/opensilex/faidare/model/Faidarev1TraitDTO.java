//******************************************************************************
//                          Faidarev1TraitDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1TraitDTO {
    private String traitDbId;
    private String name;
    private String description;
    private List<String> synonyms;
    private String mainAbbreviation;
    private List<String> alternativeAbbreviations;
    private String entity;
    private String attribute;
    private String status;
    private String xref;
    @JsonProperty("class")
    private String traitClass;

    public String getTraitDbId() {
        return traitDbId;
    }

    public Faidarev1TraitDTO setTraitDbId(String traitDbId) {
        this.traitDbId = traitDbId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Faidarev1TraitDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Faidarev1TraitDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public Faidarev1TraitDTO setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
        return this;
    }

    public String getMainAbbreviation() {
        return mainAbbreviation;
    }

    public Faidarev1TraitDTO setMainAbbreviation(String mainAbbreviation) {
        this.mainAbbreviation = mainAbbreviation;
        return this;
    }

    public List<String> getAlternativeAbbreviations() {
        return alternativeAbbreviations;
    }

    public Faidarev1TraitDTO setAlternativeAbbreviations(List<String> alternativeAbbreviations) {
        this.alternativeAbbreviations = alternativeAbbreviations;
        return this;
    }

    public String getEntity() {
        return entity;
    }

    public Faidarev1TraitDTO setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public String getAttribute() {
        return attribute;
    }

    public Faidarev1TraitDTO setAttribute(String attribute) {
        this.attribute = attribute;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Faidarev1TraitDTO setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getXref() {
        return xref;
    }

    public Faidarev1TraitDTO setXref(String xref) {
        this.xref = xref;
        return this;
    }

    public String getTraitClass() {
        return traitClass;
    }

    public Faidarev1TraitDTO setTraitClass(String traitClass) {
        this.traitClass = traitClass;
        return this;
    }
}
