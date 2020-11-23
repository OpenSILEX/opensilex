//******************************************************************************
//                          DataCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import static java.lang.Double.NaN;
import java.net.URI;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.bson.Document;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
public class DataCreationDTO{
    
    public static final String[] NA_VALUES = {"na", "n/a", "NA", "N/a"};
    public static final String[] NAN_VALUES = {"nan", "NaN", "NAN"};
    
    @ValidURI
    protected URI uri;
    
    private List<URI> scientificObjects;
    
    @NotNull
    @ValidURI
    private URI variable;
    
    @NotNull
    private DataProvenanceModel provenance;
    
    @NotNull
    @Date({DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ})
    private String date;
    
    private Object value;
    
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public DataModel newModel() throws ParseException {
        DataModel model = new DataModel();

        model.setUri(getUri());        
        model.setScientificObjects(getScientificObjects());
        model.setVariable(getVariable());
        model.setProvenance(getProvenance());       
        
        model.setConfidence(getConfidence());
        model.setMetadata(getMetadata());
        
        if(getDate() != null){
            DateFormat[] formats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ};
            LocalDateTime dateTimeUTC = null;
            String offset = null;
            for (DateFormat dateCheckFormat : formats) {
                try { 
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                    OffsetDateTime ost = OffsetDateTime.parse(date, dtf);
                    dateTimeUTC = ost.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
                    offset = ost.getOffset().toString();
                    break;
                } catch (DateTimeParseException e) {
                }                    
            }
            model.setDate(dateTimeUTC);
            model.setTimezone(offset);
        }
        
        if (getValue() instanceof String) {
            if (Arrays.asList(NA_VALUES).contains(getValue())) {
                model.setValue("NA");
            } else if (Arrays.asList(NAN_VALUES).contains(getValue())) {
                model.setValue(NaN);
            } else {
                model.setValue(getValue());
            }            
        } else {
            model.setValue(getValue());
        }
        
        return model;      
        
    }   
    
}
