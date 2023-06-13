package org.opensilex.core.variable.api;

import org.opensilex.core.variable.dal.BaseMultiLabeledIdentifierModel;
import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
import org.opensilex.sparql.response.ObjectMultiNamedResourceDTO;

import java.util.List;


public abstract class BaseMultiLabeledIdentifierGetDTO<T extends BaseMultiLabeledIdentifierModel<T>> extends ObjectMultiNamedResourceDTO {

    public BaseMultiLabeledIdentifierGetDTO() {
    }

    public BaseMultiLabeledIdentifierGetDTO(SPARQLMultiNamedResourceModel<?> model) {
        super(model);
    }

    public abstract List<String> getPrefLabels();

    public abstract List<String> getAltLabels();

    public abstract List<String> getDefinitions();

}
