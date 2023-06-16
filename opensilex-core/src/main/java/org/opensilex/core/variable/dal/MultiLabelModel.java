package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLMultiLabels;
import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiLabelModel<T extends SPARQLMultiNamedResourceModel<T>> extends SPARQLMultiNamedResourceModel<T> {



    @SPARQLProperty(
            ontology = SKOS.class,
            property = "prefLabel"
    )
    protected SPARQLLabel prefLabels;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    private SPARQLMultiLabels altsLabels;


    @SPARQLProperty(
            ontology = SKOS.class,
            property = "definition"
    )
    private SPARQLLabel definitions;

    public SPARQLLabel getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(SPARQLLabel prefLabels) {
        this.prefLabels = prefLabels;
    }

    public SPARQLMultiLabels getAltsLabels() {
        return altsLabels;
    }

    public void setAltsLabels(SPARQLMultiLabels altsLabels) {
        this.altsLabels = altsLabels;
    }

    public SPARQLLabel getDefinitions() {
        return definitions;
    }

    public void setDefinitions(SPARQLLabel definitions) {
        this.definitions = definitions;
    }

    public MultiLabelModel(){
        this.prefLabels = new SPARQLLabel();
        this.altsLabels = new SPARQLMultiLabels();
        this.definitions = new SPARQLLabel();

    }
}
