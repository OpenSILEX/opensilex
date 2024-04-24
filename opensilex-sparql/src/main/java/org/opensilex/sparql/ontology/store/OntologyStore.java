/*******************************************************************************
 *                         OntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.*;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;

/**
 * @author rcolin
 * Interface which define how to read access primitive to the vocabulary used by OpenSILEX (classes and properties)
 *
 */
public interface OntologyStore {

    /**
     * Load classes and properties from OpenSILEX SPARQL repository
     * @throws SPARQLException if some Error is encountered during SPARQL query evaluation
     */
    void load() throws SPARQLException;

    /**
     * Clear classes and properties
     */
    void clear();

    /**
     * Clear and load the store
     * @throws SPARQLException if some Error is encountered during SPARQL query evaluation
     */
    default void reload() throws SPARQLException {
        clear();
        load();
    }

    /**
     *
     * @param classURI URI of a {@link ClassModel} (required)
     * @param ancestorURI  URI of the classURI ancestor(optional). If set then the store return true if and only if this class is a sub-classOf* of parentClass
     * @return true if a class exist
     * @// TODO: 30/05/2023  subclass check is done in the DefaultOntologyStore but not in the NoOntologyStore (used for tests)
     */
    boolean classExist(URI classURI, URI ancestorURI) throws SPARQLException;

    /**
     *
     * @param classURI URI of a {@link ClassModel} (required)
     * @param ancestorURI  URI of the classURI ancestor(optional). If set then the store return a {@link ClassModel} if and only if this class is a sub-classOf* of parentClass
     * @param lang class name lang filter (only return lang which match with the given lang)
     * @return the {@link ClassModel} which have classURI as URI ({@link SPARQLResourceModel#getUri()})
     * @// TODO: 30/05/2023  subclass check is done in the DefaultOntologyStore but not in the NoOntologyStore (used for tests)
     */
    ClassModel getClassModel(URI classURI, URI ancestorURI, String lang) throws SPARQLException;

    /**
     *
     * @param classURI URI of the OWL class
     * @param includeNestedRestrictions indicate if restrictions from descendant/subclasses must be returned
     * @return the Set of all properties URI which apply on the given class, through OWL restriction
     */
    default Set<String> getOwlRestrictionsUris(URI classURI, boolean includeNestedRestrictions) throws SPARQLException {
        ClassModel classModel = getClassModel(classURI,null,null);

        // include properties which apply on the class
        Set<String> properties = new TreeSet<>();

        classModel.getRestrictionsByProperties().keySet()
                .stream()
                .map(URI::toString)
                .forEach(properties::add);

        // append properties which can apply on all subclasses
        if (includeNestedRestrictions) {

            // visit all descendant with a Breadth-first search
            classModel.visit(descendant ->
                            descendant.getRestrictionsByProperties().keySet()
                                    .stream()
                                    .map(URI::toString)
                                    .forEach(properties::add),
                    false
            );
        }
        return properties;
    }

    /**
     *
     * @param classURI URI of a {@link ClassModel}
     * @param ancestorUri URI of a {@link ClassModel} that is an ancestor of classURI
     * @return an ordered set containing all ancestors of classURI, going from ones that are highest in the hierarchy to ones that are lowest
     */
    LinkedHashSet<String> getAncestorHierarchy(URI classURI, URI ancestorUri);

    /**
     * @param classURI URI of a {@link ClassModel} (required)
     * @param namePattern name regex filter
     * @param lang class name lang filter (only return lang which match with the given lang)
     * @param excludeRoot indicate if the classURI must be included at the root of the {@link SPARQLTreeListModel}
     * @return a tree-representation of all subclasses of the given class URI
     */
    SPARQLTreeListModel<ClassModel> searchSubClasses(URI classURI, String namePattern, String lang, boolean excludeRoot) throws SPARQLException;

    /**
     *
     * @param propertyURI property URI
     * @param propertyType property RDF type (only <b>owl:DatatypeProperty></b> or <b>owl:ObjectProperty</b>)
     * @param lang property name lang filter (only return lang which match with the given lang)
     * @return the property which have propertyURI as URI
     */
    AbstractPropertyModel<?> getProperty(URI propertyURI, URI propertyType, URI domain, String lang) throws SPARQLException;

    default DatatypePropertyModel getDataProperty(URI property, URI domain, String lang) throws SPARQLException {
        return (DatatypePropertyModel) getProperty(property, URI.create(OWL2.DatatypeProperty.getURI()), domain, lang);
    }

    default ObjectPropertyModel getObjectProperty(URI property, URI domain, String lang) throws SPARQLException {
        return (ObjectPropertyModel) getProperty(property, URI.create(OWL2.ObjectProperty.getURI()), domain, lang);
    }

    /**
     *
     * @param domain URI of a {@link ClassModel} (required)
     * @param namePattern name regex filter
     * @param lang property name lang filter
     * @param includeSubClasses indicate if domain descendant must be included or not
     * @param filter post-filter on properties
     * @return a tree-representation of all data-properties which have the given domain or one of it's descendant as domain {@link PropertyModel#getDomain()}
     */
    SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, BiPredicate<DatatypePropertyModel, ClassModel> filter) throws SPARQLException;

    /**
     *
     * @param domain URI of a {@link ClassModel} (required)
     * @param namePattern name regex filter
     * @param lang property name lang filter
     * @param includeSubClasses indicate if domain descendant must be included or not
     * @param filter post-filter on properties
     * @return a tree-representation of all object-properties which have the given domain or one of it's descendant as domain {@link PropertyModel#getDomain()}
     */
    SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, BiPredicate<ObjectPropertyModel, ClassModel> filter) throws SPARQLException;

    /**
     *
     * @param domain URI of a {@link ClassModel} (required)
     * @param ancestor URI of the domain ancestor  (optional). If set then inherited properties can be returned
     * @param lang lang filter applied on properties name
     * @return the set of data-properties which are linkable to the given domain. A property is linkable, if it's domain {@link PropertyModel#getDomain()}
     * is a super-class* of the given domain and if this property is not already linked to the domain with some {@link OwlRestrictionModel}.
     */
    Set<DatatypePropertyModel> getLinkableDataProperties(URI domain, URI ancestor, String lang) throws SPARQLException;

    /**
     *
     * @param domain URI of a {@link ClassModel} (required)
     * @param ancestor URI of the domain ancestor  (optional). If set then inherited properties can be returned
     * @param lang lang filter applied on properties name
     * @return the set of object-properties which are linkable to the given domain. A property is linkable, if it's domain {@link PropertyModel#getDomain()}
     * is a super-class* of the given domain and if this property is not already linked to the domain with some {@link OwlRestrictionModel}.
     *
     * @see ClassModel#getRestrictionsByProperties() Contains {@link OwlRestrictionModel} associated to the {@link ClassModel}
     */
    Set<ObjectPropertyModel> getLinkableObjectProperties(URI domain, URI ancestor, String lang) throws SPARQLException;

}
