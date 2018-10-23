//******************************************************************************
//                                       RadiometricTargetDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 4 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Property;
import phis2ws.service.view.model.phis.RadiometricTarget;

/**
 * Allows CRUD methods of radiometric target in the triplestore.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class RadiometricTargetDAOSesame extends DAOSesame<RadiometricTarget> {
    final static Logger LOGGER = LoggerFactory.getLogger(RadiometricTargetDAOSesame.class);
    
    // This attribute is used to search all properties of the given uri
    public String uri;
    
    //The following params are used to search in the triplestore
    public String rdfType;
    public String label;
    
    /**
     * Generates the query to get the uri and the label of the radiometric targets
     * @example
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?uri  rdf:type  <http://www.phenome-fppn.fr/vocabulary/2017#RadiometricTarget> . 
     *      ?uri  rdfs:label  ?label  .
     * }
     * @return the query
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String select;
        if (uri != null) {
            select = "<" + uri + ">";
        } else {
            select = "?" + URI;
            query.appendSelect(select);
        }
        
        query.appendSelect("?" + LABEL);
        query.appendTriplet(select, Rdf.RELATION_TYPE.toString(), Vocabulary.CONCEPT_RADIOMETRIC_TARGET.toString(), null);
        query.appendTriplet(select, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        if (label != null) {
            query.appendFilter("REGEX ( ?" + LABEL + ",\".*" + label + ".*\",\"i\")");
        }
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Get a radiometric target from a given binding set.
     * Assume that the following attributes exist : uri, label.
     * @param bindingSet a binding set, result from a search query
     * @return a radiometric target with data extracted from the given binding set
     */
    private RadiometricTarget getFromBindingSet(BindingSet bindingSet) {
        RadiometricTarget radiometricTarget = new RadiometricTarget();
        
        if (uri != null) {
            radiometricTarget.setUri(uri);
        } else {
            radiometricTarget.setUri(bindingSet.getValue(URI).stringValue());
        }
        
        radiometricTarget.setLabel(bindingSet.getValue(LABEL).stringValue());
        
        return radiometricTarget;
    }
    
    /**
     * Get the radiometric targets (uri, label) of the triplestore.
     * @return the list of the radiometric target founded
     */
    public ArrayList<RadiometricTarget> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<RadiometricTarget> radiometricTargets;

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            radiometricTargets = new ArrayList<>();
            
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                RadiometricTarget radiometricTarget = getFromBindingSet(bindingSet);
                radiometricTargets.add(radiometricTarget);
            }
        }
        return radiometricTargets;
    }

    /**
     * Count query generated by the searched parameters : uri, rdfType, 
     * label, brand, variable, inServiceDate, dateOfPurchase, dateOfLastCalibration
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?uri  rdf:type  <http://www.phenome-fppn.fr/vocabulary/2017#RadiometricTarget> . 
     *      ?uri  rdfs:label  ?label  . 
     * }
     * @return Query generated to count the elements, with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount() {
        SPARQLQueryBuilder query = this.prepareSearchQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Count the number of sensors by the given searched params : uri, rdfType, 
     * label, brand, variable, inServiceDate, dateOfPurchase, dateOfLastCalibration
     * @return The number of sensors 
     * @inheritdoc
     */
    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        SPARQLQueryBuilder prepareCount = prepareCount();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, prepareCount.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        return count;
    }
    
    /**
     * Check the given list of radiometric targets (check properties domain, etc.)
     * @param radiometricTargets
     * @see PropertyDAOSesame
     * @return the result with the list of the founded errors (empty if no error)
     */
    public POSTResultsReturn check(List<RadiometricTarget> radiometricTargets) {
        POSTResultsReturn checkResult = null;
        //list of the returned status
        List<Status> status = new ArrayList<>();
        boolean validData = true;
        
        //1. check if the user is an administrator
        UserDaoPhisBrapi userDAO = new UserDaoPhisBrapi();
        if (userDAO.isAdmin(user)) {
            PropertyDAOSesame propertyDAO = new PropertyDAOSesame();
            for (RadiometricTarget radiometricTarget : radiometricTargets) {
                //1. check the radiometric target if given (for example in case of an update)
                if (radiometricTarget.getUri() != null) {
                    //TODO : use the allPaginate developped by Vincent
                    uri = radiometricTarget.getUri();
                    ArrayList<RadiometricTarget> radiometricTargetCorresponding = allPaginate();
                    
                    //Unknown radiometric target uri
                    if (radiometricTargetCorresponding.isEmpty()) {
                        validData = false;
                        status.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, 
                                            "Unknwon radiometric target uri " + radiometricTarget.getUri()));
                    }
                }
                
                //2. check properties
                for (Property property : radiometricTarget.getProperties()) {
                    //2.1 check if the property exist
                    if (existUri(property.getRelation())) {
                        //2.2 check the domain of the property
                        propertyDAO.relation = property.getRelation();
                        if (!propertyDAO.isRelationDomainCompatibleWithRdfType(Vocabulary.CONCEPT_RADIOMETRIC_TARGET.toString())) {
                            validData = false;
                            status.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, 
                                            "the type of the given uri is not in the domain of the relation " + property.getRelation()));
                        }
                    } else {
                        validData = false;
                        status.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, 
                                            StatusCodeMsg.UNKNOWN_URI + " " + property.getRelation()));
                    }
                    //SILEX:todo
                    //add the check range and the cardinality check
                    //\SILEX:todo
                    
                    //SILEX:todo
                    //add the check buisiness rules (e.g. the size property depends on the shape type)
                    //\SILEX:todo
                }
            }
        } else {
            validData = false;
            status.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, StatusCodeMsg.ADMINISTRATOR_ONLY));
        }
        
        checkResult = new POSTResultsReturn(validData, null, validData);
        checkResult.statusList = status;
        return checkResult;   
    }
    
    /**
     * Generates an insert query for the given radiometric target
     * @param radiometricTarget
     * @return the query
     * @example
     * INSERT DATA {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/set/radiometricTargets> { 
     *          <http://www.phenome-fppn.fr/id/radiometricTargets/rt002>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.phenome-fppn.fr/vocabulary/2017#RadiometricTarget> . 
     *          <http://www.phenome-fppn.fr/id/radiometricTargets/rt002>  <http://www.w3.org/2000/01/rdf-schema#label>  "rt1"  . 
     *          <http://www.phenome-fppn.fr/id/radiometricTargets/rt002>  < http://www.phenome-fppn.fr/vocabulary/2017#hasShape>  "3"  . 
     *      }
     * }
     */
    private SPARQLUpdateBuilder prepareInsertQuery(RadiometricTarget radiometricTarget) {
        SPARQLUpdateBuilder query = new SPARQLUpdateBuilder();
        
        query.appendGraphURI(Contexts.RADIOMETRIC_TARGETS.toString());
        query.appendTriplet(radiometricTarget.getUri(), Rdf.RELATION_TYPE.toString(), Vocabulary.CONCEPT_RADIOMETRIC_TARGET.toString(), null);
        query.appendTriplet(radiometricTarget.getUri(), Rdfs.RELATION_LABEL.toString(), "\"" + radiometricTarget.getLabel() + "\"", null);
        
        for (Property property : radiometricTarget.getProperties()) {
            if (property.getValue() != null) {
                if (property.getRdfType() != null) {
                    query.appendTriplet(radiometricTarget.getUri(), property.getRelation(), property.getValue(), null);
                    query.appendTriplet(property.getValue(), Rdf.RELATION_TYPE.toString(), property.getRdfType(), null);
                } else {
                    query.appendTriplet(radiometricTarget.getUri(), property.getRelation(), "\"" + property.getValue() + "\"", null);
                }
            }
        }
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Insert the given radiometric targets in the triplestore. 
     * /!\ Prerequisite : data must have been checked before calling this method
     * @see RadiometricTargetDAOSesame#check(java.util.List) 
     * @param radiometricTargets
     * @return the insertion result, with the errors list or the uri of the 
     *         radiometric targets inserted
     */
    private POSTResultsReturn insert(List<RadiometricTarget> radiometricTargets) {
        List<Status> status = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false;
        boolean insert = true;
        
        UriGenerator uriGenerator = new UriGenerator();
        
        getConnection().begin();
        for (RadiometricTarget radiometricTarget : radiometricTargets) {
            //Generate uri
            radiometricTarget.setUri(uriGenerator.generateNewInstanceUri(Vocabulary.CONCEPT_RADIOMETRIC_TARGET.toString(), null, null));
            //Insert radiometric target
            SPARQLUpdateBuilder query = prepareInsertQuery(radiometricTarget);
            
            try {
                Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
                prepareUpdate.execute();

                createdResourcesUris.add(radiometricTarget.getUri());
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
    
    /**
     * Check and insert the given radiometric targets in the triplestore
     * @param radiometricTargets
     * @return the insertion result. Message error if errors founded in data
     *         the list of the generated uri of the radiometric targets if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<RadiometricTarget> radiometricTargets) {
        POSTResultsReturn checkResult = check(radiometricTargets);
        if (checkResult.getDataState()) {
            return insert(radiometricTargets);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * 
     * @param radiometricTarget
     * @return 
     */
    private String prepareDeleteQuery(RadiometricTarget radiometricTarget) {
        String query = "DELETE WHERE {"
                + "<" + radiometricTarget.getUri() + "> <" + Rdfs.RELATION_LABEL.toString() + "> \"" + radiometricTarget.getLabel() + "\" . ";
        
        for (Property property : radiometricTarget.getProperties()) {
            query += "<" + radiometricTarget.getUri() + ">";
        }
                
        query += "}";
        
        LOGGER.debug(query);
        
        return query;
    }
    
    /**
     * Update the given radiometric targets in the triplestore. 
     * /!\ Prerequisite : data must have been checked before calling this method.
     * @see RadiometricTargetDAOSesame#check(java.util.List)
     * @param radiometricTargets
     * @return the update result with the list of all the updated radiometric targets.
     */
    private POSTResultsReturn update(List<RadiometricTarget> radiometricTargets) {
        //SILEX:info
        //If a property of a radiometric target has a null value, 
        //it will be deleted from the triplestore
        //\SILEX:info
        List<Status> updateStatus = new ArrayList<>();
        List<String> updatedResourcesUri = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        this.getConnection().begin();
        for (RadiometricTarget radiometricTarget : radiometricTargets) {
            //1. genereate query to delete already existing data
            //SILEX:info
            //We only delete the already existing data received by the client. 
            //It means that we delete only the properties given by the client.
            //\SILEX:info
            String deleteQuery = prepareDeleteQuery(radiometricTarget);
            
            //2. generate query to insert new data
            //SILEX:info
            //(insert only the triplets with a not null value)
            //\SILEX:info
            SPARQLUpdateBuilder insertQuery = prepareInsertQuery(radiometricTarget);            
            try {
                Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
                prepareDelete.execute();
                prepareUpdate.execute();
                updatedResourcesUri.add(radiometricTarget.getUri());
            } catch (MalformedQueryException e) {
                LOGGER.error(e.getMessage(), e);
                annotationUpdate = false;
                updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
            }
        }
        
        if (annotationUpdate) {
            resultState = true;
            try {
                this.getConnection().commit();
            } catch (RepositoryException ex) {
                LOGGER.error("Error during commit Triplestore statements: ", ex);
            }
        } else {
            try {
                this.getConnection().rollback();
            } catch (RepositoryException ex) {
                LOGGER.error("Error during rollback Triplestore statements : ", ex);
            }
        }
        
        if (this.getConnection() != null) {
            this.getConnection().close();
        }
        
        results = new POSTResultsReturn(resultState, annotationUpdate, true);
        results.statusList = updateStatus;
        if (resultState && !updatedResourcesUri.isEmpty()) {
            results.createdResources = updatedResourcesUri;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, updatedResourcesUri.size() + " resources updated"));
        }
        
        return results;
    }
    
    /**
     * Update the given radiometric targets in the triplestore
     * @param radiometricTargets
     * @return the update result. Message error if errors founded in data
     *         the list of the generated uri of the radiometric targets if the update has been done
     */
    public POSTResultsReturn checkAndUpdate(List<RadiometricTarget> radiometricTargets) {
        //TODO : passer en RadiometricTarget - le check doit se faire sur le modèle
        POSTResultsReturn checkResult = check(radiometricTargets);
        if (checkResult.getDataState()) {
            return update(radiometricTargets);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * Prepare a query to get the higher id of the radiometric targets.
     * @example 
     * SELECT ?uri WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.phenome-fppn.fr/vocabulary/2017#RadiometricTarget> . 
     * }
     * ORDER BY DESC(?uri) 
     * @return the query
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), Vocabulary.CONCEPT_RADIOMETRIC_TARGET.toString(), null);
        query.appendOrderBy("DESC(?" + URI + ")");
        query.appendLimit(1);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        
        return query;
    }
    
    /**
     * Get the higher id of the radiometric targets
     * @return the id
     */
    public int getLastId() {
        SPARQLQueryBuilder query = prepareGetLastId();
        //get last unit uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();
        
        String uri = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uri = bindingSet.getValue(URI).stringValue();
        }
        
        if (uri == null) {
            return 0;
        } else {
            String split = "radiometricTargets/rt";
            String[] parts = uri.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }
}
