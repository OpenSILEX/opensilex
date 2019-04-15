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
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
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
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Experiment;

/**
 * Access to the experiments in the triplestore. 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ExperimentDAOSesame extends DAOSesame<Experiment> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentDAOSesame.class);
    
    /**
     * Prepare the SPARQL query to return all variables measured by an experiment.
     * 
     * @param experimentUri The experiment uri which measures veriables
     * @return The prepared query
     * @example 
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Variable> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label ?label .
     *      <http://www.phenome-fppn.fr/2018/DIA2018-1> <http://www.opensilex.org/vocabulary/oeso#measures> ?uri
     * }
     */
    private SPARQLQueryBuilder prepareSearchVariablesQuery(String experimentUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_VARIABLE.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendTriplet(experimentUri, Oeso.RELATION_MEASURES.toString(), "?" + URI, null);
        
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
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Sensors> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label ?label .
     *      <http://www.phenome-fppn.fr/2018/DIA2018-1> <http://www.opensilex.org/vocabulary/oeso#participatesIn> ?uri
     * }
     */
    private SPARQLQueryBuilder prepareSearchSensorsQuery(String experimentUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_SENSING_DEVICE.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendTriplet("?" + URI, Oeso.RELATION_PARTICIPATES_IN.toString(), experimentUri, null);
        
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
     * /!\ Prerequisite : the information must have been checked before.
     * @see ExperimentDao#checkAndUpdateLinkedVariables(java.lang.String, java.util.List) 
     * @param experimentUri
     * @param variables
     * @return The update result.
     */
    public POSTResultsReturn updateLinkedVariables(String experimentUri, List<String> variables) {
        POSTResultsReturn result;
        List<Status> updateStatus = new ArrayList<>();
        
        boolean update = true;

        //1. Delete old object properties
        HashMap<String, String> actualLinkedVariables = getVariables(experimentUri);
        List<String> oldLinkedVariables = new ArrayList<>();
        actualLinkedVariables.entrySet().forEach((oldVariable) -> {
            oldLinkedVariables.add(oldVariable.getKey());
        });
        
        if (deleteObjectProperties(experimentUri, Oeso.RELATION_MEASURES.toString(), oldLinkedVariables)) {
            //2. Add new object properties
            if (addObjectProperties(experimentUri, Oeso.RELATION_MEASURES.toString(), variables, experimentUri)) {
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
        //Add links to sensors if needed
        if (!sensors.isEmpty()) {
            UpdateBuilder updateBuilder = new UpdateBuilder();
            Node graph = NodeFactory.createURI(experimentUri);
            Resource experiment = ResourceFactory.createResource(experimentUri);
            Property participatesIn = ResourceFactory.createProperty(Oeso.RELATION_PARTICIPATES_IN.toString());

            sensors.forEach((sensor) -> {
                Resource sensorUri = ResourceFactory.createResource(sensor);

                updateBuilder.addInsert(graph, sensorUri, participatesIn, experiment);
            });

            UpdateRequest updateRequest = updateBuilder.buildRequest();
            LOGGER.debug(SPARQL_QUERY + updateRequest.toString());

            //Insert the properties in the triplestore
            Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, updateRequest.toString());
            try {
                prepareUpdate.execute();
            } catch (UpdateExecutionException ex) {
                LOGGER.error("Add object properties error : " + ex.getMessage());
                return false;
            }
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
        //1. Generates delete query (if needed)
        if (!sensors.isEmpty()) {
            UpdateBuilder deleteBuilder = new UpdateBuilder();
            Node graph = NodeFactory.createURI(experimentUri);
            Resource experiment = ResourceFactory.createResource(experimentUri);
            Property participatesIn = ResourceFactory.createProperty(Oeso.RELATION_PARTICIPATES_IN.toString());

            for (String sensor : sensors) {
                Resource sensorRes = ResourceFactory.createResource(sensor);
                deleteBuilder.addDelete(graph, sensorRes, participatesIn, experiment);
            }

            UpdateRequest delete = deleteBuilder.buildRequest();

            LOGGER.debug("delete : " + delete.toString());

            //2. Delete data in the triplestore
            Update prepareDelete = getConnection().prepareUpdate(QueryLanguage.SPARQL, delete.toString());
            try {
                prepareDelete.execute();
            } catch (UpdateExecutionException ex) {
                LOGGER.error("Delete object properties error : " + ex.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Update the list of the sensors linked to the given experiment.
     * /!\ Prerequisite : the information must have been checked before.
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
    
    /**
     * Insert the experiment
     * SILEX:warning
     * In this first version, the experiments are created in the PostgreSQL database. 
     * The only information added in the triplestore is the uri of the experiment and its type.
     * \SILEX:warning
     * @example
     * INSERT DATA {
     *      GRAPH <http://www.opensilex.org/opensilex/DMOcampaign-1> {
     *          <http://www.opensilex.org/opensilex/DMOcampaign-1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#Experiment> .
     *      }
     *  }
     * @param experiment
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(Experiment experiment) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(experiment.getUri());
        Resource experimentUri = ResourceFactory.createResource(experiment.getUri());
        Node experimentConcept = NodeFactory.createURI(Oeso.CONCEPT_EXPERIMENT.toString());
        
        spql.addInsert(graph, experimentUri, RDF.type, experimentConcept);
        
        UpdateRequest query = spql.buildRequest();
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * Insert the given experiments in the triplestore.
     * SILEX:warning
     * In this first version, the experiments are created in the PostgreSQL database. 
     * We assume that the givent experiments does not exist in the triplestore 
     * (the existance of the URI is done by ExperimentDao#checkAndInsertExperimentList)
     * \SILEX:warning
     * @param newExperiments
     * @return the insertion result with the list of the errors or the list of the URIs of the inserted experiments.
     */
    public POSTResultsReturn insertExperiments(List<Experiment> newExperiments) {
        List<Status> status = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false;
        boolean insert = true;
        
        getConnection().begin();
        for (Experiment experiment : newExperiments) {
            //Insert experiment
            UpdateRequest query = prepareInsertQuery(experiment);
            
            try {
                Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
                prepareUpdate.execute();

                createdResourcesUris.add(experiment.getUri());
            } catch (RepositoryException ex) {
                    LOGGER.error("Error during commit or rolleback Triplestore statements: ", ex);
            } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    insert = false;
                    status.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_CREATE_QUERY + " " + e.getMessage()));
            }
        }
        
        if (insert) {
            resultState = true;
            getConnection().commit();
        } else {
            getConnection().rollback();
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        results = new POSTResultsReturn(resultState, insert, true);
        results.statusList = status;
        results.setCreatedResources(createdResourcesUris);
        if (resultState && !createdResourcesUris.isEmpty()) {
            results.createdResources = createdResourcesUris;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUris.size() + " " + StatusCodeMsg.RESOURCES_CREATED));
        }
        
        return results;
    }

    @Override
    public List<Experiment> create(List<Experiment> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Experiment> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Experiment> update(List<Experiment> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Experiment find(Experiment object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Experiment findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Experiment> objects) throws DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
