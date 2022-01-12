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

public interface OntologyStore {

    void load() throws SPARQLException;

    void clear();

    AbstractPropertyModel<?> getProperty(URI property, URI type, URI domain, String lang) throws SPARQLException;

    boolean classExist(URI rdfClass, URI parentClass) throws SPARQLException;

    ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException;

    SPARQLTreeListModel<ClassModel> searchSubClasses(URI parent, String stringPattern, String lang, boolean excludeRoot) throws SPARQLException;

    SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String lang, boolean includeSubClasses) throws SPARQLException;

    SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String lang, boolean includeSubClasses) throws SPARQLException;

}
