//******************************************************************************
//                          Faidarev1ObservationVariableDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import java.util.ArrayList;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1ObservationVariableDTO {

    private ArrayList<String> contextOfUse;
    private String crop;
    private String defaultValue;
    private String documentationURL;
    private String growthStage;
    private String institution;
    private String language; // This will change once the multilabel development is done
    private Faidarev1MethodDTO method;
    private Faidarev1ScaleDTO scale;
    private String scientist;
    private String status;
    private ArrayList<String> synonyms;
    private Faidarev1TraitDTO trait;
    private String xref;
    private String observationVariableDbId;
    private String name;
    private String date;
    private String ontologyDbId;
    private String ontologyName;

    public ArrayList<String> getContextOfUse() {
        return contextOfUse;
    }

    public Faidarev1ObservationVariableDTO setContextOfUse(ArrayList<String> contextOfUse) {
        this.contextOfUse = contextOfUse;
        return this;
    }

    public String getCrop() {
        return crop;
    }

    public Faidarev1ObservationVariableDTO setCrop(String crop) {
        this.crop = crop;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Faidarev1ObservationVariableDTO setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public Faidarev1ObservationVariableDTO setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
        return this;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public Faidarev1ObservationVariableDTO setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
        return this;
    }

    public String getInstitution() {
        return institution;
    }

    public Faidarev1ObservationVariableDTO setInstitution(String institution) {
        this.institution = institution;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public Faidarev1ObservationVariableDTO setLanguage(String language) {
        this.language = language;
        return this;
    }

    public Faidarev1MethodDTO getMethod() {
        return method;
    }

    public Faidarev1ObservationVariableDTO setMethod(Faidarev1MethodDTO method) {
        this.method = method;
        return this;
    }

    public Faidarev1ScaleDTO getScale() {
        return scale;
    }

    public Faidarev1ObservationVariableDTO setScale(Faidarev1ScaleDTO scale) {
        this.scale = scale;
        return this;
    }

    public String getScientist() {
        return scientist;
    }

    public Faidarev1ObservationVariableDTO setScientist(String scientist) {
        this.scientist = scientist;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Faidarev1ObservationVariableDTO setStatus(String status) {
        this.status = status;
        return this;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public Faidarev1ObservationVariableDTO setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
        return this;
    }

    public Faidarev1TraitDTO getTrait() {
        return trait;
    }

    public Faidarev1ObservationVariableDTO setTrait(Faidarev1TraitDTO trait) {
        this.trait = trait;
        return this;
    }

    public String getXref() {
        return xref;
    }

    public Faidarev1ObservationVariableDTO setXref(String xref) {
        this.xref = xref;
        return this;
    }

    public String getObservationVariableDbId() {
        return observationVariableDbId;
    }

    public Faidarev1ObservationVariableDTO setObservationVariableDbId(String observationVariableDbId) {
        this.observationVariableDbId = observationVariableDbId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Faidarev1ObservationVariableDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getDate() {
        return date;
    }

    public Faidarev1ObservationVariableDTO setDate(String date) {
        this.date = date;
        return this;
    }

    public String getOntologyDbId() {
        return ontologyDbId;
    }

    public Faidarev1ObservationVariableDTO setOntologyDbId(String ontologyDbId) {
        this.ontologyDbId = ontologyDbId;
        return this;
    }

    public String getOntologyName() {
        return ontologyName;
    }

    public Faidarev1ObservationVariableDTO setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
        return this;
    }
}
