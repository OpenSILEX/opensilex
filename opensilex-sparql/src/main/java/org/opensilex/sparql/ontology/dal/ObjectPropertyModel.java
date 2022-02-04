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
public class ObjectPropertyModel extends SPARQLTreeModel<ObjectPropertyModel> implements PropertyModel {

    @SPARQLIgnore()
    protected String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    protected SPARQLLabel label;
    public final static String LABEL_FIELD = "label";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    protected SPARQLLabel comment;

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

    public SPARQLLabel getLabel() {
        return label;
    }

    public void setLabel(SPARQLLabel label) {
        this.label = label;
    }

    public SPARQLLabel getComment() {
        return comment;
    }

    public void setComment(SPARQLLabel comment) {
        this.comment = comment;
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

    public ObjectPropertyModel(ObjectPropertyModel other) {
        this(other, true);
    }

    public ObjectPropertyModel(ObjectPropertyModel other, boolean readChildren) {
        fromModel(other);
        range = other.getRange();

        if (readChildren && other.getChildren() != null) {
            children = other.getChildren().stream()
                    .map(child -> new ObjectPropertyModel(child, true))
                    .collect(Collectors.toList());

            children.forEach(child -> setParent(this));

            // call super setter in order to ensure that {@link SPARQLTreeModel#children} field is set
            setChildren(children);
        }

        if (other.getParent() != null) {
            parent = new ObjectPropertyModel(other.getParent(), false);
            setParent(parent);
        }
    }

}
