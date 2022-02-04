/*******************************************************************************
 *                         OntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.*;

import java.net.URI;
import java.util.Set;
import java.util.function.BiPredicate;

public interface OntologyStore {

    void load() throws SPARQLException;

    void clear();

    default void reload() throws SPARQLException {
        clear();
        load();
    }

    AbstractPropertyModel<?> getProperty(URI property, URI propertyType, URI domain, String lang) throws SPARQLException;

    default DatatypePropertyModel getDataProperty(URI property, URI domain, String lang) throws SPARQLException {
        return (DatatypePropertyModel) getProperty(property, URI.create(OWL2.DatatypeProperty.getURI()), domain, lang);
    }

    default ObjectPropertyModel getObjectProperty(URI property, URI domain, String lang) throws SPARQLException {
        return (ObjectPropertyModel) getProperty(property, URI.create(OWL2.ObjectProperty.getURI()), domain, lang);
    }

    boolean classExist(URI rdfClass, URI parentClass) throws SPARQLException;

    ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException;

    SPARQLTreeListModel<ClassModel> searchSubClasses(URI parent, String pattern, String lang, boolean excludeRoot) throws SPARQLException;

    SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, BiPredicate<DatatypePropertyModel, ClassModel> filter) throws SPARQLException;

    SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, BiPredicate<ObjectPropertyModel, ClassModel> filter) throws SPARQLException;

    Set<DatatypePropertyModel> getLinkableDataProperties(URI domain, String lang) throws SPARQLInvalidURIException;

    Set<ObjectPropertyModel> getLinkableObjectProperties(URI domain, String lang) throws SPARQLInvalidURIException;

}
