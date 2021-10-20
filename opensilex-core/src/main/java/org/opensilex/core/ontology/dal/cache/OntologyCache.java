package org.opensilex.core.ontology.dal.cache;

import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * @author rcolin
 * Interface which define how to read, write and update {@link ClassModel}, {@link DatatypePropertyModel}, {@link ObjectPropertyModel} and {@link OwlRestrictionModel}
 * from a cache
 */
public interface OntologyCache {

    SPARQLTreeListModel<ClassModel> getSubClassesOf(URI classUri, String stringPattern, String lang, boolean ignoreRootClasses) throws OntologyCacheException;
    ClassModel getOrCreateClass(URI classUri, String lang) throws OntologyCacheException;
    ClassModel getOrCreateClass(URI classUri, URI parentClassUri, String lang) throws OntologyCacheException;
    void updateClass(ClassModel classModel) throws OntologyCacheException;
    void removeClass(URI classUris);

    void invalidate() throws OntologyCacheException;
    void populate(Set<URI> classUris) throws OntologyCacheException;

    DatatypePropertyModel getTopDatatypePropertyModel() throws OntologyCacheException;
    SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String lang) throws OntologyCacheException;
    void createDataProperty(DatatypePropertyModel property) throws OntologyCacheException;
    void updateDataProperty(DatatypePropertyModel property) throws OntologyCacheException;
    void deleteDataProperty(URI propertyURI, URI domain) throws OntologyCacheException;

    ObjectPropertyModel getTopObjectPropertyModel() throws OntologyCacheException;
    SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String lang) throws OntologyCacheException;
    void createObjectProperty(ObjectPropertyModel property) throws OntologyCacheException;
    void updateObjectProperty(ObjectPropertyModel property) throws OntologyCacheException;
    void deleteObjectProperty(URI propertyURI, URI domain) throws OntologyCacheException;

    void addRestriction(OwlRestrictionModel restriction) throws OntologyCacheException;
    void updateRestriction(OwlRestrictionModel restriction) throws OntologyCacheException;
    void deleteRestriction(URI restrictionUri, URI domain) throws OntologyCacheException;

    long length();

}
