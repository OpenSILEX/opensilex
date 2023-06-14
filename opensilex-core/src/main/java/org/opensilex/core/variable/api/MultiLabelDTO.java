package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonPropertyOrder({"prefLabels", "altLabels", "definitions"})
public class MultiLabelDTO {

    @JsonProperty("prefLabels")
    protected Map<String,String> prefLabels;

    @JsonProperty("altLabels")
    protected Map<String,List<String>> altLabels;

    @JsonProperty("definitions")
    protected Map<String,String> definitions;

    public MultiLabelDTO(){

        this.prefLabels = new HashMap<>();
        this.altLabels = new HashMap<>();
        this.definitions = new HashMap<>();

    }

    public Map<String,String> getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(Map<String,String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    public void setAltLabels(Map<String,List<String>> altLabels) {
        this.altLabels = altLabels;
    }

    public void setDefinitions(Map<String,String> definitions) {
        this.definitions = definitions;
    }

    public Map<String,List<String>> getAltLabels() {
        return altLabels;
    }

    public Map<String,String> getDefinitions() {
        return definitions;
    }

}
