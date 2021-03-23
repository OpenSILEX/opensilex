/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;

/**
 *
 * @author vmigot
 */
public class VueRDFTypePropertyDTO {

    protected URI property;

    @JsonProperty("target_property")
    protected URI targetProperty;

    protected String name;

    protected String comment;

    @JsonProperty("input_component")
    protected String inputComponent;

    @JsonProperty("input_components_by_property")
    protected Map<String, String> inputComponentsByProperty;

    @JsonProperty("view_component")
    protected String viewComponent;

    protected boolean inherited;

    @JsonProperty("is_list")
    protected boolean isList;

    @JsonProperty("is_required")
    protected boolean isRequired;

    @JsonProperty("is_custom")
    protected boolean isCustom;

    public URI getProperty() {
        return property;
    }

    public void setProperty(URI property) {
        this.property = property;
    }

    public URI getTargetProperty() {
        return targetProperty;
    }

    public void setTargetProperty(URI targetProperty) {
        this.targetProperty = targetProperty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getInputComponent() {
        return inputComponent;
    }

    public void setInputComponent(String inputComponent) {
        this.inputComponent = inputComponent;
    }

    public Map<String, String> getInputComponentsByProperty() {
        return inputComponentsByProperty;
    }

    public void setInputComponentsByProperty(Map<String, String> inputComponentsByProperty) {
        this.inputComponentsByProperty = inputComponentsByProperty;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public boolean isIsList() {
        return isList;
    }

    public void setIsList(boolean isList) {
        this.isList = isList;
    }

    public boolean isIsRequired() {
        return isRequired;
    }

    public void setIsRequired(boolean isRequired) {
        this.isRequired = isRequired;
    }

    public boolean isIsCustom() {
        return isCustom;
    }

    public void setIsCustom(boolean isCustom) {
        this.isCustom = isCustom;
    }

}
