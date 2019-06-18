//******************************************************************************
//                                StudySearchDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 mai 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.experiment;

import java.util.ArrayList;
import javax.validation.constraints.NotNull;

/**
 * Represents the request body of POST studies-search 
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudySearchDTO {
    private String commonCropName;
    private ArrayList<String> germplasmDbIds;
    private ArrayList<String> locationDbIds;
    private ArrayList<String> observationVariableDbIds;
    private ArrayList<String> programDbIds;
    private ArrayList<String> programNames;
    private ArrayList<String> seasonDbIds;
    private ArrayList<String> studyDbIds;
    private ArrayList<String> studyNames;
    private String studyType;
    private ArrayList<String> trialDbIds;
    private String sortBy;
    private String sortOrder;
    @NotNull
    private Integer page;
    @NotNull
    private Integer pageSize;

    public StudySearchDTO() {
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public void setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
    }

    public ArrayList<String> getGermplasmDbIds() {
        return germplasmDbIds;
    }

    public void setGermplasmDbIds(ArrayList<String> germplasmDbIds) {
        this.germplasmDbIds = germplasmDbIds;
    }

    public ArrayList<String> getLocationDbIds() {
        return locationDbIds;
    }

    public void setLocationDbIds(ArrayList<String> locationDbIds) {
        this.locationDbIds = locationDbIds;
    }

    public ArrayList<String> getObservationVariableDbIds() {
        return observationVariableDbIds;
    }

    public void setObservationVariableDbIds(ArrayList<String> observationVariableDbIds) {
        this.observationVariableDbIds = observationVariableDbIds;
    }

    public ArrayList<String> getProgramDbIds() {
        return programDbIds;
    }

    public void setProgramDbIds(ArrayList<String> programDbIds) {
        this.programDbIds = programDbIds;
    }

    public ArrayList<String> getProgramNames() {
        return programNames;
    }

    public void setProgramNames(ArrayList<String> programNames) {
        this.programNames = programNames;
    }

    public ArrayList<String> getSeasonDbIds() {
        return seasonDbIds;
    }

    public void setSeasonDbIds(ArrayList<String> seasonDbIds) {
        this.seasonDbIds = seasonDbIds;
    }

    public ArrayList<String> getStudyDbIds() {
        return studyDbIds;
    }

    public void setStudyDbIds(ArrayList<String> studyDbIds) {
        this.studyDbIds = studyDbIds;
    }

    public ArrayList<String> getStudyNames() {
        return studyNames;
    }

    public void setStudyNames(ArrayList<String> studyNames) {
        this.studyNames = studyNames;
    }

    public String getStudyType() {
        return studyType;
    }

    public void setStudyType(String studyType) {
        this.studyType = studyType;
    }

    public ArrayList<String> getTrialDbIds() {
        return trialDbIds;
    }

    public void setTrialDbIds(ArrayList<String> trialDbIds) {
        this.trialDbIds = trialDbIds;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
}
