package org.opensilex.core.data.bll.dataImport;

import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.utils.ImportDataIndex;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceContext {
    Map<String, DeviceModel> variableCheckedDevice = new HashMap<>();
    Map<String, DeviceModel> variableCheckedProvDevice = new HashMap<>();
    List<String> checkedVariables = new ArrayList<>();
    List<String> notExistingDevices = new ArrayList<>();
    List<String> duplicatedDevices = new ArrayList<>();
    Map<String, DeviceModel> nameURIDevices = new HashMap<>();
    HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
    List<ImportDataIndex> duplicateDataByIndex = new ArrayList<>();

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

  
}
