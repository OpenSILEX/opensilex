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
import javax.xml.bind.annotation.XmlTransient;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Annotation;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class AnnotationDTO extends AbstractVerifiedClass {

    private String motivatedBy;
    

    private String creator;

    private String bodyValue;

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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_MOTIVATION)
    public String getMotivatedBy() {
        return motivatedBy;
    }

    public void setMotivatedBy(String motivatedBy) {
        this.motivatedBy = motivatedBy;
    }
        
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_EMAIL)
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_BODYVALUE)
    public String getBodyValue() {
        return bodyValue;
    }

    public void setBodyValue(String bodyValue) {
        this.bodyValue = bodyValue;
    }
    
    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

    
    
    
    

}
