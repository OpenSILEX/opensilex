//******************************************************************************
//                          DataModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author sammy
 */
public class DataModel extends MongoModel {

    private List<ProvEntityModel> scientificObjects;
    
    private URI variable;
    
    private DataProvenanceModel provenance;

    private ZonedDateTime date;
    
    private String timezone;

    private Double value;
    
    private Float confidence = null;

    private Document metadata;  

    public List<ProvEntityModel> getScientificObjects() {
        return scientificObjects;
    }

    public void setScientificObjects(List<ProvEntityModel> scientificObjects) {
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

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
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
        /*URICompose = Arrays.stream(URICompose).filter(Objects::nonNull).toArray(String[]::new);
        return ArrayUtils.addAll(URICompose,new String[]{formatDateURI()});*/
        return new String[]{
            Timestamp.from(ZonedDateTime.now().toInstant()).toString()
            //Timestamp.from(getDate().toInstant()).toString()
        };
    }
    
//    private String formatDateURI(){
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//        String sd = dtf.format(date);
//        return sd;
//    }
    

}
