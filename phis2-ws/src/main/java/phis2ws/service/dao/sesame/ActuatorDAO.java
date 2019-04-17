//******************************************************************************
//                                       ActuatorDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 17 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Actuator;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ActuatorDAO extends DAOSesame<Actuator> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ActuatorDAO.class);

    /**
     * Generates an insert query for actuators.
     * @example
     * INSERT DATA {
     *      GRAPH <http://www.opensilex.org/opensilex/set/actuators> {
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#Actuator> .
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.w3.org/2000/01/rdf-schema#label> "par03_p" .
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#hasBrand> "Skye Instruments" .
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#personInCharge> "admin@opensilex.org" .
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> "A1E345F32" .
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> "2017-06-15" .
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#inServiceDate> "2017-06-15" .
     *          <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> "2017-06-15" .
     *      }
     * }
     * @param actuator
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(Actuator actuator) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.ACTUATORS.toString());
        
        Resource actuatorUri = ResourceFactory.createResource(actuator.getUri());
        
        Node sensorType = NodeFactory.createURI(actuator.getRdfType());
        
        spql.addInsert(graph, actuatorUri, RDF.type, sensorType);
        spql.addInsert(graph, actuatorUri, RDFS.label, actuator.getLabel());
        
        Property relationHasBrand = ResourceFactory.createProperty(Oeso.RELATION_HAS_BRAND.toString());
        Property relationPersonInCharge = ResourceFactory.createProperty(Oeso.RELATION_PERSON_IN_CHARGE.toString());
        
        spql.addInsert(graph, actuatorUri, relationHasBrand, actuator.getBrand() );
        spql.addInsert(graph, actuatorUri, relationPersonInCharge, actuator.getPersonInCharge() );
        
        if (actuator.getSerialNumber() != null) {
            Property relationSerialNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_SERIAL_NUMBER.toString());
            spql.addInsert(graph, actuatorUri, relationSerialNumber, actuator.getSerialNumber() );
        }
        
        if (actuator.getDateOfPurchase() != null) {
            Property relationDateOfPurchase = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_PURCHASE.toString());
            spql.addInsert(graph, actuatorUri, relationDateOfPurchase, actuator.getDateOfPurchase() );
        }
        
        if (actuator.getInServiceDate() != null) {
           Property relationInServiceDate = ResourceFactory.createProperty(Oeso.RELATION_IN_SERVICE_DATE.toString());
           spql.addInsert(graph, actuatorUri, relationInServiceDate, actuator.getInServiceDate()); 
        }
        
        if (actuator.getDateOfLastCalibration() != null) {
            Property relationDateOfCalibration = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_LAST_CALIBRATION.toString());
            spql.addInsert(graph, actuatorUri, relationDateOfCalibration, actuator.getDateOfLastCalibration() );
        }
        
        UpdateRequest query = spql.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Inster the given actuators in the triplestore.
     * @param actuators
     * @return The list of actuators inserted
     * @throws Exception 
     */
    @Override
    public List<Actuator> create(List<Actuator> actuators) throws Exception {
        UriGenerator uriGenerator = new UriGenerator();
        getConnection().begin();
        try {
            for (Actuator actuator : actuators) {
                //1. Generate the URI of the actuator
                actuator.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_ACTUATOR.toString(), null, null));

                //2. Insert actuator
                UpdateRequest updateQuery = prepareInsertQuery(actuator);
                Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, updateQuery.toString());
                prepareUpdate.execute();
            }
            
            getConnection().commit();
            
            return actuators;
        } catch (Exception ex) { //An error occurred, rollback
            getConnection().rollback();
            LOGGER.error("Error creation actuators", ex);
            throw new Exception(ex); //Throw the exception to return the error.
        }
    }
    
    /**
     * Generates a query to get the higher id of the actuators
     * @example 
     * SELECT  ?uri WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?type  . 
     *      ?type  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#Actuator> . 
     *      FILTER ( (regex(str(?uri), ".*\/2019/.*")) ) 
     *  }
     *  ORDER BY desc(?uri) 
     *  LIMIT 1
     * @return the generated query
     */
    private SPARQLQueryBuilder prepareGetLastIdFromYear(String year) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?type", null);
        query.appendTriplet("?type", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_ACTUATOR.toString(), null);
        query.appendFilter("regex(str(?uri), \".*/" + year + "/.*\")");
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Get the higher existing id of the actuators for a given year.
     * @param year
     * @return the id
     */
    public int getLastIdFromYear(String year) {
        SPARQLQueryBuilder query = prepareGetLastIdFromYear(year);

        //get last sensor uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        getConnection().close();
        
        String uriActuator = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriActuator = bindingSet.getValue(URI).stringValue();
        }
        
        if (uriActuator == null) {
            return 0;
        } else {
            //2018 -> 18. to get /s18
            String split = "/a" + year.substring(2, 4);
            String[] parts = uriActuator.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }

    @Override
    public void delete(List<Actuator> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Actuator> update(List<Actuator> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Actuator find(Actuator object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Actuator findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Actuator> objects) throws DAODataErrorAggregateException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Check if the given actuators respects the rules (rdfType subclass of Actuator, existing user, etc.)
     * @param actuators
     * @return the check result
     * @throws Exception 
     */
    private POSTResultsReturn check(List<Actuator> actuators) throws Exception {
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;
        
        //1. check if user is an admin
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
        if (userDao.isAdmin(user)) {
            //2. check data
            for (Actuator actuator : actuators) {
                //2.1 check type (subclass of Actuator)
                UriDaoSesame uriDaoSesame = new UriDaoSesame();
                if (!uriDaoSesame.isSubClassOf(actuator.getRdfType(), Oeso.CONCEPT_ACTUATOR.toString())) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Bad actuator type given. Must be sublass of Actuator concept"));
                }

                //2.2 check if person in charge exist
                User u = new User(actuator.getPersonInCharge());
                if (!userDao.existInDB(u)) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person in charge email"));
                }
            }
        } else { //user is not an admin
            dataOk = false;
            checkStatus.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, StatusCodeMsg.ADMINISTRATOR_ONLY));
        }
        
        check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatus;
        return check;
    }
    
    /**
     * Check a given list of actuators and insert them if no error founded.
     * @param actuators
     * @return the insertion result.
     */
    public POSTResultsReturn checkAndInsert(List<Actuator> actuators) {
        POSTResultsReturn insertResult;
        try {
            //1. Check the given actuators
            insertResult = check(actuators);
            //2. No error founded, insert actuators.
            if (insertResult.statusList.isEmpty()) {
            
                List<Actuator> insertedActuators = create(actuators);
                
                //The new actuators are inserted. Get the list of uri of the actuators.
                List<String> insertedActuatorsURIs = new ArrayList<>();
                for (Actuator actuator : insertedActuators) {
                    insertedActuatorsURIs.add(actuator.getUri());
                }
                
                insertResult = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
                insertResult.statusList = new ArrayList<>();
                insertResult.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, StatusCodeMsg.DATA_INSERTED));
                insertResult.createdResources = insertedActuatorsURIs;
            
            } 
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            insertResult = new POSTResultsReturn(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
            insertResult.statusList.add(new Status(ex.getMessage(), StatusCodeMsg.ERR, ex.getClass().toString()));
        }
        return insertResult;    
    }
    
}
