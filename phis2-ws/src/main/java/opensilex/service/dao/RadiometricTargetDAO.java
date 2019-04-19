//******************************************************************************
//                          RadiometricTargetDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 4 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Oeso;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.Property;
import opensilex.service.model.RadiometricTarget;

/**
 * Radiometric target DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class RadiometricTargetDAO extends Rdf4jDAO<RadiometricTarget> {
    final static Logger LOGGER = LoggerFactory.getLogger(RadiometricTargetDAO.class);
    
    // This attribute is used to search all properties of the given uri
    public String uri;
    
    //The following params are used to search in the triplestore
    public String rdfType;
    public String label;
    
    /**
     * Generates the query to get the uri and the label of the radiometric targets
     * @example
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?uri  rdf:type  <http://www.opensilex.org/vocabulary/oeso#RadiometricTarget> . 
     *      ?uri  rdfs:label  ?label  .
     * }
     * @return the query
     */
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
        query.appendTriplet(select, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_RADIOMETRIC_TARGET.toString(), null);
        query.appendTriplet(select, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        if (label != null) {
            query.appendFilter("REGEX ( ?" + LABEL + ",\".*" + label + ".*\",\"i\")");
        }
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Gets a radiometric target from a given binding set.
     * Assume that the following attributes exist : URI, label.
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
     * Gets the radiometric targets (URI, label) of the triplestore.
     * @return the list of the radiometric target found
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
     * Counts query generated by the searched parameters : URI, rdfType, 
     * label, brand, variable, inServiceDate, dateOfPurchase, dateOfLastCalibration
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?uri  rdf:type  <http://www.opensilex.org/vocabulary/oeso#RadiometricTarget> . 
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
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Counts the number of sensors by the given searched parameters.
     * @return The number of sensors 
     * @inheritdoc
     */
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
     * @see PropertyDAO
     * @return the result with the list of the found errors (empty if no error)
     */
    public POSTResultsReturn check(List<RadiometricTarget> radiometricTargets) {
        POSTResultsReturn checkResult;
        //list of the returned status
        List<Status> status = new ArrayList<>();
        boolean validData = true;
        
        //1. check if the user is an administrator
        UserDAO userDAO = new UserDAO();
        if (userDAO.isAdmin(user)) {
            PropertyDAO propertyDAO = new PropertyDAO();
            for (RadiometricTarget radiometricTarget : radiometricTargets) {
                //1. check the radiometric target if given (for example in case of an update)
                if (radiometricTarget.getUri() != null) {
                    uri = radiometricTarget.getUri();
                    ArrayList<RadiometricTarget> radiometricTargetCorresponding = allPaginate();
                    
                    //Unknown radiometric target uri
                    if (radiometricTargetCorresponding.isEmpty()) {
                        validData = false;
                        status.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, 
                                            "Unknown radiometric target uri " + radiometricTarget.getUri()));
                    }
                }
                
                //2. check properties
                for (Property property : radiometricTarget.getProperties()) {
                    //2.1 check if the property exist
                    if (existUri(property.getRelation())) {
                        //2.2 check the domain of the property
                        if (!propertyDAO.isRelationDomainCompatibleWithRdfType(property.getRelation(), Oeso.CONCEPT_RADIOMETRIC_TARGET.toString())) {
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
     * Generates an insert query for the given radiometric target.
     * @param radiometricTarget
     * @return the query
     * @example
     * INSERT DATA {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/set/radiometricTargets> { 
     *          <http://www.phenome-fppn.fr/id/radiometricTargets/rt002>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.opensilex.org/vocabulary/oeso#RadiometricTarget> . 
     *          <http://www.phenome-fppn.fr/id/radiometricTargets/rt002>  <http://www.w3.org/2000/01/rdf-schema#label>  "rt1"  . 
     *          <http://www.phenome-fppn.fr/id/radiometricTargets/rt002>  < http://www.opensilex.org/vocabulary/oeso#hasShape>  "3"  . 
     *      }
     * }
     */
    private UpdateRequest prepareInsertQuery(RadiometricTarget radiometricTarget) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.RADIOMETRIC_TARGETS.toString());
        Resource radiometricTargetUri = ResourceFactory.createResource(radiometricTarget.getUri());
        Node radiometricTargetConcept = NodeFactory.createURI(Oeso.CONCEPT_RADIOMETRIC_TARGET.toString());
        
        spql.addInsert(graph, radiometricTargetUri, RDF.type, radiometricTargetConcept);
        spql.addInsert(graph, radiometricTargetUri, RDFS.label, radiometricTarget.getLabel());
        
        for (Property property : radiometricTarget.getProperties()) {
            if (property.getValue() != null) {
                org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                
                if (property.getRdfType() != null) {
                    Node propertyValue = NodeFactory.createURI(property.getValue());
                    spql.addInsert(graph, radiometricTargetUri, propertyRelation, propertyValue);
                    spql.addInsert(graph, propertyValue, RDF.type, property.getRdfType());
                } else {
                    Literal propertyValue = ResourceFactory.createStringLiteral(property.getValue());
                    spql.addInsert(graph, radiometricTargetUri, propertyRelation, propertyValue);
                }
            }
        }
        
        UpdateRequest query = spql.buildRequest();
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * Inserts the given radiometric targets in the triplestore. 
     * /!\ Prerequisite : data must have been checked before calling this method
     * @see RadiometricTargetDAO#check(java.util.List) 
     * @param radiometricTargets
     * @return the insertion result, with the errors list or the URI of the 
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
            try {
                //Generate uri
                radiometricTarget.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_RADIOMETRIC_TARGET.toString(), null, null));
            } catch (Exception ex) { //In the radiometric target case, no exception should be raised
                insert = false;
            }
            //Insert radiometric target
            UpdateRequest query = prepareInsertQuery(radiometricTarget);
            
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
     * Checks and inserts the given radiometric targets in the triplestore.
     * @param radiometricTargets
     * @return the insertion result. Message error if errors found in data
     *         the list of the generated uri of the radiometric targets if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<RadiometricTarget> radiometricTargets) {
        POSTResultsReturn checkResult = check(radiometricTargets);
        if (checkResult.getDataState()) {
            return insert(radiometricTargets);
        } else { //errors found in data
            return checkResult;
        }
    }
    
    /**
     * Deletes the data about the given radiometric target. 
     * Delete all the occurrences of each relation of the properties of the radiometric target.
     * @param radiometricTarget
     * @example
     *  DELETE DATA {
     *    GRAPH <http://www.phenome-fppn.fr/diaphen/set/radiometricTargets> {
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.w3.org/2000/01/rdf-schema#label> "label" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#RadiometricTarget> .
     *      <http://www.opensilex.org/vocabulary/oeso#RadiometricTarget> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "http://www.w3.org/2002/07/owl#Class" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#hasBrand> "brand" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#hasRadiometricTargetMaterial> "carpet" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#hasShape> "rectangular" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> "2019-01-03" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> "2019-01-02" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#hasShapeLength> "31" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#hasShapeWidth> "45" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#hasTechnicalContact> "admin@opensilex.org" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#inServiceDate> "2019-01-01" .
     *      <http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt004> <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> "serial" .
     *    }
     *  }
     * @return the query
     */
    private UpdateRequest prepareDeleteQuery(RadiometricTarget radiometricTarget) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.RADIOMETRIC_TARGETS.toString());
        Resource radiometricTargetUri = ResourceFactory.createResource(radiometricTarget.getUri());
        
        spql.addDelete(graph, radiometricTargetUri, RDFS.label, radiometricTarget.getLabel());
        
        for (Property property : radiometricTarget.getProperties()) {
            if (property.getValue() != null) {
                org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                
                if (property.getRdfType() != null) {
                    Node propertyValue = NodeFactory.createURI(property.getValue());
                    spql.addDelete(graph, radiometricTargetUri, propertyRelation, propertyValue);
                    spql.addDelete(graph, propertyValue, RDF.type, property.getRdfType());
                } else {
                    Literal propertyValue = ResourceFactory.createStringLiteral(property.getValue());
                    spql.addDelete(graph, radiometricTargetUri, propertyRelation, propertyValue);
                }
            }
        }
        
        UpdateRequest request = spql.buildRequest();
        LOGGER.debug(request.toString());
        
        return request;
    }
    
    /**
     * Gets the radiometric target information using it's URI.
     * /!\ Prerequisite : the URI must have been checked and it must exist 
     *                    before calling this method.
     * @param radiometricTargetUri
     * @return the Radiometric Target information
     */
    public RadiometricTarget getRadiometricTarget(String radiometricTargetUri) {
        PropertyDAO propertyDAO = new PropertyDAO();
        RadiometricTarget radiometricTarget = new RadiometricTarget();
        propertyDAO.getAllPropertiesWithLabels(radiometricTarget, null);
        return radiometricTarget;
    }
    
    /**
     * Compares the new radiometric target given and the old one.
     * Check if some properties are missing in the new radiometric target.
     * Used for a radiometric target update update.
     * @see RadiometricTargetDAO#updateAndReturnPOSTResultsReturn(java.util.List)
     * @param newRadiometricTargetData the new radiometric target data
     * @param oldRadiometricTargetData the old radiometric target data
     * @return the list of the status with the errors if some has been found.
     *         null if no error found
     */
    private List<Status> compareNewAndOldRadiometricTarget(RadiometricTarget newRadiometricTargetData, RadiometricTarget oldRadiometricTargetData) {
        //Check each new radiometric target property to see if it has the same number or more occurences than the oldRadiometric.
        ArrayList<Status> status = new ArrayList<>();
        
        //1. Count the number of occurences of each property in each radiometric target
        HashMap<String, Integer> newOccurrences = new HashMap<>();
        HashMap<String, Integer> oldOccurrences = new HashMap<>();
        
        for (Property property : newRadiometricTargetData.getProperties()) {
            if (newOccurrences.containsKey(property.getRelation())) {
                Integer nbOccurrences = newOccurrences.get(property.getRelation()) + 1;
                newOccurrences.put(property.getRelation(), nbOccurrences);
            } else {
                newOccurrences.put(property.getRelation(), 1);
            }
        }
        
        for (Property property : oldRadiometricTargetData.getProperties()) {
            if (oldOccurrences.containsKey(property.getRelation())) {
                Integer nbOccurrences = oldOccurrences.get(property.getRelation()) + 1;
                oldOccurrences.put(property.getRelation(), nbOccurrences);
            } else {
                oldOccurrences.put(property.getRelation(), 1);
            }
        }
        
        //2. Compare numbers
        newOccurrences.forEach((relation, nbOccurrences) -> {
            //If the oldOccurrences contains the relation and has more occurrences 
            //of its relation than the newOccurrences, something is wrong
            if (oldOccurrences.containsKey(relation) && oldOccurrences.get(relation) > nbOccurrences) {
                status.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Ambiguity on the values for the property : " + relation 
                        + ". There are less occcurrences of the property in the updated data than before (" + nbOccurrences + " instead of " + oldOccurrences.get(relation) + ")"));
            }
        });
        
        return status;
    }
    
    /**
     * Updates the given radiometric targets in the triplestore. 
     * /!\ Prerequisite : data must have been checked before calling this method.
     * @see RadiometricTargetDAO#check(java.util.List)
     * @param radiometricTargets the list of the radiometric targets to update
     * @return the update result with the list of all the updated radiometric targets.
     */
    private POSTResultsReturn updateAndReturnPOSTResultsReturn(List<RadiometricTarget> radiometricTargets) {
        //SILEX:info
        //If a property of a radiometric target has a null value, 
        //it will be deleted from the triplestore
        //\SILEX:info
        List<Status> updateStatus = new ArrayList<>();
        List<String> updatedResourcesUri = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        getConnection().begin();
        for (RadiometricTarget radiometricTarget : radiometricTargets) {
            //1. get the old radiometric target data
            RadiometricTarget oldRadiometricTarget = getRadiometricTarget(radiometricTarget.getUri());
            //2. check the new given data
            //SILEX:info
            //The client must send the same number of each property than before the update. 
            //If the number of properties isn't equal to the number of properties 
            //in the triplestore, an error will be returned.
            //If a property has a null value, the relation will be deleted. 
            //\SILEX:info
            List<Status> compareNewToOldRadiometricTarget = compareNewAndOldRadiometricTarget(radiometricTarget, oldRadiometricTarget);
            if (compareNewToOldRadiometricTarget.isEmpty()) { //No error has been found
                //1. genereate query to delete already existing data
                //SILEX:info
                //We only delete the already existing data received by the client. 
                //It means that we delete only the properties given by the client.
                //\SILEX:info
                UpdateRequest deleteQuery = prepareDeleteQuery(oldRadiometricTarget);

                //2. generate query to insert new data
                //SILEX:info
                //Insert only the triplets with a not null value. 
                //\SILEX:info
                UpdateRequest insertQuery = prepareInsertQuery(radiometricTarget);            
                try {
                    Update prepareDelete = getConnection().prepareUpdate(deleteQuery.toString());
                    Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
                    prepareDelete.execute();
                    prepareUpdate.execute();
                    updatedResourcesUri.add(radiometricTarget.getUri());
                } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationUpdate = false;
                    updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                }
            } else { //errors has been found
                updateStatus.addAll(compareNewToOldRadiometricTarget);
                annotationUpdate = false;
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
     * Updates the given radiometric targets in the triplestore.
     * @param radiometricTargets
     * @return the update result. Message error if errors found in data
     *         the list of the generated URI of the radiometric targets if the update has been done
     */
    public POSTResultsReturn checkAndUpdate(List<RadiometricTarget> radiometricTargets) {
        POSTResultsReturn checkResult = check(radiometricTargets);
        if (checkResult.getDataState()) {
            return updateAndReturnPOSTResultsReturn(radiometricTargets);
        } else { //errors found in data
            return checkResult;
        }
    }
    
    /**
     * Prepares a query to get the higher id of the radiometric targets.
     * @example 
     * SELECT ?uri WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#RadiometricTarget> . 
     * }
     * ORDER BY DESC(?uri) 
     * @return the query
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_RADIOMETRIC_TARGET.toString(), null);
        query.appendOrderBy("DESC(?" + URI + ")");
        query.appendLimit(1);
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;
    }
    
    /**
     * Gets the higher id of the radiometric targets.
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

    @Override
    public List<RadiometricTarget> create(List<RadiometricTarget> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<RadiometricTarget> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<RadiometricTarget> update(List<RadiometricTarget> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RadiometricTarget find(RadiometricTarget object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RadiometricTarget findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<RadiometricTarget> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
