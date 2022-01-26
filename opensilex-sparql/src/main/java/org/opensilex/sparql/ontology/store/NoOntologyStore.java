package org.opensilex.sparql.ontology.store;

import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.*;

import java.net.URI;
import java.util.Objects;
import java.util.function.Predicate;

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
    public AbstractPropertyModel<?> getProperty(URI property, URI type, URI domain, String lang) throws SPARQLException {

        try {
            Objects.requireNonNull(type);
            if (SPARQLDeserializers.compareURIs(OWL2.DatatypeProperty.getURI(), type)) {
                return ontologyDAO.getDataProperty(property, domain, lang);
            } else if (SPARQLDeserializers.compareURIs(OWL2.ObjectProperty.getURI(), type)) {
                return ontologyDAO.getObjectProperty(property, domain, lang);
            } else {
                throw new SPARQLException("Unknown property type : "+ type);
            }
        } catch (Exception e) {
            throw new SPARQLException(e);
        }

    }

    @Override
    public boolean classExist(URI rdfClass, URI parentClass) throws SPARQLException {
        try {
            ontologyDAO.getClassModel(rdfClass, parentClass, null);
            return true;
        } catch (SPARQLInvalidURIException e) {
            return false;
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException {
        return ontologyDAO.getClassModel(rdfClass, parentClass, lang);
    }

    @Override
    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI parent, String pattern, String lang, boolean excludeRoot) throws SPARQLException {
        try {
            return ontologyDAO.searchSubClasses(parent, ClassModel.class, pattern, lang, excludeRoot, null);
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, Predicate<DatatypePropertyModel> filter) throws SPARQLException {
        try {
            return ontologyDAO.searchDataProperties(domain, namePattern, lang);
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, Predicate<ObjectPropertyModel> filter) throws SPARQLException {
        try {
            return ontologyDAO.searchObjectProperties(domain, namePattern, lang);
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }
}
