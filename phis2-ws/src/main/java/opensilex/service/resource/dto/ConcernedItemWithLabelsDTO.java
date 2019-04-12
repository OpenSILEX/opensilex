//******************************************************************************
//                       ConcernedItemWithLabelDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 20 Dec. 2018
// Contact: andreas.garcia@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.model.ConcernedItem;

/**
 * Concerned item with labels DTO.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class ConcernedItemWithLabelsDTO extends ConcernedItemDTO {
    
    /**
     * Labels
     */
    protected List<String> labels = new ArrayList();

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
    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
}
