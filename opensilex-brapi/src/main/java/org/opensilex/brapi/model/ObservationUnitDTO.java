//******************************************************************************
//                          ObservationUnitDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeDTO;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class ObservationUnitDTO {
    private String blockNumber;         
    private String entryNumber;
    private String entryType;
    private String germplasmDbId;
    private String germplasmName;
    private String observationLevel;
    private String observationLevels;
    private String observationUnitDbId;
    private List<ObservationUnitXref> observationUnitXref;
    private List<ObservationSummary> observations;
    private String plantNumber;
    private String plotNumber;
    private String positionCoordinateX;
    private String positionCoordinateXType; //LONGITUDE, LATITUDE, PLANTED_ROW, PLANTED_INDIVIDUAl, GRID_ROW, GRID_COL, MEASURED_ROW, MEASURED_COL          
    private String positionCoordinateY;
    private String positionCoordinateYType;          
    private String programName;
    private String replicate;
    private String studyDbId;
    private String studyLocation;
    private String studyLocationDbId;
    private String studyName;
    private List<ObservationTreatment> treatments;

    public String getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(String blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getEntryNumber() {
        return entryNumber;
    }

    public void setEntryNumber(String entryNumber) {
        this.entryNumber = entryNumber;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
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

    public String getObservationLevel() {
        return observationLevel;
    }

    public void setObservationLevel(String observationLevel) {
        this.observationLevel = observationLevel;
    }

    public String getObservationLevels() {
        return observationLevels;
    }

    public void setObservationLevels(String observationLevels) {
        this.observationLevels = observationLevels;
    }

    public String getObservationUnitDbId() {
        return observationUnitDbId;
    }

    public void setObservationUnitDbId(String observationUnitDbId) {
        this.observationUnitDbId = observationUnitDbId;
    }

    public List<ObservationUnitXref> getObservationUnitXref() {
        return observationUnitXref;
    }

    public void setObservationUnitXref(List<ObservationUnitXref> observationUnitXref) {
        this.observationUnitXref = observationUnitXref;
    }

    public List<ObservationSummary> getObservations() {
        return observations;
    }

    public void setObservations(List<ObservationSummary> observations) {
        this.observations = observations;
    }

    public String getPlantNumber() {
        return plantNumber;
    }

    public void setPlantNumber(String plantNumber) {
        this.plantNumber = plantNumber;
    }

    public String getPlotNumber() {
        return plotNumber;
    }

    public void setPlotNumber(String plotNumber) {
        this.plotNumber = plotNumber;
    }

    public String getPositionCoordinateX() {
        return positionCoordinateX;
    }

    public void setPositionCoordinateX(String positionCoordinateX) {
        this.positionCoordinateX = positionCoordinateX;
    }

    public String getPositionCoordinateXType() {
        return positionCoordinateXType;
    }

    public void setPositionCoordinateXType(String positionCoordinateXType) {
        this.positionCoordinateXType = positionCoordinateXType;
    }

    public String getPositionCoordinateY() {
        return positionCoordinateY;
    }

    public void setPositionCoordinateY(String positionCoordinateY) {
        this.positionCoordinateY = positionCoordinateY;
    }

    public String getPositionCoordinateYType() {
        return positionCoordinateYType;
    }

    public void setPositionCoordinateYType(String positionCoordinateYType) {
        this.positionCoordinateYType = positionCoordinateYType;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getReplicate() {
        return replicate;
    }

    public void setReplicate(String replicate) {
        this.replicate = replicate;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getStudyLocation() {
        return studyLocation;
    }

    public void setStudyLocation(String studyLocation) {
        this.studyLocation = studyLocation;
    }

    public String getStudyLocationDbId() {
        return studyLocationDbId;
    }

    public void setStudyLocationDbId(String studyLocationDbId) {
        this.studyLocationDbId = studyLocationDbId;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public List<ObservationTreatment> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<ObservationTreatment> treatments) {
        this.treatments = treatments;
    }
    
    public static ObservationUnitDTO fromModel(ScientificObjectNodeDTO model, List<FactorLevelModel> factorLevels) {
        ObservationUnitDTO observationUnit = new ObservationUnitDTO();
        if (model.getUri() != null) {
            observationUnit.setObservationUnitDbId(model.getUri().toString());
        }
        List<ObservationTreatment> treatments = new ArrayList();
        for (FactorLevelModel level:factorLevels) {
            ObservationTreatment treatment = new ObservationTreatment();
            treatment.setFactor(level.getFactor().getName());
            treatment.setModality(level.getName());
            treatments.add(treatment);
        }
        observationUnit.setTreatments(treatments);
        observationUnit.setObservationLevel(model.getTypeLabel().toString());
        
        return observationUnit;
    }
}
