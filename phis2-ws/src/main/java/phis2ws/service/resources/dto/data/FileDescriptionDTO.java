//******************************************************************************
//                                       FileDescriptionPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 7 mars 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/vincent/opensilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.resources.dto.ConcernItemDTO;
import phis2ws.service.view.model.phis.ConcernedItem;
import phis2ws.service.view.model.phis.FileDescription;

/**
 * This class describe FileDescription Metadata
 * @author vincent
 */
public class FileDescriptionDTO {
    
    String uri;
    
    String rdfType;
    
    String date;
    
    List<ConcernItemDTO> concernedItems;
    
    String provenanceUri;
    
    Map<String, Object> metadata;

    public FileDescriptionDTO(FileDescription description) {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        
        if (description.getDate() != null) {
            setDate(df.format(description.getDate()));
        }
        
        setUri(description.getUri());
        setProvenanceUri(description.getProvenanceUri());
        setRdfType(description.getRdfType());
        
        List<ConcernItemDTO> items = new ArrayList<>();
        for (ConcernedItem item : description.getConcernedItems()) {
            ConcernItemDTO itemDTO = new ConcernItemDTO();
            itemDTO.setTypeURI(item.getRdfType());
            itemDTO.setUri(item.getUri());
            items.add(itemDTO);
        }
        setConcernedItems(items);
        setMetadata(description.getMetadata());
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<ConcernItemDTO> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<ConcernItemDTO> concernedItems) {
        this.concernedItems = concernedItems;
    }

    public String getProvenanceUri() {
        return provenanceUri;
    }

    public void setProvenanceUri(String provenanceUri) {
        this.provenanceUri = provenanceUri;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
