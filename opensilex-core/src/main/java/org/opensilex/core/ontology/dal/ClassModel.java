/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
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
    public static final String NAME_FIELD = "label";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    protected SPARQLLabel comment;
    public static final String COMMENT_FIELD = "comment";

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

    public List<DatatypePropertyModel> getDatatypePropertiesWithDomain(){
        List<DatatypePropertyModel> properties = new ArrayList<>();
        this.visit(model -> properties.addAll(model.getDatatypeProperties().values()));
        return properties;
    }

    public void setDatatypeProperties(Map<URI, DatatypePropertyModel> datatypeProperties) {
        this.datatypeProperties = datatypeProperties;
    }

    public Map<URI, ObjectPropertyModel> getObjectProperties() {
        return objectProperties;
    }

    public List<ObjectPropertyModel> getObjectPropertiesWithDomain(){
        List<ObjectPropertyModel> properties = new ArrayList<>();
        this.visit(model -> properties.addAll(model.getObjectProperties().values()));
        return properties;
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
//
//    public ClassModel(ClassModel other) throws URISyntaxException {
//        this(other,null,true);
//
//        if(other.getParent() != null){
//            // copy parent from other ClassModel
//            this.parent = new ClassModel(other.getParent(),null,false);
//            if(CollectionUtils.isEmpty(this.parent.children)){
//                this.parent.children = new LinkedList<>();
//                this.parent.children.add(this);
//            }
//        }
//    }
//
//    protected ClassModel(ClassModel other, ClassModel parent, boolean copyParentChildren) throws URISyntaxException {
//        super(other);
//
//        // dont copy name, since this field is just here to override the super SPARQLNamedResourceModel.name field
//
//        if(other.getComment() != null){
//            comment = new SPARQLLabel(other.getComment());
//        }
//        if(other.getLabel() != null){
//            label = new SPARQLLabel(other.getLabel());
//        }
//
//        if(parent != null){
//            this.parent = parent;
//        }
//
//        List<ClassModel> otherChildren = other.getChildren();
//        if(copyParentChildren && !CollectionUtils.isEmpty(otherChildren)){
//            children = new ArrayList<>(otherChildren.size());
//            for(ClassModel otherChild : otherChildren){
//                children.add(new ClassModel(otherChild,this,true));
//            }
//        }
//
//        if(!MapUtils.isEmpty(other.getDatatypeProperties())){
//            datatypeProperties = new HashMap<>(other.getDatatypeProperties());
//        }
//        if(!MapUtils.isEmpty(other.getObjectProperties())){
//            objectProperties = new HashMap<>(other.getObjectProperties());
//        }
//        if(!MapUtils.isEmpty(other.getRestrictions())){
//            restrictions = new HashMap<>(other.getRestrictions());
//        }
//    }
}
