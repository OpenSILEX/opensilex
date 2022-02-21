//******************************************************************************
//                          FormGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import org.opensilex.mobile.dal.FormModel;
/**
 * This class is the Data Transfer Object that is used when we want to fetch forms
 *
 * @author Maximilian Hart
 */
public class FormGetDTO extends FormUpdateDTO{

    public static FormGetDTO fromModel(FormModel model) {
        FormGetDTO formGetDTO = new FormGetDTO();
        formGetDTO.setCreationDate(model.getCreationDate().toString());
        formGetDTO.setUpdateDate(model.getLastUpdateDate().toString());
        formGetDTO.setUri(model.getUri());
        formGetDTO.setCommitAddress(model.getCommitAddress());
        formGetDTO.setCodeLot(model.getCodeLot());
        formGetDTO.setChildren(model.getChildren());
        formGetDTO.setRoot(model.isRoot());
        formGetDTO.setType(model.getType());
        formGetDTO.setOffset(model.getOffset());
        formGetDTO.setSectionUris(model.getSectionUris());
        formGetDTO.setParents(model.getParents());

        return formGetDTO;
    }
}
