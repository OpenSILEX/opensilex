/*******************************************************************************
 *                         DatatypePropertyModel.java
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

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author vmigot
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "DatatypeProperty",
        ignoreValidation = true
)
public class DatatypePropertyModel extends AbstractPropertyModel<DatatypePropertyModel> {

    @SPARQLIgnore()
    protected String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf",
            inverse = true
    )
    protected List<DatatypePropertyModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf"
    )
    protected DatatypePropertyModel parent;

    protected Set<DatatypePropertyModel> parents;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "range"
    )
    protected URI range;

    public DatatypePropertyModel() {
        children = new LinkedList<>();
        setChildren(children);
        parents = new HashSet<>();
    }

    public URI getRange() {
        return range;
    }

    public void setRange(URI range) {
        this.range = range;
    }

    @Override
    public Set<DatatypePropertyModel> getParents() {
        return parents;
    }

    @Override
    public void setParents(Set<DatatypePropertyModel> parents) {
        this.parents = parents;
    }

    @Override
    public URI getRangeURI() {
        return range;
    }
}
