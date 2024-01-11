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
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@ApiModel
@JsonPropertyOrder({
        "uri", "description", "targets", "motivation", "published", "publisher",
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

    @JsonProperty("published")
    protected OffsetDateTime published;

    @JsonProperty("publisher")
    protected URI publisher;

    public AnnotationGetDTO() {
    }

    public AnnotationGetDTO(AnnotationModel model) {
        uri = model.getUri();

        description = model.getDescription();
        targets = model.getTargets();
        published = model.getPublicationDate();
        publisher = model.getPublisher();

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

    @ApiModelProperty(value = "Annotation publisher URI" ,example = "http://opensilex.dev/users#Admin.OpenSilex")
    public URI getPublisher() {
        return publisher;
    }

    public void setPublisher(URI publisher) {
        this.publisher = publisher;
    }

    public MotivationGetDTO getMotivation() {
        return motivation;
    }

    public void setMotivation(MotivationGetDTO motivation) {
        this.motivation = motivation;
    }
}
