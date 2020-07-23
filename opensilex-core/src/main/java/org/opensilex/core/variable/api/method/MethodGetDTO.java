/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.method;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.MethodModel;


/**
 *
 * @author vidalmor
 */
public class MethodGetDTO extends SKOSReferencesDTO {

    private URI uri;

    private String label;

    private String comment;

    private URI type;

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/method/ImageAnalysis")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "ImageAnalysis")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty(example = "Based on a software")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Method")
    public URI getType() { return type; }

    public void setType(URI type) { this.type = type; }

    public static MethodGetDTO fromModel(MethodModel model) {
        MethodGetDTO dto = new MethodGetDTO();

        dto.uri = model.getUri();
        dto.label = model.getName();
        dto.comment = model.getComment();
        dto.type = model.getType();

        dto.setSkosReferencesFromModel(model);
        return dto;
    }
}
