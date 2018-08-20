//******************************************************************************
//                                       Study.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 *
 * @author boizetal
 */
public class StudiesSearch {
    private String studyDbId;
    private String name;
    private String trialDbId;
    private String trialName;
    private String studyType;
    private ArrayList<String> seasons; 
    private String locationDbId;
    private String locationName;
    private ArrayList<String> programDbIds;
    private ArrayList<String> programNames;
    private String startDate;
    private String endDate;
    private Boolean active;

    public StudiesSearch() {
    }

    public StudiesSearch(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getStudyType() {
        return studyType;
    }

    public void setStudyType(String studyType) {
        this.studyType = studyType;
    }

    public ArrayList<String> getSeasons() {
        return seasons;
    }

    public void setSeasons(ArrayList<String> seasons) {
        this.seasons = seasons;
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
    
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}


