//******************************************************************************
//                                       SensorDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 9 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.mongo.DatasetDAOMongo;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.resources.dto.SensorDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Dataset;
import phis2ws.service.view.model.phis.Sensor;

/**
 * allows CRUD methods of sensors in the triplestore rdf4j
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorDAOSesame extends DAOSesame<Sensor> {

    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAOSesame.class);

    //The following attributes are used to search sensors in the triplestore
    //uri of the sensor
    public String uri;
    //type uri of the sensor(s)
    public String rdfType;
    //alias of the sensor(s)
    public String label;
    //brand of the sensor(s)
    public String brand;
    private final String BRAND = "brand";
    //serial number of the sensor(s)
    public String serialNumber;
    private final String SERIAL_NUMBER = "serialNumber";
    //service date of the sensor(s)
    public String inServiceDate;
    private final String IN_SERVICE_DATE = "inServiceDate";
    //date of purchase of the sensor(s)
    public String dateOfPurchase;
    private final String DATE_OF_PURCHASE = "dateOfPurchase";
    //date of last calibration of the sensor(s)
    public String dateOfLastCalibration;
    private final String DATE_OF_LAST_CALIBRATION = "dateOfLastCalibration";
    //person in charge of the sensor(s)
    public String personInCharge;
    private final String PERSON_IN_CHARGE = "personInCharge";

    /**
     * prepare a query to get the higher id of the sensors
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastIdFromYear(String year) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?type", null);
        query.appendTriplet("?type", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_SENSING_DEVICE.toString(), null);
        query.appendFilter("regex(str(?uri), \".*/" + year + "/.*\")");
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * get the higher existing id of the sensor for a given year
     * @param year
     * @return the id
     */
    public int getLastIdFromYear(String year) {
        SPARQLQueryBuilder query = prepareGetLastIdFromYear(year);

        //get last sensor uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        getConnection().close();
        
        String uriSensor = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriSensor = bindingSet.getValue(URI).stringValue();
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
    
    /**
     * generates a search query (search by uri, type, label, brand, variable,
     * inServiceDate, dateOfPurchase, dateOfLastCalibration)
     * @return the query to execute.
     * e.g.
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String sensorUri;
        if (uri != null) {
            sensorUri = "<" + uri + ">";
        } else {
            sensorUri = "?" + URI;
            query.appendSelect(sensorUri);
        }
        
        if (rdfType != null) {
            query.appendTriplet(sensorUri, Rdf.RELATION_TYPE.toString(), rdfType, null);
        } else {
            query.appendSelect("?" + RDF_TYPE);
            query.appendTriplet(sensorUri, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
            query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_SENSING_DEVICE.toString(), null);
        }        

        if (label != null) {
            query.appendTriplet(sensorUri, Rdfs.RELATION_LABEL.toString(), "\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?" + LABEL);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + LABEL + " . ");
            query.endBodyOptional();
        }

        if (brand != null) {
            query.appendTriplet(sensorUri, Vocabulary.RELATION_HAS_BRAND.toString(), "\"" + brand + "\"", null);
        } else {
            query.appendSelect(" ?" + BRAND);
            query.appendTriplet(sensorUri, Vocabulary.RELATION_HAS_BRAND.toString(), "?" + BRAND, null);
        }
        
        if (serialNumber != null) {
            query.appendTriplet(sensorUri, Vocabulary.RELATION_SERIAL_NUMBER.toString(), "\"" + serialNumber + "\"", null);
        } else {
            query.appendSelect("?" + SERIAL_NUMBER);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Vocabulary.RELATION_SERIAL_NUMBER.toString() + "> ?" + SERIAL_NUMBER + " . ");
            query.endBodyOptional();
        }

        if (inServiceDate != null) {
            query.appendTriplet(sensorUri, Vocabulary.RELATION_IN_SERVICE_DATE.toString(), "\"" + inServiceDate + "\"", null);
        } else {
            query.appendSelect(" ?" + IN_SERVICE_DATE);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Vocabulary.RELATION_IN_SERVICE_DATE.toString() + "> " + "?" + IN_SERVICE_DATE + " . ");
            query.endBodyOptional();
        }

        if (dateOfPurchase != null) {
            query.appendTriplet(sensorUri, Vocabulary.RELATION_DATE_OF_PURCHASE.toString(), "\"" + dateOfPurchase + "\"", null);
        } else {
            query.appendSelect("?" + DATE_OF_PURCHASE);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Vocabulary.RELATION_DATE_OF_PURCHASE.toString() + "> " + "?" + DATE_OF_PURCHASE + " . ");
            query.endBodyOptional();
        }

        if (dateOfLastCalibration != null) {
            query.appendTriplet(sensorUri, Vocabulary.RELATION_DATE_OF_LAST_CALIBRATION.toString(), "\"" + dateOfLastCalibration + "\"", null);
        } else {
            query.appendSelect("?" + DATE_OF_LAST_CALIBRATION);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Vocabulary.RELATION_DATE_OF_LAST_CALIBRATION.toString() + "> " + "?" + DATE_OF_LAST_CALIBRATION + " . ");
            query.endBodyOptional();
        }
        
        if (personInCharge != null) {
            query.appendTriplet(sensorUri, Vocabulary.RELATION_PERSON_IN_CHARGE.toString(), "\"" + personInCharge + "\"", null);
        } else {
            query.appendSelect(" ?" + PERSON_IN_CHARGE);
            query.appendTriplet(sensorUri, Vocabulary.RELATION_PERSON_IN_CHARGE.toString(), "?" + PERSON_IN_CHARGE, null);
        }
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());

        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }

    /**
     * Count query generated by the searched parameters : uri, rdfType, 
     * label, brand, variable, inServiceDate, dateOfPurchase, dateOfLastCalibration
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?uri  ?0  ?rdfType  . 
     *      ?rdfType  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#SensingDevice> . 
     *      OPTIONAL {
     *          ?uri rdfs:label ?label . 
     *      }
     *      ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasBrand>  ?brand  . 
     *      OPTIONAL {
     *          ?uri <http://www.phenome-fppn.fr/vocabulary/2017#serialNumber> ?serialNumber . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.phenome-fppn.fr/vocabulary/2017#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.phenome-fppn.fr/vocabulary/2017#dateOfPurchase> ?dateOfPurchase . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.phenome-fppn.fr/vocabulary/2017#dateOfLastCalibration> ?dateOfLastCalibration . 
     *      }
     *      ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#personInCharge>  ?personInCharge  . 
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
     * Query to count the number of cameras in the triplestore
     * @example
     * SELECT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#Camera> . 
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
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Count the number of cameras 
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
     * get a sensor from a given binding set.
     * Assume that the following attributes exist :
     * uri, rdfType, label, brand, variable, inServiceDate, dateOfPurchase,
     * dateOfLastCalibration
     * @param bindingSet a bindingSet from a search query
     * @return a sensor with data extracted from the given bindingSet
     */
    private Sensor getSensorFromBindingSet(BindingSet bindingSet) {
        Sensor sensor = new Sensor();

        if (uri != null) {
            sensor.setUri(uri);
        } else if (bindingSet.getValue(URI) != null) {
            sensor.setUri(bindingSet.getValue(URI).stringValue());
        }

        if (rdfType != null) {
            sensor.setRdfType(rdfType);
        } else if (bindingSet.getValue(RDF_TYPE) != null) {
            sensor.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
        }

        if (label != null) {
            sensor.setLabel(label);
        } else if (bindingSet.getValue(LABEL) != null ){
            sensor.setLabel(bindingSet.getValue(LABEL).stringValue());
        }

        if (brand != null) {
            sensor.setBrand(brand);
        } else if (bindingSet.getValue(BRAND) != null) {
            sensor.setBrand(bindingSet.getValue(BRAND).stringValue());
        }
        
        if (serialNumber != null) {
            sensor.setSerialNumber(serialNumber);
        } else if (bindingSet.getValue(SERIAL_NUMBER) != null) {
            sensor.setSerialNumber(bindingSet.getValue(SERIAL_NUMBER).stringValue());
        }

        if (inServiceDate != null) {
            sensor.setInServiceDate(inServiceDate);
        } else if (bindingSet.getValue(IN_SERVICE_DATE) != null) {
            sensor.setInServiceDate(bindingSet.getValue(IN_SERVICE_DATE).stringValue());
        }

        if (dateOfPurchase != null) {
            sensor.setDateOfPurchase(dateOfPurchase);
        } else if (bindingSet.getValue(DATE_OF_PURCHASE) != null) {
            sensor.setDateOfPurchase(bindingSet.getValue(DATE_OF_PURCHASE).stringValue());
        }

        if (dateOfLastCalibration != null) {
            sensor.setDateOfLastCalibration(dateOfLastCalibration);
        } else if (bindingSet.getValue(DATE_OF_LAST_CALIBRATION) != null) {
            sensor.setDateOfLastCalibration(bindingSet.getValue(DATE_OF_LAST_CALIBRATION).stringValue());
        }
        
        if (personInCharge != null) {
            sensor.setPersonInCharge(personInCharge);
        } else if (bindingSet.getValue(PERSON_IN_CHARGE) != null) {
            sensor.setPersonInCharge(bindingSet.getValue(PERSON_IN_CHARGE).stringValue());
        }

        return sensor;
    }
    
    /**
     * search all the sensors corresponding to the search params given by the user
     * @return the list of the sensors which match the given search params.
     */
    public ArrayList<Sensor> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Sensor> sensors = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Sensor sensor = getSensorFromBindingSet(bindingSet);
                HashMap<String, String>  variables = getVariables(sensor.getUri());
                sensor.setVariables(variables);
                sensors.add(sensor);
            }
        }
        return sensors;
    }
    
    private SPARQLQueryBuilder prepareIsSensorQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_SENSING_DEVICE.toString(), null);
        
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
     * check if a given uri is a sensor
     * @param uri
     * @return true if the uri corresponds to a sensor
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
     * check the given sensor's metadata
     * @param sensors
     * @return the result with the list of the errors founded (empty if no error founded)
     */
    public POSTResultsReturn check(List<SensorDTO> sensors) {
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;
        
        //1. checl if user is an admin
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
        if (userDao.isAdmin(user)) {
            //2. check data
            for (SensorDTO sensor : sensors) {
                try {
                    //2.1 check type (subclass of SensingDevice)
                    UriDaoSesame uriDaoSesame = new UriDaoSesame();
                    if (!uriDaoSesame.isSubClassOf(sensor.getRdfType(), Vocabulary.CONCEPT_SENSING_DEVICE.toString())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Bad sensor type given. Must be sublass of SensingDevice concept"));
                    }

                    //2.2 check if person in charge exist
                    User u = new User(sensor.getPersonInCharge());
                    if (!userDao.existInDB(u)) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person in charge email"));
                    }
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(SensorDAOSesame.class.getName()).log(Level.SEVERE, null, ex);
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
     * generates an insert query for sensors.
     * e.g.
     * INSERT DATA {
     *  GRAPH <http://www.phenome-fppn.fr/diaphen/sensors> { 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdf:type  <http://www.phenome-fppn.fr/vocabulary/2017#Thermocouple> . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdfs:label  "par03_p"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.phenome-fppn.fr/vocabulary/2017#hasBrand>  "Homemade"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.phenome-fppn.fr/vocabulary/2017#inServiceDate>  "2017-06-15"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.phenome-fppn.fr/vocabulary/2017#personInCharge>  "morgane.vidal@inra.fr"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.phenome-fppn.fr/vocabulary/2017#serialNumber>  "A1E345F32"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.phenome-fppn.fr/vocabulary/2017#dateOfPurchase>  "2017-06-15"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.phenome-fppn.fr/vocabulary/2017#dateOfLastCalibration>  "2017-06-15"  . 
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
        
        Property relationHasBrand = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_BRAND.toString());
        Property relationInServiceDate = ResourceFactory.createProperty(Vocabulary.RELATION_IN_SERVICE_DATE.toString());
        Property relationPersonInCharge = ResourceFactory.createProperty(Vocabulary.RELATION_PERSON_IN_CHARGE.toString());
        
        spql.addInsert(graph, sensorUri, relationHasBrand, sensor.getBrand() );
        spql.addInsert(graph, sensorUri, relationInServiceDate, sensor.getInServiceDate());
        spql.addInsert(graph, sensorUri, relationPersonInCharge, sensor.getPersonInCharge() );
        
        if (sensor.getSerialNumber() != null) {
            Property relationSerialNumber = ResourceFactory.createProperty(Vocabulary.RELATION_SERIAL_NUMBER.toString());
            spql.addInsert(graph, sensorUri, relationSerialNumber, sensor.getSerialNumber() );
        }
        
        if (sensor.getDateOfPurchase() != null) {
            Property relationDateOfPurchase = ResourceFactory.createProperty(Vocabulary.RELATION_DATE_OF_PURCHASE.toString());
            spql.addInsert(graph, sensorUri, relationDateOfPurchase, sensor.getDateOfPurchase() );
        }
        
        if (sensor.getDateOfLastCalibration() != null) {
            Property relationDateOfCalibration = ResourceFactory.createProperty(Vocabulary.RELATION_DATE_OF_LAST_CALIBRATION.toString());
            spql.addInsert(graph, sensorUri, relationDateOfCalibration, sensor.getDateOfLastCalibration() );
        }
        
        UpdateRequest query = spql.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }
    
    /**
     * insert the given sensors in the triplestore
     * @param sensorsDTO
     * @return the insertion result, with the errors list or the uri of the inserted
     *         sensors
     */
    public POSTResultsReturn insert(List<SensorDTO> sensorsDTO) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results; 
        boolean resultState = false;
        boolean annotationInsert = true;
        
        UriGenerator uriGenerator = new UriGenerator();
        
        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        
        for (SensorDTO sensorDTO : sensorsDTO) {
            Sensor sensor = sensorDTO.createObjectFromDTO();
            sensor.setUri(uriGenerator.generateNewInstanceUri(sensorDTO.getRdfType(), null, null));
            
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
     * check and insert the given sensors in the triplestore
     * @param sensors
     * @return the insertion result. Message error if errors founded in data
     *         the list of the generated uri of the sensors if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<SensorDTO> sensors) {
        POSTResultsReturn checkResult = check(sensors);
        if (checkResult.getDataState()) {
            return insert(sensors);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * prepare a delete query of the triplets corresponding to the given sensor
     * e.g.
     * DELETE WHERE { 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> rdf:type <http://www.phenome-fppn.fr/vocabulary/2017#Thermocouple> . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> rdfs:label "par03_p" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.phenome-fppn.fr/vocabulary/2017#hasBrand> "Skye Instruments" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.phenome-fppn.fr/vocabulary/2017#inServiceDate> "2017-06-15" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.phenome-fppn.fr/vocabulary/2017#personInCharge> "morgane.vidal@inra.fr" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.phenome-fppn.fr/vocabulary/2017#serialNumber> "A1E345F32" .
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.phenome-fppn.fr/vocabulary/2017#dateOfPurchase> "2017-06-15" .
     *      <http://www.phenome-fppn.fr/diaphen/2018/s18142> <http://www.phenome-fppn.fr/vocabulary/2017#dateOfLastCalibration> "2017-06-15"
     * }
     * @param sensor
     * @return 
     */
    private String prepareDeleteQuery(Sensor sensor) {
        String query;
        query = "DELETE WHERE { "
                + "<" + sensor.getUri() + "> <" + Rdf.RELATION_TYPE.toString() + "> <" + sensor.getRdfType() + "> . "
                + "<" + sensor.getUri() + "> <" + Rdfs.RELATION_LABEL.toString() + "> \"" + sensor.getLabel() + "\" . "
                + "<" + sensor.getUri() + "> <" + Vocabulary.RELATION_HAS_BRAND.toString() + "> \"" + sensor.getBrand() + "\" . "
                + "<" + sensor.getUri() + "> <" + Vocabulary.RELATION_IN_SERVICE_DATE.toString() + "> \"" + sensor.getInServiceDate() + "\" . "
                + "<" + sensor.getUri() + "> <" + Vocabulary.RELATION_PERSON_IN_CHARGE.toString() + "> \"" + sensor.getPersonInCharge() + "\" . ";
        
        if (sensor.getSerialNumber() != null) {
            query += "<" + sensor.getUri() + "> <" + Vocabulary.RELATION_SERIAL_NUMBER.toString() + "> \"" + sensor.getSerialNumber() + "\" . ";
        }
        if (sensor.getDateOfPurchase() != null) {
            query += "<" + sensor.getUri() + "> <" + Vocabulary.RELATION_DATE_OF_PURCHASE.toString() + "> \"" + sensor.getDateOfPurchase() + "\" . ";
        }
        if (sensor.getDateOfLastCalibration() != null) {
            query += "<" + sensor.getUri() + "> <" + Vocabulary.RELATION_DATE_OF_LAST_CALIBRATION.toString() + "> \"" + sensor.getDateOfLastCalibration() + "\" . ";
        }
        
        query += " }";
        
        return query;
    }
    
    /**
     * update a list of sensors. The sensors data must have been checked before
     * @see SensorDAOSesame#check(java.util.List)
     * @param sensors 
     * @return the updated result
     */
    private POSTResultsReturn update(List<SensorDTO> sensors) {
        List<Status> updateStatus = new ArrayList<>();
        List<String> updatedResourcesUri = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        for (SensorDTO sensorDTO : sensors) {
            //1. delete already existing data
            //1.1 get informations that will be updated (to delete the right triplets)
            uri = sensorDTO.getUri();
            ArrayList<Sensor> sensorsCorresponding = allPaginate();
            if (sensorsCorresponding.size() > 0) {
                String deleteQuery = prepareDeleteQuery(sensorsCorresponding.get(0));
                
                //2. insert new data
                UpdateRequest insertQuery = prepareInsertQuery(sensorDTO.createObjectFromDTO());
                try {
                    this.getConnection().begin();
                    Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                    Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
                    LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                    LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                    prepareDelete.execute();
                    prepareUpdate.execute();
                    updatedResourcesUri.add(sensorDTO.getUri());
                } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationUpdate = false;
                    updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                }
            } else {
                annotationUpdate = false;
                updateStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown sensor " + uri));
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
     * check and update the given sensors in the triplestore
     * @see SensorDAOSesame#check(java.util.List)
     * @see SensorDAOSesame#update(java.util.List)
     * @param sensors
     * @return the update result. Message error if errors founded in data,
     *         the list of the updated sensors's uri if they has been updated correctly
     */
    public POSTResultsReturn checkAndUpdate(List<SensorDTO> sensors) {
        POSTResultsReturn checkResult = check(sensors);
        if (checkResult.getDataState()) {
            return update(sensors);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * Generates the query to get the uri, label and rdf type of all the cameras
     * @example 
     * SELECT DISTINCT  ?uri ?label ?rdfType WHERE {
     *      ?uri  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#Camera> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label  ?label  .
     * }
     * @return the query
     */
    private SPARQLQueryBuilder prepareSearchCamerasQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendSelect("?" + URI + " ?" + RDF_TYPE + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_CAMERA.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendOrderBy("DESC(?" + LABEL + ")");
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Get the cameras (type, label, uri) of the triplestore.
     * @return The list of the cameras
     */
    public ArrayList<Sensor> getCameras() {
        SPARQLQueryBuilder query = prepareSearchCamerasQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Sensor> cameras = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Sensor camera = getSensorFromBindingSet(bindingSet);
                cameras.add(camera);
            }
        }
        return cameras;
    }
    
    /**
     * Check if a given sensor measured a given variable (is the sensor linked to the variable ?).
     * @see SensorDAOSesame#getVariables(java.lang.String)
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
     * Check the given data to update the list of the measured variables linked to the sensor.
     * @param sensorUri
     * @param variables
     * @return the check result.
     */
    private POSTResultsReturn checkMeasuredVariables(String sensorUri, List<String> variables) {
        POSTResultsReturn checkResult = new POSTResultsReturn();
        List<Status> checkStatus = new ArrayList<>();
        
        boolean dataOk = true;
        
        //1. Check if the sensorUri is a sensor in the triplestore
        if (existAndIsSensor(sensorUri)) {
            VariableDaoSesame variableDaoSesame = new VariableDaoSesame();
            
            for (String variableUri : variables) {
                //2. Check for each variable uri given if it exist and if it is really a variable
                if (!variableDaoSesame.existAndIsVariable(variableUri)) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Unknwon variable : " + variableUri));
                }
            }
            
            //3. Check if some variables links are removed. We do not check this if some problems has been founded before.
            if (dataOk) {
                //Get all the actual variables for the sensor
                HashMap<String, String> actualMeasuredVariables = getVariables(sensorUri);
                DatasetDAOMongo datasetDAO = new DatasetDAOMongo();
                
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
                    "Unknwon sensor : " + sensorUri));
        }
        
        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }
    
    /**
     * Update the list of the variables linked to the sensor.
     * /!\ Prerequisite : the data must have been checked before
     * @see SensorDAOSesame#checkMeasuredVariables(java.lang.String, java.util.List) 
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
        
        if (deleteObjectProperties(sensorUri, Vocabulary.RELATION_MEASURES.toString(), oldMeasuredVariables)) {
            //2. Add new object properties
            if (addObjectProperties(sensorUri, Vocabulary.RELATION_MEASURES.toString(), variables, Contexts.SENSORS.toString())) {
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
     * Check and update the variables measured by the given sensors
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
     * Prepare the SPARQL query to return all variables measured by a sensor.
     * 
     * @param sensor The sensor uri which measures veriables
     * @return The prepared query
     * @example 
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#Variable> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label ?label .
     *      <http://www.phenome-fppn.fr/2018/s18001> <http://www.phenome-fppn.fr/vocabulary/2017#measures> ?uri
     * }
     */
    private SPARQLQueryBuilder prepareSearchVariablesQuery(String sensorUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_VARIABLE.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendTriplet(sensorUri, Vocabulary.RELATION_MEASURES.toString(), "?" + URI, null);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Return a HashMap of uri => label of the variables measured by the given sensor.
     * 
     * @param sensor The sensor uri which measures veriables
     * @return HashMap of uri => label
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
}
