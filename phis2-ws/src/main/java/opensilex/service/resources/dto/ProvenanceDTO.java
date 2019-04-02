//******************************************************************************
//                             ProvenanceDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 17 Jan. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resources.dto.manager.AbstractVerifiedClass;
import opensilex.service.resources.validation.interfaces.Date;
import opensilex.service.resources.validation.interfaces.ProvenanceDate;
import opensilex.service.resources.validation.interfaces.URL;
import opensilex.service.model.Provenance;

/**
 * Provenance DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@ProvenanceDate
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
