//******************************************************************************
//                                       BrapiVariable.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 27 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;
import org.joda.time.DateTime;

/**
 * Represents a variable according to brapi specifications
 * @See https://brapi.docs.apiary.io/#reference/observation-variables
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiVariable {
    //The id of the variable in the triplestore e.g. http://www.phenome-fppn.fr/platform/id/variables/v001
    private String ObservationVariableDbId;
    //The label of the variable in the triplestore e.g. "Leaf-Area_Index_m2.m2"   
    private String ObservationVariableName;
    //SILEX:todo
    //class ontologyReference and get info
    private String ontologyReference;
    //\SILEX
    private String ontologyDbId;
    private String ontologyName;
    private ArrayList<String> synonyms;
    private ArrayList<String> contextOfUse;
    private String growthStage;
    private String status;
    private String xref;
    private String institution;
    private String scientist;
    private DateTime submissionTimesTamp;
    private String language;
    private String crop;
    //The id of the trait in the triplestore e.g. http://www.phenome-fppn.fr/platform/id/traits/t001
    private BrapiVariableTrait trait;
    //The id of the method in the triplestore e.g. http://www.phenome-fppn.fr/platform/id/methods/m001
    private BrapiMethod method;
    //The id of the unit in the triplestore e.g. http://www.phenome-fppn.fr/platform/id/units/u001
    private BrapiScale scale;
    private String defaultValue;    
    private String documentationURL;

    public BrapiVariable() {
    }

    public String getObservationVariableDbId() {
        return ObservationVariableDbId;
    }

    public void setObservationVariableDbId(String ObservationVariableDbId) {
        this.ObservationVariableDbId = ObservationVariableDbId;
    }

    public String getObservationVariableName() {
        return ObservationVariableName;
    }

    public void setObservationVariableName(String ObservationVariableName) {
        this.ObservationVariableName = ObservationVariableName;
    }

    public String getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(String ontologyReference) {
        this.ontologyReference = ontologyReference;
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

    public DateTime getSubmissionTimesTamp() {
        return submissionTimesTamp;
    }

    public void setSubmissionTimesTamp(DateTime submissionTimesTamp) {
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

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }
    
}
