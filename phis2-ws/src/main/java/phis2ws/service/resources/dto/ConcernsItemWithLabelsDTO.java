//******************************************************************************
//                                       ConcernsItemWithLabelDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 20 déc. 2018
// Contact: andreas.garcia@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

import java.util.ArrayList;
import phis2ws.service.view.model.phis.ConcernItem;

/**
 * DTO of Concern Item with label 
 * @author andreas
 */
public class ConcernsItemWithLabelsDTO extends ConcernItemDTO{
    
    // labels
    protected ArrayList<String> labels = new ArrayList();

    /**
     *
     * @param concernItem
     */
    public ConcernsItemWithLabelsDTO(ConcernItem concernItem) {
        this.uri = concernItem.getUri();
        this.typeURI = concernItem.getRdfType();
        this.labels = concernItem.getLabels();
    }

    /**
     *
     * @return concerns Item model
     */
    @Override
    public ConcernItem createObjectFromDTO() {
        ConcernItem concernItem = super.createObjectFromDTO();
        concernItem.setLabels(labels);
        return concernItem;
    }
    
    /**
     *
     * @return labels
     */
    public ArrayList<String> getLabels() {
        return labels;
    }

    /**
     *
     * @param labels
     */
    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }
}
