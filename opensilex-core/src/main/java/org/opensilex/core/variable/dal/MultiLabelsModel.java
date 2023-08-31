package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLMultiLabels;
import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
import org.opensilex.sparql.ontology.OesoSparql;

public class MultiLabelsModel<T extends SPARQLMultiNamedResourceModel<T>> extends SPARQLMultiNamedResourceModel<T> {

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "prefLabel"
    )
    protected SPARQLLabel prefLabels;

    @SPARQLProperty(
            ontology = OesoSparql.class,
            property = "shortLabel"
    )
    protected SPARQLLabel shortLabels;


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

    @Override
    public SPARQLLabel getShortLabels() {
        return shortLabels;
    }

    @Override
    public void setShortLabels(SPARQLLabel shortLabel) {
        this.shortLabels = shortLabel;
    }

    public MultiLabelsModel(){
        this.prefLabels = new SPARQLLabel();
        this.shortLabels = new SPARQLLabel();
        this.altsLabels = new SPARQLMultiLabels();
        this.definitions = new SPARQLLabel();

    }



}
