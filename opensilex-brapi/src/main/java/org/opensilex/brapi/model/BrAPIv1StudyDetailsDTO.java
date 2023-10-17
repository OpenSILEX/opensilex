//******************************************************************************
//                          BrAPIv1StudyDetailsDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1StudyDetailsDTO extends BrAPIv1SuperStudyDTO {
    private final String SCIENTIFIC_SUPERVISOR = "ScientificSupervisor";
    private final String TECHNICAL_SUPERVISOR = "TechnicalSupervisor";

    private List<BrAPIv1ContactDTO> contacts;
    private List<BrAPIv1DataLinkDTO> dataLinks;
    private String license;
    private BrAPIv1LocationDTO location;
    private Double altitude;
    private String countryCode;
    private String countryName;
    private String instituteAddress;
    private String instituteName;
    private Double latitude;
    private Double longitude;
    private String studyDescription;
    private List<String> seasons;

    public List<BrAPIv1ContactDTO> getContacts() {
        return contacts;
    }

    public void setContacts(List<BrAPIv1ContactDTO> contacts) {
        this.contacts = contacts;
    }

    public List<BrAPIv1DataLinkDTO> getDataLinks() {
        return dataLinks;
    }

    public void setDataLinks(List<BrAPIv1DataLinkDTO> dataLinks) {
        this.dataLinks = dataLinks;
    }

    public BrAPIv1LocationDTO getLocation() {
        return location;
    }

    public void setLocation(BrAPIv1LocationDTO location) {
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

    public BrAPIv1StudyDetailsDTO extractFromModel(ExperimentModel model, FacilityDAO facilityDAO, OrganizationDAO organizationDAO, AccountModel currentAccount) throws Exception {
        super.extractFromModel(model);

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
            BrAPIv1LocationDTO locationDTO = BrAPIv1LocationDTO.fromModel(facility, facilityDAO, organizationDAO, currentAccount);
            this.setLocation(locationDTO);
            this.setLatitude(locationDTO.getLatitude());
            this.setLongitude(locationDTO.getLongitude());
        }

        List<BrAPIv1ContactDTO> studyContacts = new ArrayList<>();
        List<PersonModel> experimentScientificSupervisors = model.getScientificSupervisors();
        if (!experimentScientificSupervisors.isEmpty()) {
            studyContacts.addAll(experimentScientificSupervisors.stream().map(personModel -> BrAPIv1ContactDTO.fromModel(personModel, SCIENTIFIC_SUPERVISOR)).collect(Collectors.toList()));
        }
        List<PersonModel> experimentTechnicalSupervisors = model.getTechnicalSupervisors();
        if (!experimentTechnicalSupervisors.isEmpty()) {
            studyContacts.addAll(experimentTechnicalSupervisors.stream().map(personModel -> BrAPIv1ContactDTO.fromModel(personModel, TECHNICAL_SUPERVISOR)).collect(Collectors.toList()));
        }
        this.setContacts(studyContacts);

        return this;
    }

    public static BrAPIv1StudyDetailsDTO fromModel(ExperimentModel model, FacilityDAO facilityDAO, OrganizationDAO organizationDAO, AccountModel currentAccount) throws Exception {
        BrAPIv1StudyDetailsDTO study = new BrAPIv1StudyDetailsDTO();
        return study.extractFromModel(model, facilityDAO, organizationDAO, currentAccount);
    }
}
