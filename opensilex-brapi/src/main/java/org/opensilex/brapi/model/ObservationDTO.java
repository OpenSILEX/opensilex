//******************************************************************************
//                          ObservationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.data.dal.DataModel;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class ObservationDTO {
    private String germplasmDbId;
    private String germplasmName;
    private String observationDbId; 
    private String observationLevel;
    private String observationTimeStamp;
    private String observationUnitDbId;
    private String observationUnitName;
    private String observationVariableDbId;
    private String observationVariableName;
    private String operator;
    private Season season; 
    private String studyDbId;
    private String uploadedBy;
    private String value;

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

    public String getObservationTimeStamp() {
        return observationTimeStamp;
    }

    public void setObservationTimeStamp(String observationTimeStamp) {
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

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public static ObservationDTO fromModel(DataModel model) {
        ObservationDTO observation = new ObservationDTO();
        if (model.getUri() != null) {
            observation.setObservationDbId(model.getUri().toString());
        }
        
        if (model.getDate() != null) {
            observation.setObservationTimeStamp(model.getDate().toString());
        }
        
        if (model.getTarget() != null) {
            observation.setObservationUnitDbId(model.getTarget().toString());
        }        
        
        if (model.getVariable() != null) {
            observation.setObservationVariableDbId(model.getVariable().toString());
        }
        
        if (model.getValue() != null) {
            observation.setValue(model.getValue().toString());
        }
        
        return observation;
    }
}
