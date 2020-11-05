//******************************************************************************
//                          DataFileCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.bson.Document;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Alice Boizet
 */
public class DataFileCreationDTO {
    
    @ValidURI
    private URI uri;
       
    @ValidURI
    @NotNull
    private URI rdfType; 
    
    private List<ProvEntityModel> scientificObjects;
    
    @NotNull
    private DataProvenanceModel provenance;
    
    @NotNull
    @Date({DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ})
    private String date;
    
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

    public List<ProvEntityModel> getScientificObjects() {
        return scientificObjects;
    }

    public void setScientificObjects(List<ProvEntityModel> scientificObjects) {
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

    public Document getMetadata() {
        return metadata;
    }

    public void setMetadata(Document metadata) {
        this.metadata = metadata;
    }
      
    
    public DataFileModel newModel() throws ParseException {
        DataFileModel model = new DataFileModel();
        model.setMetadata(metadata);
        model.setProvenance(provenance);
        model.setRdfType(rdfType);
        model.setScientificObjects(scientificObjects);
        model.setUri(uri);
        
        if(date != null){
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
    
    /**
     * Method to unserialize DataFileCreationDTO.
     * Required because this data is received as @FormDataParam.
     * @param param
     * @return
     * @throws IOException 
     */
    public static DataFileCreationDTO fromString(String param) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(param, DataFileCreationDTO.class);
    }
}
