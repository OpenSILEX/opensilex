//******************************************************************************
//                                       BrapiObservationDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 11 févr. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.data;

import java.util.Date;
import phis2ws.service.view.model.phis.Dataset;

/**
 * Represents the response of the brapi call get Observations by study
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiObservationDTO {
    //The ID which uniquely identifies a germplasm 
    private String germplasmDbId;
    //Name of the germplasm
    private String germplasmName;
    //The ID which uniquely identifies an observation
    private String observationDbId; 
    //The level of an observation unit. ex. \"plot\", \"plant\
    private String observationLevel;
    //The date and time  when this observation was made
    private Date observationTimeStamp;
    //The ID which uniquely identifies an observation unit = scientific object
    private String observationUnitDbId;
    //A human readable name for an observation unit
    private String observationUnitName;
    //The ID which uniquely identifies an observation variable
    private String observationVariableDbId;
    //A human readable name for an observation variable
    private String observationVariableName;
    //The name or identifier of the entity which collected the observation
    private String operator;
    //SILEX:todo
    // create a class season with season, seasonDbId, year
    private String season; 
    //\SILEX
    //The ID which uniquely identifies a study within the given database server
    private String studyDbId;
    //The name or id of the user who uploaded the observation to the database system
    private String uploadedBy;
    //The value of the data collected as an observation
    private Object value;

    public BrapiObservationDTO() {
    }
    
    public BrapiObservationDTO(Dataset dataset) {
        this.studyDbId = dataset.getExperiment();
    }

    public String getGermplasmDbId() {
        return germplasmDbId;
    }

    public void setGermplasmDbId(String germplasmDbId) {
        this.germplasmDbId = germplasmDbId;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public void setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
    }

    public String getObservationDbId() {
        return observationDbId;
    }

    public void setObservationDbId(String observationDbId) {
        this.observationDbId = observationDbId;
    }

    public String getObservationLevel() {
        return observationLevel;
    }

    public void setObservationLevel(String observationLevel) {
        this.observationLevel = observationLevel;
    }

    public Date getObservationTimeStamp() {
        return observationTimeStamp;
    }

    public void setObservationTimeStamp(Date observationTimeStamp) {
        this.observationTimeStamp = observationTimeStamp;
    }

    public String getObservationUnitDbId() {
        return observationUnitDbId;
    }

    public void setObservationUnitDbId(String observationUnitDbId) {
        this.observationUnitDbId = observationUnitDbId;
    }

    public String getObservationUnitName() {
        return observationUnitName;
    }

    public void setObservationUnitName(String observationUnitName) {
        this.observationUnitName = observationUnitName;
    }

    public String getObservationVariableDbId() {
        return observationVariableDbId;
    }

    public void setObservationVariableDbId(String observationVariableDbId) {
        this.observationVariableDbId = observationVariableDbId;
    }

    public String getObservationVariableName() {
        return observationVariableName;
    }

    public void setObservationVariableName(String observationVariableName) {
        this.observationVariableName = observationVariableName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }  
}
    
