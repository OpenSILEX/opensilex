package org.opensilex.front.api;

import java.util.List;

public class AgroportalOntologiesConfigDTO {

    private List<String> entityOntologies;
    private List<String> traitOntologies;

    public List<String> getEntityOntologies() {
        return entityOntologies;
    }

    public void setEntityOntologies(List<String> entityOntologies) {
        this.entityOntologies = entityOntologies;
    }

    public List<String> getTraitOntologies() {
        return traitOntologies;
    }

    public void setTraitOntologies(List<String> traitOntologies) {
        this.traitOntologies = traitOntologies;
    }
}
