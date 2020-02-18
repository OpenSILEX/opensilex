//******************************************************************************
//                            AnnotationPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 06 March 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.annotation;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Annotation;

/**
 * Annotation POST DTO.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationPostDTO extends AbstractVerifiedClass {

    /** 
     * URI that represents the author.
     * @example http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy
     */
    private String creator;

    /** 
     * Motivation instance URI that describe the purpose of this annotation.
     * @example http://www.w3.org/ns/oa#commenting
     */ 
    private String motivatedBy;

    /**
     * Represents the annotation's body values.
     * @link https://www.w3.org/TR/annotation-model/#cardinality-of-bodies-and-targets
     */
    private List<String> bodyValues;

    /**
     * URIs concerned by this annotation.
     * @example http://www.phenome-fppn.fr/diaphen/2017/o1032481
     * @link https://www.w3.org/TR/annotation-model/#cardinality-of-bodies-and-targets
     */
    private List<String> targets;
    
    /**
     * Constructor to create a DTO from an annotation model.
     * @param annotation 
     */
    public AnnotationPostDTO(Annotation annotation) {        
        this.creator = annotation.getCreator();
        this.bodyValues = annotation.getBodyValues();
        this.motivatedBy = annotation.getMotivatedBy();
        this.targets = annotation.getTargets();
    }

    @Override
    public Annotation createObjectFromDTO() {
        return new Annotation(null, DateTime.now(), creator, bodyValues, motivatedBy, targets);
    }

    @URL
    @NotNull
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_MOTIVATED_BY)
    public String getMotivatedBy() {
        return motivatedBy;
    }

    public void setMotivatedBy(String motivatedBy) {
        this.motivatedBy = motivatedBy;
    }

    @URL
    @NotNull
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_CREATOR)
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @ApiModelProperty(notes = "Need to be an array of text")
    public List<String> getBodyValues() {
        return bodyValues;
    }

    public void setBodyValues(List<String> bodyValues) {
        this.bodyValues = bodyValues;
    }

    @URL
    @NotEmpty
    @NotNull
    @ApiModelProperty(notes = "Need to be an array of URI")
    public List<String> getTargets() {
        return targets;
    }
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }
}
