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
import org.opensilex.sparql.model.VocabularyModel;

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
public class ClassModel extends SPARQLTreeModel<ClassModel> implements VocabularyModel<ClassModel> {

    @SPARQLIgnore()
    protected String name;

//    @SPARQLProperty(
//            ontology = RDFS.class,
//            property = "label"
//    )
    protected SPARQLLabel label;
//    public static final String NAME_FIELD = "label";
//
//    @SPARQLProperty(
//            ontology = RDFS.class,
//            property = "comment"
//    )
    protected SPARQLLabel comment;
//    public static final String COMMENT_FIELD = "comment";

//    @SPARQLProperty(
//            ontology = RDFS.class,
//            property = "subClassOf",
//            inverse = true
//    )
//    protected List<ClassModel> children;

//    @SPARQLProperty(
//            ontology = RDFS.class,
//            property = "subClassOf"
//    )
//    protected ClassModel parent;

    protected Set<ClassModel> parents;

    public ClassModel(){
        super();
        children = new LinkedList<>();
        parents = new HashSet<>();
    }

    public ClassModel(ClassModel other){

        uri = other.getUri();
        rdfType = other.getType();

        if(other.getLabel() != null){
            label = new SPARQLLabel(other.getLabel());
        }
        if(other.getComment() != null){
            comment = new SPARQLLabel(other.getComment());
        }
        if(other.getTypeLabel() != null){
            rdfTypeName = new SPARQLLabel(other.getTypeLabel());
        }

        setChildren(other.getChildren());
        setParent(other.getParent());
        setParents(other.getParents());

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

        this();

        uri = classModel.getUri();
        if(classModel.getLabel() != null){
            label = new SPARQLLabel(classModel.getLabel());
        }
        if(classModel.getComment() != null){
            comment = new SPARQLLabel(classModel.getComment());
        }
        rdfType = classModel.getType();
        if(classModel.getTypeLabel() != null){
            rdfTypeName = new SPARQLLabel(classModel.getTypeLabel());
        }

        if(readChildren && classModel.getChildren() != null){
            children = classModel.getChildren().stream()
                    .map(child -> new ClassModel(child,true))
                    .collect(Collectors.toList());

            children.forEach(child -> setParent(this));

            // call super setter in order to ensure that {@link SPARQLTreeModel#children} field is set
            setChildren(children);
        }



        if(classModel.getParent() != null){
            this.parent = new ClassModel(classModel.getParent(),false);
        }

        // call super setter in order to ensure that {@link SPARQLTreeModel#parent} field is set
        setParent(parent);

        datatypeProperties = classModel.getDatatypeProperties();
        objectProperties = classModel.getObjectProperties();
        restrictions = classModel.getRestrictions();
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

    public Set<ClassModel> getParents() {
        return parents;
    }

    public void setParents(Set<ClassModel> parents) {
        this.parents = parents;
    }
}
