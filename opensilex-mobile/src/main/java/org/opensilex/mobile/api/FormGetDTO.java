/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.mobile.api;

import org.opensilex.mobile.dal.FormModel;

/**
 *
 * @author hart
 */
public class FormGetDTO extends FormUpdateDTO{
    
    public static FormGetDTO fromModel(FormModel model) {
        FormGetDTO formGetDTO = new FormGetDTO();
        formGetDTO.setCreationDate(model.getCreationDate().toString());
        formGetDTO.setFormData(model.getFormData());
        formGetDTO.setTimezone(model.getOffset());
        formGetDTO.setType(model.getType());
        formGetDTO.setUri( model.getUri());
        return formGetDTO;
    }
    ///This will call from model
//    public static FormCreationDTO getDtoFromModel(FormModel model){
//        FormGetDTO dto = new FormGetDTO();
//        dto.fromModel(model);
//        return dto;
//    }
    
}
