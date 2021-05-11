//******************************************************************************
//                          DataModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

/**
 *
 * @author sammy
 */
public class DataModel extends MongoModel {

    private URI scientificObject;
    
    private URI variable;
    
    private DataProvenanceModel provenance;

    private Instant date;
    
    private Boolean isDateTime;
    
    private String offset;

    private Object value;
    
    private List<Object> rawData;
    
    private Float confidence = null;

    private Document metadata;  

    public URI getScientificObject() {
        return scientificObject;
    }

    public void setScientificObject(URI scientificObject) {
        this.scientificObject = scientificObject;
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

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public Boolean getIsDateTime() {
        return isDateTime;
    }

    public void setIsDateTime(Boolean isDateTime) {
        this.isDateTime = isDateTime;
    }
    
    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
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
           
    @Override
    public String[] getUriSegments(MongoModel instance) {
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        String provenanceString = "";
        try {
            provenanceString = mapper.writeValueAsString(getProvenance());
        } catch (JsonProcessingException ex) {            
        }
        
        String objectsString = "";
        if (getScientificObject() != null) {
            objectsString = getScientificObject().toString();
        }
        
        String md5Hash = DigestUtils.md5Hex(getVariable().toString() + objectsString + provenanceString);
        
        return new String[]{            
            String.valueOf(getDate().getEpochSecond()),
            md5Hash
        };
    } 

}
