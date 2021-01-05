//******************************************************************************
//                          AnnotationGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.sparql.response.ResourceDTO;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@ApiModel
public class AnnotationGetDTO {

    protected URI uri;
    protected String bodyValue;
    protected List<URI> targets;
    protected MotivationGetDTO motivation;
    protected URI creator;
    protected LocalDate created;

    public AnnotationGetDTO() {
    }

    public AnnotationGetDTO(AnnotationModel model) {
        uri = model.getUri();

        bodyValue = model.getBodyValue();
        targets = model.getTargets();
        created = model.getCreated();
        creator = model.getCreator();

        MotivationModel motivationModel = model.getMotivation();
        motivation = new MotivationGetDTO(motivationModel);
    }

    @ApiModelProperty(value = "Annotation URI", example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Content of the annotation", example = "The pest attack lasted 20 minutes")
    public String getBodyValue() {
        return bodyValue;
    }

    public void setBodyValue(String bodyValue) {
        this.bodyValue = bodyValue;
    }


    @ApiModelProperty(value = "Target URI(s) of the annotation")
    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }

    @ApiModelProperty(value = "Creation date" ,example = "2020-02-20")
    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    @ApiModelProperty(value = "Annotation creator URI" ,example = "http://opensilex.dev/users#Admin.OpenSilex")
    public URI getCreator() {
        return creator;
    }

    public void setCreator(URI creator) {
        this.creator = creator;
    }


    public MotivationGetDTO getMotivation() {
        return motivation;
    }

    public void setMotivation(MotivationGetDTO motivation) {
        this.motivation = motivation;
    }
}
