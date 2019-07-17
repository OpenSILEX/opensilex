//******************************************************************************
//                                SensorDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 9 Mar. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.ws.rs.NotFoundException;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
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
import opensilex.service.model.User;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Oeso;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.Dataset;
import opensilex.service.model.Sensor;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.vocabulary.XSD;

/**
 * Sensor DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Vincent Migot] 17 July 2019: Update getLastIdFromYear method to fix bug and limitation in URI generation
 */
public class SensorDAO extends Rdf4jDAO<Sensor> {

    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAO.class);

    //The following attributes are used to search sensors in the triplestore
    //brand of the sensor(s)
    private final String BRAND = "brand";
    //serial number of the sensor(s)
    private final String SERIAL_NUMBER = "serialNumber";
    //model of the sensor(s)
    private final String MODEL = "model";
    //service date of the sensor(s)
    private final String IN_SERVICE_DATE = "inServiceDate";
    //date of purchase of the sensor(s)
    private final String DATE_OF_PURCHASE = "dateOfPurchase";
    //date of last calibration of the sensor(s)
    private final String DATE_OF_LAST_CALIBRATION = "dateOfLastCalibration";
    //person in charge of the sensor(s)
    private final String PERSON_IN_CHARGE = "personInCharge";

    private static final String TYPE = "type";
    private static final String MAX_ID = "maxID";
    
    /**
     * Prepares a query to get the higher id of the sensors.
     * @example
     * <pre>
     * SELECT  ?maxID WHERE {
     *      ?uri a ?type .
     *      ?type (rdfs:subClassOf)* <http://www.opensilex.org/vocabulary/oeso#SensingDevice>
     *      FILTER regex(str(?uri), ".* /2019/.*", "")
     *      BIND(xsd:integer(strafter(str(?uri), "http://www.opensilex.org/diaphen/2019/s19")) AS ?maxID)
     * }
     * ORDER BY DESC(?maxID)
     * LIMIT 1
     * </pre>
     * @return 
     */
    private Query prepareGetLastIdFromYear(String year) {
        SelectBuilder query = new SelectBuilder();
        
        Var uri = makeVar(URI);
        Var type = makeVar(TYPE);
        Var maxID = makeVar(MAX_ID);
        
        // Select the highest identifier
        query.addVar(maxID);
        
        // Get sensor type
        query.addWhere(uri, RDF.type, type);
        // Filter by type subclass of sensing device
        Node sensorConcept = NodeFactory.createURI(Oeso.CONCEPT_SENSING_DEVICE.toString());
        query.addWhere(type, PathFactory.pathZeroOrMore1(PathFactory.pathLink(RDFS.subClassOf.asNode())), sensorConcept);
        
        ExprFactory expr = new ExprFactory();
        
        // Filter by year prefix
        Expr yearFilter =  expr.regex(expr.str(uri), ".*/" + year + "/.*", "");
        query.addFilter(yearFilter);
        
        // Binding to extract the last part of the URI as a MAX_ID integer
        Expr indexBinding =  expr.function(
            XSD.integer.getURI(), 
            ExprList.create(Arrays.asList(
                expr.strafter(expr.str(uri), UriGenerator.getSensorUriPatternByYear(year)))
            )
        );
        query.addBind(indexBinding, maxID);
        
        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID,  Query.ORDER_DESCENDING));
        query.setLimit(1);
        
        return query.build();
    }
    
    /**
     * Gets the higher existing id of the sensor for a given year.
     * @param year
     * @return the id
     */
    public int getLastIdFromYear(String year) {
        Query query = prepareGetLastIdFromYear(year);

        //get last sensor uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        getConnection().close();
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            return Integer.valueOf(bindingSet.getValue(MAX_ID).stringValue());
        } else {
            return 0;
        }
    }
    
    /**
     * Generates a search query by the given search parameters
     * @param page
     * @param pageSize
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param dateOfLastCalibration
     * @param inServiceDate
     * @param dateOfPurchase
     * @param model
     * @param personInCharge
     * @example
     * SELECT DISTINCT  ?rdfType ?uri ?label ?brand ?serialNumber ?model 
     *                  ?inServiceDate ?dateOfPurchase ?dateOfLastCalibration ?personInCharge 
     * WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *      ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#SensingDevice> . 
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
     *  }
     *  LIMIT 20 
     *  OFFSET 0 
     * @return the query to execute.
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
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_SENSING_DEVICE.toString(), null);
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
     * Count query generated by the searched parameters given.
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param dateOfLastCalibration
     * @param inServiceDate
     * @param dateOfPurchase
     * @param model
     * @param personInCharge
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?uri  ?0  ?rdfType  . 
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#SensingDevice> . 
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
     * @return Query generated to count the elements, with the searched parameters
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
     * Counts the number of sensors by the given searched parameters.
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param dateOfLastCalibration
     * @param inServiceDate
     * @param dateOfPurchase
     * @param model
     * @param personInCharge
     * @return The number of sensors 
     * @inheritdoc
     */
    public Integer count(String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
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
     * Generates a query to count the number of cameras in the triplestore.
     * @example
     * SELECT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Camera> . 
     *      ?uri  rdf:type  ?rdfType  . 
     *      ?uri  rdfs:label  ?label  . 
     * }
     * @return Query generated to count the elements
     */
    private SPARQLQueryBuilder prepareCountCameras() {
        SPARQLQueryBuilder query = this.prepareSearchCamerasQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.clearOrderBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Counts the number of cameras.
     * @return The number of cameras 
     * @inheritdoc
     */
    public Integer countCameras() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        SPARQLQueryBuilder prepareCount = prepareCountCameras();
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
     * Gets a sensor from a given binding set.
     * Assume that the following attributes exist :
     * uri, rdfType, label, brand, variable, inServiceDate, dateOfPurchase,
     * dateOfLastCalibration
     * @param bindingSet a bindingSet from a search query
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
     * @return a sensor with data extracted from the given bindingSet
     */
    private Sensor getSensorFromBindingSet(BindingSet bindingSet, String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) {
        Sensor sensor = new Sensor();

        if (bindingSet.getValue(URI) != null) {
            sensor.setUri(bindingSet.getValue(URI).stringValue());
        }

        if (bindingSet.getValue(RDF_TYPE) != null) {
            sensor.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
        }

        if (bindingSet.getValue(LABEL) != null ){
            sensor.setLabel(bindingSet.getValue(LABEL).stringValue());
        }

        if (bindingSet.getValue(BRAND) != null) {
            sensor.setBrand(bindingSet.getValue(BRAND).stringValue());
        }
        
        if (bindingSet.getValue(SERIAL_NUMBER) != null) {
            sensor.setSerialNumber(bindingSet.getValue(SERIAL_NUMBER).stringValue());
        }
        
        if(bindingSet.getValue(MODEL) != null) {
            sensor.setModel(bindingSet.getValue(MODEL).stringValue());
        }

        if (bindingSet.getValue(IN_SERVICE_DATE) != null) {
            sensor.setInServiceDate(bindingSet.getValue(IN_SERVICE_DATE).stringValue());
        }

        if (bindingSet.getValue(DATE_OF_PURCHASE) != null) {
            sensor.setDateOfPurchase(bindingSet.getValue(DATE_OF_PURCHASE).stringValue());
        }

        if (bindingSet.getValue(DATE_OF_LAST_CALIBRATION) != null) {
            sensor.setDateOfLastCalibration(bindingSet.getValue(DATE_OF_LAST_CALIBRATION).stringValue());
        }
        
        if (bindingSet.getValue(PERSON_IN_CHARGE) != null) {
            sensor.setPersonInCharge(bindingSet.getValue(PERSON_IN_CHARGE).stringValue());
        }

        return sensor;
    }

    private SPARQLQueryBuilder prepareIsSensorQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_SENSING_DEVICE.toString(), null);
        
        query.appendAsk("");
        LOGGER.debug(query.toString());
        return query;
    }
    
    private boolean isSensor(String uri) {
        SPARQLQueryBuilder query = prepareIsSensorQuery(uri);
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        
        return booleanQuery.evaluate();
    }
     
    /**
     * Checks if a given URI is a sensor.
     * @param uri
     * @return true if the URI corresponds to a sensor
     *         false if it does not exist or if it is not a sensor
     */
    public boolean existAndIsSensor(String uri) {
        if (existUri(uri)) {
            return isSensor(uri);
            
        } else {
            return false;
        }
    }
    
    /**
     * Checks the given sensor's metadata.
     * @param sensors
     * @return the result with the list of the errors founded (empty if no error founded)
     */
    public POSTResultsReturn check(List<Sensor> sensors) {
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;
        //1. checl if user is an admin
        UserDAO userDao = new UserDAO();
        if (userDao.isAdmin(user)) {
            //2. check data
            UriDAO uriDao = new UriDAO();
            for (Sensor sensor : sensors) {
                try {
                    //2.1 check type (subclass of SensingDevice)
                    if (!uriDao.isSubClassOf(sensor.getRdfType(), Oeso.CONCEPT_SENSING_DEVICE.toString())) {
                        dataOk = false;
                        checkStatus.add(new Status(
                                StatusCodeMsg.DATA_ERROR, 
                                StatusCodeMsg.ERR, 
                                "Bad sensor type given. Must be sublass of SensingDevice concept"));
                    }

                    //2.2 check if person in charge exist
                    User u = new User(sensor.getPersonInCharge());
                    if (!userDao.existInDB(u)) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person in charge email"));
                    }
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(SensorDAO.class.getName()).log(Level.SEVERE, null, ex);
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
     * Generates an insert query for sensors.
     * @example
     * INSERT DATA {
     *  GRAPH <http://www.phenome-fppn.fr/diaphen/sensors> { 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdf:type  <http://www.opensilex.org/vocabulary/oeso#Thermocouple> . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdfs:label  "par03_p"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#hasBrand>  "Homemade"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#inServiceDate>  "2017-06-15"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#personInCharge>  "morgane.vidal@inra.fr"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#serialNumber>  "A1E345F32"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase>  "2017-06-15"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration>  "2017-06-15"  . 
     *  }
     * }
     * @param sensor
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(Sensor sensor) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.SENSORS.toString());
        
        Resource sensorUri = ResourceFactory.createResource(sensor.getUri());
        
        Node sensorType = NodeFactory.createURI(sensor.getRdfType());
        
        spql.addInsert(graph, sensorUri, RDF.type, sensorType);
        spql.addInsert(graph, sensorUri, RDFS.label, sensor.getLabel());
        
        Property relationHasBrand = ResourceFactory.createProperty(Oeso.RELATION_HAS_BRAND.toString());
        Property relationInServiceDate = ResourceFactory.createProperty(Oeso.RELATION_IN_SERVICE_DATE.toString());
        Property relationPersonInCharge = ResourceFactory.createProperty(Oeso.RELATION_PERSON_IN_CHARGE.toString());
        
        spql.addInsert(graph, sensorUri, relationHasBrand, sensor.getBrand() );
        spql.addInsert(graph, sensorUri, relationInServiceDate, sensor.getInServiceDate());
        spql.addInsert(graph, sensorUri, relationPersonInCharge, sensor.getPersonInCharge() );
        
        if (sensor.getSerialNumber() != null) {
            Property relationSerialNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_SERIAL_NUMBER.toString());
            spql.addInsert(graph, sensorUri, relationSerialNumber, sensor.getSerialNumber() );
        }
        
        if (sensor.getModel() != null) {
            Property relationModel = ResourceFactory.createProperty(Oeso.RELATION_HAS_MODEL.toString());
            spql.addInsert(graph, sensorUri, relationModel, sensor.getModel());
        }
        
        if (sensor.getDateOfPurchase() != null) {
            Property relationDateOfPurchase = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_PURCHASE.toString());
            spql.addInsert(graph, sensorUri, relationDateOfPurchase, sensor.getDateOfPurchase() );
        }
        
        if (sensor.getDateOfLastCalibration() != null) {
            Property relationDateOfCalibration = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_LAST_CALIBRATION.toString());
            spql.addInsert(graph, sensorUri, relationDateOfCalibration, sensor.getDateOfLastCalibration() );
        }
        
        UpdateRequest query = spql.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }
    
    /**
     * Inserts the given sensors in the triplestore.
     * @param sensors
     * @return the insertion result, with the errors list or the uri of the inserted
     *         sensors
     */
    public POSTResultsReturn insert(List<Sensor> sensors) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results; 
        boolean resultState = false;
        boolean annotationInsert = true;
        
        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        
        for (Sensor sensor : sensors) {
            try {
                sensor.setUri(UriGenerator.generateNewInstanceUri(sensor.getRdfType(), null, null));
            } catch (Exception ex) { //In the sensors case, no exception should be raised
                annotationInsert = false;
            }
            
            UpdateRequest query = prepareInsertQuery(sensor);
            Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
            
            createdResourcesUri.add(sensor.getUri());
        }
        
        if (annotationInsert) {
            resultState = true;
            getConnection().commit();
        } else {
            getConnection().rollback();
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatus;
        results.setCreatedResources(createdResourcesUri);
        if (resultState && !createdResourcesUri.isEmpty()) {
            results.createdResources = createdResourcesUri;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUri.size() + " new resource(s) created"));
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        return results;
    }
    
    /**
     * Checks and inserts the given sensors in the triplestore.
     * @param sensors
     * @return the insertion result. Message error if errors founded in data
     *         the list of the generated URI of the sensors if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<Sensor> sensors) {
        POSTResultsReturn checkResult = check(sensors);
        if (checkResult.getDataState()) {
            return insert(sensors);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * Prepares a delete query of the triplets corresponding to the given sensor.
     * @examle
     *  DELETE DATA {
     *    GRAPH <http://www.phenome-fppn.fr/diaphen/set/sensors> {
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> rdf:type <http://www.opensilex.org/vocabulary/oeso#Thermocouple> . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> rdfs:label "par03_p" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.opensilex.org/vocabulary/oeso#hasBrand> "Skye Instruments" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.opensilex.org/vocabulary/oeso#inServiceDate> "2017-06-15" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.opensilex.org/vocabulary/oeso#personInCharge> "morgane.vidal@inra.fr" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.opensilex.org/vocabulary/oeso#serialNumber> "A1E345F32" .
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> "2017-06-15" .
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> "2017-06-15"
     *    }
     * }
     * @param sensor
     * @return 
     */
    private UpdateRequest prepareDeleteQuery(Sensor sensor) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.SENSORS.toString());
        
        Resource sensorUri = ResourceFactory.createResource(sensor.getUri());
        
        Node sensorType = NodeFactory.createURI(sensor.getRdfType());
        
        spql.addDelete(graph, sensorUri, RDF.type, sensorType);
        spql.addDelete(graph, sensorUri, RDFS.label, sensor.getLabel());
        
        Property relationHasBrand = ResourceFactory.createProperty(Oeso.RELATION_HAS_BRAND.toString());
        Property relationInServiceDate = ResourceFactory.createProperty(Oeso.RELATION_IN_SERVICE_DATE.toString());
        Property relationPersonInCharge = ResourceFactory.createProperty(Oeso.RELATION_PERSON_IN_CHARGE.toString());
        
        spql.addDelete(graph, sensorUri, relationHasBrand, sensor.getBrand() );
        spql.addDelete(graph, sensorUri, relationInServiceDate, sensor.getInServiceDate());
        spql.addDelete(graph, sensorUri, relationPersonInCharge, sensor.getPersonInCharge() );
        
        if (sensor.getSerialNumber() != null) {
            Property relationSerialNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_SERIAL_NUMBER.toString());
            spql.addDelete(graph, sensorUri, relationSerialNumber, sensor.getSerialNumber() );
        }
        
        if (sensor.getModel() != null) {
            Property relationHasModel = ResourceFactory.createProperty(Oeso.RELATION_HAS_MODEL.toString());
            spql.addDelete(graph, sensorUri, relationHasModel, sensor.getModel());
        }
        
        if (sensor.getDateOfPurchase() != null) {
            Property relationDateOfPurchase = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_PURCHASE.toString());
            spql.addDelete(graph, sensorUri, relationDateOfPurchase, sensor.getDateOfPurchase() );
        }
        
        if (sensor.getDateOfLastCalibration() != null) {
            Property relationDateOfCalibration = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_LAST_CALIBRATION.toString());
            spql.addDelete(graph, sensorUri, relationDateOfCalibration, sensor.getDateOfLastCalibration() );
        }
        
        UpdateRequest query = spql.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }
    
    /**
     * Updates a list of sensors. 
     * The sensors data must have been checked before.
     * @see SensorDAO#check(java.util.List)
     * @param sensors 
     * @return the updated result
     */
    private POSTResultsReturn updateAndReturnPOSTResultsReturn(List<Sensor> sensors) throws Exception {
        List<Status> updateStatus = new ArrayList<>();
        List<String> updatedResourcesUri = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        for (Sensor sensor : sensors) {
            //1. delete already existing data
            //1.1 get informations that will be updated (to delete the right triplets)
            Sensor sensorCorresponding = findById(sensor.getUri());
            if (sensorCorresponding!= null) {
                UpdateRequest deleteQuery = prepareDeleteQuery(sensorCorresponding);
                
                //2. insert new data
                UpdateRequest insertQuery = prepareInsertQuery(sensor);
                try {
                    this.getConnection().begin();
                    Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery.toString());
                    LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                    prepareDelete.execute();
                    Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
                    LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                    prepareUpdate.execute();
                    updatedResourcesUri.add(sensor.getUri());
                } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationUpdate = false;
                    updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                }
            } else {
                annotationUpdate = false;
                updateStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown sensor " + sensor.getUri()));
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
        
        results = new POSTResultsReturn(resultState, annotationUpdate, true);
        results.statusList = updateStatus;
        if (resultState && !updatedResourcesUri.isEmpty()) {
            results.createdResources = updatedResourcesUri;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, updatedResourcesUri.size() + " resources updated"));
        }
        
        return results;
    }
    
    /**
     * Checks and updates the given sensors in the triplestore.
     * @see SensorDAO#check(java.util.List)
     * @see SensorDAO#updateAndReturnPOSTResultsReturn(java.util.List)
     * @param sensors
     * @return the update result. Message error if errors founded in data,
     *         the list of the updated sensors's uri if they has been updated correctly
     */
    public POSTResultsReturn checkAndUpdate(List<Sensor> sensors) throws Exception {
        POSTResultsReturn checkResult = check(sensors);
        if (checkResult.getDataState()) {
            return updateAndReturnPOSTResultsReturn(sensors);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * Generates the query to get the URI, label and RDF type of all the cameras.
     * @example 
     * SELECT DISTINCT  ?uri ?label ?rdfType WHERE {
     *      ?uri  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Camera> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label  ?label  .
     * }
     * @return the query
     */
    private SPARQLQueryBuilder prepareSearchCamerasQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendSelect("?" + URI + " ?" + RDF_TYPE + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_CAMERA.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendOrderBy("DESC(?" + LABEL + ")");
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Gets the cameras (type, label, URI) of the triplestore.
     * @return The list of the cameras
     */
    public ArrayList<Sensor> getCameras() {
        SPARQLQueryBuilder query = prepareSearchCamerasQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Sensor> cameras = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Sensor camera = getSensorFromBindingSet(bindingSet, null, null, null, null, null, null, null, null, null, null);
                cameras.add(camera);
            }
        }
        return cameras;
    }
    
    /**
     * Checks if a given sensor measured a given variable (is the sensor linked to the variable ?).
     * @see SensorDAO#getVariables(java.lang.String)
     * @param sensorUri
     * @param variableUri
     * @return true if the sensor measured the variable (i.e. sensor linked to variable with the measures object property)
     *         false if not
     */
    public boolean isSensorMeasuringVariable(String sensorUri, String variableUri) {
        HashMap<String, String> measuredVariables = getVariables(sensorUri);
        
        return measuredVariables.containsKey(variableUri);
    }
    
    /**
     * Checks the given data to update the list of the measured variables linked to the sensor.
     * @param sensorUri
     * @param variables
     * @return the check result.
     */
    private POSTResultsReturn checkMeasuredVariables(String sensorUri, List<String> variables) {
        POSTResultsReturn checkResult;
        List<Status> checkStatus = new ArrayList<>();
        
        boolean dataOk = true;
        
        //1. Check if the sensorUri is a sensor in the triplestore
        if (existAndIsSensor(sensorUri)) {
            VariableDAO variableDao = new VariableDAO();
            
            for (String variableUri : variables) {
                //2. Check for each variable uri given if it exist and if it is really a variable
                if (!variableDao.existAndIsVariable(variableUri)) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Unknown variable : " + variableUri));
                }
            }
            
            //3. Check if some variables links are removed. We do not check this if some problems has been founded before.
            if (dataOk) {
                //Get all the actual variables for the sensor
                HashMap<String, String> actualMeasuredVariables = getVariables(sensorUri);
                DatasetDAO datasetDAO = new DatasetDAO();
                
                for(Map.Entry<String, String> varibale : actualMeasuredVariables.entrySet()) {
                    // Check if link to the variable can be removed.
                    if (!variables.contains(varibale.getKey())) {
                        datasetDAO.sensor = sensorUri;
                        datasetDAO.variable = varibale.getKey();
                        ArrayList<Dataset> dataAboutVariableAndSensor = datasetDAO.allPaginate();
                        
                        if (dataAboutVariableAndSensor.get(0).getData().size() > 0) {//data founded, the association of the sensor and the variable can not be removed
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                                "Existing data for the given sensor (" + sensorUri + ") and the variable " + varibale.getKey() + ". You cannot remove the link between them."));
                        }
                    }
                }
            }             
        } else {
            dataOk = false;
            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                    "Unknown sensor : " + sensorUri));
        }
        
        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }
    
    /**
     * Updates the list of variables linked to the sensor.
     * /!\ Prerequisite: the data must have been checked before
     * @see SensorDAO#checkMeasuredVariables(java.lang.String, java.util.List) 
     * @param sensorUri
     * @param variables
     * @return the update result
     */
    private POSTResultsReturn updateMeasuredVariables(String sensorUri, List<String> variables) {
        POSTResultsReturn result;
        List<Status> updateStatus = new ArrayList<>();
        
        boolean update = true;
        
        
        //1. Delete old object properties
        HashMap<String, String> actualMeasuredVariables = getVariables(sensorUri);
        List<String> oldMeasuredVariables = new ArrayList<>();
        actualMeasuredVariables.entrySet().forEach((oldVariable) -> {
            oldMeasuredVariables.add(oldVariable.getKey());
        });
        
        if (deleteObjectProperties(sensorUri, Oeso.RELATION_MEASURES.toString(), oldMeasuredVariables)) {
            //2. Add new object properties
            if (addObjectProperties(sensorUri, Oeso.RELATION_MEASURES.toString(), variables, Contexts.SENSORS.toString())) {
                updateStatus.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, "The sensor " + sensorUri + " has now " + variables.size() + " linked variables"));
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
        result.createdResources.add(sensorUri); 
        return result;
    }
    
    /**
     * Checks and updates the variables measured by the given sensors.
     * @param sensorUri
     * @param variables
     * @return the update result
     */
    public POSTResultsReturn checkAndUpdateMeasuredVariables(String sensorUri, List<String> variables) {
        POSTResultsReturn checkResult = checkMeasuredVariables(sensorUri, variables);
        if (checkResult.getDataState()) {
             return updateMeasuredVariables(sensorUri, variables);
        } else { //Error in the data
            return checkResult;
        }
    }
    
    /**
     * Prepares the SPARQL query to return all variables measured by a sensor.
     * @param sensor The sensor uri which measures veriables
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
     * Returns a HashMap of URI => label of the variables measured by the given sensor.
     * @param sensor The sensor URI which measures veriables
     * @return HashMap of URI => label
     */
    private HashMap<String, String> getVariables(String sensorUri) {
        SPARQLQueryBuilder query = prepareSearchVariablesQuery(sensorUri);
        
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

    @Override
    public List<Sensor> create(List<Sensor> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Sensor> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public List<Sensor> update(List<Sensor> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Sensor find(Sensor object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Sensor findById(String id) throws Exception {
        SPARQLQueryBuilder findQuery = prepareSearchQuery(null, null, id, null, null, null, null, null, null, null, null, null);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, findQuery.toString());
        
        Sensor sensor = new Sensor();
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                sensor = getSensorFromBindingSet(result.next(), id, null, null, null, null, null, null, null, null, null);
                
                //get variables associated to the sensor
                HashMap<String, String> variables = getVariables(sensor.getUri());
                sensor.setVariables(variables);
            } else {
                throw new NotFoundException(id + " not found.");
            }
        }
        return sensor;
    }
    
    /**
     * Search all the sensors corresponding to the search params given.
     * @param page
     * @param pageSize
     * @param uri
     * @param rdfType
     * @param dateOfPurchase
     * @param label
     * @param brand
     * @param serialNumber
     * @param model
     * @param dateOfLastCalibration
     * @param inServiceDate
     * @param personInCharge
     * @return the list of the sensors.
     */
    public ArrayList<Sensor> find(Integer page, Integer pageSize, String uri, String rdfType, String label, String brand, String serialNumber, String model, String inServiceDate, String dateOfPurchase, String dateOfLastCalibration, String personInCharge) {
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Sensor> sensors = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Sensor sensor = getSensorFromBindingSet(bindingSet, uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
                sensors.add(sensor);
            }
        }
        return sensors;
    }

    @Override
    public void validate(List<Sensor> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
