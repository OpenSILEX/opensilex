//******************************************************************************
//                          BrAPIv1TraitDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1TraitDTO {
    private List<String> alternativeAbbreviations;
    private String attribute;
    @JsonProperty("class")
    private String brapiClass;
    private String description;
    private String entity;
    private String mainAbbreviation;
    private BrAPIv1OntologyReferenceDTO ontologyReference;
    private String status;
    private List<String> synonyms;
    private String traitDbId;
    private String traitName;
    private String xref;

    public List<String> getAlternativeAbbreviations() {
        return alternativeAbbreviations;
    }

    public void setAlternativeAbbreviations(List<String> alternativeAbbreviations) {
        this.alternativeAbbreviations = alternativeAbbreviations;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

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

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getMainAbbreviation() {
        return mainAbbreviation;
    }

    public void setMainAbbreviation(String mainAbbreviation) {
        this.mainAbbreviation = mainAbbreviation;
    }

    public BrAPIv1OntologyReferenceDTO getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(BrAPIv1OntologyReferenceDTO ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public String getTraitDbId() {
        return traitDbId;
    }

    public void setTraitDbId(String traitDbId) {
        this.traitDbId = traitDbId;
    }

    public String getTraitName() {
        return traitName;
    }

    public void setTraitName(String traitName) {
        this.traitName = traitName;
    }

    public String getXref() {
        return xref;
    }

    public void setXref(String xref) {
        this.xref = xref;
    }
        
}
