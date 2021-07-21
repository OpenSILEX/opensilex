//******************************************************************************
//                          ActivityGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.bson.Document;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.provenance.dal.ActivityModel;

/**
 * Activity ActivityGetDTO
 * @author Alice Boizet
 */
@JsonPropertyOrder({"rdf_type", "uri", "start_date","end_date", "settings"})
public class ActivityGetDTO {  
    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "activity type defined in the ontology", example = "oeso:ImageAcquisition")
    URI rdfType;   

    @ApiModelProperty(value = "external uri of the activity or process")
    URI uri;
    
    @JsonProperty("start_date")
    @ApiModelProperty(value = "start date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE)
    String startDate;
    
    @JsonProperty("end_date")
    @ApiModelProperty(value = "end date or datetime", example = DataAPI.DATA_EXAMPLE_MAXIMAL_DATE)
    String endDate;
    
    @ApiModelProperty(value = "a key-value system to store process parameters")
    Document settings;  

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }

    public ActivityGetDTO fromModel(ActivityModel model) {
        ActivityGetDTO dto = new ActivityGetDTO();
        
        dto.setRdfType(model.getRdfType());
        dto.setUri(model.getUri());
        dto.setSettings(model.getSettings());
        
        
        if (model.getStartDate() != null) {
            dto.setStartDate(model.getStartDate().toString());
        }
        
        if (model.getEndDate() != null) {
            dto.setEndDate(model.getEndDate().toString());
        }
        
        return dto;
    }
    
}
