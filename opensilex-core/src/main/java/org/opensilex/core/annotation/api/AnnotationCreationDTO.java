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
public class AnnotationCreationDTO extends ResourceDTO<AnnotationModel> {

    protected String bodyValue;
    protected List<URI> targets;
    protected URI motivation;
    protected URI creator;

    @ValidURI
    @Override
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(required = true, example = "The pest attack lasted 20 minutes")
    @Required
    public String getBodyValue() {
        return bodyValue;
    }

    @NotEmpty
    @ValidURI
    @ApiModelProperty(required = true)
    public List<URI> getTargets() {
        return targets;
    }

    @NotNull
    @ValidURI
    @ApiModelProperty(required = true)
    public URI getMotivation() {
        return motivation;
    }

    @ApiModelProperty(hidden = true)
    public void setCreator(URI creator) {
        this.creator = creator;
    }

    @Override
    public AnnotationModel newModelInstance() {
        return new AnnotationModel();
    }

    public void setBodyValue(String bodyValue) {
        this.bodyValue = bodyValue;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }

    public void setMotivation(URI motivation) {
        this.motivation = motivation;
    }

    public URI getCreator() {
        return creator;
    }


    @Override
    public void toModel(AnnotationModel model) {
        super.toModel(model);
        model.setBodyValue(bodyValue);
        model.setCreated(LocalDate.now());
        model.setTargets(targets);

        if (motivation != null) {
            MotivationModel motivationModel = new MotivationModel();
            motivationModel.setUri(motivation);
            model.setMotivation(motivationModel);
        }
    }
}
