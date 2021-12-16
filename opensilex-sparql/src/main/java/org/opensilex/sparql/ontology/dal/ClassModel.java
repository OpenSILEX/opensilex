/*******************************************************************************
 *                         ClassModel.java
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
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "Class",
        ignoreValidation = true
)
public class ClassModel extends SPARQLTranslatedTreeModel<ClassModel> {

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


    public ClassModel(){
        super();
        children = new LinkedList<>();
    }

    public ClassModel(ClassModel other){
        super(other);

        children = other.getChildren();
        setChildren(children);

        parents = other.getParents();

        datatypeProperties = new HashMap<>(other.getDatatypeProperties());
        objectProperties = new HashMap<>(other.getObjectProperties());
        restrictions= new HashMap<>(other.getRestrictions());
    }

    /**
     * Copy constructor
     * @param classModel class to copy
     * @param readChildren flag which indicate if this constructor must iterate over classModel children
     */
    public ClassModel(ClassModel classModel, boolean readChildren){
    }


    protected Map<URI, DatatypePropertyModel> datatypeProperties = new HashMap<>();
    protected Map<URI, ObjectPropertyModel> objectProperties = new HashMap<>();
    protected Map<URI, OwlRestrictionModel> restrictions = new HashMap<>();

    @Override
    public String getName() {
        SPARQLLabel slabel = getLabel();
        if (slabel != null) {
            return getLabel().getDefaultValue();
        } else {
            return getUri().toString();
        }
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
    public Set<ClassModel> getParents() {
        return parents;
    }

    @Override
    public void setParents(Set<ClassModel> parents) {
        this.parents = parents;
    }
}
