//******************************************************************************
//                                       PropertyDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 29 mai 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  29 mai 2018
// Subject: access and manipulation of the properties of the ontology in the triplestore
//******************************************************************************
package phis2ws.service.dao.sesame;

//SILEX:todo

import java.util.ArrayList;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Property;

//use this DAO in the agronomical objects DAO
//\SILEX:todo

/**
 * CRUD methods for the properties stored in the triplestore (rdf4j)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyDAOSesame extends DAOSesame<Property> {
    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAOSesame.class);
    
    //The following attributes are used to search properties in the triplestore
    //the property relation name. 
    //the relation term is used because it only represents the "vocabulary:property" 
    //and it does not represents everything around such as domain, range, etc. 
    //which is represented by the "Property" label
    public String relation;
    
    //the domain label used to query triplestore
    private final String DOMAIN = "domain";
    
    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();
    private final static String TRIPLESTORE_RELATION_DOMAIN = NAMESPACES.getRelationsProperty("domain");
    private final static String TRIPLESTORE_RELATION_UNION_OF = NAMESPACES.getRelationsProperty("unionOf");
    private final static String TRIPLESTORE_RELATION_REST = NAMESPACES.getRelationsProperty("rest");
    private final static String TRIPLESTORE_RELATION_FIRST = NAMESPACES.getRelationsProperty("first");

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * prepare the sparql query to get the domain of a relation
     * @return the builded query
     * e.g.
     * SELECT ?domain
     * WHERE {
     *      <http://www.phenome-fppn.fr/vocabulary/2017#wavelength> rdfs:domain ?domain
     * }
     */
    private SPARQLQueryBuilder prepareGetDomainQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + DOMAIN);
        query.appendTriplet(relation, 
                TRIPLESTORE_RELATION_DOMAIN + "/(" + TRIPLESTORE_RELATION_UNION_OF + "/" + TRIPLESTORE_RELATION_REST + "*/" + TRIPLESTORE_RELATION_FIRST + ")*" , "?" + DOMAIN, null);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * get in the triplestore the domain of the property if it exist
     * @return the domain of the property (attributes relation)
     */
    public ArrayList<String> getPropertyDomain() {
        SPARQLQueryBuilder query = prepareGetDomainQuery();
        ArrayList<String> propertyDomains = new ArrayList<>();
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                propertyDomains.add(bindingSet.getValue(DOMAIN).toString());
            }
        }
        
        return propertyDomains;
    }
}
