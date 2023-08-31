package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseMultiLabelResourceGetDTO;
import org.opensilex.core.variable.dal.EntityModel;

import java.net.URI;
import java.util.List;
import java.util.Map;


public class EntityGetDTO extends BaseMultiLabelResourceGetDTO<EntityModel> {

    public EntityGetDTO(EntityModel model) {
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
    public Map<String,String> getPrefLabels() {
        return prefLabels;
    }

    @Override
    public Map<String, String> getShortLabels() {
        return shortLabels;
    }

    @Override
    public Map<String,List<String>> getAltLabels() {
        return altLabels;
    }

    @Override
    public Map<String,String> getDefinitions() {
        return definitions;
    }



}
