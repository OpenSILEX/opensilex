//******************************************************************************
//                          ProvenancePostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 March 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.provenance;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.view.model.provenance.Provenance;

/**
 * Provenance POST DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProvenancePostDTO extends AbstractVerifiedClass {
    
    /**
     * The label of the provenance. 
     * @example PROV2019-LEAF
     */
    protected String label;
    
    /**
     * A comment associated to the provenance.
     * @example In this provenance we have count the number of leaf per plant.
     */
    protected String comment;
    
    /**
     * Additional information for the provenance. 
     * Its content depends on the type of the provenance. 
     * @example 
     * [ 
     *   "SensingDevice" => "http://www.opensilex.org/demo/s001",
     *   "Vector" => "http://www.opensilex.org/demo/v001"
     * ]
     */
    protected Object metadata;

    @Override
    public Provenance createObjectFromDTO() {
        Provenance provenance = new Provenance();
        provenance.setLabel(label);
        provenance.setComment(comment);
        provenance.setMetadata(metadata);
        
        return provenance;
    }
    
    @Required
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
