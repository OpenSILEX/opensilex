package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.utils.OrderBy;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.net.URI;
import java.util.List;

@JsonPropertyOrder({"start_date", "end_date","timezone", "experiments", "objects", "variables", "devices", "provenances", "min_confidence", "max_confidence","metadata","mode","with_raw_data","order_by"})
public class DataSearchDTO {

    @ApiModelProperty(value = "start date" ,example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE)
    @JsonProperty("start_date")
    protected String startDate;

    @ApiModelProperty(value = "end date", example = DataAPI.DATA_EXAMPLE_MAXIMAL_DATE)
    @JsonProperty("end_date")
    protected String endDate;

    @ApiModelProperty(value = "to specify if the offset is not in the date and if the timezone is different from the default one",example = DataAPI.DATA_EXAMPLE_TIMEZONE)
    protected String timezone;

    @ValidURI
    @JsonProperty("experiments")
    protected List<URI> experiments;

    @ValidURI
    @JsonProperty("targets")
    protected List<URI> objects;

    @ValidURI
    @JsonProperty("variables")
    protected List<URI> variables;

    @ValidURI
    @JsonProperty("devices")
    protected List<URI> devices;

    @Min(0)
    @Max(1)
    @ApiModelProperty(value = "confidence index", example = DataAPI.DATA_EXAMPLE_CONFIDENCE)
    @JsonProperty("min_confidence")
    protected Float confidenceMin;

    @Min(0)
    @Max(1)
    @ApiModelProperty(value = "confidence index", example = DataAPI.DATA_EXAMPLE_CONFIDENCE)
    @JsonProperty("max_confidence")
    protected Float confidenceMax;

    @ValidURI
    @JsonProperty("provenances")
    protected List<URI> provenances;

    @ApiModelProperty(value = "key-value system to store additional information that can be used to query data", example = DataAPI.DATA_EXAMPLE_METADATA)
    protected String metadata;
    @Required
    @ApiModelProperty(value = "format wide or long", example = "wide")
    @JsonProperty("mode")
    protected String csvFormat;

    @JsonProperty("with_raw_data")
    @ApiModelProperty(value = "export also raw_data", example = "false")
    protected boolean withRawData;

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<URI> experiments) {
        this.experiments = experiments;
    }

    public List<URI> getObjects() {
        return objects;
    }

    public void setObjects(List<URI> objects) {
        this.objects = objects;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public void setVariables(List<URI> variables) {
        this.variables = variables;
    }

    public List<URI> getDevices() {
        return devices;
    }

    public void setDevices(List<URI> devices) {
        this.devices = devices;
    }

    public Float getConfidenceMin() {
        return confidenceMin;
    }

    public void setConfidenceMin(Float confidenceMin) {
        this.confidenceMin = confidenceMin;
    }

    public Float getConfidenceMax() {
        return confidenceMax;
    }

    public void setConfidenceMax(Float confidenceMax) {
        this.confidenceMax = confidenceMax;
    }

    public List<URI> getProvenances() {
        return provenances;
    }

    public void setProvenances(List<URI> provenances) {
        this.provenances = provenances;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getCsvFormat() {
        return csvFormat;
    }

    public void setCsvFormat(String csvFormat) {
        this.csvFormat = csvFormat;
    }

    public boolean isWithRawData() {
        return withRawData;
    }

    public void setWithRawData(boolean withRawData) {
        this.withRawData = withRawData;
    }
}
