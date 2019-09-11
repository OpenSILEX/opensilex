//******************************************************************************
//                                StudySearchDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 mai 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.experiment;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;

/**
 * Represents the request body of POST studies-search 
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudySearchDTO {
    private String commonCropName;
    //private ArrayList<String> germplasmDbIds;
    //private ArrayList<String> locationDbIds;
    //private ArrayList<String> observationVariableDbIds;
    //private ArrayList<String> programDbIds;
    //private ArrayList<String> programNames;
    private String seasonDbId;
    private ArrayList<String> studyDbIds;
    private ArrayList<String> studyNames;
    //private String studyType;
    //private ArrayList<String> trialDbIds;
    private String sortBy;
    private String sortOrder;
    private Integer page;
    @ApiModelProperty(example = "20")
    private Integer pageSize;

    public StudySearchDTO() {
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public void setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
    }

//    public ArrayList<String> getGermplasmDbIds() {
//        return germplasmDbIds;
//    }
//
//    public void setGermplasmDbIds(ArrayList<String> germplasmDbIds) {
//        this.germplasmDbIds = germplasmDbIds;
//    }

    public String getSeasonDbId() {
        return seasonDbId;
    }

    public void setSeasonDbId(String seasonDbIds) {
        this.seasonDbId = seasonDbIds;
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
