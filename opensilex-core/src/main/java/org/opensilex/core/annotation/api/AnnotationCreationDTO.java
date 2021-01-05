//******************************************************************************
//                          AnnotationCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceDTO;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@ApiModel
public class AnnotationCreationDTO {

    protected URI uri;
    protected String bodyValue;
    protected List<URI> targets;
    protected URI motivation;
    protected URI creator;


    @ApiModelProperty(value = "Annotation URI", example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(required = true, example = "The pest attack lasted 20 minutes")
    @Required
    public String getBodyValue() {
        return bodyValue;
    }

    public void setBodyValue(String bodyValue) {
        this.bodyValue = bodyValue;
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
    @ApiModelProperty(required = true)
    public URI getMotivation() {
        return motivation;
    }

    public void setMotivation(URI motivation) {
        this.motivation = motivation;
    }

    public URI getCreator() {
        return creator;
    }

    @ApiModelProperty(hidden = true)
    public void setCreator(URI creator) {
        this.creator = creator;
    }


    public AnnotationModel newModel() {

        AnnotationModel model = new AnnotationModel();

        model.setUri(uri);
        model.setBodyValue(bodyValue);
        model.setCreated(LocalDate.now());
        model.setTargets(targets);

        MotivationModel motivationModel = new MotivationModel();
        motivationModel.setUri(motivation);
        model.setMotivation(motivationModel);

        return model;
    }
}
