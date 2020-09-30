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

    private String name;

    private String comment;

    private URI type;

    private String typeLabel;

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/quality/Height")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "Height")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getTypeLabel() {
        return typeLabel;
    }

    @ApiModelProperty(example = "Quality")
    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public static QualityGetDTO fromModel(QualityModel model) {
        QualityGetDTO dto = new QualityGetDTO();

        dto.uri = model.getUri();
        dto.name = model.getName();
        dto.typeLabel = model.getTypeLabel().getDefaultValue();
        dto.type = model.getType();
        dto.comment = model.getComment();

        dto.setSkosReferencesFromModel(model);
        return dto;
    }
}
