//******************************************************************************
//                          DataModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 *
 * @author sammy
 */
public class DataModel extends MongoModel {

    private List<URI> scientificObjects;
    
    private URI variable;
    
    private DataProvenanceModel provenance;

    private LocalDateTime date;
    
    private String timezone;

    private Object value;
    
    private Float confidence = null;

    private Document metadata;  

    public List<URI> getScientificObjects() {
        return scientificObjects;
    }

    public void setScientificObjects(List<URI> scientificObjects) {
        this.scientificObjects = scientificObjects;
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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
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
        return new String[]{
            Timestamp.from(ZonedDateTime.now().toInstant()).toString(),
            Timestamp.from(getDate().atOffset(ZoneOffset.of(getTimezone())).toInstant()).toString()
        };
    } 

}
