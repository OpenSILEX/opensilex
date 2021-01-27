package org.opensilex.core.species.api;


import org.opensilex.core.species.dal.SpeciesModel;

import java.net.URI;

public class SpeciesDTO {

    protected URI uri;
    protected String name;

    public URI getUri() {
        return uri;
    }

    public SpeciesDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public SpeciesDTO setName(String label) {
        this.name = label;
        return this;
    }

    public static SpeciesDTO fromModel(SpeciesModel model) {
        return new SpeciesDTO()
                .setUri(model.getUri())
                .setName(model.getLabel().getDefaultValue());
    }
}
