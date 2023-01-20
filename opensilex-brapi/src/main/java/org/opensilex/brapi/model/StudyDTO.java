//******************************************************************************
//                          StudyDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opensilex.core.experiment.dal.ExperimentModel;

/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author Alice Boizet, Bernhard Gschloessl
 */
public class StudyDTO {
    private String active; //v2.1 : A flag to indicate if a Study is currently active and ongoing
    private Map additionalInfo; //v2.1 : A free space containing any additional information related to a particular object.
    private String commonCropName; //v2.1 : Common name for the crop associated with this study
    private String documentationURL; //v2.1 : A URL to the human readable documentation of an object
    private String endDate; //v2.1 : The date the study ends MIAPPE V1.1 (DM-15) End date of study
    private String locationDbId; //v2.1 : The unique identifier for a Location
    private String locationName; //v2.1 : A human readable name for this location MIAPPE V1.1 (DM-18) Experimental site name
    private String name; //old BrAPI -> v1.3
    private String programDbId; //old BrAPI -> v1.3
    private String programName; //old BrAPI -> v1.3
    private List<Season> seasons;//Attention: pas conforme avec BrAPI v2.1 (<-> List<String> seasons) : https://brapicore21.docs.apiary.io/#/reference/study/get-studies-study-db-id
    private String startDate; //v2.1 : The date this study started MIAPPE V1.1 (DM-14) Start date of study
    private String studyDbId; //v2.1 : The ID which uniquely identifies a study within the given database server MIAPPE V1.1 (DM-11) Study unique ID -
    private String studyName; //v2.1 : The human readable name for a study MIAPPE V1.1 (DM-12) Study title
    private String studyType; //v2.1 : The type of study being performed. ex. "Yield Trial", etc
    private String studyTypeDbId; //old BrAPI -> v1.3
    private String studyTypeName; //old BrAPI -> v1.3
    private String trialDbId; //v2.1 : The ID which uniquely identifies a trial
    private String trialName;  //v2.1 : The human readable name of a trial

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public Map getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public void setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<Season> seasons) {
        this.seasons = seasons;
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

    public String getStudyType() {
        return studyType;
    }

    public void setStudyType(String studyType) {
        this.studyType = studyType;
    }

    public String getStudyTypeDbId() {
        return studyTypeDbId;
    }

    public void setStudyTypeDbId(String studyTypeDbId) {
        this.studyTypeDbId = studyTypeDbId;
    }

    public String getStudyTypeName() {
        return studyTypeName;
    }

    public void setStudyTypeName(String studyTypeName) {
        this.studyTypeName = studyTypeName;
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
    
    public static StudyDTO fromModel(ExperimentModel model) {
        StudyDTO study = new StudyDTO();
        
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
