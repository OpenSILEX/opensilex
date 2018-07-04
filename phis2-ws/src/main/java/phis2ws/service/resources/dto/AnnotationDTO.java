//******************************************************************************
//                                       AnnotationDTO.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 june 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  04 July 2018
// Subject: Represents the JSON submitted by the client for the annotation POST service 
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
 * Represents the JSON submitted by the client for the annotation POST service
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class AnnotationDTO extends AbstractVerifiedClass {

    // motivation instance uri that describe the purpose of this annotation  eg. http://www.w3.org/ns/oa#commenting
    private String motivatedBy;

    // creator of this annotations eg. http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy
    @NotNull
    private String creator;

    // represent body value
    private String comment;

    // uris that are annoted by this annotation  eg. http://www.phenome-fppn.fr/diaphen/2017/o1032481
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

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_MOTIVATEDBY, notes = "Need to be an URI (instance of oa:Motivation concept)")
    public String getMotivatedBy() {
        return motivatedBy;
    }

    public void setMotivatedBy(String motivatedBy) {
        this.motivatedBy = motivatedBy;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_CREATOR, notes = "Need to be an URI")
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

    @ApiModelProperty(notes = "Need to be an URI array")
    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

}
