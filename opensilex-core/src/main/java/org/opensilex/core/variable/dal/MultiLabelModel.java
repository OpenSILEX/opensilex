package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiLabelModel<T extends SPARQLNamedResourceModel<T>> extends SPARQLNamedResourceModel<T> {

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "prefLabel"
    )
    protected List<String> prefLabels;
    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    private List<String> altsLabels;


    @SPARQLProperty(
            ontology = SKOS.class,
            property = "definition"
    )
    private List<String> definitions;

    public List<String> getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(List<String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    public List<String> getAltsLabels() {
        return altsLabels;
    }

    public void setAltsLabels(List<String> altsLabels) {
        this.altsLabels = altsLabels;
    }

    public List<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<String> definitions) {
        this.definitions = definitions;
    }
    public MultiLabelModel(){
        this.prefLabels = new ArrayList<>();
        this.altsLabels = new ArrayList<>();
        this.definitions = new ArrayList<>();
    }
}
