/*******************************************************************************
 *                         OntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.*;

import java.net.URI;
import java.util.function.Predicate;

public interface OntologyStore {

    void load() throws SPARQLException;

    void clear();

    default void reload() throws SPARQLException {
        clear();
        load();
    }

    AbstractPropertyModel<?> getProperty(URI property, URI propertyType, URI domain, String lang) throws SPARQLException;

    ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException;

    SPARQLTreeListModel<ClassModel> searchSubClasses(URI parent, String pattern, String lang, boolean excludeRoot) throws SPARQLException;

    SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, Predicate<DatatypePropertyModel> filter) throws SPARQLException;

    SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, Predicate<ObjectPropertyModel> filter) throws SPARQLException;

}
