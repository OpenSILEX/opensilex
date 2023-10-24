package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
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

    protected List<OrderBy> orderByList;

    @JsonProperty("with_raw_data")
    @ApiModelProperty(value = "export also raw_data", example = "false")
    protected boolean withRawData;

    public String getStartDate() {
        return startDate;
    }

    public DataSearchDTO setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public DataSearchDTO setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getTimezone() {
        return timezone;
    }

    public DataSearchDTO setTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public DataSearchDTO setExperiments(List<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public List<URI> getObjects() {
        return objects;
    }

    public DataSearchDTO setObjects(List<URI> objects) {
        this.objects = objects;
        return this;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public DataSearchDTO setVariables(List<URI> variables) {
        this.variables = variables;
        return this;
    }

    public List<URI> getDevices() {
        return devices;
    }

    public DataSearchDTO setDevices(List<URI> devices) {
        this.devices = devices;
        return this;
    }

    public Float getConfidenceMin() {
        return confidenceMin;
    }

    public DataSearchDTO setConfidenceMin(Float confidenceMin) {
        this.confidenceMin = confidenceMin;
        return this;
    }

    public Float getConfidenceMax() {
        return confidenceMax;
    }

    public DataSearchDTO setConfidenceMax(Float confidenceMax) {
        this.confidenceMax = confidenceMax;
        return this;
    }

    public List<URI> getProvenances() {
        return provenances;
    }

    public DataSearchDTO setProvenances(List<URI> provenances) {
        this.provenances = provenances;
        return this;
    }

    public String getMetadata() {
        return metadata;
    }

    public DataSearchDTO setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    public String getCsvFormat() {
        return csvFormat;
    }

    public DataSearchDTO setCsvFormat(String csvFormat) {
        this.csvFormat = csvFormat;
        return this;
    }

    public boolean isWithRawData() {
        return withRawData;
    }

    public DataSearchDTO setWithRawData(boolean withRawData) {
        this.withRawData = withRawData;
        return this;
    }

    public List<OrderBy> getOrderByList() {
        return orderByList;
    }

    public DataSearchDTO setOrderByList(List<OrderBy> orderByList) {
        this.orderByList = orderByList;
        return this;
    }
}
