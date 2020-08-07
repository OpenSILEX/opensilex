/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.entity;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.EntityModel;


/**
 *
 * @author vidalmor
 */
public class EntityGetDTO extends SKOSReferencesDTO {

    private URI uri;

    private String name;

    private String comment;

    private URI type;

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "Plant")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "The entity which describe a plant")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Entity")
    public URI getType() { return type; }

    public void setType(URI type) { this.type = type; }

    public static EntityGetDTO fromModel(EntityModel model) {

        EntityGetDTO dto = new EntityGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setType(model.getType());
        dto.setComment(model.getComment());
        dto.setSkosReferencesFromModel(model);

        return dto;
    }
}
