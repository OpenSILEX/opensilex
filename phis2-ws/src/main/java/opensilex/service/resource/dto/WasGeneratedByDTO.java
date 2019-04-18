//******************************************************************************
//                             WasGeneratedByDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 17 Jan. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.WasGeneratedBy;

/**
 * Generator DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class WasGeneratedByDTO extends AbstractVerifiedClass {
    
    /**
     * The data generator URI.
     * @example a script that calculated data 
     * http://www.phenome-fppn.fr/phis_field/documents/documente597f57ba71d421a86277d830f4b9885
     */
    private String wasGeneratedByDocument;
    
    /**
     * Description of how the dataset was generated.
     * @example Phenoscript v1.3
     */
    private String wasGeneratedByDescription;

    @Override
    public WasGeneratedBy createObjectFromDTO() {
        WasGeneratedBy wasGeneratedBy = new WasGeneratedBy();
        wasGeneratedBy.setWasGeneratedBy(wasGeneratedByDocument);
        wasGeneratedBy.setWasGeneratedByDescription(wasGeneratedByDescription);
        return wasGeneratedBy;
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_WAS_GENERATED_BY_DOCUMENT)
    public String getWasGeneratedByDocument() {
        return wasGeneratedByDocument;
    }

    public void setWasGeneratedByDocument(String wasGeneratedByDocument) {
        this.wasGeneratedByDocument = wasGeneratedByDocument;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_WAS_GENERATED_BY_DESCRIPTION)
    public String getWasGeneratedByDescription() {
        return wasGeneratedByDescription;
    }

    public void setWasGeneratedByDescription(String wasGeneratedByDescription) {
        this.wasGeneratedByDescription = wasGeneratedByDescription;
    }
}
