//******************************************************************************
//                              VocabularyDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 18 June 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.URINamespaces;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.PropertyVocabularyDTO;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.model.Namespace;

/**
 * Vocabulary DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class VocabularyDAO extends Rdf4jDAO<PropertyVocabularyDTO> {

    final static Logger LOGGER = LoggerFactory.getLogger(VocabularyDAO.class);

    //the domain wanted for the searched properties
    //@example http://www.phenome-fppn.fr/vocabulary/2018#UAV
    public String domainRdfType;

    final static String LANGUAGE_EN = "en";
    final static String LABEL_LABEL_EN = "label";
    final static String LABEL_COMMENT_EN = "comment";
    final static String PROPERTY = "property";

    /**
     * Gets the list of RDFs properties usually used.
     * @return the list of the RDFs properties
     */
    public ArrayList<PropertyVocabularyDTO> allPaginateRdfsProperties() {
        ArrayList<PropertyVocabularyDTO> rdfsPropertyes = new ArrayList<>();

        //label property
        PropertyVocabularyDTO label = new PropertyVocabularyDTO();
        label.setRelation(Rdfs.RELATION_LABEL.toString());
        label.addLabel(LANGUAGE_EN, LABEL_LABEL_EN);
        rdfsPropertyes.add(label);

        //comment property
        PropertyVocabularyDTO comment = new PropertyVocabularyDTO();
        comment.setRelation(Rdfs.RELATION_COMMENT.toString());
        comment.addLabel(LANGUAGE_EN, LABEL_COMMENT_EN);
        rdfsPropertyes.add(comment);

        return rdfsPropertyes;
    }

    /**
     * Generates the SPARQL query to get the list of the contact properties 
     * @example
     * SELECT ?contactProperty 
     * WHERE { 
     *  ?contactProperty rdfs:subPropertyOf* <http://www.opensilex.org/vocabulary/oeso#hasContact> . 
     * }
     * @return
     */
    protected SPARQLQueryBuilder prepareGetContactProperties() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + PROPERTY);
        query.appendTriplet("?" + PROPERTY, "<" + Rdfs.RELATION_SUBPROPERTY_OF.toString() + ">*", Oeso.RELATION_HAS_CONTACT.toString(), null);
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }

    /**
     * Generates the SPARQL query to get the list of the device properties
     * @example
     * SELECT ?property 
     * WHERE { 
     *  ?property rdfs:subPropertyOf <http://www.opensilex.org/vocabulary/oeso#deviceProperty> . 
     * }
     * @return
     */
    protected SPARQLQueryBuilder prepareGetDeviceProperties() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + PROPERTY);
        query.appendTriplet("?" + PROPERTY, "<" + Rdfs.RELATION_SUBPROPERTY_OF.toString() + ">*", Oeso.RELATION_DEVICE_PROPERTY.toString(), null);
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }

    /**
     * Checks if a given property has the class attribute domainRdfType as domain.
     * @param property
     * @return true if the class attribute domainRdfType can have the property
     *         false if not
     */
    protected boolean isPropertyDomainContainsRdfType(String property) throws DAOPersistenceException {
        PropertyDAO propertyDAO = new PropertyDAO();
        ArrayList<String> propertyDomains = propertyDAO.getPropertyDomain(property);

        UriDAO uriDao = new UriDAO();
        for (String propertyDomain : propertyDomains) {
            if (propertyDomain.equals(domainRdfType)
                    || uriDao.isSubClassOf(domainRdfType, propertyDomain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a vocabulary with the given property URI. 
     * @see UriDAO#getLabels()
     * @param propertyUri
     * @return the property
     */
    private PropertyVocabularyDTO propertyStringToPropertyVocabularyDTO(String propertyUri) {
        PropertyVocabularyDTO property = new PropertyVocabularyDTO();
        property.setRelation(propertyUri);

        UriDAO uriDao = new UriDAO();
        uriDao.uri = propertyUri;
        property.setLabels(uriDao.getLabels().asMap());
        property.setComments(uriDao.getComments().asMap());
        return property;
    }

    /**
     * Searches the list of contact properties that can be added to an instance of
     * a given concept.
     * @return list of contact properties
     */
    public ArrayList<PropertyVocabularyDTO> allPaginateContactProperties() throws DAOPersistenceException {
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
     * Searches the list of device properties that can be added to an instance of
     * a given concept.
     * @return list of device properties
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    public ArrayList<PropertyVocabularyDTO> allPaginateDeviceProperties() throws DAOPersistenceException {
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
     * Gets all the triplestore namespaces.
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

    @Override
    public List<PropertyVocabularyDTO> create(List<PropertyVocabularyDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<PropertyVocabularyDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<PropertyVocabularyDTO> update(List<PropertyVocabularyDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PropertyVocabularyDTO find(PropertyVocabularyDTO object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PropertyVocabularyDTO findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<PropertyVocabularyDTO> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
