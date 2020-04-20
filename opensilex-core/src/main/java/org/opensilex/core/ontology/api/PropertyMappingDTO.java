/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import org.opensilex.core.ontology.OpenSilexApiOntology;
import org.opensilex.core.ontology.dal.PropertyMappingModel;
import org.opensilex.sparql.annotations.SPARQLProperty;

/**
 *
 * @author vince
 */
public class PropertyMappingDTO {
    
    private URI about;
    
    private String className;
    
    private boolean list;
    
    private boolean required;
    
    private boolean inverse;
    
    private boolean ignoreUpdateIfNull;
    
    private boolean cascadeDelete;
    
    private int order;
    
    private String viewComponent;
    
    private String editComponent;
    
    public URI getAbout() {
        return about;
    }
    
    public void setAbout(URI about) {
        this.about = about;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public boolean isList() {
        return list;
    }
    
    public void setList(boolean list) {
        this.list = list;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public boolean isInverse() {
        return inverse;
    }
    
    public void setInverse(boolean inverse) {
        this.inverse = inverse;
    }
    
    public boolean isIgnoreUpdateIfNull() {
        return ignoreUpdateIfNull;
    }
    
    public void setIgnoreUpdateIfNull(boolean ignoreUpdateIfNull) {
        this.ignoreUpdateIfNull = ignoreUpdateIfNull;
    }
    
    public boolean isCascadeDelete() {
        return cascadeDelete;
    }
    
    public void setCascadeDelete(boolean cascadeDelete) {
        this.cascadeDelete = cascadeDelete;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public String getViewComponent() {
        return viewComponent;
    }
    
    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }
    
    public String getEditComponent() {
        return editComponent;
    }
    
    public void setEditComponent(String editComponent) {
        this.editComponent = editComponent;
    }
    
    public static PropertyMappingDTO fromModel(PropertyMappingModel model) {
        PropertyMappingDTO dto = new PropertyMappingDTO();
        
        dto.setAbout(model.getAbout());
        dto.setCascadeDelete(model.isCascadeDelete());
        dto.setClassName(model.getClassName());
        dto.setIgnoreUpdateIfNull(model.isIgnoreUpdateIfNull());
        dto.setInverse(model.isInverse());
        dto.setList(model.isList());
        dto.setOrder(model.getOrder());
        dto.setRequired(model.isRequired());
        dto.setViewComponent(model.getViewComponent());
        dto.setEditComponent(model.getEditComponent());
        
        return dto;
    }
}
