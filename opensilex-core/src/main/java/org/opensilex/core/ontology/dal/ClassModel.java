/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
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
        ontology = OWL2.class,
        resource = "Class",
        ignoreValidation = true
)
public class ClassModel extends SPARQLTreeModel<ClassModel> {

    @SPARQLIgnore()
    protected String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label"
    )
    protected SPARQLLabel label;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    protected SPARQLLabel comment;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subClassOf",
            inverse = true
    )
    protected List<ClassModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subClassOf"
    )
    protected ClassModel parent;

    protected Map<URI, DatatypePropertyModel> datatypeProperties;

    protected Map<URI, ObjectPropertyModel> objectProperties;

    protected List<OwlRestrictionModel> restrictions;

    @Override
    public String getName() {
        SPARQLLabel slabel = getLabel();
        if (slabel != null) {
            return getLabel().getDefaultValue();
        } else {
            return getUri().toString();
        }
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

    public Map<URI, DatatypePropertyModel> getDatatypeProperties() {
        return datatypeProperties;
    }

    public void setDatatypeProperties(Map<URI, DatatypePropertyModel> datatypeProperties) {
        this.datatypeProperties = datatypeProperties;
    }

    public Map<URI, ObjectPropertyModel> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(Map<URI, ObjectPropertyModel> objectProperties) {
        this.objectProperties = objectProperties;
    }

    public List<OwlRestrictionModel> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List< OwlRestrictionModel> restrictions) {
        this.restrictions = restrictions;
    }

    public void forEachPropertyRestriction(BiConsumer<PropertyModel, OwlRestrictionModel> lambda) {
        for (OwlRestrictionModel restriction : getRestrictions()) {
            if (restriction.isDatatypePropertyRestriction()) {
                DatatypePropertyModel property = getDatatypeProperties().get(restriction.getOnProperty());
                lambda.accept(property, restriction);
            } else if (restriction.isObjectPropertyRestriction()) {
                ObjectPropertyModel property = getObjectProperties().get(restriction.getOnClass());
                lambda.accept(property, restriction);
            }
        }
    }

}
