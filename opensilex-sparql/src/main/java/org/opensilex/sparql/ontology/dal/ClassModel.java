/*******************************************************************************
 *                         ClassModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.dal;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.model.VocabularyModel;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author vince
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "Class",
        ignoreValidation = true
)
public class ClassModel extends VocabularyModel<ClassModel> {

    @SPARQLIgnore()
    protected String name;

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

    protected Set<ClassModel> parents;

    public ClassModel() {
        super();
        children = new LinkedList<>();
        parents = new HashSet<>();
    }

    public ClassModel(ClassModel other) {

        uri = other.getUri();
        rdfType = other.getType();
        label = other.getLabel();
        comment = other.getComment();
        rdfTypeName = other.getTypeLabel();

        children = other.getChildren();
        parent = other.getParent();
        parents = other.getParents();

        datatypeProperties = other.getDatatypeProperties();
        objectProperties = other.getObjectProperties();
        restrictions = other.getRestrictions();
    }

    /**
     * Copy constructor
     *
     * @param classModel   class to copy
     * @param readChildren flag which indicate if this constructor must iterate over classModel children
     */
    public ClassModel(ClassModel classModel, boolean readChildren) {
    }


    protected Map<URI, DatatypePropertyModel> datatypeProperties = new HashMap<>();
    protected Map<URI, ObjectPropertyModel> objectProperties = new HashMap<>();
    protected Map<URI, OwlRestrictionModel> restrictions = new HashMap<>();


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

    public Map<URI, OwlRestrictionModel> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Map<URI, OwlRestrictionModel> restrictions) {
        this.restrictions = restrictions;
    }

    public boolean isDatatypePropertyRestriction(URI datatype) {
        if (datatype == null) {
            return false;
        }

        return getDatatypeProperties().containsKey(datatype);
    }

    public boolean isObjectPropertyRestriction(URI classURI) {
        if (classURI == null) {
            return false;
        }

        return getObjectProperties().containsKey(classURI);
    }

    public PropertyModel getDatatypeProperty(URI propertyURI) {
        return getDatatypeProperties().get(propertyURI);
    }

    public PropertyModel getObjectProperty(URI propertyURI) {
        return getObjectProperties().get(propertyURI);
    }

    @Override
    public List<ClassModel> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<ClassModel> children) {
        this.children = children;
    }

    @Override
    public ClassModel getParent() {
        return parent;
    }

    @Override
    public void setParent(ClassModel parent) {
        this.parent = parent;
    }

    public Set<ClassModel> getParents() {
        return parents;
    }

    public void setParents(Set<ClassModel> parents) {
        this.parents = parents;
    }
}
