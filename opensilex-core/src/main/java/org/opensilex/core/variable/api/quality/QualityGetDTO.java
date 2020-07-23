/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.quality;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.QualityModel;


/**
 *
 * @author vidalmor
 */
public class QualityGetDTO extends SKOSReferencesDTO {

    private URI uri;

    private String label;

    private String comment;

    private URI type;

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/quality/Height")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "Height")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty(example = "Describe the height of a an entity")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Quality")
    public URI getType() { return type; }

    public void setType(URI type) { this.type = type; }

    public static QualityGetDTO fromModel(QualityModel model) {
        QualityGetDTO dto = new QualityGetDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getName());
        dto.setType(model.getType());
        dto.setComment(model.getComment());
        dto.setSkosReferencesFromModel(model);
        return dto;
    }
}
