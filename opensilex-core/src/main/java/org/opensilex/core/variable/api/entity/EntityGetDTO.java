package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseMultiLabeledIdentifierGetDTO;
import org.opensilex.core.variable.dal.EntityMultiLabelModel;

import java.net.URI;
import java.util.List;


public class EntityGetDTO extends BaseMultiLabeledIdentifierGetDTO<EntityMultiLabelModel> {

    public EntityGetDTO(EntityMultiLabelModel model) {
        super(model);
    }

    public EntityGetDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    @Override
    public List<String> getPrefLabels() {
        return prefLabels;
    }

    @Override
    public List<String> getAltLabels() {
        return altLabels;
    }

    @Override
    public List<String> getDefinitions() {
        return definitions;
    }

//    @Override
//    @ApiModelProperty(example = "Plant")
//    public String getName() {
//        return name;
//    }



}
