//******************************************************************************
//                                       BrapiObservationSummaryDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 2 avr. 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/boizetal/OpenSilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.scientificObject;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;

/**
 * Represents the observations part in the response of the brapi call get ObservationUnits by study
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiObservationSummaryDTO {
    //The name or identifier of the entity which collected the observation
    private String collector;
    //The ID which uniquely identifies an observation
    private String observationDbId; 
    //The date and time  when this observation was made
    private String observationTimeStamp;
    //The ID which uniquely identifies an observation unit = scientific object
    private String observationVariableDbId;
    //A human readable name for an observation variable
    private String observationVariableName;
    //SILEX:todo
    // create a class season with season, seasonDbId, year
    private String season; 
    //\SILEX
    //The value of the data collected as an observation
    private String value;

    public BrapiObservationSummaryDTO() {
    }

    public String getCollector() {
        return collector;
    }

    public void setCollector(String collector) {
        this.collector = collector;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATA_URI)
    public String getObservationDbId() {
        return observationDbId;
    }

    public void setObservationDbId(String observationDbId) {
        this.observationDbId = observationDbId;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATETIME)
    public String getObservationTimeStamp() {
        return observationTimeStamp;
    }

    public void setObservationTimeStamp(String observationTimeStamp) {
        this.observationTimeStamp = observationTimeStamp;
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

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATA_VALUE)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
