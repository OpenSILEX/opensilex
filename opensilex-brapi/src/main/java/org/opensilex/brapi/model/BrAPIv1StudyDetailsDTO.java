//******************************************************************************
//                          BrAPIv1StudyDetailsDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.dal.ExperimentModel;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1StudyDetailsDTO extends BrAPIv1SuperStudyDTO {
    private List<BrAPIv1ContactDTO> contacts;
    private List<BrAPIv1DataLinkDTO> dataLinks;
    private String license;
    private BrAPIv1LocationDTO location;
    private Float altitude;
    private String countryCode;
    private String countryName;
    private String instituteAddress;
    private String instituteName;
    private Float latitude;
    private Float longitude;
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

    public Float getAltitude() {
        return altitude;
    }

    public void setAltitude(Float altitude) {
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

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
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

    public BrAPIv1StudyDetailsDTO extractFromModel(ExperimentModel model) {
        super.extractFromModel(model);
        this.setStudyDescription(model.getDescription());

        if (model.getEndDate() != null){
            List<String> seasons = new ArrayList<>();
            for (int studyYear = model.getStartDate().getYear(); studyYear <= model.getEndDate().getYear(); studyYear++){
                seasons.add(String.valueOf(studyYear));
            }
            this.setSeasons(seasons);
        }

        return this;
    }

    public static BrAPIv1StudyDetailsDTO fromModel(ExperimentModel model) {
        BrAPIv1StudyDetailsDTO study = new BrAPIv1StudyDetailsDTO();
        return study.extractFromModel(model);
    }
}
