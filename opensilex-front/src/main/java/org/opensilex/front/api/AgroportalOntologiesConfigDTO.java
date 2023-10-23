package org.opensilex.front.api;

import org.opensilex.front.config.OntologyPortalItem;

import java.util.List;

public class AgroportalOntologiesConfigDTO {

    private List<OntologyPortalItem> ontologyPortals;
    private List<String> entityOntologies;
    private List<String> traitOntologies;

    public List<OntologyPortalItem> getOntologyPortals() {
        return ontologyPortals;
    }

    public void setOntologyPortals(List<OntologyPortalItem> ontologyPortals) {
        this.ontologyPortals = ontologyPortals;
    }

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
