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
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationDTO extends AbstractVerifiedClass {

    // motivation instance uri that describe the purpose of this annotation 
    // e.g. http://www.w3.org/ns/oa#commenting
    private String motivatedBy;

    // Is an uri that represents the author
    // of this annotations 
    // e.g. http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy
    @NotNull
    private String creator;

    /**
     * represents annotation bodies value
     * @link https://www.w3.org/TR/annotation-model/#cardinality-of-bodies-and-targets
     */
    private ArrayList<String> comments;

    /**
     * uris that are annoted by this annotation. e.g.
     * http://www.phenome-fppn.fr/diaphen/2017/o1032481
     * @link https://www.w3.org/TR/annotation-model/#cardinality-of-bodies-and-targets
     */
    private ArrayList<String> targets;

    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("motivatedBy", Boolean.TRUE);
        rules.put("creator", Boolean.TRUE);
        rules.put("comments", Boolean.FALSE);
        rules.put("targets", Boolean.TRUE);

        return rules;
    }

    @Override
    public Annotation createObjectFromDTO() {
        Annotation annotation = new Annotation();
        annotation.setBodiesValue(comments);
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

    @ApiModelProperty(notes = "Need to be an array of text")
    public ArrayList<String> getComments() {
        return comments;
    }

    public void setComments(ArrayList<String> comments) {
        this.comments = comments;
    }

    @ApiModelProperty(notes = "Need to be an array of URI")
    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

}
