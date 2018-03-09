//******************************************************************************
//                                       VectorDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 9 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  9 mars 2018
// Subject:access to the vectors in the triplestore
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
 * CRUD methods of vectors, in the triplestore rdf4j
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VectorDAOSesame extends DAOSesame<Object> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAOSesame.class);

    /**
     * generates the query to get the number of vectors in the triplestore for 
     * a specific year
     * @param the 
     * @return query of number of vectors
     */
    private SPARQLQueryBuilder prepareGetVectorsNumber(String year) {
        URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder queryNumberVectors = new SPARQLQueryBuilder();
        queryNumberVectors.appendSelect("count(?sensor) as ?count");
        queryNumberVectors.appendTriplet("?vector", uriNamespaces.getRelationsProperty("type"), uriNamespaces.getObjectsProperty("cVector"), null);
        queryNumberVectors.appendFilter("FILTER regex(?vector, \".*\\/" + year + "\\/.*\")");
        
        LOGGER.debug("SPARQL query : " + queryNumberVectors.toString());
        return queryNumberVectors;
    }
    
    /**
     * get the number of vectors in the triplestore, for a given year
     * @param year
     * @see VectorDAOSesame#prepareGetVectorsNumber() 
     * @return the number of vectors in the triplestore
     */
    public int getNumberOfVectors(String year) {
        SPARQLQueryBuilder queryNumberVectors = prepareGetVectorsNumber(year);
        //SILEX:test
        //for the pool connection problems. 
        //WARNING this is a quick fix
        rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
        rep.initialize();
        setConnection(rep.getConnection());
        //\SILEX:test
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryNumberVectors.toString());
        TupleQueryResult result = tupleQuery.evaluate();        
        //SILEX:test
        //for the pool connection problems.
        getConnection().close();
        //\SILEX:test
        
        BindingSet bindingSet = result.next();
        String numberVectors = bindingSet.getValue("count").stringValue();
        
        return Integer.parseInt(numberVectors);
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
