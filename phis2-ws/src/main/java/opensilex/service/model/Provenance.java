//******************************************************************************
//                              Provenance.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Provenance model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Provenance {
    private String uri;
    private String creationDate;
    private WasGeneratedBy wasGeneratedBy;
    private ArrayList<String> documentsUris = new ArrayList<>();
    
    public Provenance() {
        
    }
    
    public Provenance(Provenance prov) {
        uri = prov.getUri();
        creationDate = prov.getCreationDate();
        wasGeneratedBy = prov.getWasGeneratedBy();
        
        if (prov.getDocumentsUris() != null) {
            prov.getDocumentsUris().forEach((docUri) -> {
                documentsUris.add(docUri);
            });
        }
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public WasGeneratedBy getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public void setWasGeneratedBy(WasGeneratedBy wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public ArrayList<String> getDocumentsUris() {
        return documentsUris;
    }

    public void setDocumentsUris(ArrayList<String> documentsUris) {
        this.documentsUris = documentsUris;
    }
    
    public void addDocumentUri(String documentUri) {
        this.documentsUris.add(uri);
    }
}
