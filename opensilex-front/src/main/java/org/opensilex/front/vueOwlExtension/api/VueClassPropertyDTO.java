/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import java.net.URI;
import org.opensilex.front.vueOwlExtension.dal.VueClassPropertyExtensionModel;

/**
 *
 * @author vmigot
 */
public class VueClassPropertyDTO {
    
    protected URI property;
    
    protected Integer order;
    
    protected String inputComponent;
    
    protected String viewComponent;
    
    public void setProperty(URI property) {
        this.property = property;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public String getInputComponent() {
        return inputComponent;
    }
    
    public void setInputComponent(String inputComponent) {
        this.inputComponent = inputComponent;
    }
    
    public String getViewComponent() {
        return viewComponent;
    }
    
    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }
    
    static VueClassPropertyDTO fromModel(VueClassPropertyExtensionModel extProperty) {
        VueClassPropertyDTO dto = new VueClassPropertyDTO();
        dto.setProperty(extProperty.getUri());
        dto.setViewComponent(extProperty.getViewComponent());
        dto.setInputComponent(extProperty.getInputComponent());
        dto.setOrder(extProperty.getHasDisplayOrder());
        
        return dto;
    }
    
}
