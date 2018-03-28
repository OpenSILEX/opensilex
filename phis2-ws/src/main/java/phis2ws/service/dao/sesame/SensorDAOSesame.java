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
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;

/**
 * allows CRUD methods of sensors in the triplestore rdf4j
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorDAOSesame extends DAOSesame<Object> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAOSesame.class);
    
    
    /**
     * prepare a query to get the higher id of the sensors
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastIdFromYear(String year) {
        URINamespaces uriNamespace = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", uriNamespace.getRelationsProperty("type"), "?type", null);
        query.appendTriplet("?type", uriNamespace.getRelationsProperty("subClassOf*"), uriNamespace.getObjectsProperty("cSensor"), null);
        query.appendFilter("regex(str(?sensor), \".*/" + year + "/.*\")");
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * get the higher id of the variables
     * @param year
     * @return the id
     */
    public int getLastIdFromYear(String year) {
        SPARQLQueryBuilder query = prepareGetLastIdFromYear(year);
        
        //SILEX:test
        //All the triplestore connection has to been checked and updated
        //This is an unclean hot fix
        String sesameServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, SESAME_SERVER);
        String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, REPOSITORY_ID);
        rep = new HTTPRepository(sesameServer, repositoryID); //Stockage triplestore Sesame
        rep.initialize();
        this.setConnection(rep.getConnection());
        this.getConnection().begin();
        //\SILEX:test

        //get last variable uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        //SILEX:test
        //For the pool connection problems
        getConnection().commit();
        getConnection().close();
        //\SILEX:test
        
        String uriSensor = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriSensor = bindingSet.getValue("uri").stringValue();
        }
        
        if (uriSensor == null) {
            return 0;
        } else {
            //2018 -> 18. to get /s18
            String split = "/s" + year.substring(2, 4);
            String[] parts = uriSensor.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
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
