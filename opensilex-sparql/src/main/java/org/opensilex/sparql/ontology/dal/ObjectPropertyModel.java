/*******************************************************************************
 *                         ObjectPropertyModel.java
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
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author vmigot
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "ObjectProperty",
        ignoreValidation = true
)
public class ObjectPropertyModel extends AbstractPropertyModel<ObjectPropertyModel>{

    @SPARQLIgnore()
    protected String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf",
            inverse = true
    )
    protected List<ObjectPropertyModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf"
    )
    protected ObjectPropertyModel parent;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "domain"
    )
    protected ClassModel domain;
    public final static String DOMAIN_FIELD = "domain";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "range"
    )
    protected ClassModel range;
    public final static String RANGE_FIELD = "range";

    protected URI typeRestriction;

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        SPARQLLabel slabel = getLabel();
        if (slabel != null) {
            return getLabel().getDefaultValue();
        } else {
            return getUri().toString();
        }
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public ClassModel getDomain() {
        return domain;
    }

    public void setDomain(ClassModel domain) {
        this.domain = domain;
    }

    public ClassModel getRange() {
        return range;
    }

    public void setRange(ClassModel range) {
        this.range = range;
    }

    public URI getTypeRestriction() {
        return typeRestriction;
    }

    public void setTypeRestriction(URI typeRestriction) {
        this.typeRestriction = typeRestriction;
    }

    public ObjectPropertyModel() {
    }

//    public ObjectPropertyModel(ObjectPropertyModel other) {
//        super(other);
//    }


}
