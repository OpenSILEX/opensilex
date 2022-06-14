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

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subClassOf",
            required = true,
            inverse = true
    )
    ClassModel domain;
    public static final String DOMAIN_FIELD = "domain";

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
            ontology = OWL2.class,
            property = ON_CLASS
    )
    URI onClass;
    public static final String ON_CLASS = "onClass";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "minQualifiedCardinality"
    )
    Integer minQualifiedCardinality;
    public static final String MIN_CARDINALITY_FIELD = "minQualifiedCardinality";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "maxQualifiedCardinality"
    )
    Integer maxQualifiedCardinality;
    public static final String MAX_CARDINALITY_FIELD = "maxQualifiedCardinality";

    @SPARQLProperty(
            ontology = OWL2.class,
            property = "qualifiedCardinality"
    )
    Integer qualifiedCardinality;
    public static final String CARDINALITY_FIELD = "qualifiedCardinality";

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

    public URI getOnClass() {
        return onClass;
    }

    public void setOnClass(URI onClass) {
        this.onClass = onClass;
    }

    public Integer getMinQualifiedCardinality() {
        return minQualifiedCardinality;
    }

    public void setMinQualifiedCardinality(Integer minQualifiedCardinality) {
        this.minQualifiedCardinality = minQualifiedCardinality;
    }

    public Integer getMaxQualifiedCardinality() {
        return maxQualifiedCardinality;
    }

    public void setMaxQualifiedCardinality(Integer maxQualifiedCardinality) {
        this.maxQualifiedCardinality = maxQualifiedCardinality;
    }

    public Integer getQualifiedCardinality() {
        return qualifiedCardinality;
    }

    public void setQualifiedCardinality(Integer qualifiedCardinality) {
        this.qualifiedCardinality = qualifiedCardinality;
    }

    public URI getSomeValuesFrom() {
        return someValuesFrom;
    }

    public void setSomeValuesFrom(URI someValuesFrom) {
        this.someValuesFrom = someValuesFrom;
    }

    public ClassModel getDomain() {
        return domain;
    }

    public void setDomain(ClassModel domain) {
        this.domain = domain;
    }

    public boolean isList() {

        // [0,*] cardinality -> list
        if(getMinQualifiedCardinality() == null && getMaxQualifiedCardinality() == null){
            return true;
        }

        // [n] with n > 1
        if (getQualifiedCardinality() != null) {
            return getQualifiedCardinality() > 1;
        }

        // [?,n] with n > 1
        if (getMaxQualifiedCardinality() != null) {
            return getMaxQualifiedCardinality() > 1;
        }

        // [2,?]
        if (getMinQualifiedCardinality() != null) {
            return getMinQualifiedCardinality() > 1 || getMaxQualifiedCardinality() == null;
        }

        return getSomeValuesFrom() != null;
    }

    public boolean isOptional() {
        if (getMinQualifiedCardinality() != null) {
            return getMinQualifiedCardinality() == 0;
        }

        return !isRequired();
    }

    public boolean isRequired() {
        if (getQualifiedCardinality() != null) {
            return getQualifiedCardinality() >= 1;
        }
        if (getMinQualifiedCardinality() != null) {
            return getMinQualifiedCardinality() >= 1;
        }

        return false;
    }

    public URI getSubjectURI() {
        if (getOnDataRange() != null) {
            return getOnDataRange();
        }
        if (getOnClass() != null) {
            return getOnClass();
        }
        if (getSomeValuesFrom() != null) {
            return getSomeValuesFrom();
        }

        return null;
    }
}
