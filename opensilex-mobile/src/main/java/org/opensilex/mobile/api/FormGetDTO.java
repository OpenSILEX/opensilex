//******************************************************************************
//                          FormGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import org.opensilex.mobile.dal.FormModel;

/**
 *
 * @author Maximilian Hart
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
}
