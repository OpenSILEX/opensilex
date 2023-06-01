package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.SKOSReferences;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.util.List;


public abstract class LabelModel<T extends SPARQLNamedResourceModel<T>> extends SPARQLNamedResourceModel<T> implements SKOSReferences {

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "prefLabel"
    )
    private String prefLabel;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    private List<String> altLabels;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "definition"
    )
    private String definition;


    protected LabelModel() {
    }


}