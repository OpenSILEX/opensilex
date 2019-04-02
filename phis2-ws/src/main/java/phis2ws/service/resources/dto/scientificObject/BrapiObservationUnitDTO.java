//******************************************************************************
//                                       BrapiObservationUnitDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 2 avr. 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/boizetal/OpenSilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.scientificObject;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import phis2ws.service.documentation.DocumentationAnnotation;

/**
 * Represents the response of the brapi call get Observations by study
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiObservationUnitDTO {
    //The block number for an observation unit. Different systems may use different block designs
    private String blockNumber;
    //The entry number for an observation unit. Different systems may use different entry systems
    private String entryNumber;
    //The type of entry for this observation unit. ex. \"check\", \"test\", \"filler\"
    private String entryType;
    //The ID which uniquely identifies a germplasm
    private String germplasmDbId;
    //Name of the germplasm. It can be the prefered name and does not have to be unique
    private String germplasmName;
    //The ID which uniquely identifies a location, associated with this study
    private String locationDbId;
    //The human readable name of a location associated with this study
    private String locationName;
    //The level of an observation unit. ex. \"plot\", \"plant\"
    private String observationLevel;
    //Concatenation of the levels of this observationUnit. Used to handle non canonical level structures. Format levelType:levelID,levelType:levelID
    private String observationLevels;
    //The ID which uniquely identifies an observation unit
    private String observationUnitDbId;
    //A human readable name for an observation unit
    private String observationUnitName;
    //A list of external references to this observation unit
    private String observationUnitXref;
    //List of observations associated with this observation unit
    private ArrayList<BrapiObservationSummaryDTO> observations;
    //The string representation of the pedigree of this observation unit
    private String pedigree;
    //The plant number in a field. Applicable for observationLevel: \"plant\"
    private String plantNumber;
    //The plot number in a field. Applicable for observationLevel: \"plot\"
    private String plotNumber;
    //The X position coordinate for an observation unit. Different systems may use different coordinate systems
    private String positionCoordinateX;
    //The type of positional coordinate used. Must be one of the following values\nLONGITUDE - ISO 6709 standard, WGS84 geodetic datum
    private String positionCoordinateXType;
    //The Y position coordinate for an observation unit. Different systems may use different coordinate systems
    private String positionCoordinateY;
    //The type of positional coordinate used. Must be one of the following values\nLONGITUDE - ISO 6709 standard, WGS84 geodetic datum
    private String positionCoordinateYType;
    //The ID which uniquely identifies a program
    private String programDbId;
    //The human readable name of a program
    private String programName;
    //The replicate number of an observation unit. May be the same as blockNumber
    private String replicate;
    //The ID which uniquely identifies a study within the given database server
    private String studyDbId;
    //The human readable name for a study
    private String studyName;
    //List of treatments applied to an observation unit
    //SILEX:todo
    //create a class treatment
    private ArrayList<String> treatments;
    //\SILEX
    //The ID which uniquely identifies a trial
    private String trialDbId;
    //The human readable name of a trial
    private String trialName;

    public BrapiObservationUnitDTO(String observationUnitDbId) {
        this.observationUnitDbId = observationUnitDbId;
    }

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

    public String getObservationLevel() {
        return observationLevel;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_TYPE)
    public void setObservationLevel(String observationLevel) {
        this.observationLevel = observationLevel;
    }

    public String getObservationLevels() {
        return observationLevels;
    }

    public void setObservationLevels(String observationLevels) {
        this.observationLevels = observationLevels;
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

    public String getObservationUnitXref() {
        return observationUnitXref;
    }

    public void setObservationUnitXref(String observationUnitXref) {
        this.observationUnitXref = observationUnitXref;
    }

    public ArrayList<BrapiObservationSummaryDTO> getObservations() {
        return observations;
    }

    public void setObservations(ArrayList<BrapiObservationSummaryDTO> observations) {
        this.observations = observations;
    }

    public String getPedigree() {
        return pedigree;
    }

    public void setPedigree(String pedigree) {
        this.pedigree = pedigree;
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

    public String getReplicate() {
        return replicate;
    }

    public void setReplicate(String replicate) {
        this.replicate = replicate;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI)
    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_ALIAS)
    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public ArrayList<String> getTreatments() {
        return treatments;
    }

    public void setTreatments(ArrayList<String> treatments) {
        this.treatments = treatments;
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
