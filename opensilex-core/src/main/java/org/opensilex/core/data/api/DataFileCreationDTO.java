//******************************************************************************
//                          DataFileCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.bson.Document;
import org.opensilex.core.data.dal.DataFileModel;
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
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "rdf_type", "date","timezone", "scientific_objects", "provenance",  "metadata"})
public class DataFileCreationDTO {
    
    @ValidURI
    protected URI uri;
       
    @ValidURI
    @NotNull
    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "file type", example = DataFilesAPI.DATAFILE_EXAMPLE_TYPE)
    private URI rdfType; 
    
    @Required
    @ApiModelProperty(value = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE)
    private String date;
    
    @JsonProperty("scientific_objects")
    @ApiModelProperty(value = "scientific objects URIs on which the data have been collected")
    private List<URI> scientificObjects;
    
    @NotNull
    private DataProvenanceModel provenance;
    
    @ApiModelProperty(value = "to specify if the offset is not in the date and if the timezone is different from the default one", example = DataAPI.DATA_EXAMPLE_TIMEZONE)
    private String timezone;

    @ApiModelProperty(value = "key-value system to store additional information that can be used to query data", example = DataAPI.DATA_EXAMPLE_METADATA)
    private Document metadata;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public List<URI> getScientificObjects() {
        return scientificObjects;
    }

    public void setScientificObjects(List<URI> scientificObjects) {
        this.scientificObjects = scientificObjects;
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
    
    public Document getMetadata() {
        return metadata;
    }

    public void setMetadata(Document metadata) {
        this.metadata = metadata;
    }
      
    
    public DataFileModel newModel() throws UnableToParseDateException, TimezoneAmbiguityException, TimezoneException {
        DataFileModel model = new DataFileModel();
        model.setMetadata(metadata);
        model.setProvenance(provenance);
        model.setRdfType(rdfType);
        model.setScientificObjects(scientificObjects);
        model.setUri(uri);
        
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(getDate(), getTimezone());
        model.setDate(parsedDateTimeMongo.getInstant());
        model.setOffset(parsedDateTimeMongo.getOffset());
        model.setIsDateTime(parsedDateTimeMongo.getIsDateTime());
        
        return model;
    }
    
    /**
     * Method to unserialize DataFileCreationDTO.
     * Required because this data is received as @FormDataParam.
     * @param param
     * @return
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    public static DataFileCreationDTO fromString(String param) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(param, DataFileCreationDTO.class);
    }
}
