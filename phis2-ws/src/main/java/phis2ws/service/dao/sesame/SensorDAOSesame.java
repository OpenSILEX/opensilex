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
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
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
    //service date of the sensor(s)
    public String inServiceDate;
    private final String IN_SERVICE_DATE = "inServiceDate";
    //date of purchase of the sensor(s)
    public String dateOfPurchase;
    private final String DATE_OF_PURCHASE = "dateOfPurchase";
    //date of last calibration of the sensor(s)
    public String dateOfLastCalibration;
    private final String DATE_OF_LAST_CALIBRATION = "dateOfLastCalibration";

    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();
    final static String TRIPLESTORE_CONCEPT_SENSING_DEVICE = NAMESPACES.getObjectsProperty("cSensingDevice");
    final static String TRIPLESTORE_RELATION_TYPE = NAMESPACES.getRelationsProperty("type");
    final static String TRIPLESTORE_RELATION_LABEL = NAMESPACES.getRelationsProperty("label");
    final static String TRIPLESTORE_RELATION_BRAND = NAMESPACES.getRelationsProperty("rHasBrand");
    final static String TRIPLESTORE_RELATION_VARIABLE = NAMESPACES.getRelationsProperty("rMeasuredVariable");
    final static String TRIPLESTORE_RELATION_IN_SERVICE_DATE = NAMESPACES.getRelationsProperty("rInServiceDate");
    final static String TRIPLESTORE_RELATION_DATE_OF_PURCHASE = NAMESPACES.getRelationsProperty("rDateOfPurchase");
    final static String TRIPLESTORE_RELATION_DATE_OF_LAST_CALIBRATION = NAMESPACES.getRelationsProperty("rDateOfLastCalibration");
    final static String TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE = NAMESPACES.getRelationsProperty("subClassOf*");

    /**
     * prepare a query to get the higher id of the sensors
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastIdFromYear(String year) {
        URINamespaces uriNamespace = new URINamespaces();
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
     * get the higher id of the variables
     * @param year
     * @return the id
     */
    public int getLastIdFromYear(String year) {
        SPARQLQueryBuilder query = prepareGetLastIdFromYear(year);

        //get last variable uri inserted
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
}
