package org.opensilex.core.species.api;


import org.opensilex.core.species.dal.SpeciesModel;

import java.net.URI;

public class SpeciesDTO {

    protected URI uri;
    protected String label;

    public URI getUri() {
        return uri;
    }

    public SpeciesDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public SpeciesDTO setLabel(String label) {
        this.label = label;
        return this;
    }

    public static SpeciesDTO fromModel(SpeciesModel model) {
        return new SpeciesDTO()
                .setUri(model.getUri())
                .setLabel(model.getLabel().getDefaultValue());
    }
}
