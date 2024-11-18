package org.opensilex.core.dataV2.model;

import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.utils.ImportDataIndex;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceContext {
    Map<String, DeviceModel> variableCheckedDevice;
    Map<String, DeviceModel> variableCheckedProvDevice;
    List<String> checkedVariables;
    List<String> notExistingDevices;
    List<String> duplicatedDevices;
    Map<String, DeviceModel> nameURIDevices;
    HashMap<URI, URI> mapVariableUriDataType;
    List<ImportDataIndex> duplicateDataByIndex;

    public Map<String, DeviceModel> getVariableCheckedDevice() {
        return variableCheckedDevice;
    }

    public void setVariableCheckedDevice(Map<String, DeviceModel> variableCheckedDevice) {
        this.variableCheckedDevice = variableCheckedDevice;
    }

    public Map<String, DeviceModel> getVariableCheckedProvDevice() {
        return variableCheckedProvDevice;
    }

    public void setVariableCheckedProvDevice(Map<String, DeviceModel> variableCheckedProvDevice) {
        this.variableCheckedProvDevice = variableCheckedProvDevice;
    }

    public List<String> getCheckedVariables() {
        return checkedVariables;
    }

    public void setCheckedVariables(List<String> checkedVariables) {
        this.checkedVariables = checkedVariables;
    }

    public List<String> getNotExistingDevices() {
        return notExistingDevices;
    }

    public void setNotExistingDevices(List<String> notExistingDevices) {
        this.notExistingDevices = notExistingDevices;
    }

    public List<String> getDuplicatedDevices() {
        return duplicatedDevices;
    }

    public void setDuplicatedDevices(List<String> duplicatedDevices) {
        this.duplicatedDevices = duplicatedDevices;
    }

    public Map<String, DeviceModel> getNameURIDevices() {
        return nameURIDevices;
    }

    public void setNameURIDevices(Map<String, DeviceModel> nameURIDevices) {
        this.nameURIDevices = nameURIDevices;
    }

    public HashMap<URI, URI> getMapVariableUriDataType() {
        return mapVariableUriDataType;
    }

    public void setMapVariableUriDataType(HashMap<URI, URI> mapVariableUriDataType) {
        this.mapVariableUriDataType = mapVariableUriDataType;
    }

    public List<ImportDataIndex> getDuplicateDataByIndex() {
        return duplicateDataByIndex;
    }

    public void setDuplicateDataByIndex(List<ImportDataIndex> duplicateDataByIndex) {
        this.duplicateDataByIndex = duplicateDataByIndex;
    }

    public static DeviceContext buildDeviceContext(List<String> duplicatedDevices, List<ImportDataIndex> duplicateDataByIndex, List<String> checkedVariables, Map<String, DeviceModel> nameURIDevices, List<String> notExistingDevices, Map<String, DeviceModel> variableCheckedDevice, Map<String, DeviceModel> variableCheckedProvDevice, HashMap<URI, URI> mapVariableUriDataType) {
        DeviceContext deviceContext = new DeviceContext();
        deviceContext.setDuplicatedDevices(duplicatedDevices);
        deviceContext.setDuplicateDataByIndex(duplicateDataByIndex);
        deviceContext.setCheckedVariables(checkedVariables);
        deviceContext.setNameURIDevices(nameURIDevices);
        deviceContext.setNotExistingDevices(notExistingDevices);
        deviceContext.setVariableCheckedDevice(variableCheckedDevice);
        deviceContext.setVariableCheckedProvDevice(variableCheckedProvDevice);
        deviceContext.setMapVariableUriDataType(mapVariableUriDataType);
        return deviceContext;
    }
}
