/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.factor.dal;

import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * @author Arnaud Charleroy
 A simple model which define an instance of the FactorCategorySKOSModel class
 */
@SPARQLResource(
        ontology = SKOS.class,
        resource = "Concept",
        graph = "http://aims.fao.org/aos/agrovoc/factors"
)
public class FactorCategorySKOSModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "prefLabel",
            ignoreUpdateIfNull = true,
            required = true
    )
    protected String name;
    public static final String NAME_FIELD = "name";

     public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
