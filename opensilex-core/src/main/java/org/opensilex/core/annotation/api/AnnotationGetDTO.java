//******************************************************************************
//                          AnnotationGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;

import java.net.URI;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@ApiModel
@JsonPropertyOrder({
        "uri", "targets", "description", "motivation", "creator", "created"
})
public class AnnotationGetDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("description")
    protected String description;

    @JsonProperty("targets")
    protected List<URI> targets;

    @JsonProperty("motivation")
    protected MotivationGetDTO motivation;

    @JsonProperty("creator")
    protected URI creator;

    @JsonProperty("created")
    protected String created;

    public AnnotationGetDTO() {
    }

    public AnnotationGetDTO(AnnotationModel model) {
        uri = model.getUri();

        description = model.getBodyValue();
        targets = model.getTargets();
        created = model.getCreated().toString();
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
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }

    @ApiModelProperty(value = "Creation date" ,example = "2019-09-08T12:00:00+01:00")
    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
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
