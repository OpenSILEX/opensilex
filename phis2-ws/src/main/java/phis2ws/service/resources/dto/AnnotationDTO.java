//******************************************************************************
//                                       AnnotationDTO.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  20 juin 2018
// Subject: Represents the JSON submitted for the annotation
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Annotation;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class AnnotationDTO extends AbstractVerifiedClass {

    private String motivatedBy;

    @NotNull
    private String creator;

    // represent body value
    private String comment;

    private ArrayList<String> targets;

    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("motivatedBy", Boolean.TRUE);
        rules.put("creator", Boolean.TRUE);
        rules.put("bodyValue", Boolean.TRUE);
        rules.put("targets", Boolean.TRUE);

        return rules;
    }

    @Override
    public Annotation createObjectFromDTO() {
        Annotation annotation = new Annotation();
        annotation.setBodyValue(comment);
        DateTime currentTime = DateTime.now();
        annotation.setCreated(currentTime);
        annotation.setCreator(creator);
        annotation.setTargets(targets);
        annotation.setMotivatedBy(motivatedBy);

        return annotation;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_MOTIVATEDBY)
    public String getMotivatedBy() {
        return motivatedBy;
    }

    public void setMotivatedBy(String motivatedBy) {
        this.motivatedBy = motivatedBy;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_CREATOR)
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_COMMENT)
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ApiModelProperty(notes = "Need to be an URI")
    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

}
