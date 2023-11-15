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
    private Double altitude;
    private String countryCode;
    private String countryName;
    private String instituteAddress;
    private String instituteName;
    private Double latitude;
    private Double longitude;
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

    public Double getAltitude() {
        return altitude;
    }

    public Faidarev1StudyDTO setAltitude(Double altitude) {
        this.altitude = altitude;
        return this;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Faidarev1StudyDTO setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public String getCountryName() {
        return countryName;
    }

    public Faidarev1StudyDTO setCountryName(String countryName) {
        this.countryName = countryName;
        return this;
    }

    public String getInstituteAddress() {
        return instituteAddress;
    }

    public Faidarev1StudyDTO setInstituteAddress(String instituteAddress) {
        this.instituteAddress = instituteAddress;
        return this;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public Faidarev1StudyDTO setInstituteName(String instituteName) {
        this.instituteName = instituteName;
        return this;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Faidarev1StudyDTO setLatitude(Double latitude) {
        this.latitude = latitude;
        return this;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Faidarev1StudyDTO setLongitude(Double longitude) {
        this.longitude = longitude;
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
