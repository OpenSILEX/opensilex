//******************************************************************************
//                                       BrapiVariable.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 27 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 * Represents a variable according to brapi specifications
 * @See https://brapi.docs.apiary.io/#reference/observation-variables
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiVariable {
    private String ObservationVariableDbId;
    private String name;
    private String ontologyDbId;
    private String ontologyName;
    private ArrayList<String> synonyms;
    private ArrayList<String>  contextOfUse;
    private String growthStage;
    private String status;
    private String xref;
    private String institution;
    private String scientist;
    private String submissionTimesTamp;
    private String language;
    private String crop;
    private BrapiVariableTrait trait;
    private BrapiMethod method;
    private BrapiScale scale;
    private String defaultValue;    

    public BrapiVariable() {
    }

    public String getObservationVariableDbId() {
        return ObservationVariableDbId;
    }

    public void setObservationVariableDbId(String ObservationVariableDbId) {
        this.ObservationVariableDbId = ObservationVariableDbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOntologyDbId() {
        return ontologyDbId;
    }

    public void setOntologyDbId(String ontologyDbId) {
        this.ontologyDbId = ontologyDbId;
    }

    public String getOntologyName() {
        return ontologyName;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public ArrayList<String> getContextOfUse() {
        return contextOfUse;
    }

    public void setContextOfUse(ArrayList<String> contextOfUse) {
        this.contextOfUse = contextOfUse;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
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

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getScientist() {
        return scientist;
    }

    public void setScientist(String scientist) {
        this.scientist = scientist;
    }

    public String getSubmissionTimesTamp() {
        return submissionTimesTamp;
    }

    public void setSubmissionTimesTamp(String submissionTimesTamp) {
        this.submissionTimesTamp = submissionTimesTamp;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public BrapiVariableTrait getTrait() {
        return trait;
    }

    public void setTrait(BrapiVariableTrait trait) {
        this.trait = trait;
    }

    public BrapiMethod getMethod() {
        return method;
    }

    public void setMethod(BrapiMethod method) {
        this.method = method;
    }

    public BrapiScale getScale() {
        return scale;
    }

    public void setScale(BrapiScale scale) {
        this.scale = scale;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
