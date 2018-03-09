//******************************************************************************
//                                       SensorDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 9 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  9 mars 2018
// Subject: access to the sensors in the triplestore
//******************************************************************************
package phis2ws.service.dao.sesame;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;

/**
 * allows CRUD methods of sensors in the triplestore sesame
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorDAOSesame extends DAOSesame<Object> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAOSesame.class);

    /**
     * generates the query to get the number of sensors in the triplestore for 
     * a specific year
     * @param the 
     * @return query of number of sensors
     */
    private SPARQLQueryBuilder prepareGetSensorsNumber(String year) {
        URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder queryNumberSensors = new SPARQLQueryBuilder();
        queryNumberSensors.appendSelect("count(?sensor) as ?count");
        queryNumberSensors.appendTriplet("?sensor", uriNamespaces.getRelationsProperty("type"), uriNamespaces.getObjectsProperty("cSensor"), null);
        queryNumberSensors.appendFilter("FILTER regex(?sensor, \".*\\/" + year + "\\/.*\")");
        
        LOGGER.debug("SPARQL query : " + queryNumberSensors.toString());
        return queryNumberSensors;
    }
    
    /**
     * get the number of sensors in the triplestore, for a given year
     * @param year
     * @see SensorDAOSesame#prepareGetSensorsNumber() 
     * @return the number of sensors in the triplestore
     */
    public int getNumberOfSensors(String year) {
        SPARQLQueryBuilder queryNumberSensors = prepareGetSensorsNumber(year);
        //SILEX:test
        //for the pool connection problems. 
        //WARNING this is a quick fix
        rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
        rep.initialize();
        setConnection(rep.getConnection());
        //\SILEX:test
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryNumberSensors.toString());
        TupleQueryResult result = tupleQuery.evaluate();        
        //SILEX:test
        //for the pool connection problems.
        getConnection().close();
        //\SILEX:test
        
        BindingSet bindingSet = result.next();
        String numberSensors = bindingSet.getValue("count").stringValue();
        
        return Integer.parseInt(numberSensors);
    }
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }    
}
