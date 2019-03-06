//******************************************************************************
//                                       ProvenanceDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 6 mars 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.provenance;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.provenance.Provenance;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProvenanceDTO extends AbstractVerifiedClass {
    
    //The uri of the provenance.
    //@example http://www.opensilex.org/id/provenance/02465464
    protected String uri;
    //The label of the provenance. 
    //@example PROV2019-LEAF
    protected String label;
    //A comment associated to the provenance
    //@example In this provenance we have count the number of leaf per plant.
    protected String comment;
    //Additional informations for the provenance. Its containt depends of the type of provenance. 
    //@example [ "SensingDevice" => "http://www.opensilex.org/demo/s001",
    //           "Vector" => "http://www.opensilex.org/demo/v001"]
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

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_METADATA_FILTER)
    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}
