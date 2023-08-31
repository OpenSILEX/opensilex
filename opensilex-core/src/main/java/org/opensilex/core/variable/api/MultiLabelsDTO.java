package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.variable.dal.VariableModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonPropertyOrder({"prefLabels", "shortLabels", "altLabels", "definitions"})
public class MultiLabelsDTO {


    @JsonProperty("prefLabels")
    protected Map<String,String> prefLabels;

    @JsonProperty("shortLabels")
    protected Map<String,String> shortLabels;

    @JsonProperty("altLabels")
    protected Map<String,List<String>> altLabels;

    @JsonProperty("definitions")
    protected Map<String,String> definitions;



    public MultiLabelsDTO(){

        this.prefLabels = new HashMap<>();
        this.altLabels = new HashMap<>();
        this.definitions = new HashMap<>();
        this.shortLabels = new HashMap<>();

    }

    public MultiLabelsDTO(VariableModel model){

        this.prefLabels = model.getPrefLabels().getAllTranslations();
        this.shortLabels = model.getShortLabels().getAllTranslations();
        this.altLabels = model.getAltsLabels().getTranslations();
        this.definitions = model.getDefinitions().getAllTranslations();

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
