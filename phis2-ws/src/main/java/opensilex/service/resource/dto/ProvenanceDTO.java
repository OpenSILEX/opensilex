//******************************************************************************
//                             ProvenanceDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 17 Jan. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.ProvenanceDate;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Provenance;

/**
 * Provenance DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@ProvenanceDate
public class ProvenanceDTO extends AbstractVerifiedClass {
    
    /**
     * Provenance URI
     * @example http://www.phenome-fppn.fr/mtp/2018/pv181515071552)
     */
    private String uri; 
    
    /**
     * Date of the dataset creation
     * @example 2017-06-14
     */
    private String creationDate;
    
    /**
     * The way the dataset was generated (script, script version, acquisition 
     * method).
     */
    private WasGeneratedByDTO wasGeneratedBy;
    
    /**
     * The list of the documents associated with the dataset.
     */
    private ArrayList<String> documentsUris = new ArrayList<>();

    @Override
    public Provenance createObjectFromDTO() {
        Provenance provenance = new Provenance();
        provenance.setUri(uri);
        provenance.setCreationDate(creationDate);
        provenance.setWasGeneratedBy(wasGeneratedBy.createObjectFromDTO());
        provenance.setDocumentsUris(documentsUris);
        
        return provenance;
    }
    
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @Date(DateFormat.YMD)
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_DATE)
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
    
    @Valid
    public WasGeneratedByDTO getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public void setWasGeneratedBy(WasGeneratedByDTO wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    @URL
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
