package org.opensilex.core.ontology.dal.cache;

import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLTreeListModel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author rcolin
 */
public interface OntologyCache {

    SPARQLTreeListModel<ClassModel> getSubClassesOf(URI classUri, String stringPattern, String lang, boolean ignoreRootClasses) throws SPARQLException;
    ClassModel getClassModel(URI classUri, String lang) throws SPARQLException;
    ClassModel getClassModel(URI classUri, URI parentClassUri, String lang) throws SPARQLException;
    void removeClass(URI classUris);

    void invalidate();
    void populate(List<URI> classUris) throws SPARQLException;

    SPARQLTreeListModel<DatatypePropertyModel> getDataProperties(URI domain, String lang) throws SPARQLException;
    void createDataProperty(DatatypePropertyModel property) throws SPARQLException;
    void updateDataProperty(DatatypePropertyModel property) throws SPARQLException;
    void deleteDataProperty(URI propertyURI, URI domain) throws SPARQLException;

    SPARQLTreeListModel<ObjectPropertyModel> getObjectProperties(URI domain, String lang) throws SPARQLException;
    void createObjectProperty(ObjectPropertyModel property) throws SPARQLException;
    void updateObjectProperty(ObjectPropertyModel property) throws SPARQLException;
    void deleteObjectProperty(URI propertyURI, URI domain) throws SPARQLException;

    void addRestriction(OwlRestrictionModel restriction) throws SPARQLException;
    void updateRestriction(OwlRestrictionModel restriction) throws SPARQLException;
    void deleteRestriction(URI restrictionUri, URI domain) throws SPARQLException;

}
