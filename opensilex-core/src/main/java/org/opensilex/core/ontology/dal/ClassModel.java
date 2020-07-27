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
        ontology = OWL2.class,
        resource = "Class",
        ignoreValidation = true
)
public class ClassModel<T extends ClassModel> extends SPARQLTreeModel<T> {

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

    @SPARQLProperty(
            ontology = OpenSilexOwlExtension.class,
            property = "isAbstractClass"
    )
    protected Boolean isAbstractClass;

    protected Map<URI, PropertyModel> datatypeProperties;

    protected Map<URI, PropertyModel> objectProperties;

    protected Map<URI, OwlRestrictionModel> restrictions;

    protected Map<URI, ClassPropertyExtensionModel> propertyExtensions;

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

    public Map<URI, PropertyModel> getDatatypeProperties() {
        return datatypeProperties;
    }

    public void setDatatypeProperties(Map<URI, PropertyModel> datatypeProperties) {
        this.datatypeProperties = datatypeProperties;
    }

    public Map<URI, PropertyModel> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(Map<URI, PropertyModel> objectProperties) {
        this.objectProperties = objectProperties;
    }

    public Map<URI, OwlRestrictionModel> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(Map<URI, OwlRestrictionModel> restrictions) {
        this.restrictions = restrictions;
    }

    public List<OwlRestrictionModel> getOrderedRestrictions() {
        Map<URI, ClassPropertyExtensionModel> extensions = getPropertyExtensions();

        return restrictions.values().stream().sorted((r1, r2) -> {
            if (extensions == null) {
                return 0;
            }

            ClassPropertyExtensionModel ext1 = extensions.get(r1.getOnProperty());
            ClassPropertyExtensionModel ext2 = extensions.get(r2.getOnProperty());

            Integer o1 = null;
            Integer o2 = null;
            if (ext1 != null) {
                o1 = ext1.getHasDisplayOrder();
            }

            if (ext2 != null) {
                o2 = ext2.getHasDisplayOrder();
            }

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

    public PropertyModel getDatatypeProperty(URI propertyURI) {
        return getDatatypeProperties().get(propertyURI);
    }

    public PropertyModel getObjectProperty(URI propertyURI) {
        return getObjectProperties().get(propertyURI);
    }

    public Map<URI, ClassPropertyExtensionModel> getPropertyExtensions() {
        return propertyExtensions;
    }

    public void setPropertyExtensions(Map<URI, ClassPropertyExtensionModel> propertyExtensions) {
        this.propertyExtensions = propertyExtensions;
    }

    public Boolean getIsAbstractClass() {
        return isAbstractClass;
    }

    public void setIsAbstractClass(Boolean isAbstractClass) {
        this.isAbstractClass = isAbstractClass;
    }
}
