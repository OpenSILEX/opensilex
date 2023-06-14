package org.opensilex.core.variable.api;

import org.opensilex.core.variable.dal.BaseMultiLabeledIdentifierModel;
import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
import org.opensilex.sparql.response.ObjectMultiNamedResourceDTO;

import java.util.List;
import java.util.Map;


public abstract class BaseMultiLabeledIdentifierGetDTO<T extends BaseMultiLabeledIdentifierModel<T>> extends ObjectMultiNamedResourceDTO {

    public BaseMultiLabeledIdentifierGetDTO() {
    }

    public BaseMultiLabeledIdentifierGetDTO(SPARQLMultiNamedResourceModel<?> model) {
        super(model);
    }

    public abstract Map<String,String> getPrefLabels();

    public abstract Map<String,String> getAltLabels();

    public abstract Map<String,String> getDefinitions();

}
