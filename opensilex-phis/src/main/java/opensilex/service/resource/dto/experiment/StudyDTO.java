//******************************************************************************
//                                StudyDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 mai 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.experiment;

import java.util.ArrayList;
import opensilex.service.resource.brapi.AdditionalInfo;

/**
 * Represents the response of get studies et post studies-search
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudyDTO {
    private String active;
    private AdditionalInfo additionalInfo;
    private String commonCropName;
    private String documentationURL;
    private String endDate;
    private String locationDbId;
    private String locationName;
    private String name;
    private String programDbId;
    private String programName;
    private ArrayList seasons;
    private String startDate;
    private String studyDbId;
    private String studyName;
    private String studyType;
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

    public AdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(AdditionalInfo additionalInfo) {
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

    public ArrayList getSeasons() {
        return seasons;
    }

    public void setSeasons(ArrayList seasons) {
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

}
