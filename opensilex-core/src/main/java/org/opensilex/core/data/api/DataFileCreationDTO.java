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
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
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
public class DataFileCreationDTO{
    
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
    
    private Map metadata;

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

    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata(Map metadata) {
        this.metadata = metadata;
    }
      
    
    public DataFileModel newModel() throws ParseException {
        DataFileModel model = new DataFileModel();
        model.setDate(date);
        model.setMetadata(metadata);
        model.setProvUsed(provenance.getProvUsed());
        model.setProvenanceSettings(provenance.getSettings());
        model.setProvenanceURI(provenance.getUri());
        model.setRdfType(rdfType);
        model.setObject(scientificObjects);
        model.setUri(uri);
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
