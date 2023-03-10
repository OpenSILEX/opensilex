//******************************************************************************
//                          MetricPeriodDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.metrics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;


/**
 * @author Bernhard Gschloessl
 */

@JsonPropertyOrder({"start_date", "end_date", "scientific_object_list", "experiment_list", "data_list", "device_list", "germplasm_list"})

public class MetricPeriodDTO {
    @JsonProperty("start_date")
    private Instant startDate;

    @JsonProperty("end_date")
    private Instant endDate;

    @JsonProperty("scientific_object_list")
    private CountListItemPeriodDTO scientificObjectList;

    @JsonProperty("experiment_list")
    private CountListItemPeriodDTO experimentList;

    @JsonProperty("data_list")
    private CountListItemPeriodDTO dataList;

    @JsonProperty("device_list")
    private CountListItemPeriodDTO deviceList;

    @JsonProperty("germplasm_list")
    private CountListItemPeriodDTO germplasmList;

    public CountListItemPeriodDTO getScientificObjectList() {
        return scientificObjectList;
    }

    public void setScientificObjectList(CountListItemPeriodDTO scientificObjectList) {
        this.scientificObjectList = scientificObjectList;
    }

    public CountListItemPeriodDTO getExperimentList() {
        return experimentList;
    }

    public void setExperimentList(CountListItemPeriodDTO experimentList) {
        this.experimentList = experimentList;
    }

    public CountListItemPeriodDTO getDataList() {
        return dataList;
    }

    public void setDataList(CountListItemPeriodDTO dataList) {
        this.dataList = dataList;
    }

    public CountListItemPeriodDTO getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(CountListItemPeriodDTO deviceList) {
        this.deviceList = deviceList;
    }

    public CountListItemPeriodDTO getGermplasmList() {
        return germplasmList;
    }

    public void setGermplasmList(CountListItemPeriodDTO germplasmList) {
        this.germplasmList = germplasmList;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
}
