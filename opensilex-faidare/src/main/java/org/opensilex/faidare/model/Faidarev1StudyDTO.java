//******************************************************************************
//                          faidarev1StudyDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public String getActive() {
        return active;
    }
    private final String SCIENTIFIC_SUPERVISOR = "ScientificSupervisor";
    private final String TECHNICAL_SUPERVISOR = "TechnicalSupervisor";

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

    public List<Faidarev1ContactDTO> getContacts() {
        return contacts;
    }

    public void setContacts(List<Faidarev1ContactDTO> contacts) {
        this.contacts = contacts;
    }

    public List<Faidarev1DataLinkDTO> getDataLinks() {
        return dataLinks;
    }

    public void setDataLinks(List<Faidarev1DataLinkDTO> dataLinks) {
        this.dataLinks = dataLinks;
    }

    public Faidarev1LocationDTO getLocation() {
        return location;
    }

    public void setLocation(Faidarev1LocationDTO location) {
        this.location = location;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getInstituteAddress() {
        return instituteAddress;
    }

    public void setInstituteAddress(String instituteAddress) {
        this.instituteAddress = instituteAddress;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public void setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
    }

    public List<String> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<String> seasons) {
        this.seasons = seasons;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getLocationDbId() {
        return locationDbId;
    }

    public void setLocationDbId(String locationDbId) {
        this.locationDbId = locationDbId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getProgramDbId() {
        return programDbId;
    }

    public void setProgramDbId(String programDbId) {
        this.programDbId = programDbId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public String getTrialDbId() {
        return trialDbId;
    }

    public void setTrialDbId(String trialDbId) {
        this.trialDbId = trialDbId;
    }

    public String getTrialName() {
        return trialName;
    }

    public void setTrialName(String trialName) {
        this.trialName = trialName;
    }

    public Faidarev1StudyDTO extractFromModel(ExperimentModel model, FacilityDAO facilityDAO, OrganizationDAO organizationDAO, AccountModel currentAccount) throws Exception {

        this.setStudyDbId(model.getUri().toString());
        this.setStudyName(model.getName());

        if (model.getStartDate() != null) {
            this.setStartDate(model.getStartDate().toString());
        }

        if (model.getEndDate() != null) {
            this.setEndDate(model.getEndDate().toString());
        }

        LocalDate date = LocalDate.now();
        if ((model.getStartDate() != null && model.getStartDate().isAfter(date)) || (model.getEndDate() != null && model.getEndDate().isBefore(date)))  {
            this.setActive("false");
        } else {
            this.setActive("true");
        }

        if (!model.getProjects().isEmpty()){
            // ProgramName not a list so only the first one is kept
            this.setProgramName(model.getProjects().get(0).getName());
            this.setProgramDbId(model.getProjects().get(0).getUri().toString());
        }


        if (!model.getDescription().isEmpty()){
            this.setStudyDescription(model.getDescription());
        }

        if (model.getEndDate() != null){
            List<String> seasons = new ArrayList<>();
            for (int studyYear = model.getStartDate().getYear(); studyYear <= model.getEndDate().getYear(); studyYear++){
                seasons.add(String.valueOf(studyYear));
            }
            this.setSeasons(seasons);
        }

        List<FacilityModel> facilitiesList = model.getFacilities();
        if (facilitiesList.size() >= 1){
            FacilityModel facility = facilitiesList.get(0);
            Faidarev1LocationDTO locationDTO = Faidarev1LocationDTO.fromModel(facility, facilityDAO, organizationDAO, currentAccount);
            this.setLocation(locationDTO);
            this.setLatitude(locationDTO.getLatitude());
            this.setLongitude(locationDTO.getLongitude());
        }

        List<Faidarev1ContactDTO> studyContacts = new ArrayList<>();
        List<PersonModel> experimentScientificSupervisors = model.getScientificSupervisors();
        if (!experimentScientificSupervisors.isEmpty()) {
            studyContacts.addAll(experimentScientificSupervisors.stream().map(personModel -> Faidarev1ContactDTO.fromModel(personModel, SCIENTIFIC_SUPERVISOR)).collect(Collectors.toList()));
        }
        List<PersonModel> experimentTechnicalSupervisors = model.getTechnicalSupervisors();
        if (!experimentTechnicalSupervisors.isEmpty()) {
            studyContacts.addAll(experimentTechnicalSupervisors.stream().map(personModel -> Faidarev1ContactDTO.fromModel(personModel, TECHNICAL_SUPERVISOR)).collect(Collectors.toList()));
        }
        this.setContacts(studyContacts);

        return this;
    }

    public static Faidarev1StudyDTO fromModel(ExperimentModel model, FacilityDAO facilityDAO, OrganizationDAO organizationDAO, AccountModel currentAccount) throws Exception {
        Faidarev1StudyDTO study = new Faidarev1StudyDTO();
        return study.extractFromModel(model, facilityDAO, organizationDAO, currentAccount);

    }
}
