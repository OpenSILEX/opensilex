//******************************************************************************
//                                       ProvenanceDTO.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 17 janv. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  17 janv. 2018
// Subject: Represents the submitted JSON for the data
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Provenance;

/**
 * Represents the submitted JSON for the Provenance data
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProvenanceDTO extends AbstractVerifiedClass {
    
    //Provenance uri (e.g. http://www.phenome-fppn.fr/mtp/2018/pv181515071552)
    private String uri; 
    //date of the dataset creation (e.g. 2017-06-14)
    private String creationDate;
    //the way the dataset was generated (script, script version, 
    //acquisition method)
    private WasGeneratedByDTO wasGeneratedBy;
    //The liste of the documents associated to the dataset
    private ArrayList<String> documentsUris = new ArrayList<>();

    //SILEX:test
    // Need to be tested
    //\SILEX:test
//    @Override
//    public Map rules() {
//        Map<String, Boolean> rules = new HashMap<>();
//        rules.put("uri", Boolean.TRUE);
//        if (uri == null) {
//            rules.put("creationDate", Boolean.TRUE);
//        } else {
//            rules.put("creationDate", Boolean.FALSE);
//        }
//        rules.put("documentsUris", Boolean.FALSE);
//        return rules;
//    }

    @Override
    public Provenance createObjectFromDTO() {
        Provenance provenance = new Provenance();
        provenance.setUri(uri);
        provenance.setCreationDate(creationDate);
        provenance.setWasGeneratedBy(wasGeneratedBy.createObjectFromDTO());
        provenance.setDocumentsUris(documentsUris);
        
        return provenance;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_DATE)
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public WasGeneratedByDTO getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public void setWasGeneratedBy(WasGeneratedByDTO wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public ArrayList<String> getDocumentsUris() {
        return documentsUris;
    }

    public void setDocumentsUris(ArrayList<String> documentsUris) {
        this.documentsUris = documentsUris;
    }
    
    public void addDocument(String documentUri) {
        documentsUris.add(documentUri);
    }
}
