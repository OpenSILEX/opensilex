//******************************************************************************
//                              BrapiVariableTrait.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 27 Sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

/**
 * BrAPI variable trait model.
 * @See https://brapi.docs.apiary.io/#reference/observation-variables
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiVariableTrait {
    
    /**
     * The id of the trait.
     * @example http://www.phenome-fppn.fr/platform/id/traits/t001
     */
    private String traitDbId;
    
    /**
     * The label of the trait.
     * @example Leaf_Area_Index
     */
    private String traitName;
    
    @SerializedName("class")
    private String brapiclass;
    
    /**
     * The comment of the trait in the triplestore
     * @example one-sided green leaf area per unit ground surface area
     */
    private String description;
    private ArrayList<String> synonyms;
    private String mainAbbreviation;
    private ArrayList<String> alternativeAbbreviations;
    private String entity;
    private String attribute;
    private String status;
    private String xref;   
    private String ontologyReference;

    public BrapiVariableTrait() {
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

    public void setTraitName(String name) {
        this.traitName = name;
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

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public String getMainAbbreviation() {
        return mainAbbreviation;
    }

    public void setMainAbbreviation(String mainAbbreviation) {
        this.mainAbbreviation = mainAbbreviation;
    }

    public ArrayList<String> getAlternativeAbbreviations() {
        return alternativeAbbreviations;
    }

    public void setAlternativeAbbreviations(ArrayList<String> alternativeAbbreviations) {
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

    public String getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(String ontologyReference) {
        this.ontologyReference = ontologyReference;
    }
}
