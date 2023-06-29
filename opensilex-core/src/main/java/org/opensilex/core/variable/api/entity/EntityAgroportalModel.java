package org.opensilex.core.variable.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityAgroportalModel {

    @JsonProperty("prefLabel")
    private String prefLabel;

    @JsonProperty("definition")
    private String[] definitions;
    @JsonProperty("ontologyType")
    private String ontologyType;
    @JsonProperty("@id")
    private String id;
    @JsonProperty("@type")
    private String type;

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    public String[] getDefinition() {
        return definitions;
    }

    public void setDefinition(String[] definition) {
        this.definitions = definition;
    }

    public String getOntologyType() {
        return ontologyType;
    }

    public void setOntologyType(String ontologyType) {
        this.ontologyType = ontologyType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
