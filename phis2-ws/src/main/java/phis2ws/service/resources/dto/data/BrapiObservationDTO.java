//******************************************************************************
//                                       BrapiObservationDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 11 févr. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.data;

import io.swagger.annotations.ApiModelProperty;
import phis2ws.service.documentation.DocumentationAnnotation;


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
    private String observationTimeStamp;
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
    private String value;

    public BrapiObservationDTO() {
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
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATA_URI)
    public String getObservationDbId() {
        return observationDbId;
    }

    public void setObservationDbId(String observationDbId) {
        this.observationDbId = observationDbId;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_TYPE)
    public String getObservationLevel() {
        return observationLevel;
    }

    public void setObservationLevel(String observationLevel) {
        this.observationLevel = observationLevel;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATETIME)
    public String getObservationTimeStamp() {
        return observationTimeStamp;
    }

    public void setObservationTimeStamp(String observationTimeStamp) {
        this.observationTimeStamp = observationTimeStamp;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI)
    public String getObservationUnitDbId() {
        return observationUnitDbId;
    }

    public void setObservationUnitDbId(String observationUnitDbId) {
        this.observationUnitDbId = observationUnitDbId;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_ALIAS)
    public String getObservationUnitName() {
        return observationUnitName;
    }

    public void setObservationUnitName(String observationUnitName) {
        this.observationUnitName = observationUnitName;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI)
    public String getObservationVariableDbId() {
        return observationVariableDbId;
    }

    public void setObservationVariableDbId(String observationVariableDbId) {
        this.observationVariableDbId = observationVariableDbId;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VARIABLE_LABEL)
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

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI)
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

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATA_VALUE)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }  
}