package org.opensilex.sparql.ontology.store;

import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.*;

import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Implementation of Ontology storage based on the {@link OntologyDAO}
 */
public class NoOntologyStore implements OntologyStore {

    private final OntologyDAO ontologyDAO;

    public NoOntologyStore(OntologyDAO ontologyDAO) {
        this.ontologyDAO = ontologyDAO;
    }

    @Override
    public void load() throws SPARQLException {
        // no-store impl
    }

    @Override
    public void clear() {
        // no-store impl
    }

    @Override
    public AbstractPropertyModel<?> getProperty(URI propertyURI, URI type, URI domain, String lang) throws SPARQLException {

        try {
            Objects.requireNonNull(type);
            if (SPARQLDeserializers.compareURIs(OWL2.DatatypeProperty.getURI(), type)) {
                return ontologyDAO.getDataProperty(propertyURI, domain, lang);
            } else if (SPARQLDeserializers.compareURIs(OWL2.ObjectProperty.getURI(), type)) {
                return ontologyDAO.getObjectProperty(propertyURI, domain, lang);
            } else {
                throw new SPARQLException("Unknown property type : "+ type);
            }
        } catch (Exception e) {
            throw new SPARQLException(e);
        }

    }

    @Override
    public boolean classExist(URI classURI, URI ancestorURI) throws SPARQLException {
        try {
            ontologyDAO.getClassModel(classURI, ancestorURI, null);
            return true;
        } catch (SPARQLInvalidURIException e) {
            return false;
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public ClassModel getClassModel(URI classURI, URI ancestorURI, String lang) throws SPARQLException {
        return ontologyDAO.getClassModel(classURI, ancestorURI, lang);
    }

    @Override
    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI classURI, String namePattern, String lang, boolean excludeRoot) throws SPARQLException {
        try {
            return ontologyDAO.searchSubClasses(classURI, ClassModel.class, namePattern, lang, excludeRoot, null);
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, BiPredicate<DatatypePropertyModel,ClassModel> filter) throws SPARQLException {
        try {
            return ontologyDAO.searchDataProperties(domain, namePattern, lang);
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, BiPredicate<ObjectPropertyModel,ClassModel> filter) throws SPARQLException {
        try {
            return ontologyDAO.searchObjectProperties(domain, namePattern, lang);
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public Set<DatatypePropertyModel> getLinkableDataProperties(URI domain, URI ancestor, String lang) throws SPARQLException {
        return ontologyDAO.getLinkableDataProperties(domain,ancestor,lang);
    }

    @Override
    public Set<ObjectPropertyModel> getLinkableObjectProperties(URI domain, URI ancestor, String lang) throws SPARQLException {
        return ontologyDAO.getLinkableObjectProperties(domain,ancestor,lang);
    }
}
