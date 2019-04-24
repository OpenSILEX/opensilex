//******************************************************************************
//                                DeviceDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.HashMap;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.model.Device;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device DAO. 
 * SILEX:refactor
 * The SensorDAO and ActuatorDAO are DAO about devices. 
 * Thoses classes should extends DeviceDAO.
 * \SILEX:refactor
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DeviceDAO extends Rdf4jDAO<Device> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(DeviceDAO.class);

    private SPARQLQueryBuilder prepareIsDeviceQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_DEVICE.toString(), null);
        
        query.appendAsk("");
        LOGGER.debug(query.toString());
        return query;
    }
    
    private boolean isDevice(String uri) {
        SPARQLQueryBuilder query = prepareIsDeviceQuery(uri);
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        
        return booleanQuery.evaluate();
    }
     
    /**
     * Checks if a given URI is a device.
     * @param uri
     * @return true if the URI corresponds to a device
     *         false if it does not exist or if it is not a device
     */
    public boolean existAndIsDevice(String uri) {
        if (existUri(uri)) {
            return isDevice(uri);
            
        } else {
            return false;
        }
    }
    
    /**
     * Prepares the SPARQL query to return all variables measured by a device.
     * @param sensor The device uri which measures veriables
     * @return The prepared query
     * @example 
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Variable> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label ?label .
     *      <http://www.phenome-fppn.fr/2018/s18001> <http://www.opensilex.org/vocabulary/oeso#measures> ?uri
     * }
     */
    private SPARQLQueryBuilder prepareSearchVariablesQuery(String sensorUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_VARIABLE.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendTriplet(sensorUri, Oeso.RELATION_MEASURES.toString(), "?" + URI, null);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Returns a HashMap of URI => label of the variables measured by the given device.
     * @param sensor The device URI which measures veriables
     * @return HashMap of URI => label
     */
    private HashMap<String, String> getVariables(String deviceUri) {
        SPARQLQueryBuilder query = prepareSearchVariablesQuery(deviceUri);
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        HashMap<String, String> variables = new HashMap<>();
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();   
                
                variables.put(
                    bindingSet.getValue(URI).stringValue(), 
                    bindingSet.getValue(LABEL).stringValue()
                );
            }
        }
        return variables;
    }
    
    /**
     * Checks if a given sensor measured a given variable (is the sensor linked to the variable ?).
     * @see SensorDAO#getVariables(java.lang.String)
     * @param deviceUri
     * @param variableUri
     * @return true if the sensor measured the variable (i.e. sensor linked to variable with the measures object property)
     *         false if not
     */
    public boolean isDeviceMeasuringVariable(String deviceUri, String variableUri) {
        HashMap<String, String> measuredVariables = getVariables(deviceUri);
        
        return measuredVariables.containsKey(variableUri);
    }

    @Override
    public List<Device> create(List<Device> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Device> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Device> update(List<Device> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Device find(Device object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Device findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Device> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
