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
public class AnnotationGetDTO extends ResourceDTO<AnnotationModel> {

    protected String bodyValue;
    protected List<URI> targets;
    protected URI motivation;
    protected String motivationName;
    protected URI creator;
    protected LocalDate created;

    @ApiModelProperty(value = "Annotation URI", example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415")
    @Override
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(hidden = true)
    @Override
    public URI getType() {
        return type;
    }

    // don't allow to set a specific annotation type
    @ApiModelProperty(hidden = true)
    @Override
    public void setType(URI type) {

    }

    @Override
    @ApiModelProperty(hidden = true)
    public String getTypeLabel() {
        return super.getTypeLabel();
    }

    // don't allow to set a specific type label
    @Override
    @ApiModelProperty(hidden = true)
    public void setTypeLabel(String typeLabel) {

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

    @ApiModelProperty(value = "URI of the annotation motivation", example = "http://www.w3.org/ns/oa#describing")
    public URI getMotivation() {
        return motivation;
    }

    public void setMotivation(URI motivation) {
        this.motivation = motivation;
    }

    @ApiModelProperty(value = "Name of the annotation motivation", example = "describing")
    public String getMotivationName() {
        return motivationName;
    }

    public void setMotivationName(String motivationName) {
        this.motivationName = motivationName;
    }

    @Override
    public AnnotationModel newModelInstance() {
        return new AnnotationModel();
    }


    @Override
    public void fromModel(AnnotationModel model) {

        super.fromModel(model);
        
        bodyValue = model.getBodyValue();
        targets = model.getTargets();
        created = model.getCreated();
        creator = model.getCreator();

        MotivationModel motivationModel = model.getMotivation();
        motivation = motivationModel.getUri();
        motivationName = motivationModel.getName();
    }

}
