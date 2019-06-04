//******************************************************************************
//                                       ActuatorDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 17 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Actuator;
import opensilex.service.model.Dataset;
import opensilex.service.model.User;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
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
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for the actuators. They are stored in the trisplestore.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ActuatorDAO extends Rdf4jDAO<Actuator> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ActuatorDAO.class);
    
    //Constants to query the triplestore.
    private final String BRAND = "brand";
    private final String SERIAL_NUMBER = "serialNumber";
    private final String IN_SERVICE_DATE = "inServiceDate";
    private final String MODEL = "model";
    private final String DATE_OF_PURCHASE = "dateOfPurchase";
    private final String DATE_OF_LAST_CALIBRATION = "dateOfLastCalibration";
    private final String PERSON_IN_CHARGE = "personInCharge";

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
        
        if (actuator.getModel() != null) {
            Property relationModel = ResourceFactory.createProperty(Oeso.RELATION_HAS_MODEL.toString());
            spql.addInsert(graph, actuatorUri, relationModel, actuator.getModel());
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
     * Insert the given actuators in the triplestore.
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
    
    /**
     * Generates a query to delete the given actuator.
     * @param actuator
     * @example
     *      DELETE DATA {
     *          GRAPH <http://www.opensilex.org/opensilex/set/actuators> {
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#Actuator> .
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.w3.org/2000/01/rdf-schema#label> "parq03_p" .
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#hasBrand> "Skye Instruments" .
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#personInCharge> "guest@opensilex.org" .
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> "A1E45F32" .
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> "2017-06-15" .
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#inServiceDate> "2017-06-25" .
     *              <http://www.opensilex.org/opensilex/2019/a19001> <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> "2017-06-30" .
     *      }
     * }
     * @return the generated query
     */
    private UpdateRequest prepareDeleteQuery(Actuator actuator) {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.ACTUATORS.toString());
        
        Resource actuatorUri = ResourceFactory.createResource(actuator.getUri());
        
        Node actuatorType = NodeFactory.createURI(actuator.getRdfType());
        
        updateBuilder.addDelete(graph, actuatorUri, RDF.type, actuatorType);
        updateBuilder.addDelete(graph, actuatorUri, RDFS.label, actuator.getLabel());
        
        Property relationHasBrand = ResourceFactory.createProperty(Oeso.RELATION_HAS_BRAND.toString());
        Property relationPersonInCharge = ResourceFactory.createProperty(Oeso.RELATION_PERSON_IN_CHARGE.toString());
        
        updateBuilder.addDelete(graph, actuatorUri, relationHasBrand, actuator.getBrand() );
        updateBuilder.addDelete(graph, actuatorUri, relationPersonInCharge, actuator.getPersonInCharge() );
        
        if (actuator.getSerialNumber() != null) {
            Property relationSerialNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_SERIAL_NUMBER.toString());
            updateBuilder.addDelete(graph, actuatorUri, relationSerialNumber, actuator.getSerialNumber() );
        }
        
        if (actuator.getModel()!= null) {
            Property relationModel = ResourceFactory.createProperty(Oeso.RELATION_HAS_MODEL.toString());
            updateBuilder.addDelete(graph, actuatorUri, relationModel, actuator.getModel());
        }
        
        if (actuator.getDateOfPurchase() != null) {
            Property relationDateOfPurchase = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_PURCHASE.toString());
            updateBuilder.addDelete(graph, actuatorUri, relationDateOfPurchase, actuator.getDateOfPurchase() );
        }
        
        if (actuator.getInServiceDate() != null) {
            Property relationInServiceDate = ResourceFactory.createProperty(Oeso.RELATION_IN_SERVICE_DATE.toString());
            updateBuilder.addDelete(graph, actuatorUri, relationInServiceDate, actuator.getInServiceDate() );
        }
        
        if (actuator.getDateOfLastCalibration() != null) {
            Property relationDateOfCalibration = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_LAST_CALIBRATION.toString());
            updateBuilder.addDelete(graph, actuatorUri, relationDateOfCalibration, actuator.getDateOfLastCalibration() );
        }
        
        UpdateRequest query = updateBuilder.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + query.toString());
        return query;
    }

    /**
     * Update the list of the actuators given. 
     * /!\ Prerequisite : the actuators must have been checked before, using the check method.
     * @see ActuatorDAO#check(java.util.List) 
     * @param actuators
     * @return the list of the actuators updated.
     * @throws Exception 
     */
    @Override
    public List<Actuator> update(List<Actuator> actuators) throws Exception {
        getConnection().begin();
        try {
            for (Actuator actuator : actuators) {
                //1. Get old actuator data
                Actuator oldActuator = findById(actuator.getUri());
                
                //2. Delete old actuators data
                UpdateRequest deleteQuery = prepareDeleteQuery(oldActuator);
                Update prepareDelete = getConnection().prepareUpdate(deleteQuery.toString());
                prepareDelete.execute();
                
                //2. Insert new actuators data
                UpdateRequest insertQuery = prepareInsertQuery(actuator);
                Update prepareUpdate = getConnection().prepareUpdate(insertQuery.toString());
                prepareUpdate.execute();
            }
            
            getConnection().commit();
            
            return actuators;
        } catch (RepositoryException ex) { //An error occurred, rollback
            getConnection().rollback();
            LOGGER.error("Error update actuators", ex);
            throw new RepositoryException(ex); //Throw the exception to return the error.
        } catch (NotFoundException ex) { //An actuator was not found.
            getConnection().rollback();
            LOGGER.error("Error update actuators", ex);
            throw new NotFoundException(ex); //Throw the exception to return the error.
        }
    }

    @Override
    public Actuator find(Actuator object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Genrates the query to search actuators by the search parameters given.
     * @param page
     * @param pageSize
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param model
     * @param inServiceDate
     * @param dateOfPurchase
     * @param dateOfLastCalibration
     * @param personInCharge
     * @example
     * SELECT DISTINCT  ?rdfType ?uri ?label ?brand ?serialNumber ?model ?inServiceDate 
     * ?dateOfPurchase ?dateOfLastCalibration ?personInCharge 
     * WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *      ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#Actuator> . 
     *      OPTIONAL {
     *          ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#hasBrand> ?brand . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> ?serialNumber . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#hasModel> ?model . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> ?dateOfPurchase . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> ?dateOfLastCalibration . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#personInCharge> ?personInCharge . 
     *      }
     *      FILTER ( (REGEX ( str(?uri),".*op.*","i")) ) 
     * }
     * LIMIT 20 
     * OFFSET 0 
     * @return the generated query
     */
    protected SPARQLQueryBuilder prepareSearchQuery(Integer page, Integer pageSize, String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        //RDF Type filter
        if (rdfType == null) {
            rdfType = "";
        }
        query.appendSelect("?" + RDF_TYPE);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_ACTUATOR.toString(), null);
        query.appendAndFilter("REGEX ( str(?" + RDF_TYPE + "),\".*" + rdfType + ".*\",\"i\")");
        
        //URI filter
        if (uri == null) {
            uri = "";
        }
        query.appendSelect("?" + URI);
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
        
        //Label filter
        query.appendSelect("?" + LABEL);
        if (label == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + LABEL + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
            query.appendAndFilter("REGEX ( str(?" + LABEL + "),\".*" + label + ".*\",\"i\")");
        }
        
        //Brand filter
        query.appendSelect("?" + BRAND);
        if (brand == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_BRAND.toString() + "> " + "?" + BRAND + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_BRAND.toString(), "?" + BRAND, null);
            query.appendAndFilter("REGEX ( str(?" + BRAND + "),\".*" + brand + ".*\",\"i\")");
        }
        
        //Serial number filter
        query.appendSelect("?" + SERIAL_NUMBER);
        if (serialNumber == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_SERIAL_NUMBER.toString() + "> " + "?" + SERIAL_NUMBER + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_SERIAL_NUMBER.toString(), "?" + SERIAL_NUMBER, null);
            query.appendAndFilter("REGEX ( str(?" + SERIAL_NUMBER + "),\".*" + serialNumber + ".*\",\"i\")");
        }
        
        //Model filter
        query.appendSelect("?" + MODEL);
        if (model == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_MODEL.toString() + "> " + "?" + MODEL + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_MODEL.toString(), "?" + MODEL, null);
            query.appendAndFilter("REGEX ( str(?" + MODEL + "),\".*" + model + ".*\",\"i\")");
        }
        
        //In service date filter
        query.appendSelect("?" + IN_SERVICE_DATE);
        if (inServiceDate == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_IN_SERVICE_DATE.toString() + "> " + "?" + IN_SERVICE_DATE + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_IN_SERVICE_DATE.toString(), "?" + IN_SERVICE_DATE, null);
            query.appendAndFilter("REGEX ( str(?" + IN_SERVICE_DATE + "),\".*" + inServiceDate + ".*\",\"i\")");
        }
        
        //Date of purchase filter
        query.appendSelect("?" + DATE_OF_PURCHASE);
        if (dateOfPurchase == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_DATE_OF_PURCHASE.toString() + "> " + "?" + DATE_OF_PURCHASE + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_DATE_OF_PURCHASE.toString(), "?" + DATE_OF_PURCHASE, null);
            query.appendAndFilter("REGEX ( str(?" + DATE_OF_PURCHASE + "),\".*" + dateOfPurchase + ".*\",\"i\")");
        }
        
        //Date of last calibration filter
        query.appendSelect("?" + DATE_OF_LAST_CALIBRATION);
        if (dateOfLastCalibration == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_DATE_OF_LAST_CALIBRATION.toString() + "> " + "?" + DATE_OF_LAST_CALIBRATION + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_DATE_OF_LAST_CALIBRATION.toString(), "?" + DATE_OF_LAST_CALIBRATION, null);
            query.appendAndFilter("REGEX ( str(?" + DATE_OF_LAST_CALIBRATION + "),\".*" + dateOfLastCalibration + ".*\",\"i\")");
        }
        
        //Person in charge filter
        query.appendSelect("?" + PERSON_IN_CHARGE);
        if (personInCharge == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_PERSON_IN_CHARGE.toString() + "> " + "?" + PERSON_IN_CHARGE + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_PERSON_IN_CHARGE.toString(), "?" + PERSON_IN_CHARGE, null);
            query.appendAndFilter("REGEX ( str(?" + PERSON_IN_CHARGE + "),\".*" + personInCharge + ".*\",\"i\")");
        }
        
        if (page != null && pageSize != null) {
            query.appendLimit(pageSize);
            query.appendOffset(page * pageSize);
        }

        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Get an actuator from a binding set given resulting from a query to the triplestore.
     * @param bindingSet
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param model
     * @param inServiceDate
     * @param dateOfPurchase
     * @param dateOfLastCalibration
     * @param personInCharge
     * @return 
     */
    private Actuator getActuatorFromBindingSet(BindingSet bindingSet, String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) {
        Actuator actuator = new Actuator();

        if (bindingSet.getValue(URI) != null) {
            actuator.setUri(bindingSet.getValue(URI).stringValue());
        }

        if (bindingSet.getValue(RDF_TYPE) != null) {
            actuator.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
        }

        if (bindingSet.getValue(LABEL) != null ){
            actuator.setLabel(bindingSet.getValue(LABEL).stringValue());
        }

        if (bindingSet.getValue(BRAND) != null) {
            actuator.setBrand(bindingSet.getValue(BRAND).stringValue());
        }
        
        if (bindingSet.getValue(SERIAL_NUMBER) != null) {
            actuator.setSerialNumber(bindingSet.getValue(SERIAL_NUMBER).stringValue());
        }
        
        if(bindingSet.getValue(MODEL) != null) {
            actuator.setModel(bindingSet.getValue(MODEL).stringValue());
        }

        if (bindingSet.getValue(IN_SERVICE_DATE) != null) {
            actuator.setInServiceDate(bindingSet.getValue(IN_SERVICE_DATE).stringValue());
        }

        if (bindingSet.getValue(DATE_OF_PURCHASE) != null) {
            actuator.setDateOfPurchase(bindingSet.getValue(DATE_OF_PURCHASE).stringValue());
        }

        if (bindingSet.getValue(DATE_OF_LAST_CALIBRATION) != null) {
            actuator.setDateOfLastCalibration(bindingSet.getValue(DATE_OF_LAST_CALIBRATION).stringValue());
        }
        
        if (bindingSet.getValue(PERSON_IN_CHARGE) != null) {
            actuator.setPersonInCharge(bindingSet.getValue(PERSON_IN_CHARGE).stringValue());
        }

        return actuator;
    }
    
    /**
     * Prepare the SPARQL query to return all variables measured by an actuator.
     * @param actuatorUri The actuator uri
     * @return The prepared query
     * @example 
     * SELECT  ?uri ?label WHERE {
     *      ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#Variable> . 
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *      ?uri  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . 
     *      <http://www.opensilex.org/opensilex/2019/a19001>  <http://www.opensilex.org/vocabulary/oeso#measures>  ?uri  . 
     * }
     */
    private SPARQLQueryBuilder prepareSearchVariablesQuery(String actuatorUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_VARIABLE.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendTriplet(actuatorUri, Oeso.RELATION_MEASURES.toString(), "?" + URI, null);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Return a HashMap of uri => label of the variables measured by the given actuator.
     * @param actuator The actuator uri
     * @return HashMap of uri => label
     */
    private HashMap<String, String> getVariables(String actuatorUri) {
        SPARQLQueryBuilder query = prepareSearchVariablesQuery(actuatorUri);
        
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
     * Find an actuator by its URI.
     * @param id
     * @return the actuator if founded.
     * @throws Exception 
     */
    @Override
    public Actuator findById(String id) throws Exception {
        SPARQLQueryBuilder findQuery = prepareSearchQuery(null, null, id, null, null, null, null, null, null, null, null, null);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, findQuery.toString());
        
        Actuator actuator = new Actuator();
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                actuator = getActuatorFromBindingSet(result.next(), id, null, null, null, null, null, null, null, null, null);
                
                //get variables associated to the actuator
                HashMap<String, String> variables = getVariables(actuator.getUri());
                actuator.setVariables(variables);
            } else {
                throw new NotFoundException(id + " not found.");
            }
        }
        return actuator;
        
    }

    @Override
    public void validate(List<Actuator> objects) throws DAODataErrorAggregateException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Generates a query to check if a given uri is an actuator.
     * @param uri
     * @example 
     * @return the generated query.
     */
    private SPARQLQueryBuilder prepareIsActuatorQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_ACTUATOR.toString(), null);
        
        query.appendAsk("");
        LOGGER.debug(query.toString());
        return query;
    }
    
    /**
     * Check if the given uri exist in the triplestore and is an actuator.
     * @param uri
     * @return true if the sensor exist and is an actuator
     *         false if not.
     */
    public boolean existAndIsActuator(String uri) {
        if (existUri(uri)) {
            SPARQLQueryBuilder query = prepareIsActuatorQuery(uri);
            BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());

            return booleanQuery.evaluate();
        } else {
            return false;
        }
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
        UserDAO userDao = new UserDAO();
        if (userDao.isAdmin(user)) {
            //2. check data
            for (Actuator actuator : actuators) {
                //2.1 Check if the uri of the actuator exist and corresponds to an actuator.
                if (actuator.getUri() != null) {
                    if (!existAndIsActuator(actuator.getUri())) {
                         dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown actuator : " + actuator.getUri()));
                    }
                }
                //2.2 check type (subclass of Actuator)
                UriDAO uriDAO = new UriDAO();
                if (!uriDAO.isSubClassOf(actuator.getRdfType(), Oeso.CONCEPT_ACTUATOR.toString())) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Bad actuator type given. Must be sublass of Actuator concept"));
                }

                //2.3 check if person in charge exist
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
    
    /**
     * Check and update a given list of actuators and return the update result.
     * @param actuators
     * @return 
     */
    public POSTResultsReturn checkAndUpdate(List<Actuator> actuators) {
        POSTResultsReturn updateResult;
        try {
            //1. Check the given actuators
            updateResult = check(actuators);
            //2. No error founded, insert actuators.
            if (updateResult.statusList.isEmpty()) {
            
                List<Actuator> updatedActuators = update(actuators);
                
                //The new actuators are inserted. Get the list of uri of the actuators.
                List<String> updatedActuatorsURIs = new ArrayList<>();
                for (Actuator actuator : updatedActuators) {
                    updatedActuatorsURIs.add(actuator.getUri());
                }
                
                updateResult = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
                updateResult.statusList = new ArrayList<>();
                updateResult.statusList.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, StatusCodeMsg.RESOURCES_UPDATED));
                updateResult.createdResources = updatedActuatorsURIs;
            
            } 
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            updateResult = new POSTResultsReturn(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
            updateResult.statusList.add(new Status(ex.getMessage(), StatusCodeMsg.ERR, ex.getClass().toString()));
        }
        return updateResult;
    }
    
    /**
     * Count query generated by the searched parameters given.
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param dateOfLastCalibration
     * @param inServiceDate
     * @param dateOfPurchase
     * @param personInCharge
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?uri  ?0  ?rdfType  . 
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Actuator> . 
     *      OPTIONAL {
     *          ?uri rdfs:label ?label . 
     *      }
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#hasBrand>  ?brand  . 
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> ?serialNumber . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> ?dateOfPurchase . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> ?dateOfLastCalibration . 
     *      }
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#personInCharge>  ?personInCharge  . 
     * }
     * @return Query generated to count the actuators, with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount(String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) {
        SPARQLQueryBuilder query = this.prepareSearchQuery(null, null, uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Counts the number of actuators by the given searched parameters.
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param dateOfLastCalibration
     * @param inServiceDate
     * @param dateOfPurchase
     * @param personInCharge
     * @return The number of sensors 
     * @inheritdoc
     */
    public Integer count(String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) throws RepositoryException, MalformedQueryException {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
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
     * Search all the actuators corresponding to the search params given.
     * @param page
     * @param pageSize
     * @param uri
     * @param rdfType
     * @param dateOfPurchase
     * @param label
     * @param brand
     * @param serialNumber
     * @param dateOfLastCalibration
     * @param inServiceDate
     * @param personInCharge
     * @return the list of the actuators.
     */
    public ArrayList<Actuator> find(Integer page, Integer pageSize, String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) {
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Actuator> actuators = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Actuator actuator = getActuatorFromBindingSet(bindingSet, uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
                HashMap<String, String> variables = getVariables(actuator.getUri());
                actuator.setVariables(variables);
                actuators.add(actuator);
            }
        }
        return actuators;
    }
    
    /**
     * Checks the given data to update the list of the measured variables linked to the actuator.
     * @param actuatorUri
     * @param variables
     * @return the check result.
     */
    private POSTResultsReturn checkMeasuredVariables(String actuatorUri, List<String> variables) {
        POSTResultsReturn checkResult;
        List<Status> checkStatus = new ArrayList<>();
        
        boolean dataOk = true;
        
        //1. Check if the actuator exist and is an actuator in the triplestore
        if (existAndIsActuator(actuatorUri)) {
            VariableDAO variableDao = new VariableDAO();
            
            for (String variableUri : variables) {
                //2. Check for each variable uri given if it exist and if it is really a variable
                if (!variableDao.existAndIsVariable(variableUri)) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Unknwon variable : " + variableUri));
                }
            }
            
            //3. Check if some variables links are removed. We do not check this if some problems has been founded before.
            if (dataOk) {
                //Get all the actual variables for the actuator
                HashMap<String, String> actualMeasuredVariables = getVariables(actuatorUri);
                DatasetDAO datasetDAO = new DatasetDAO();
                
                for(Map.Entry<String, String> varibale : actualMeasuredVariables.entrySet()) {
                    // Check if link to the variable can be removed.
                    if (!variables.contains(varibale.getKey())) {
                        datasetDAO.sensor = actuatorUri;
                        datasetDAO.variable = varibale.getKey();
                        ArrayList<Dataset> dataAboutVariableAndSensor = datasetDAO.allPaginate();
                        
                        if (dataAboutVariableAndSensor.get(0).getData().size() > 0) {//data founded, the association of the sensor and the variable can not be removed
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                                "Existing data for the given actuator (" + actuatorUri + ") and the variable " + varibale.getKey() + ". You cannot remove the link between them."));
                        }
                    }
                }
            }             
        } else {
            dataOk = false;
            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                    "Unknwon actuator : " + actuatorUri));
        }
        
        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }
    
    /**
     * Updates the list of variables linked to the actuator.
     * /!\ Prerequisite: the data must have been checked before
     * @see ActuatorDAO#checkMeasuredVariables(java.lang.String, java.util.List) 
     * @param actuatorUri
     * @param variables
     * @return the update result
     */
    private POSTResultsReturn updateMeasuredVariables(String actuatorUri, List<String> variables) {
        POSTResultsReturn result;
        List<Status> updateStatus = new ArrayList<>();
        
        boolean update = true;
        
        
        //1. Delete old object properties
        HashMap<String, String> actualMeasuredVariables = getVariables(actuatorUri);
        List<String> oldMeasuredVariables = new ArrayList<>();
        actualMeasuredVariables.entrySet().forEach((oldVariable) -> {
            oldMeasuredVariables.add(oldVariable.getKey());
        });
        
        if (deleteObjectProperties(actuatorUri, Oeso.RELATION_MEASURES.toString(), oldMeasuredVariables)) {
            //2. Add new object properties
            if (addObjectProperties(actuatorUri, Oeso.RELATION_MEASURES.toString(), variables, Contexts.SENSORS.toString())) {
                updateStatus.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, "The actuator " + actuatorUri + " has now " + variables.size() + " linked variables"));
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
        result.createdResources.add(actuatorUri); 
        return result;
    }
    
    /**
     * Checks and updates the variables measured by the given actuators.
     * @param actuatorUri
     * @param variables
     * @return the update result
     */
    public POSTResultsReturn checkAndUpdateMeasuredVariables(String actuatorUri, List<String> variables) {
        POSTResultsReturn checkResult = checkMeasuredVariables(actuatorUri, variables);
        if (checkResult.getDataState()) {
             return updateMeasuredVariables(actuatorUri, variables);
        } else { //Error in the data
            return checkResult;
        }
    }
}
