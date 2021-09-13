/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.mobile.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author hart
 */
public class FormUpdateDTO extends FormCreationDTO{
    
    protected URI uri;
    
    @NotNull
    @ValidURI
    //@Override
    @ApiModelProperty(value = "URI of the form being updated", required = true) 
    public URI getUri() {
        return uri;
    }
    
    @Override
    public FormModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        FormModel model = super.newModel();
        model.setUri(uri);
        return model;
    }
    /*
    @Override
    public void fromModel(FormModel model) {
        super.fromModel(model);
        setUri(model.getUri());
    }
    
    public static FormUpdateDTO getDtoFromModel(FormModel model){
        FormUpdateDTO dto = new FormUpdateDTO();
        dto.fromModel(model);
        return dto;
    }
    */
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
}
