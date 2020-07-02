package org.opensilex.core.variable.api.trait;

import org.opensilex.core.variable.dal.trait.TraitModel;

import java.net.URI;

public class TraitDTO {

    private URI traitUri;
    private String traitLabel;
    private URI traitClass;

    public URI getTraitUri() {
        return traitUri;
    }

    public void setTraitUri(URI traitUri) {
        this.traitUri = traitUri;
    }

    public String getTraitLabel() {
        return traitLabel;
    }

    public void setTraitLabel(String traitLabel) {
        this.traitLabel = traitLabel;
    }

    public URI getTraitClass() {
        return traitClass;
    }

    public void setTraitClass(URI traitClass) {
        this.traitClass = traitClass;
    }

    public static TraitDTO fromModel(TraitModel model) {
        TraitDTO dto = new TraitDTO();
        dto.setTraitUri(model.getUri());
        dto.setTraitClass(model.getTraitClass());
        dto.setTraitLabel(model.getLabel());
        return dto;
    }

    public TraitModel newModel() {
        return defineModel(new TraitModel());
    }

    public TraitModel defineModel(TraitModel model) {
        model.setUri(getTraitUri());
        model.setTraitClass(getTraitClass());
        model.setLabel(getTraitLabel());
        return model;
    }
}
