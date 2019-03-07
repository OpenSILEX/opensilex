//******************************************************************************
//                      ConcernedItemWithLabelDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 20 déc. 2018
// Contact: andreas.garcia@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.view.model.phis.ConcernedItem;

/**
 * DTO of a concerned item with labels
 * @author andreas
 */
public class ConcernedItemWithLabelsDTO extends ConcernItemDTO {
    
    /**
     * Labels
     */
    protected ArrayList<String> labels = new ArrayList();

    public ConcernedItemWithLabelsDTO(ConcernedItem concernedItem) {
        this.uri = concernedItem.getUri();
        this.typeURI = concernedItem.getRdfType();
        this.labels = concernedItem.getLabels();
    }

    @Override
    public ConcernedItem createObjectFromDTO() {
        ConcernedItem concernedItem = super.createObjectFromDTO();
        concernedItem.setLabels(labels);
        return concernedItem;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI)
    @Required
    public ArrayList<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }
}
