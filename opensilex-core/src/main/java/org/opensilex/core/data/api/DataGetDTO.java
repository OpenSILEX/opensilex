/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import java.util.Date;
import org.opensilex.core.data.dal.DataModel;

/**
 *
 * @author sammy
 */
public class DataGetDTO extends DataDTO{
    public static DataGetDTO fromModel(DataModel model){
        DataGetDTO dto = new DataGetDTO();
        
        dto.setUri(model.getUri());
        dto.setObject(model.getObject());
        dto.setVariable(model.getVariable());
        dto.setProvenance(model.getProvenance());
        
        dto.setDate(model.getDate());
        dto.setConfidence(model.getConfidence());
        dto.setValue(model.getValue());
        
        return dto;
    }
}
