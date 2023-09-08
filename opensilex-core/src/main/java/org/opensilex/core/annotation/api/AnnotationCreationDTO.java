//******************************************************************************
//                          AnnotationCreationDTO.java
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
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@ApiModel
@JsonPropertyOrder({
        "uri", "description","targets", "motivation"
})
public class AnnotationCreationDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("description")
    protected String description;

    @JsonProperty("targets")
    protected List<URI> targets;

    @JsonProperty("motivation")
    protected URI motivation;

    @ApiModelProperty(example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(required = true, example = "The pest attack lasted 20 minutes")
    @Required
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotEmpty
    @ValidURI
    @ApiModelProperty(required = true)
    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }


    @NotNull
    @ValidURI
    @ApiModelProperty(required = true, example = "http://www.w3.org/ns/oa#describing")
    public URI getMotivation() {
        return motivation;
    }

    public void setMotivation(URI motivation) {
        this.motivation = motivation;
    }


    public AnnotationModel newModel() {

        AnnotationModel model = new AnnotationModel();

        model.setUri(uri);
        model.setDescription(description);
        model.setTargets(targets);

        MotivationModel motivationModel = new MotivationModel();
        motivationModel.setUri(motivation);
        model.setMotivation(motivationModel);

        return model;
    }
}
