//******************************************************************************
//                          BrAPIv1StudyDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.utils.ListWithPagination;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1SuperStudyDTO {
    private String active;
    private Map<String, String> additionalInfo;
    private String commonCropName;
    private String documentationURL;
    private String endDate;
    private String locationDbId;
    private String locationName;
    private String programDbId;
    private String programName;
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

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
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

    public BrAPIv1SuperStudyDTO extractFromModel(ExperimentModel model, GermplasmDAO germplasmDAO, AccountModel user) throws Exception {

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

        ListWithPagination<GermplasmModel> germplasms = germplasmDAO.brapiSearch(
                user,
                null,
                null,
                null,
                0,
                0
        );
        if (!germplasms.getList().isEmpty()){
            Set<GermplasmModel> species = germplasms.getList().stream().map(GermplasmModel::getSpecies).collect(Collectors.toSet());
            if (species.size() == 1){
                this.setCommonCropName(species.iterator().next().getName());
            }
        }
        return this;
    }

    public static BrAPIv1SuperStudyDTO fromModel(ExperimentModel model, GermplasmDAO germplasmDAO, AccountModel user) throws Exception {
        BrAPIv1SuperStudyDTO study = new BrAPIv1SuperStudyDTO();
        return study.extractFromModel(model, germplasmDAO, user);

    }
}
