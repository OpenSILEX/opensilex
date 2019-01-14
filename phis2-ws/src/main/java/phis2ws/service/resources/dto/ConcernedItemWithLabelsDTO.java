//******************************************************************************
//                      ConcernedItemWithLabelDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 20 déc. 2018
// Contact: andreas.garcia@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

import java.util.ArrayList;
import phis2ws.service.view.model.phis.ConcernItem;

/**
 * DTO of a concerned item with labels
 * @author andreas
 */
public class ConcernedItemWithLabelsDTO extends ConcernItemDTO{
    
    // labels
    protected ArrayList<String> labels = new ArrayList();

    public ConcernedItemWithLabelsDTO(ConcernItem concernedItem) {
        this.uri = concernedItem.getUri();
        this.typeURI = concernedItem.getRdfType();
        this.labels = concernedItem.getLabels();
    }

    @Override
    public ConcernItem createObjectFromDTO() {
        ConcernItem concernedItem = super.createObjectFromDTO();
        concernedItem.setLabels(labels);
        return concernedItem;
    }

    public ArrayList<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }
}
