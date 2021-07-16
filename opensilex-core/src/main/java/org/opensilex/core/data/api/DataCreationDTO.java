//******************************************************************************
//                          DataCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import static java.lang.Double.NaN;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
@JsonPropertyOrder({"uri", "date","timezone", "target", "variable", "value", "confidence", "provenance",  "metadata"})
public class DataCreationDTO {
    
    public static final String[] NA_VALUES = {"na", "n/a", "NA", "N/A"};
    public static final String[] NAN_VALUES = {"nan", "NaN", "NAN"};
    
    @ValidURI
    @ApiModelProperty(example = DataAPI.DATA_EXAMPLE_URI) 
    protected URI uri;
    
    @Required
    @ApiModelProperty(value = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, required = true)
    private String date;
    
    @ApiModelProperty(value = "target URI on which the data have been collected (e.g. a scientific object)", example = "http://plot01")
    private URI target;
    
    @ApiModelProperty(value = "to specify if the offset is not in the date and if the timezone is different from the default one")
    protected String timezone;
    
    @ValidURI
    @NotNull
    @ApiModelProperty(value = "variable URI", example = DataAPI.DATA_EXAMPLE_VARIABLEURI, required = true)
    private URI variable;   

    @NotNull
    @ApiModelProperty(value = "can be decimal, integer, boolean, string or date", example = DataAPI.DATA_EXAMPLE_VALUE)
    private Object value;
    
    @JsonProperty("raw_data")
    @ApiModelProperty(value = "list of repetition values")
    private List<Object> rawData;
    
    @Min(0)
    @Max(1)
    @ApiModelProperty(value = "confidence index", example = DataAPI.DATA_EXAMPLE_CONFIDENCE)
    private Float confidence = null;    
        
    @NotNull
    private DataProvenanceModel provenance;
    
    @ApiModelProperty(value = "key-value system to store additional information that can be used to query data", example = DataAPI.DATA_EXAMPLE_METADATA)
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

        model.setUri(getUri());        
        model.setTarget(getTarget());
        model.setVariable(getVariable());
        model.setProvenance(getProvenance());       
        
        model.setConfidence(getConfidence());
        model.setMetadata(getMetadata());
        
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(getDate(), getTimezone());
        if (parsedDateTimeMongo == null) {
            throw new UnableToParseDateException(getDate());
        } else {
            model.setDate(parsedDateTimeMongo.getInstant());
            model.setOffset(parsedDateTimeMongo.getOffset());
            model.setIsDateTime(parsedDateTimeMongo.getIsDateTime());
        }
        
        if (getValue() instanceof String) {
            if (Arrays.asList(NA_VALUES).contains(getValue())) {
                model.setValue(null);
            } else if (Arrays.asList(NAN_VALUES).contains(getValue())) {
                model.setValue(NaN);
            } else {
                model.setValue(getValue());
            }            
        } else {
            model.setValue(getValue());
        }       
        
        if (getRawData() != null) {
            model.setRawData(getRawData());
        }

        return model;      
        
    }   
    
}
