//******************************************************************************
//                          faidarev1StudyDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import java.util.List;
import java.util.Map;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1StudyDTO {
    private String active;
    private Map<String, String> additionalInfo;
    private String documentationURL;
    private String endDate;
    private String locationDbId;
    private String locationName;
    private String programDbId;
    private String programName;
    private String startDate;
    private String studyDbId;
    private String studyName;
    private String trialDbId;
    private String trialName;
    private List<Faidarev1ContactDTO> contacts;
    private List<Faidarev1DataLinkDTO> dataLinks;
    private String license;
    private Faidarev1LocationDTO location;
    private String studyDescription;
    private List<String> seasons;

    public String getActive() {
        return active;
    }

    public Faidarev1StudyDTO setActive(String active) {
        this.active = active;
        return this;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public Faidarev1StudyDTO setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public Faidarev1StudyDTO setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public Faidarev1StudyDTO setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getLocationDbId() {
        return locationDbId;
    }

    public Faidarev1StudyDTO setLocationDbId(String locationDbId) {
        this.locationDbId = locationDbId;
        return this;
    }

    public String getLocationName() {
        return locationName;
    }

    public Faidarev1StudyDTO setLocationName(String locationName) {
        this.locationName = locationName;
        return this;
    }

    public String getProgramDbId() {
        return programDbId;
    }

    public Faidarev1StudyDTO setProgramDbId(String programDbId) {
        this.programDbId = programDbId;
        return this;
    }

    public String getProgramName() {
        return programName;
    }

    public Faidarev1StudyDTO setProgramName(String programName) {
        this.programName = programName;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public Faidarev1StudyDTO setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public Faidarev1StudyDTO setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
        return this;
    }

    public String getStudyName() {
        return studyName;
    }

    public Faidarev1StudyDTO setStudyName(String studyName) {
        this.studyName = studyName;
        return this;
    }

    public String getTrialDbId() {
        return trialDbId;
    }

    public Faidarev1StudyDTO setTrialDbId(String trialDbId) {
        this.trialDbId = trialDbId;
        return this;
    }

    public String getTrialName() {
        return trialName;
    }

    public Faidarev1StudyDTO setTrialName(String trialName) {
        this.trialName = trialName;
        return this;
    }

    public List<Faidarev1ContactDTO> getContacts() {
        return contacts;
    }

    public Faidarev1StudyDTO setContacts(List<Faidarev1ContactDTO> contacts) {
        this.contacts = contacts;
        return this;
    }

    public List<Faidarev1DataLinkDTO> getDataLinks() {
        return dataLinks;
    }

    public Faidarev1StudyDTO setDataLinks(List<Faidarev1DataLinkDTO> dataLinks) {
        this.dataLinks = dataLinks;
        return this;
    }

    public String getLicense() {
        return license;
    }

    public Faidarev1StudyDTO setLicense(String license) {
        this.license = license;
        return this;
    }

    public Faidarev1LocationDTO getLocation() {
        return location;
    }

    public Faidarev1StudyDTO setLocation(Faidarev1LocationDTO location) {
        this.location = location;
        return this;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public Faidarev1StudyDTO setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
        return this;
    }

    public List<String> getSeasons() {
        return seasons;
    }

    public Faidarev1StudyDTO setSeasons(List<String> seasons) {
        this.seasons = seasons;
        return this;
    }
}
