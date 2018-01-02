//**********************************************************************************************
//                                       ConcernItemDTO.java 
//
// Author(s): Arnaud CHARLEROY, Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: March 2017
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  June, 2017
// Subject: Represents the JSON submitted for the objects concerned by the annotation
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.ConcernItem;

public class ConcernItemDTO extends AbstractVerifiedClass{
    private String uri;
    private String typeURI;

    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("uri", Boolean.TRUE);
        rules.put("typeURI", Boolean.TRUE);
        return rules;
    }

    @Override
    public ConcernItem createObjectFromDTO() {
        ConcernItem concernedItem = new ConcernItem();
        concernedItem.setRdfType(typeURI);
        concernedItem.setUri(uri);
        
        return concernedItem;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DOCUMENT_CONCERNED_TYPE_URI)
    public String getTypeURI() {
        return typeURI;
    }

    public void setTypeURI(String typeURI) {
        this.typeURI = typeURI;
    }
}
