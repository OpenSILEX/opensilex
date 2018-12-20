//******************************************************************************
//                                       ConcernsItemWithLabelDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 20 déc. 2018
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/andreas/Documents/OpenSILEX/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

import java.util.ArrayList;
import phis2ws.service.view.model.phis.ConcernItem;

/**
 *
 * @author andreas
 */
public class ConcernsItemWithLabelsDTO extends ConcernItemDTO{
    
    protected ArrayList<String> labels = new ArrayList();

    public ConcernsItemWithLabelsDTO(ConcernItem concernItem) {
        this.uri = concernItem.getUri();
        this.typeUri = concernItem.getRdfType();
        this.labels = concernItem.getLabels();
    }

    @Override
    public ConcernItem createObjectFromDTO() {
        ConcernItem concernItem = super.createObjectFromDTO();
        concernItem.setLabels(labels);
        return concernItem;
    }
    
    public ArrayList<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }
}
