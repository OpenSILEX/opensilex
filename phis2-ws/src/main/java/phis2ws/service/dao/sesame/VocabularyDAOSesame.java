//******************************************************************************
//                                       VocabularyDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 18 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  18 juin 2018
// Subject: dao to get the vocabulary (and add data) of the phis instance
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Collections;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.resources.dto.PropertyVocabularyDTO;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Namespace;

/**
 * dao to get the vocabulary (and add data) of the phis instance
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VocabularyDAOSesame extends DAOSesame<Object> {

    final static Logger LOGGER = LoggerFactory.getLogger(VocabularyDAOSesame.class);

    //the domain wanted for the searched properties
    //e.g. http://www.phenome-fppn.fr/vocabulary/2018#UAV
    public String domainRdfType;

    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();

    final static String TRIPLESTORE_RELATION_HAS_CONTACT = NAMESPACES.getRelationsProperty("rHasContact");
    final static String TRIPLESTORE_RELATION_COMMENT = NAMESPACES.getRelationsProperty("comment");
    final static String TRIPLESTORE_RELATION_DEVICE_PROPERTY = NAMESPACES.getRelationsProperty("rDeviceProperty");
    final static String TRIPLESTORE_RELATION_LABEL = NAMESPACES.getRelationsProperty("label");
    final static String TRIPLESTORE_RELATION_SUBPROPERTY_OF_MULTIPLE = NAMESPACES.getRelationsProperty("subPropertyOf*");
    final static String LANGUAGE_EN = "en";
    final static String LABEL_LABEL_EN = "label";
    final static String LABEL_COMMENT_EN = "comment";
    final static String PROPERTY = "property";
    
  
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * get the list of rdfs properties usually used
     *
     * @return the list of the rdfs properties
     */
    public ArrayList<PropertyVocabularyDTO> allPaginateRdfsProperties() {
        ArrayList<PropertyVocabularyDTO> rdfsPropertyes = new ArrayList<>();

        //label property
        PropertyVocabularyDTO label = new PropertyVocabularyDTO();
        label.setRelation(TRIPLESTORE_RELATION_LABEL);
        label.addLabel(LANGUAGE_EN, LABEL_LABEL_EN);
        rdfsPropertyes.add(label);

        //comment property
        PropertyVocabularyDTO comment = new PropertyVocabularyDTO();
        comment.setRelation(TRIPLESTORE_RELATION_COMMENT);
        comment.addLabel(LANGUAGE_EN, LABEL_COMMENT_EN);
        rdfsPropertyes.add(comment);

        return rdfsPropertyes;
    }

    /**
     * generates the SPARQL query to get the list of the contact properties e.g.
     * SELECT ?contactProperty 
     * WHERE { 
     *  ?contactProperty rdfs:subPropertyOf <http://www.phenome-fppn.fr/vocabulary/2017#hasContact> . 
     * }
     *
     * @return
     */
    protected SPARQLQueryBuilder prepareGetContactProperties() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + PROPERTY);
        query.appendTriplet("?" + PROPERTY, TRIPLESTORE_RELATION_SUBPROPERTY_OF_MULTIPLE, TRIPLESTORE_RELATION_HAS_CONTACT, null);

        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());

        return query;
    }

    /**
     * generates the SPARQL query to get the list of the device properties e.g.
     * SELECT ?property 
     * WHERE { 
     *  ?property rdfs:subPropertyOf <http://www.phenome-fppn.fr/vocabulary/2017#deviceProperty> . 
     * }
     *
     * @return
     */
    protected SPARQLQueryBuilder prepareGetDeviceProperties() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + PROPERTY);
        query.appendTriplet("?" + PROPERTY, TRIPLESTORE_RELATION_SUBPROPERTY_OF_MULTIPLE, TRIPLESTORE_RELATION_DEVICE_PROPERTY, null);

        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());

        return query;
    }

    /**
     * check if a given property has the class attribute domainRdfType as
     * domain.
     *
     * @param property
     * @return true if the class attribute domainRdfType can have the property
     * false if not
     */
    protected boolean isPropertyDomainContainsRdfType(String property) {
        PropertyDAOSesame propertyDAO = new PropertyDAOSesame();
        propertyDAO.relation = property;

        ArrayList<String> propertyDomains = propertyDAO.getPropertyDomain();

        UriDaoSesame uriDao = new UriDaoSesame();
        for (String propertyDomain : propertyDomains) {
            if (propertyDomain.equals(domainRdfType)
                    || uriDao.isSubClassOf(domainRdfType, propertyDomain)) {
                return true;
            }
        }

        return false;
    }

    /**
     * generates a PropertyVocabularyDTO with the given property URI. Get the
     * labels from the triplestore
     *
     * @see UriDaoSesame#getLabels()
     * @param propertyUri
     * @return the property
     */
    private PropertyVocabularyDTO propertyStringToPropertyVocabularyDTO(String propertyUri) {
        PropertyVocabularyDTO property = new PropertyVocabularyDTO();
        property.setRelation(propertyUri);

        UriDaoSesame uriDao = new UriDaoSesame();
        uriDao.uri = propertyUri;
        property.setLabels(uriDao.getLabels().asMap());
        property.setComments(uriDao.getComments().asMap());

        return property;
    }

    /**
     * search the list of contact properties that can be added to an instance of
     * a given concept
     *
     * @return list of contact properties
     */
    public ArrayList<PropertyVocabularyDTO> allPaginateContactProperties() {
        //1. get all the subproperties of the hasContact Property
        SPARQLQueryBuilder query = prepareGetContactProperties();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        ArrayList<PropertyVocabularyDTO> properties = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            //2. for each founded property, check if the property can be used on the rdfType
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String propertyFounded = bindingSet.getValue(PROPERTY).stringValue();
                //2.1 check if domainRdfType can have the property
                if (isPropertyDomainContainsRdfType(propertyFounded)) {
                    properties.add(propertyStringToPropertyVocabularyDTO(propertyFounded));
                }
            }
        }

        return properties;
    }

    /**
     * search the list of device properties that can be added to an instance of
     * a given concept
     *
     * @return list of device properties
     */
    public ArrayList<PropertyVocabularyDTO> allPaginateDeviceProperties() {
        //1. get all the subproperties of the hasContact Property
        SPARQLQueryBuilder query = prepareGetDeviceProperties();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        ArrayList<PropertyVocabularyDTO> properties = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            //2. for each founded property, check if the property can be used on the rdfType
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String propertyFounded = bindingSet.getValue(PROPERTY).stringValue();
                //2.1 check if domainRdfType can have the property
                if (isPropertyDomainContainsRdfType(propertyFounded)) {
                    properties.add(propertyStringToPropertyVocabularyDTO(propertyFounded));
                }
            }
        }

        return properties;
    }

    /**
     * Get all rdf4j namespaces.
     * [
     *  {
     *   prefix : oa 
     *   namespace : https://www.w3.org/ns/oa
     *  }
     * ]
     * @return arraylist of namespaces
     */
    public ArrayList<Namespace> allNamespacesProperties() {
        ArrayList<Namespace> namespacesList = new ArrayList<>();
        // Set custom namespaces set in the webservice
        URINamespaces.USER_SPECIFIC_NAMESPACES.forEach((prefixSpecific, namespaceSpecific)->{
                Namespace namespace = new Namespace(prefixSpecific, namespaceSpecific);
                namespacesList.add(namespace);
	});
        // Retreive namespace from RDF4J
        try (RepositoryResult<org.eclipse.rdf4j.model.Namespace> iterator = getConnection().getNamespaces()) {
            while (iterator.hasNext()) {
                org.eclipse.rdf4j.model.Namespace rdf4jNamespace = iterator.next();
                if(!URINamespaces.USER_SPECIFIC_NAMESPACES.containsValue(rdf4jNamespace.getName())){
                    Namespace namespace = new Namespace(rdf4jNamespace.getPrefix(), rdf4jNamespace.getName());
                    namespacesList.add(namespace);
                }
               
            }
        }
        
        // Sort list after addition of specific concepts (order namespace)
        Collections.sort(namespacesList);
        
        return namespacesList;
    }
}
