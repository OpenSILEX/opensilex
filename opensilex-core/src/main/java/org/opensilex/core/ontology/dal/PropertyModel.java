/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.OpenSilexOwlExtension;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = RDF.class,
        resource = "Property",
        ignoreValidation = true
)
public abstract class PropertyModel extends SPARQLTreeModel<PropertyModel> {

    @SPARQLIgnore()
    protected String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label"
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
    protected List<PropertyModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf"
    )
    protected PropertyModel parent;

    protected URI typeRestriction;

    protected boolean isDatatypeProperty;

    protected boolean isObjectProperty;

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

    public URI getTypeRestriction() {
        return typeRestriction;
    }

    public void setTypeRestriction(URI typeRestriction) {
        this.typeRestriction = typeRestriction;
    }

    public boolean isIsDatatypeProperty() {
        return isDatatypeProperty;
    }

    public void setIsDatatypeProperty(boolean isDatatypeProperty) {
        this.isDatatypeProperty = isDatatypeProperty;
    }

    public boolean isIsObjectProperty() {
        return isObjectProperty;
    }

    public void setIsObjectProperty(boolean isObjectProperty) {
        this.isObjectProperty = isObjectProperty;
    }

}
