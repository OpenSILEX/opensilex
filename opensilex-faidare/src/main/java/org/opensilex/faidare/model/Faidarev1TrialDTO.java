//******************************************************************************
//                          Faidarev1TrialDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1TrialDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import java.util.List;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1TrialDTO {
    private Boolean active;
    private Faidarev1TrialAdditionalInfoDTO additionalInfo;
    private String documentationURL;
    private String endDate;
    private String startDate;
    private String trialName;
    private String trialDbId;
    private String trialType;
    private Faidarev1DatasetAuthorshipDTO datasetAuthorship;
    private List<Faidarev1StudySummaryDTO> studies;
    private List<Faidarev1ContactDTO> contacts;
    private String programDbId;
    private String programName;

    public Boolean getActive() {
        return active;
    }

    public Faidarev1TrialDTO setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public Faidarev1TrialAdditionalInfoDTO getAdditionalInfo() {
        return additionalInfo;
    }

    public Faidarev1TrialDTO setAdditionalInfo(Faidarev1TrialAdditionalInfoDTO additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public Faidarev1TrialDTO setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public Faidarev1TrialDTO setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public Faidarev1TrialDTO setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getTrialName() {
        return trialName;
    }

    public Faidarev1TrialDTO setTrialName(String trialName) {
        this.trialName = trialName;
        return this;
    }

    public String getTrialDbId() {
        return trialDbId;
    }

    public Faidarev1TrialDTO setTrialDbId(String trialDbId) {
        this.trialDbId = trialDbId;
        return this;
    }

    public String getTrialType() {
        return trialType;
    }

    public Faidarev1TrialDTO setTrialType(String trialType) {
        this.trialType = trialType;
        return this;
    }

    public Faidarev1DatasetAuthorshipDTO getDatasetAuthorship() {
        return datasetAuthorship;
    }

    public Faidarev1TrialDTO setDatasetAuthorship(Faidarev1DatasetAuthorshipDTO datasetAuthorship) {
        this.datasetAuthorship = datasetAuthorship;
        return this;
    }

    public List<Faidarev1StudySummaryDTO> getStudies() {
        return studies;
    }

    public Faidarev1TrialDTO setStudies(List<Faidarev1StudySummaryDTO> studies) {
        this.studies = studies;
        return this;
    }

    public List<Faidarev1ContactDTO> getContacts() {
        return contacts;
    }

    public Faidarev1TrialDTO setContacts(List<Faidarev1ContactDTO> contacts) {
        this.contacts = contacts;
        return this;
    }

    public String getProgramDbId() {
        return programDbId;
    }

    public Faidarev1TrialDTO setProgramDbId(String programDbId) {
        this.programDbId = programDbId;
        return this;
    }

    public String getProgramName() {
        return programName;
    }

    public Faidarev1TrialDTO setProgramName(String programName) {
        this.programName = programName;
        return this;
    }
}
