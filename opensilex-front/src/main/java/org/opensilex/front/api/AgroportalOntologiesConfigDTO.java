package org.opensilex.front.api;

import java.util.List;

public class AgroportalOntologiesConfigDTO {

    private List<String> entityOntologies;
    private List<String> traitOntologies;
    private List<String> methodOntologies;
    private List<String> unitOntologies;

    public List<String> getMethodOntologies() {
        return methodOntologies;
    }

    public void setMethodOntologies(List<String> methodOntologies) {
        this.methodOntologies = methodOntologies;
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

    public List<String> getUnitOntologies() {
        return unitOntologies;
    }

    public void setUnitOntologies(List<String> unitOntologies) {
        this.unitOntologies = unitOntologies;
    }
}
