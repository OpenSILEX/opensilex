//**********************************************************************************************
//                                       ConcernItemDTO.java 
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: March 2017
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 03 2018
// Subject: Represents the JSON submitted for the objects concerned by the annotation
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.ConcernItem;
import phis2ws.service.resources.validation.interfaces.URL;

/**
 * corresponds to the submitted JSON for the objects concerned (by an annotation for example)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ConcernItemDTO extends AbstractVerifiedClass {
    protected String uri;
    protected String typeUri;

    @Override
    public ConcernItem createObjectFromDTO() {
        ConcernItem concernedItem = new ConcernItem();
        concernedItem.setRdfType(typeUri);
        concernedItem.setUri(uri);
        
        return concernedItem;
    }
    
    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DOCUMENT_CONCERNED_TYPE_URI)
    public String getTypeURI() {
        return typeUri;
    }

    public void setTypeURI(String typeURI) {
        this.typeUri = typeURI;
    }
}
