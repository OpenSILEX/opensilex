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
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
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
     * @param sensor The experiment uri which measures veriables
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
     * Return a HashMap of uri => label of the variables linked to an experiment.
     * 
     * @param experimentUri The experiment uri which measures veriables
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
     * Update the list of the variables linked to the given experiment.
     * /!\ Prerequisite : the data must have been checked before.
     * @see ExperimentDao#checkAndUpdateObservedVariables(java.lang.String, java.util.List) 
     * @param experimentUri
     * @param variables
     * @return The update result.
     */
    public POSTResultsReturn updateObservedVariables(String experimentUri, List<String> variables) {
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
}
