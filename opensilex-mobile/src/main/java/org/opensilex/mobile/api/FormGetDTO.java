package org.opensilex.mobile.api;

import org.opensilex.mobile.dal.FormModel;

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
