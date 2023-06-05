package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonPropertyOrder({"prefLabel", "altLabels", "definition"})
public class MultiLabelDTO {

    @JsonProperty("prefLabel")
    protected List<String> prefLabels;

    @JsonProperty("altLabels")
    protected List<String> altLabels;

    @JsonProperty("definition")
    protected List<String> definitions;

    public MultiLabelDTO(){

        this.prefLabels = new ArrayList<>();
        this.altLabels = new ArrayList<>();
        this.definitions = new ArrayList<>();

    }

    public List<String> getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(List<String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    public void setAltLabels(List<String> altLabels) {
        this.altLabels = altLabels;
    }

    public void setDefinitions(List<String> definitions) {
        this.definitions = definitions;
    }

    public List<String> getAltLabels() {
        return altLabels;
    }

    public List<String> getDefinitions() {
        return definitions;
    }

}
