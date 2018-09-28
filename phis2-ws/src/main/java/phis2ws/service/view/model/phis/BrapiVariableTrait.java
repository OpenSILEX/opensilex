//******************************************************************************
//                                       BrapiVariableTrait.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 27 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a variable trait according to brapi specification
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiVariableTrait {
    private String traitDbId;
    private String name;
    @SerializedName("class")
    private String brapiclass;
    private String description;
    private String synonyms;
    private String mainAbbreviation;
    private String alternativeAbbreviations;
    private String entity;
    private String attribute;
    private String status;
    private String xref;   

    public BrapiVariableTrait() {
    }

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

    public String getBrapiclass() {
        return brapiclass;
    }

    public void setBrapiclass(String classe) {
        this.brapiclass = classe;
    }  
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getMainAbbreviation() {
        return mainAbbreviation;
    }

    public void setMainAbbreviation(String mainAbbreviation) {
        this.mainAbbreviation = mainAbbreviation;
    }

    public String getAlternativeAbbreviations() {
        return alternativeAbbreviations;
    }

    public void setAlternativeAbbreviations(String alternativeAbbreviations) {
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
}
