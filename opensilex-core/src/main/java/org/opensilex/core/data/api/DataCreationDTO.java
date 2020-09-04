/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import java.text.ParseException;
import org.opensilex.core.data.dal.DataModel;

/**
 *
 * @author sammy
 */
public class DataCreationDTO extends DataDTO {
    
    public DataModel newModel() throws ParseException {
        return defineModel(new DataModel());
    }
    
    private DataModel defineModel(DataModel model) throws ParseException {

        model.setUri(getUri());
        model.setObject(getObject());
        model.setVariable(getVariable());
        model.setProvenance(getProvenance());
        
        model.setDate(getDate());
        //model.setTimezone(getTimezone());
        
        model.setValue(getValue());
        
        model.setConfidence(getConfidence());
        return model;
        
    }
}
