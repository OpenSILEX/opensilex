//******************************************************************************
//                          DataGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.variable.api.VariableGetDTO;

import java.util.List;

/**
 * This class defines series of data associated with one variable.
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "variable", "data_series", "calculated_series"
})
public class DataVariableSeriesGetDTO {

    @JsonProperty("variable")
    private VariableGetDTO variable;

    @JsonProperty("data_series")
    private List<DataSerieGetDTO> dataSeries;

    @JsonProperty("calculated_series")
    private List<DataSerieGetDTO> calculatedSeries;


    public DataVariableSeriesGetDTO(VariableGetDTO variable) {
        this.variable = variable;
    }

    public DataVariableSeriesGetDTO(VariableGetDTO variable, List<DataSerieGetDTO> dataSeries, List<DataSerieGetDTO> calculatedSeries) {
        this.variable = variable;
        this.dataSeries = dataSeries;
        this.calculatedSeries = calculatedSeries;
    }

    public VariableGetDTO getVariable() {
        return variable;
    }

    public void setVariable(VariableGetDTO variable) {
        this.variable = variable;
    }

    public List<DataSerieGetDTO> getDataSeries() {
        return dataSeries;
    }

    public void setDataSeries(List<DataSerieGetDTO> dataSeries) {
        this.dataSeries = dataSeries;
    }

    public List<DataSerieGetDTO> getCalculatedSeries() {
        return calculatedSeries;
    }

    public void setCalculatedSeries(List<DataSerieGetDTO> calculatedSeries) {
        this.calculatedSeries = calculatedSeries;
    }
}
