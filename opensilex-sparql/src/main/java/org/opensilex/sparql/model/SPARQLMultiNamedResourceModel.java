package org.opensilex.sparql.model;

import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.util.List;

public class SPARQLMultiNamedResourceModel <T extends SPARQLMultiNamedResourceModel> extends SPARQLResourceModel implements ClassURIGenerator<T> {

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
    public static final String NAME_FIELD = "name";


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

    @Override
    public String[] getInstancePathSegments(T instance) {
        return new String[]{

//                instance.getName()
        };
    }
}
