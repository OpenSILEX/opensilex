//******************************************************************************
//                          DataCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import static java.lang.Double.NaN;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.bson.Document;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.server.rest.serialization.uri.UriJsonDeserializer;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * @author sammy
 */
@JsonPropertyOrder({"uri", "date", "timezone", "target", "variable", "value", "confidence", "provenance", "metadata"})
public class DataCreationDTO {

    public static final Collection<String> NA_VALUES = Set.of(
            "na", "n/a", "NA", "N/A"
    );
    public static final Collection<String> NAN_VALUES = Set.of("nan", "NaN", "NAN");

    @ValidURI
    @Schema(example = DataAPI.DATA_EXAMPLE_URI)
    protected URI uri;

    @Required
    @Schema(description = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, requiredMode = Schema.RequiredMode.REQUIRED)
    private String date;

    @Schema(description = "target URI on which the data have been collected (e.g. a scientific object)", example = "http://plot01")
    @JsonDeserialize(using = UriJsonDeserializer.class)
    private URI target;

    @Schema(description = "to specify if the offset is not in the date and if the timezone is different from the default one")
    protected String timezone;

    @ValidURI
    @NotNull
    @Schema(description = "variable URI", example = DataAPI.DATA_EXAMPLE_VARIABLEURI, requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonDeserialize(using = UriJsonDeserializer.class)
    private URI variable;

    @NotNull
    @Schema(description = "can be decimal, integer, boolean, string or date", example = DataAPI.DATA_EXAMPLE_VALUE)
    private Object value;

    @JsonProperty("raw_data")
    @Schema(description = "list of repetition values")
    private List<Object> rawData;

    @Min(0)
    @Max(1)
    @Schema(description = "confidence index", example = DataAPI.DATA_EXAMPLE_CONFIDENCE)
    private Float confidence = null;

    @Valid
    @NotNull
    private DataProvenanceModel provenance;

    @Schema(description = "key-value system to store additional information that can be used to query data", example = DataAPI.DATA_EXAMPLE_METADATA)
    private Document metadata;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getTarget() {
        return target;
    }

    public void setTarget(URI target) {
        this.target = target;
    }

    public URI getVariable() {
        return variable;
    }

    public void setVariable(URI variable) {
        this.variable = variable;
    }

    public DataProvenanceModel getProvenance() {
        return provenance;
    }

    public void setProvenance(DataProvenanceModel provenance) {
        this.provenance = provenance;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<Object> getRawData() {
        return rawData;
    }

    public void setRawData(List<Object> rawData) {
        this.rawData = rawData;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public Document getMetadata() {
        return metadata;
    }

    public void setMetadata(Document metadata) {
        this.metadata = metadata;
    }

    public DataModel newModel() throws UnableToParseDateException, TimezoneAmbiguityException, TimezoneException {
        DataModel model = new DataModel();

        model.setUri(uri);
        model.setTarget(target);
        model.setVariable(variable);
        model.setProvenance(provenance);

        model.setConfidence(confidence);
        model.setMetadata(metadata);

        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(date, timezone);
        if (parsedDateTimeMongo == null) {
            throw new UnableToParseDateException(date);
        } else {
            model.setDate(parsedDateTimeMongo.getInstant());
            model.setOffset(parsedDateTimeMongo.getOffset());
            model.setIsDateTime(parsedDateTimeMongo.getIsDateTime());
        }

        model.setValue(value);

        if (rawData != null) {
            model.setRawData(rawData);
        }

        return model;
    }
}