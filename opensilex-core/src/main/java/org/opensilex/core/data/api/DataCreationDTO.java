//******************************************************************************
//                          DataCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.net.URI;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.bson.Document;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
public class DataCreationDTO{
    
    @ValidURI
    protected URI uri;
    
    private List<ProvEntityModel> scientificObjects;
    
    @NotNull
    @ValidURI
    private URI variable;
    
    @NotNull
    private DataProvenanceModel provenance;
    
    @NotNull
    @Date({DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ})
    private String date;
    
    @NotNull
    private Double value;
    
    @Min(0)
    @Max(1)
    private Float confidence = null;
    
    private Document metadata;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public DataModel newModel() throws ParseException {
        DataModel model = new DataModel();

        model.setUri(getUri());        
        model.setScientificObjects(getScientificObjects());
        model.setVariable(getVariable());
        model.setProvenance(getProvenance());       
        model.setValue(getValue());        
        model.setConfidence(getConfidence());
        model.setMetadata(getMetadata());
        
        if(getDate() != null){
            DateFormat[] formats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ};
            ZonedDateTime zdt = null;
            for (DateFormat dateCheckFormat : formats) {
                try { 
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                    zdt = ZonedDateTime.parse(date, dtf);
                    break;
                } catch (DateTimeParseException e) {
                }                    
            }
            model.setDate(zdt);
            model.setTimezone(zdt.getZone().toString());
        }
        
        return model;      
        
    }   
    
}
