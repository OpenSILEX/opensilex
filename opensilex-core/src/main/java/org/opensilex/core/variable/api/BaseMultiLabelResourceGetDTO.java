package org.opensilex.core.variable.api;

import org.opensilex.core.variable.dal.BaseMultiLabelsResourceModel;
import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
import org.opensilex.sparql.response.ObjectMultiNamedResourceDTO;

import java.util.List;
import java.util.Map;


public abstract class BaseMultiLabelResourceGetDTO<T extends BaseMultiLabelsResourceModel<T>> extends ObjectMultiNamedResourceDTO {

    public BaseMultiLabelResourceGetDTO() {
    }

    public BaseMultiLabelResourceGetDTO(SPARQLMultiNamedResourceModel<?> model) {
        super(model);
    }

    public abstract Map<String,String> getPrefLabels();

    public abstract Map<String,String> getShortLabels();


    public abstract Map<String,List<String>> getAltLabels();

    public abstract Map<String,String> getDefinitions();

}
