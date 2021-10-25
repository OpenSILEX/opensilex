/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.factor.dal;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * @author Arnaud Charleroy
 A simple model which define an instance of the FactorCategoryModel class
 */
@SPARQLResource(
        ontology = OWL.class,
        resource = "Class",
        graph = "http://www.opensilex.org/vocabulary/set/factor/category"
)
public class FactorCategoryModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
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
