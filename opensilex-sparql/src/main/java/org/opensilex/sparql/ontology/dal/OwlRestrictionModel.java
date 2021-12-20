/*******************************************************************************
 *                         OwlRestrictionModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.ontology.dal;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;

/**
 * @author vince
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "Restriction",
        ignoreValidation = true,
        allowBlankNode = true
)
public class OwlRestrictionModel extends SPARQLResourceModel {

    @SPARQLIgnore
    URI domain;

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "onProperty",
            required = true
    )
    URI onProperty;
    public static final String ON_PROPERTY_FIELD = "onProperty";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "onDataRange"
    )
    URI onDataRange;
    public static final String ON_DATA_RANGE_FIELD = "onDataRange";


    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subClassOf",
            required = true,
            inverse = true
    )
    ClassModel onClass;
    public static final String ON_CLASS_FIELD = "onClass";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "minQualifiedCardinality"
    )
    Integer minCardinality;
    public static final String MIN_CARDINALITY_FIELD = "minCardinality";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "maxQualifiedCardinality"
    )
    Integer maxCardinality;
    public static final String MAX_CARDINALITY_FIELD = "maxCardinality";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "qualifiedCardinality"
    )
    Integer cardinality;
    public static final String CARDINALITY_FIELD = "cardinality";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "someValuesFrom"
    )
    URI someValuesFrom;
    public static final String SOME_VALUES_FROM_FIELD = "someValuesFrom";

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

    public ClassModel getOnClass() {
        return onClass;
    }

    public void setOnClass(ClassModel onClass) {
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

    public URI getSomeValuesFrom() {
        return someValuesFrom;
    }

    public void setSomeValuesFrom(URI someValuesFrom) {
        this.someValuesFrom = someValuesFrom;
    }

    public URI getDomain() {
        return domain;
    }

    public void setDomain(URI domain) {
        this.domain = domain;
    }

    public boolean isList() {
        if (getCardinality() != null) {
            return getCardinality() > 1;
        }
        if (getMaxCardinality() != null) {
            return getMaxCardinality() > 1;
        }
        if (getMinCardinality() != null) {
            return getMinCardinality() > 1 || getMaxCardinality() == null;
        }

        return getSomeValuesFrom() != null;
    }

    public boolean isOptional() {
        if (getMinCardinality() != null) {
            return getMinCardinality() == 0;
        }

        return !isRequired();
    }

    public boolean isRequired() {
        if (getCardinality() != null) {
            return getCardinality() >= 1;
        }
        if (getMinCardinality() != null) {
            return getMinCardinality() >= 1;
        }

        return false;
    }

    public URI getSubjectURI() {
        if (getOnDataRange() != null) {
            return getOnDataRange();
        }
        if (getOnClass() != null) {
            return getOnClass().getUri();
        }
        if (getSomeValuesFrom() != null) {
            return getSomeValuesFrom();
        }

        return null;
    }
}
