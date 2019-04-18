//******************************************************************************
//                              ProvenanceDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 6 March 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.provenance;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.view.model.provenance.Provenance;

/**
 * Provenance DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProvenanceDTO extends AbstractVerifiedClass {
    
    /**
     * URI of the provenance.
     * @example http://www.opensilex.org/id/provenance/02465464
     */
    protected String uri;
    
    /**
     * Label of the provenance. 
     * @example PROV2019-LEAF
     */
    protected String label;
    
    /**
     * Comment associated to the provenance.
     * @example In this provenance we have count the number of leaf per plant.
     */
    protected String comment;
    
    /**
     * Additional information for the provenance. 
     * Its content depends on the type of provenance. 
     * @example 
     * [ 
     *   "SensingDevice" => "http://www.opensilex.org/demo/s001",
     *   "Vector" => "http://www.opensilex.org/demo/v001"
     * ]
     */
    protected Object metadata;

    public ProvenanceDTO(Provenance provenance) {
        this.uri = provenance.getUri();
        this.label = provenance.getLabel();
        this.comment = provenance.getComment();
        this.metadata = provenance.getMetadata();
    }
    
    @Override
    public Provenance createObjectFromDTO() {
        Provenance provenance = new Provenance();
        provenance.setUri(uri);
        provenance.setLabel(label);
        provenance.setComment(comment);
        provenance.setMetadata(metadata);
        
        return provenance;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_LABEL)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_COMMENT)
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_METADATA)
    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}
