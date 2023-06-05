//******************************************************************************
//                          DataGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.device.api.DeviceGetDTO;
import org.opensilex.core.variable.api.VariableDetailsDTO;
import org.opensilex.server.rest.validation.Required;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * This class defines series of data associated with one variable.
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "variable", "provenances", "devices", "data_series", "calculated_series", "last_data_stored"
})
public class DataVariableSeriesGetDTO {

    @Valid
    @Required
    @JsonProperty("variable")
    private VariableDetailsDTO variable;

    @Valid
    @JsonProperty("provenances")
    private List<DataSimpleProvenanceGetDTO> provenances;

    @Valid
    @JsonProperty("devices")
    private List<DeviceGetDTO> devices;

    @Valid
    @JsonProperty("data_series")
    private List<DataSerieGetDTO> dataSeries;

    @Valid
    @JsonProperty("calculated_series")
    private List<DataSerieGetDTO> calculatedSeries;

    @JsonProperty("last_data_stored")
    private DataComputedGetDTO lastData;


    public DataVariableSeriesGetDTO(VariableDetailsDTO variable) {
        this.variable = variable;
        this.dataSeries = new ArrayList<>();
        this.calculatedSeries = new ArrayList<>();
    }

    public DataVariableSeriesGetDTO(VariableDetailsDTO variable, List<DataSerieGetDTO> dataSeries, List<DataSerieGetDTO> calculatedSeries) {
        this.variable = variable;
        this.dataSeries = dataSeries;
        this.calculatedSeries = calculatedSeries;
    }

    public VariableDetailsDTO getVariable() {
        return variable;
    }

    public void setVariable(VariableDetailsDTO variable) {
        this.variable = variable;
    }

    public List<DataSimpleProvenanceGetDTO> getProvenances() {
        return provenances;
    }

    public void setProvenances(List<DataSimpleProvenanceGetDTO> provenances) {
        this.provenances = provenances;
    }

    public List<DeviceGetDTO> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceGetDTO> devices) {
        this.devices = devices;
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

    public DataComputedGetDTO getLastData() {
        return lastData;
    }

    public void setLastData(DataComputedGetDTO lastData) {
        this.lastData = lastData;
    }
}
