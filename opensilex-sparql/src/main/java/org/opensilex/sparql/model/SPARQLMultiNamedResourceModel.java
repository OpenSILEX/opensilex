package org.opensilex.sparql.model;

import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.util.List;
import java.util.Map;

public class SPARQLMultiNamedResourceModel <T extends SPARQLMultiNamedResourceModel> extends SPARQLResourceModel implements ClassURIGenerator<T> {

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
    public static final String NAME_FIELD = "name";


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
    public String[] getInstancePathSegments(T instance) {
        return new String[]{

//                instance.getName()
        };
    }
}
