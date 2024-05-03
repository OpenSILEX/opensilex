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
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.VocabularyModel;

import java.net.URI;
import java.util.*;

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

    protected Map<URI, DatatypePropertyModel> datatypeProperties = new HashMap<>();
    protected Map<URI, ObjectPropertyModel> objectProperties = new HashMap<>();
    protected Map<URI, OwlRestrictionModel> restrictionsByProperties = new HashMap<>();

    public ClassModel() {
        super();
        children = new LinkedList<>();
        super.children = children;
        parents = new HashSet<>();
    }

    public ClassModel(ClassModel other) {

        uri = other.getUri();
        rdfType = other.getType();
        label = other.getLabel();
        comment = other.getComment();
        rdfTypeName = other.getTypeLabel();
        if (Objects.nonNull(other.getPublisher())){
            publisher = other.getPublisher();
        }
        if (Objects.nonNull(other.getPublicationDate())){
            publicationDate = other.getPublicationDate();
        }
        if (Objects.nonNull(other.getLastUpdateDate())){
            lastUpdateDate = other.getLastUpdateDate();
        }

        children = other.getChildren();
        setChildren(children);
        parent = other.getParent();
        setParent(parent);
        parents = other.getParents();

        datatypeProperties = other.getDatatypeProperties();
        objectProperties = other.getObjectProperties();
        restrictionsByProperties = other.getRestrictionsByProperties();
    }


    @Override
    public SPARQLLabel getLabel() {
        return label;
    }

    @Override
    public void setLabel(SPARQLLabel label) {
        this.label = label;
    }

    @Override
    public SPARQLLabel getComment() {
        return comment;
    }

    @Override
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

    public Map<URI, OwlRestrictionModel> getRestrictionsByProperties() {
        return restrictionsByProperties;
    }

    public void setRestrictionsByProperties(Map<URI, OwlRestrictionModel> restrictionsByProperties) {
        this.restrictionsByProperties = restrictionsByProperties;
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

    /**
     *
     * @param restriction restriction to check,
     * @return true if the restriction is inherited from parent class, false else
     *
     * @apiNote
     * <ul>
     *     <li>Use {@link OwlRestrictionModel#getOnProperty()} to check if the restriction concerns this class</li>
     *     <li>Use {@link OwlRestrictionModel#getDomain()}  to check if the restriction concerns only this class and it's descendant </li>
     * </ul>
     */
    public boolean isInherited(OwlRestrictionModel restriction) {
        if (!restrictionsByProperties.containsKey(restriction.getOnProperty())) {
            return false;
        }
        //  restriction is inherited if the restriction has a different domain than this Class URI
        return !SPARQLDeserializers.compareURIs(uri, restriction.getDomain().getUri());
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
