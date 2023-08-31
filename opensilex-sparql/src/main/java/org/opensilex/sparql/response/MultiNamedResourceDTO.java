package org.opensilex.sparql.response;

import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;

import java.util.Map;

public class MultiNamedResourceDTO <T extends SPARQLMultiNamedResourceModel> extends ResourceDTO<T> {

    protected Map<String, String> prefLabels;

    public Map<String, String> getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(Map<String, String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    @Override
    public void toModel(T model) {
        super.toModel(model);
        model.getPrefLabels().setTranslations(getPrefLabels());
    }

    @Override
    public void fromModel(T model) {
        super.fromModel(model);
        setPrefLabels(model.getPrefLabels().getTranslations());
    }

    @Override
    public T newModelInstance() {
        return (T) new SPARQLMultiNamedResourceModel<>();
    }

    public static MultiNamedResourceDTO getDTOFromModel(SPARQLMultiNamedResourceModel model) {
        MultiNamedResourceDTO dto = new MultiNamedResourceDTO();
        dto.fromModel(model);

        return dto;
    }
}
