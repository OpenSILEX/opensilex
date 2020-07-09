/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    protected Map<URI, OwlRestrictionModel> restrictions;

    protected Map<URI, Integer> propertiesOrder;
    
    protected boolean abstractClass;

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

    public List<OwlRestrictionModel> getOrderedRestrictions() {
        return restrictions.values().stream().sorted((r1, r2) -> {
            if (propertiesOrder == null) {
                return 0;
            }
            
            Integer o1 = propertiesOrder.get(r1.getOnProperty());
            Integer o2 = propertiesOrder.get(r2.getOnProperty());
            
            if (o1 == null) {
                if (o2 != null) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                if (o2 == null) {
                    return -1;
                } else {
                    return o1.compareTo(o2);
                }
            }
            
        }).collect(Collectors.toList());
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

    public DatatypePropertyModel getDatatypeProperty(URI propertyURI) {
        return getDatatypeProperties().get(propertyURI);
    }

    public ObjectPropertyModel getObjectProperty(URI propertyURI) {
        return getObjectProperties().get(propertyURI);
    }

    public Map<URI, Integer> getPropertiesOrder() {
        return propertiesOrder;
    }

    public void setPropertiesOrder(Map<URI, Integer> propertiesOrder) {
        this.propertiesOrder = propertiesOrder;
    }

    public boolean isAbstractClass() {
        return abstractClass;
    }

    public void setAbstractClass(boolean abstractClass) {
        this.abstractClass = abstractClass;
    }

    
}
