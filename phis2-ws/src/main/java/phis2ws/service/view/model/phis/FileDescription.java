//******************************************************************************
//                                       FileDescription.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 7 mars 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This model represent the description metadata of a data file
 * @author Vincent Migot
 */
public class FileDescription {

    String uri;
    
    String filename;
    
    String path;
        
    String rdfType;
    
    Date date;
    
    List<ConcernedItem> concernedItems;
    
    String provenanceUri;
    
    Map<String, Object> metadata;
    
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<ConcernedItem> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<ConcernedItem> concernedItems) {
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
