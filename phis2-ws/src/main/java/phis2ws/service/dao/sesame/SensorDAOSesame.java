//******************************************************************************
//                                       SensorDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 9 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  9 mars 2018
// Subject: access to the sensors in the triplestore
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.resources.dto.SensorDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
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
    private final String URI = "uri";
    //type uri of the sensor(s)
    public String rdfType;
    private final String RDF_TYPE = "rdfType";
    //alias of the sensor(s)
    public String label;
    private final String LABEL = "label";
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

    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();
    
    final static String TRIPLESTORE_CONTEXT_SENSOR = NAMESPACES.getContextsProperty("sensors");
    
    final static String TRIPLESTORE_CONCEPT_SENSING_DEVICE = NAMESPACES.getObjectsProperty("cSensingDevice");
    
    final static String TRIPLESTORE_RELATION_BRAND = NAMESPACES.getRelationsProperty("rHasBrand");
    final static String TRIPLESTORE_RELATION_DATE_OF_LAST_CALIBRATION = NAMESPACES.getRelationsProperty("rDateOfLastCalibration");
    final static String TRIPLESTORE_RELATION_DATE_OF_PURCHASE = NAMESPACES.getRelationsProperty("rDateOfPurchase");
    final static String TRIPLESTORE_RELATION_IN_SERVICE_DATE = NAMESPACES.getRelationsProperty("rInServiceDate");
    final static String TRIPLESTORE_RELATION_LABEL = NAMESPACES.getRelationsProperty("label");
    final static String TRIPLESTORE_RELATION_PERSON_IN_CHARGE = NAMESPACES.getRelationsProperty("rPersonInCharge");
    final static String TRIPLESTORE_RELATION_TYPE = NAMESPACES.getRelationsProperty("type");
    final static String TRIPLESTORE_RELATION_SERIAL_NUMBER = NAMESPACES.getRelationsProperty("rSerialNumber");
    final static String TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE = NAMESPACES.getRelationsProperty("subClassOf*");
    final static String TRIPLESTORE_RELATION_VARIABLE = NAMESPACES.getRelationsProperty("rMeasuredVariable");

    /**
     * prepare a query to get the higher id of the sensors
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastIdFromYear(String year) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, TRIPLESTORE_RELATION_TYPE, "?type", null);
        query.appendTriplet("?type", TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE, TRIPLESTORE_CONCEPT_SENSING_DEVICE, null);
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
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_TYPE, rdfType, null);
        } else {
            query.appendSelect("?" + RDF_TYPE);
            query.appendTriplet(sensorUri, rdfType, "?" + RDF_TYPE, null);
            query.appendTriplet("?" + RDF_TYPE, TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE, TRIPLESTORE_CONCEPT_SENSING_DEVICE, null);
        }        

        if (label != null) {
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_LABEL, "\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?" + LABEL);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " " + TRIPLESTORE_RELATION_LABEL + " " + "?" + LABEL + " . ");
            query.endBodyOptional();
        }

        if (brand != null) {
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_BRAND, "\"" + brand + "\"", null);
        } else {
            query.appendSelect(" ?" + BRAND);
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_BRAND, "?" + BRAND, null);
        }
        
        if (serialNumber != null) {
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_SERIAL_NUMBER, "\"" + serialNumber + "\"", null);
        } else {
            query.appendSelect("?" + SERIAL_NUMBER);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + TRIPLESTORE_RELATION_SERIAL_NUMBER + "> ?" + SERIAL_NUMBER + " . ");
            query.endBodyOptional();
        }

        if (inServiceDate != null) {
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_IN_SERVICE_DATE, "\"" + inServiceDate + "\"", null);
        } else {
            query.appendSelect(" ?" + IN_SERVICE_DATE);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + TRIPLESTORE_RELATION_IN_SERVICE_DATE + "> " + "?" + IN_SERVICE_DATE + " . ");
            query.endBodyOptional();
        }

        if (dateOfPurchase != null) {
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_DATE_OF_PURCHASE, "\"" + dateOfPurchase + "\"", null);
        } else {
            query.appendSelect("?" + DATE_OF_PURCHASE);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + TRIPLESTORE_RELATION_DATE_OF_PURCHASE + "> " + "?" + DATE_OF_PURCHASE + " . ");
            query.endBodyOptional();
        }

        if (dateOfLastCalibration != null) {
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_DATE_OF_LAST_CALIBRATION, "\"" + dateOfLastCalibration + "\"", null);
        } else {
            query.appendSelect("?" + DATE_OF_LAST_CALIBRATION);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + TRIPLESTORE_RELATION_DATE_OF_LAST_CALIBRATION + "> " + "?" + DATE_OF_LAST_CALIBRATION + " . ");
            query.endBodyOptional();
        }
        
        if (personInCharge != null) {
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_PERSON_IN_CHARGE, "\"" + personInCharge + "\"", null);
        } else {
            query.appendSelect(" ?" + PERSON_IN_CHARGE);
            query.appendTriplet(sensorUri, TRIPLESTORE_RELATION_PERSON_IN_CHARGE, "?" + PERSON_IN_CHARGE, null);
        }

        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        } else {
            sensor.setUri(bindingSet.getValue(URI).stringValue());
        }

        if (rdfType != null) {
            sensor.setRdfType(rdfType);
        } else {
            sensor.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
        }

        if (label != null) {
            sensor.setLabel(label);
        } else if (bindingSet.getValue(LABEL) != null ){
            sensor.setLabel(bindingSet.getValue(LABEL).stringValue());
        }

        if (brand != null) {
            sensor.setBrand(brand);
        } else {
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
        } else {
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
                sensors.add(sensor);
            }
        }
        return sensors;
    }
    
    private SPARQLQueryBuilder prepareIsSensorQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendTriplet("<" + uri + ">", TRIPLESTORE_RELATION_TYPE, "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE, TRIPLESTORE_CONCEPT_SENSING_DEVICE, null);
        
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
        if (existObject(uri)) {
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
                //2.1 check required fields
                if ((boolean) sensor.isOk().get(AbstractVerifiedClass.STATE)) {
                    try {
                        //2.2 check date formats
                        if (!Dates.isDateYMD(sensor.getInServiceDate())) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.EXPECTED_DATE_FORMAT_YMD + " for the inServiceDate field"));
                        }
                        if (sensor.getDateOfPurchase() != null && !Dates.isDateYMD(sensor.getDateOfPurchase())) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.EXPECTED_DATE_FORMAT_YMD + " for the dateOfPurchase field"));
                        }
                        if (sensor.getDateOfLastCalibration()!= null && !Dates.isDateYMD(sensor.getDateOfLastCalibration())) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.EXPECTED_DATE_FORMAT_YMD + " for the dateOfLastCalibration field"));
                        }
                        
                        //2.3 check type (subclass of SensingDevice)
                        UriDaoSesame uriDaoSesame = new UriDaoSesame();
                        if (!uriDaoSesame.isSubClassOf(sensor.getRdfType(), TRIPLESTORE_CONCEPT_SENSING_DEVICE)) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Bad sensor type given. Must be sublass of SensingDevice concept"));
                        }
                        
                        //2.4 check if person in charge exist
                        User u = new User(sensor.getPersonInCharge());
                        if (!userDao.existInDB(u)) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person in charge email"));
                        }
                    } catch (Exception ex) {
                        java.util.logging.Logger.getLogger(SensorDAOSesame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else { //Missing required fields
                    dataOk = false;
                    sensor.isOk().remove(AbstractVerifiedClass.STATE);
                    checkStatus.add(new Status(StatusCodeMsg.BAD_DATA_FORMAT, StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(sensor.isOk()).toString()));
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
    private SPARQLUpdateBuilder prepareInsertQuery(Sensor sensor) {
        SPARQLUpdateBuilder query = new SPARQLUpdateBuilder();
        
        query.appendGraphURI(TRIPLESTORE_CONTEXT_SENSOR);
        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_TYPE, sensor.getRdfType(), null);
        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_LABEL, "\"" + sensor.getLabel() + "\"", null);
        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_BRAND, "\"" + sensor.getBrand() + "\"", null);
        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_IN_SERVICE_DATE, "\"" + sensor.getInServiceDate() + "\"", null);
        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_PERSON_IN_CHARGE, "\"" + sensor.getPersonInCharge() + "\"", null);
        
        if (sensor.getSerialNumber() != null) {
            query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_SERIAL_NUMBER, "\"" + sensor.getSerialNumber() + "\"", null);
        }
        
        if (sensor.getDateOfPurchase() != null) {
            query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_DATE_OF_PURCHASE, "\"" + sensor.getDateOfPurchase() + "\"", null);
        }
        
        if (sensor.getDateOfLastCalibration() != null) {
            query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_DATE_OF_LAST_CALIBRATION, "\"" + sensor.getDateOfLastCalibration() + "\"", null);
        }
        
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
            
            SPARQLUpdateBuilder query = prepareInsertQuery(sensor);
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
                + "<" + sensor.getUri() + "> " + TRIPLESTORE_RELATION_TYPE + " <" + sensor.getRdfType() + "> . "
                + "<" + sensor.getUri() + "> " + TRIPLESTORE_RELATION_LABEL + " \"" + sensor.getLabel() + "\" . "
                + "<" + sensor.getUri() + "> <" + TRIPLESTORE_RELATION_BRAND + "> \"" + sensor.getBrand() + "\" . "
                + "<" + sensor.getUri() + "> <" + TRIPLESTORE_RELATION_IN_SERVICE_DATE + "> \"" + sensor.getInServiceDate() + "\" . "
                + "<" + sensor.getUri() + "> <" + TRIPLESTORE_RELATION_PERSON_IN_CHARGE + "> \"" + sensor.getPersonInCharge() + "\" . ";
        
        if (sensor.getSerialNumber() != null) {
            query += "<" + sensor.getUri() + "> <" + TRIPLESTORE_RELATION_SERIAL_NUMBER + "> \"" + sensor.getSerialNumber() + "\" . ";
        }
        if (sensor.getDateOfPurchase() != null) {
            query += "<" + sensor.getUri() + "> <" + TRIPLESTORE_RELATION_DATE_OF_PURCHASE + "> \"" + sensor.getDateOfPurchase() + "\" . ";
        }
        if (sensor.getDateOfLastCalibration() != null) {
            query += "<" + sensor.getUri() + "> <" + TRIPLESTORE_RELATION_DATE_OF_LAST_CALIBRATION + "> \"" + sensor.getDateOfLastCalibration() + "\" . ";
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
                SPARQLUpdateBuilder insertQuery = prepareInsertQuery(sensorDTO.createObjectFromDTO());
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
}
