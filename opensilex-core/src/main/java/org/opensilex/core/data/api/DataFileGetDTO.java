//******************************************************************************
//                          DataFileGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;

/**
 *
 * @author Alice Boizet
 */
public class DataFileGetDTO {

    private URI uri;
       
    private URI rdfType;       
    
    private List<ProvEntityModel> scientificObjects;
    
    private DataProvenanceModel provenance;
    
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
    
    public static DataFileGetDTO fromModel(DataFileModel model){
        DataFileGetDTO dto = new DataFileGetDTO();        
        dto.setUri(model.getUri());
        dto.setRdfType(model.getRdfType());
        dto.setScientificObjects(model.getScientificObjects());
        dto.setDate(model.getDate().toString());        
        dto.setMetadata(model.getMetadata());   
        dto.setProvenance(model.getProvenance());
        
        return dto;
    }
}
