//******************************************************************************
//                          StudyDetailsDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.dal.ExperimentModel;

/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author Alice Boizet, Bernhard Gschloessl
 */
public class StudyDetailsDTO extends StudyDTO {
    private List<Contact> contacts;//v2.1 List of contact entities associated with this study
    private String culturalPractices; //v2.1 : MIAPPE V1.1 (DM-28) Cultural practices - General description of the cultural practices of the study.
    private List<DataLinkDTO> dataLinks; //v2.1 : List of links to extra data files associated with this study.
    private List<EnvironmentParametersDTO> environmentParameters; //v2.1 : Environmental parameters that were kept constant throughout the study and did not change between observation units. MIAPPE V1.1 (DM-57) Environment -
    private ExperimentalDesignDTO experimentalDesign; //v2.1 : The experimental and statistical design full description plus a category PUI taken from crop research ontology or agronomy ontology
    private List<ExternalReferencesDTO> externalReferences; //v2.1 : An array of external reference ids.
    private GrowthFacilityDTO growthFacility; //v2.1 : Short description of the facility in which the study was carried out.
    private LastUpdateDTO lastUpdate; //v2.1 : The date and time when this study was last modified
    private String license; //v2.1 : The usage license associated with the study data
    private List<ObservationLevelsDTO> observationLevels; //v2.1 : Observation levels indicate the granularity level at which the measurements are taken.

    private String observationUnitsDescription; //v2.1 : MIAPPE V1.1 (DM-25) Observation unit description
    //TODO HIER WEITER
    private List<String> observationVariableDbIds; //v2.1 The list of Observation Variables being used in this study.
    //private List<String> seasons; //v2.1 List of seasons over which this study was performed. conflict with seasons (BrAPI v1.3 in StudyDTO.java)

    //BEGIN voir Gabriel si StudyDTO.java
    private String studyCode; //v2.1 : A short human readable code for a study
    private String studyDescription; //v2.1 : The description of this study MIAPPE V1.1 (DM-13) Study description
    private String studyPUI; //v2.1 : A permanent unique identifier associated with this study data.
    //END voir Gabriel si StudyDTO.java

    private Location location; //old BrAPI -> v1.3

    public String getCulturalPractices() {
        return culturalPractices;
    }

    public void setCulturalPractices(String culturalPractices) {
        this.culturalPractices = culturalPractices;
    }

    public List<DataLinkDTO> getDataLinks() {
        return dataLinks;
    }

    public void setDatalinks(List<DataLinkDTO> dataLinks) {
        this.dataLinks = dataLinks;
    }

    public List<EnvironmentParametersDTO> getEnvironmentParameters() {
        return environmentParameters;
    }

    public void setEnvironmentParameters(List<EnvironmentParametersDTO> environmentParameters) {
        this.environmentParameters = environmentParameters;
    }

    public ExperimentalDesignDTO getExperimentalDesign() {
        return experimentalDesign;
    }

    public void setExperimentalDesign(ExperimentalDesignDTO experimentalDesign) {
        this.experimentalDesign = experimentalDesign;
    }

    public List<ExternalReferencesDTO> getExternalReferences() {
        return externalReferences;
    }

    public void setExternalReferences(List<ExternalReferencesDTO> externalReferences) {
        this.externalReferences = externalReferences;
    }

    public GrowthFacilityDTO getGrowthFacility() {
        return growthFacility;
    }

    public void setGrowthFacility(GrowthFacilityDTO growthFacility) {
        this.growthFacility = growthFacility;
    }

    public LastUpdateDTO getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LastUpdateDTO lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getObservationUnitsDescription() {
        return observationUnitsDescription;
    }

    public void setObservationUnitsDescription(String observationUnitsDescription) {
        this.observationUnitsDescription = observationUnitsDescription;
    }

    public List<ObservationLevelsDTO> getObservationLevels() {
        return observationLevels;
    }

    public void setObservationLevels(List<ObservationLevelsDTO> observationLevels) {
        this.observationLevels = observationLevels;
    }

    public List<String> getObservationVariableDbIds() {
        return observationVariableDbIds;
    }

    public void setObservationVariableDbIds(List<String> observationVariableDbIds) {
        this.observationVariableDbIds = observationVariableDbIds;
    }

    public String getStudyCode() {
        return studyCode;
    }

    public void setStudyCode(String studyCode) {
        this.studyCode = studyCode;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public void setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
    }

    public String getStudyPUI() {
        return studyPUI;
    }

    public void setStudyPUI(String studyPUI) {
        this.studyPUI = studyPUI;
    }

    public List<Contact> getContacts() {
        return contacts;
    }
    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
    
    public static StudyDetailsDTO fromModel(ExperimentModel model) {
        StudyDetailsDTO study = new StudyDetailsDTO();
        if (model.getUri() != null) {
           study.setStudyDbId(model.getUri().toString()); 
        }
        
        study.setName(model.getName());
        study.setStudyName(model.getName());
        
        if (model.getStartDate() != null) {
            study.setStartDate(model.getStartDate().toString());
        }
        
        if (model.getEndDate() != null) {
            study.setEndDate(model.getEndDate().toString());
        } 
        
        LocalDate date = LocalDate.now();
        if ((model.getStartDate() != null && model.getStartDate().isAfter(date)) || (model.getEndDate() != null && model.getEndDate().isBefore(date)))  {
            study.setActive("false");
        } else {
            study.setActive("true");
        }
        
        List<Season> seasons = new ArrayList<>();
        Season season = new Season();
        //season.setYear(model.getCampaign());
        seasons.add(season);
        study.setSeasons(seasons);
        
        return study;
    }

    public static StudyDetailsDTO fromBraAPIv2Model(ExperimentModel model) {
        StudyDetailsDTO study = new StudyDetailsDTO();
        if (model.getUri() != null) {
            study.setStudyDbId(model.getUri().toString());
        }

        study.setName(model.getName());
        study.setStudyName(model.getName());

        if (model.getStartDate() != null) {
            study.setStartDate(model.getStartDate().toString());
        }

        if (model.getEndDate() != null) {
            study.setEndDate(model.getEndDate().toString());
        }

        LocalDate date = LocalDate.now();
        if ((model.getStartDate() != null && model.getStartDate().isAfter(date)) || (model.getEndDate() != null && model.getEndDate().isBefore(date)))  {
            study.setActive("false");
        } else {
            study.setActive("true");
        }

        List<Season> seasons = new ArrayList<>();
        Season season = new Season();
        //season.setYear(model.getCampaign());
        seasons.add(season);
        study.setSeasons(seasons);

        return study;
    }
    
}
