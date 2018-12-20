//******************************************************************************
//                                       ExperimentDaoSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 18 déc. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Experiment;

/**
 * Access to the experiments in the triplestore. 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ExperimentDAOSesame extends DAOSesame<Experiment> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentDAOSesame.class);

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Prepare the SPARQL query to return all variables measured by an experiment.
     * 
     * @param experimentUri The experiment uri which measures veriables
     * @return The prepared query
     * @example 
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#Variable> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label ?label .
     *      <http://www.phenome-fppn.fr/2018/DIA2018-1> <http://www.phenome-fppn.fr/vocabulary/2017#measures> ?uri
     * }
     */
    private SPARQLQueryBuilder prepareSearchVariablesQuery(String experimentUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_VARIABLE.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendTriplet(experimentUri, Vocabulary.RELATION_MEASURES.toString(), "?" + URI, null);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Prepare the SPARQL query to return all sensors which participates in the given experiment.
     * 
     * @param experimentUri The experiment uri which measures veriables
     * @return The prepared query
     * @example 
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#Sensors> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label ?label .
     *      <http://www.phenome-fppn.fr/2018/DIA2018-1> <http://www.phenome-fppn.fr/vocabulary/2017#participatesIn> ?uri
     * }
     */
    private SPARQLQueryBuilder prepareSearchSensorsQuery(String experimentUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_SENSING_DEVICE.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendTriplet("?" + URI, Vocabulary.RELATION_PARTICIPATES_IN.toString(), experimentUri, null);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Return a HashMap of uri => label of the variables linked to an experiment.
     * 
     * @param experimentUri The experiment uri which measures variables
     * @return HashMap of uri => label
     */
    public HashMap<String, String> getVariables(String experimentUri) {
        SPARQLQueryBuilder query = prepareSearchVariablesQuery(experimentUri);
        
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
     * Return a HashMap of uri => label of the sensors linked to an experiment.
     * 
     * @param experimentUri The experiment uri
     * @return HashMap of uri => label
     */
    public HashMap<String, String> getSensors(String experimentUri) {
        SPARQLQueryBuilder query = prepareSearchSensorsQuery(experimentUri);
        
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
     * Update the list of the variables linked to the given experiment.
     * /!\ Prerequisite : the data must have been checked before.
     * @see ExperimentDao#checkAndUpdateObservedVariables(java.lang.String, java.util.List) 
     * @param experimentUri
     * @param variables
     * @return The update result.
     */
    public POSTResultsReturn updateMeasuredVariables(String experimentUri, List<String> variables) {
        POSTResultsReturn result;
        List<Status> updateStatus = new ArrayList<>();
        
        boolean update = true;

        //1. Delete old object properties
        HashMap<String, String> actualMeasuredVariables = getVariables(experimentUri);
        List<String> oldMeasuredVariables = new ArrayList<>();
        actualMeasuredVariables.entrySet().forEach((oldVariable) -> {
            oldMeasuredVariables.add(oldVariable.getKey());
        });
        
        if (deleteObjectProperties(experimentUri, Vocabulary.RELATION_MEASURES.toString(), oldMeasuredVariables)) {
            //2. Add new object properties
            if (addObjectProperties(experimentUri, Vocabulary.RELATION_MEASURES.toString(), variables, experimentUri)) {
                updateStatus.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, "The experiment " + experimentUri + " has now " + variables.size() + " linked variables"));
            } else {
                update = false;
                updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "An error occurred during the update."));
            }
        } else {
            update = false;
            updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "An error occurred during the update."));
        }
        
        result = new POSTResultsReturn(update, update, update);
        result.statusList = updateStatus;
        result.createdResources.add(experimentUri); 
        return result;
    }
    
    /**
     * Add participatesIn relations between a list of sensors and an experiment.
     * @param sensors
     * @param experimentUri
     * @example
     * INSERT DATA {
     *      GRAPH <http://www.opensilex.fr/platform/OSL2018-1> { 
     *          <http://www.opensilex.fr/platform/2018/s18533>  <http://www.opensilex.fr/vocabulary/2017#participatesIn>  <http://www.opensilex.fr/platform/OSL2018-1> . 
     * }}
     * @return true if the insertion has been done
     *         false if an error occurred (see the error logs to get more details)
     */
    private boolean linkSensorsToExperiment(List<String> sensors, String experimentUri) {
        SPARQLUpdateBuilder query = new SPARQLUpdateBuilder();
        query.appendGraphURI(experimentUri);
        sensors.forEach((sensor) -> {
            query.appendTriplet(sensor, Vocabulary.RELATION_PARTICIPATES_IN.toString(), experimentUri, null);
        });
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        
        //Insert the properties in the triplestore
        Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
        try {
            prepareUpdate.execute();
        } catch (UpdateExecutionException ex) {
            LOGGER.error("Add object properties error : " + ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Delete the participatesIn relations between the given sensors and the given experiment.
     * @param subjectUri
     * @param predicateUri
     * @param objectPropertiesUris
     * @example
     * DELETE WHERE { 
     *  <http://www.opensilex.fr/platform/2018/s18533> <http://www.opensilex.fr/vocabulary/2017#participatesIn> <http://www.opensilex.fr/platform/OSL2018-1> .  
     * }
     * @return true if the relations have been deleted
     *         false if the delete has not been done.
     */
    private boolean deleteLinksSensorsExperiment(List<String> sensors, String experimentUri) {
        //1. Generates delete query
        String deleteQuery = "DELETE WHERE { ";
        
        for (String sensor : sensors) {
            deleteQuery += "<" + sensor + "> <" + Vocabulary.RELATION_PARTICIPATES_IN.toString() + "> <" + experimentUri + "> . ";
        }
        
        deleteQuery += " }";
        
        LOGGER.debug(deleteQuery);
        
        //2. Delete data in the triplestore
        Update prepareDelete = getConnection().prepareUpdate(QueryLanguage.SPARQL, deleteQuery);
        try {
            prepareDelete.execute();
        } catch (UpdateExecutionException ex) {
            LOGGER.error("Delete object properties error : " + ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Update the list of the sensors linked to the given experiment.
     * /!\ Prerequisite : the data must have been checked before.
     * @see ExperimentDao#checkAndUpdateLinkedSensors(java.lang.String, java.util.List)
     * @param experimentUri
     * @param sensors
     * @return The update result.
     */
    public POSTResultsReturn updateLinkedSensors(String experimentUri, List<String> sensors) {
        POSTResultsReturn result;
        List<Status> updateStatus = new ArrayList<>();
        
        boolean update = true;

        //1. Delete old object properties
        HashMap<String, String> actualLinkedSensors = getSensors(experimentUri);
        List<String> oldLinkedSensors = new ArrayList<>();
        actualLinkedSensors.entrySet().forEach((oldSensor) -> {
            oldLinkedSensors.add(oldSensor.getKey());
        });
        
        if (deleteLinksSensorsExperiment(oldLinkedSensors, experimentUri)) {
            //2. Add new object properties
            if (linkSensorsToExperiment(sensors, experimentUri)) {
                updateStatus.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, "The experiment " + experimentUri + " has now " + sensors.size() + " linked sensors."));
            } else {
                update = false;
                updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "An error occurred during the update."));
            }
        } else {
            update = false;
            updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "An error occurred during the update."));
        }
        
        result = new POSTResultsReturn(update, update, update);
        result.statusList = updateStatus;
        result.createdResources.add(experimentUri); 
        return result; 
    }
}
