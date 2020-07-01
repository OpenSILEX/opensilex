/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "Restriction",
        ignoreValidation = true,
        allowBlankNode = true
)
public class OwlRestrictionModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "onProperty"
    )
    URI onProperty;

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "onDataRange"
    )
    URI onDataRange;

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "onClass"
    )
    URI onClass;

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "minQualifiedCardinality"
    )
    Integer minCardinality;

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "maxQualifiedCardinality"
    )
    Integer maxCardinality;

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "qualifiedCardinality"
    )
    Integer cardinality;

    public URI getOnProperty() {
        return onProperty;
    }

    public void setOnProperty(URI onProperty) {
        this.onProperty = onProperty;
    }

    public URI getOnDataRange() {
        return onDataRange;
    }

    public void setOnDataRange(URI onDataRange) {
        this.onDataRange = onDataRange;
    }

    public URI getOnClass() {
        return onClass;
    }

    public void setOnClass(URI onClass) {
        this.onClass = onClass;
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

    public boolean isList() {
        return getCardinality() > 1 || getMaxCardinality() > 1 || getMinCardinality() > 1;
    }

    public boolean isOptional() {
        return getMinCardinality() == 0;
    }

    public boolean isRequired() {
        return getCardinality() >= 1 || getMinCardinality() >= 1;
    }

    public boolean isDatatypePropertyRestriction() {
        return this.getOnDataRange() != null;
    }

    public boolean isObjectPropertyRestriction() {
        return this.getOnClass() != null;
    }
}
