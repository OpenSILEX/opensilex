package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.variable.dal.VariableModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@JsonPropertyOrder({"prefLabel", "shortLabels", "altLabels", "definition"})
public class LabelDTO {


    @JsonProperty("prefLabel")
    protected Map<String,String> prefLabels;

    @JsonProperty("shortLabel")
    protected Map<String,String> shortLabels;

    @JsonProperty("altLabels")
    protected Map<String, List<String>> altLabels;

    @JsonProperty("definition")
    protected Map<String,String> definitions;

    public LabelDTO(){

        this.prefLabels = new HashMap<>();
        this.shortLabels = new HashMap<>();
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

    public Map<String, String> getShortLabels() {
        return shortLabels;
    }

    public void setShortLabels(Map<String, String> shortLabels) {
        this.shortLabels = shortLabels;
    }
}
