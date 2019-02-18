//******************************************************************************
//                          AnnotationDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 June 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.Annotation;

/**
 * Represents the JSON submitted by the client for the annotation POST service
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationDTO extends AbstractVerifiedClass {

    /**
     * Uri
     * @example http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2
     */
    @URL
    private String uri;

    /** 
     * Creation date string format yyyy-MM-ddTHH:mm:ssZ
     * @example 2018-06-25T15:13:59+0200
     */
    private String creationDate;

    /** 
     * Uri that represents the author 
     * @example http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy
     */
    @URL
    @Required
    private String creator;

    /** 
     * Motivation instance uri that describe the purpose of this annotation 
     * @example http://www.w3.org/ns/oa#commenting
     */ 
    @URL
    @Required
    private String motivatedBy;

    /**
     * Represents annotation body values
     * @link https://www.w3.org/TR/annotation-model/#cardinality-of-bodies-and-targets
     */
    private ArrayList<String> comments;

    /**
     * Uris concerned by this annotation 
     * @example http://www.phenome-fppn.fr/diaphen/2017/o1032481
     * @link https://www.w3.org/TR/annotation-model/#cardinality-of-bodies-and-targets
     */
    @URL
    @NotEmpty
    @NotNull
    private ArrayList<String> targets;
    
    /**
     * Constructor to create a DTO from an annotation model
     * @param annotation 
     */
    public AnnotationDTO(Annotation annotation) {
        this.uri = annotation.getUri();
        
        DateTime annotationCreated = annotation.getCreated();
        if (annotationCreated != null){
            this.creationDate = DateTimeFormat
                    .forPattern(DateFormat.YMDTHMSZZ.toString())
                    .print(annotationCreated);
        }
        else{
            this.creationDate = null;
        }
        this.creator = annotation.getCreator();
        this.comments = annotation.getBodiesValue();
        this.motivatedBy = annotation.getMotivatedBy();
        this.targets = annotation.getTargets();
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCreated() {
        return creationDate;
    }

    public void setCreated(String created) {
        this.creationDate = created;
    }
}
