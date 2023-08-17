//******************************************************************************
//                          BrAPIv1StudyDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.experiment.dal.ExperimentModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1StudyDTO {
    private String active;
    private Map<String, String> additionalInfo;
    private String commonCropName;
    private String documentationURL;
    private String endDate;
    private String locationDbId;
    private String locationName;
    private String programDbId;
    private String programName;
    private List<BrAPIv1SeasonDTO> seasons;
    private String startDate;
    private String studyDbId;
    private String studyName;
    private String studyTypeDbId;
    private String studyTypeName;
    private String trialDbId;
    private String trialName;  

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

    public List<BrAPIv1SeasonDTO> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<BrAPIv1SeasonDTO> seasons) {
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

    public BrAPIv1StudyDTO extractFromModel(ExperimentModel model) {

        if (model.getUri() != null) {
            this.setStudyDbId(model.getUri().toString());
        }
        this.setStudyName(model.getName());

        if (model.getStartDate() != null) {
            this.setStartDate(model.getStartDate().toString());
        }

        if (model.getEndDate() != null) {
            this.setEndDate(model.getEndDate().toString());
        }

        // TODO : Active state could be an attribute of the class? Here it is done in the DTO but it is also done in the frontend
        LocalDate date = LocalDate.now();
        if ((model.getStartDate() != null && model.getStartDate().isAfter(date)) || (model.getEndDate() != null && model.getEndDate().isBefore(date)))  {
            this.setActive("false");
        } else {
            this.setActive("true");
        }

        if (model.getEndDate() != null){
            List<BrAPIv1SeasonDTO> seasons = new ArrayList<>();
            for (int studyYear = model.getStartDate().getYear(); studyYear <= model.getEndDate().getYear(); studyYear++){
                BrAPIv1SeasonDTO season = new BrAPIv1SeasonDTO();
                season.setYear(String.valueOf(studyYear));
                seasons.add(season);
            }
            this.setSeasons(seasons);
        }

        if (!model.getProjects().isEmpty()){
            // ProgramName not a list so only the first one is kept
            this.setProgramName(model.getProjects().get(0).getName());
        }

        return this;
    }

    public static BrAPIv1StudyDTO fromModel(ExperimentModel model) {
        BrAPIv1StudyDTO study = new BrAPIv1StudyDTO();
        return study.extractFromModel(model);

    }
}
