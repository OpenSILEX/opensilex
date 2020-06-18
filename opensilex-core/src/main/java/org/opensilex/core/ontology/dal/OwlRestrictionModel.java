/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import org.apache.jena.vocabulary.OWL;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OWL.class,
        resource = "Restriction",
        ignoreValidation = true
)
public class OwlRestrictionModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = OWL.class,
            property = "onProperty",
            required = true
    )
    PropertyModel onProperty;

    @SPARQLProperty(
            ontology = OWL.class,
            property = "minCardinality"
    )
    Integer minCardinality;

    @SPARQLProperty(
            ontology = OWL.class,
            property = "maxCardinality"
    )
    Integer maxCardinality;

    @SPARQLProperty(
            ontology = OWL.class,
            property = "cardinality"
    )
    Integer cardinality;

    public PropertyModel getOnProperty() {
        return onProperty;
    }

    public void setOnProperty(PropertyModel onProperty) {
        this.onProperty = onProperty;
    }

    public Integer getMinCardinality() {
        return minCardinality;
    }

    public void setMinCardinality(Integer minCardinality) {
        this.minCardinality = minCardinality;
    }

    public Integer getMaxCardinality() {
        return maxCardinality;
    }

    public void setMaxCardinality(Integer maxCardinality) {
        this.maxCardinality = maxCardinality;
    }

    public Integer getCardinality() {
        return cardinality;
    }

    public void setCardinality(Integer cardinality) {
        this.cardinality = cardinality;
    }

}
