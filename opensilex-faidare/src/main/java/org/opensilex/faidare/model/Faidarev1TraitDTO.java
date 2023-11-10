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
    private String traitClass;

    public String getTraitDbId() {
        return traitDbId;
    }

    public void setTraitDbId(String traitDbId) {
        this.traitDbId = traitDbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public String getMainAbbreviation() {
        return mainAbbreviation;
    }

    public void setMainAbbreviation(String mainAbbreviation) {
        this.mainAbbreviation = mainAbbreviation;
    }

    public List<String> getAlternativeAbbreviations() {
        return alternativeAbbreviations;
    }

    public void setAlternativeAbbreviations(List<String> alternativeAbbreviations) {
        this.alternativeAbbreviations = alternativeAbbreviations;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getXref() {
        return xref;
    }

    public void setXref(String xref) {
        this.xref = xref;
    }

    public String getTraitClass() {
        return traitClass;
    }

    public void setTraitClass(String traitClass) {
        this.traitClass = traitClass;
    }
}
