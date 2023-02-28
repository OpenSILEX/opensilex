/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.device.api.DeviceGetDTO;

import javax.xml.crypto.Data;
import java.util.List;


/**
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "variable", "devices", "data"
})
public class VariableWithDevicesDTO {

    @JsonProperty("variable")
    private VariableGetDTO variable;

    @JsonProperty("devices")
    private List<DeviceGetDTO> devices;

    @JsonProperty("data")
    private List<DataGetDTO> data;


    public VariableGetDTO getVariable() {
        return this.variable;
    }

    public void setVariable(VariableGetDTO variable) {
        this.variable = variable;
    }

    public List<DeviceGetDTO> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceGetDTO> devices) {
        this.devices = devices;
    }

    public List<DataGetDTO> getData() {
        return data;
    }

    public void setData(List<DataGetDTO> data) {
        this.data = data;
    }

    public VariableWithDevicesDTO(VariableGetDTO variable, List<DeviceGetDTO> devices, List<DataGetDTO> data) {
        this.setVariable(variable);
        this.setDevices(devices);
        this.setData(data);
    }

}
