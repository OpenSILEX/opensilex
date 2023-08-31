package org.opensilex.sparql.model;

import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.ontology.OesoSparql;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.util.List;
import java.util.Map;

public class SPARQLMultiNamedResourceModel <T extends SPARQLMultiNamedResourceModel> extends SPARQLResourceModel implements ClassURIGenerator<T> {

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "prefLabel"
    )
    protected SPARQLLabel prefLabels;
    public static final String PREF_LABELS_FIELD = "prefLabels";

    @SPARQLProperty(
            ontology = OesoSparql.class,
            property = "shortLabel"
    )
    protected SPARQLLabel shortLabels;

    public static final String SHORT_LABEL_FIELD = "shortLabels";

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    private SPARQLMultiLabels altsLabels;
    public static final String ALT_LABELS_FIELD = "altsLabels";


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

    public SPARQLLabel getShortLabels() {
        return shortLabels;
    }

    public void setShortLabels(SPARQLLabel shortLabels) {
        this.shortLabels = shortLabels;
    }

    @Override
    public String[] getInstancePathSegments(T instance) {
        return new String[]{

//                instance.getName()
        };
    }
}
