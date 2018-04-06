//******************************************************************************
//                                       VectorDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 9 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  9 mars 2018
// Subject:access to the vectors in the triplestore
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
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Vector;

/**
 * CRUD methods of vectors, in the triplestore rdf4j
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VectorDAOSesame extends DAOSesame<Vector> {
    
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
    
    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();
    final static String TRIPLESTORE_CONCEPT_VECTOR = NAMESPACES.getObjectsProperty("cVector");
    final static String TRIPLESTORE_RELATION_TYPE = NAMESPACES.getRelationsProperty("type");
    final static String TRIPLESTORE_RELATION_LABEL = NAMESPACES.getRelationsProperty("label");
    final static String TRIPLESTORE_RELATION_BRAND = NAMESPACES.getRelationsProperty("rHasBrand");
    final static String TRIPLESTORE_RELATION_VARIABLE = NAMESPACES.getRelationsProperty("rMeasuredVariable");
    final static String TRIPLESTORE_RELATION_IN_SERVICE_DATE = NAMESPACES.getRelationsProperty("rInServiceDate");
    final static String TRIPLESTORE_RELATION_DATE_OF_PURCHASE = NAMESPACES.getRelationsProperty("rDateOfPurchase");
    final static String TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE = NAMESPACES.getRelationsProperty("subClassOf*");
    
    /**
     * generates the query to get the number of vectors in the triplestore for 
     * a specific year
     * @param the 
     * @return query of number of vectors
     */
    private SPARQLQueryBuilder prepareGetVectorsNumber(String year) {
        URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder queryNumberVectors = new SPARQLQueryBuilder();
        queryNumberVectors.appendSelect("(count(distinct ?vector) as ?count)");
        queryNumberVectors.appendTriplet("?vector", TRIPLESTORE_RELATION_TYPE, "?rdfType", null);
        queryNumberVectors.appendTriplet("?rdfType", TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE, TRIPLESTORE_CONCEPT_VECTOR, null);
        queryNumberVectors.appendFilter("regex(str(?vector), \".*/" + year + "/.*\")");
        
        LOGGER.debug(SPARQL_SELECT_QUERY + queryNumberVectors.toString());
        return queryNumberVectors;
    }
    
    /**
     * get the number of vectors in the triplestore, for a given year
     * @param year
     * @see VectorDAOSesame#prepareGetVectorsNumber() 
     * @return the number of vectors in the triplestore
     */
    public int getNumberOfVectors(String year) {
        SPARQLQueryBuilder queryNumberVectors = prepareGetVectorsNumber(year);
        //SILEX:test
        //for the pool connection problems. 
        //WARNING this is a quick fix
        rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
        rep.initialize();
        setConnection(rep.getConnection());
        //\SILEX:test
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryNumberVectors.toString());
        TupleQueryResult result = tupleQuery.evaluate();        
        //SILEX:test
        //for the pool connection problems.
        getConnection().close();
        //\SILEX:test
        
        BindingSet bindingSet = result.next();
        String numberVectors = bindingSet.getValue("count").stringValue();
        
        return Integer.parseInt(numberVectors);
    }
    
    /**
     * generates a search query (search by uri, type, label, brand, variable,
     * inServiceDate, dateOfPurchase, dateOfLastCalibration)
     * @return the query to execute.
     * e.g. 
     * SELECT DISTINCT ?uri?rdfType ?label ?brand ?inServiceDate?dateOfPurchase 
     * WHERE {
     *      ?uri  ?0  ?rdfType  . 
     *      ?rdfType  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#Vector> . 
     *      OPTIONAL {
     *          ?uri rdfs:label ?label . 
     *      }
     *      ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasBrand>  ?brand  . 
     *      OPTIONAL {
     *          ?uri <http://www.phenome-fppn.fr/vocabulary/2017#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.phenome-fppn.fr/vocabulary/2017#dateOfPurchase> ?dateOfPurchase . 
     * }}
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
            query.appendTriplet("?" + RDF_TYPE, TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE, TRIPLESTORE_CONCEPT_VECTOR, null);
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

        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * get a vector from a given binding set.
     * Assume that the following attributes exist :
     * uri, rdfType, label, brand, variable, inServiceDate, dateOfPurchase
     * @param bindingSet a bindingSet from a search query
     * @return a sensor with data extracted from the given bindingSet
     */
    private Vector getVectorFromBindingSet(BindingSet bindingSet) {
        Vector vector = new Vector();

        if (uri != null) {
            vector.setUri(uri);
        } else {
            vector.setUri(bindingSet.getValue(URI).stringValue());
        }

        if (rdfType != null) {
            vector.setRdfType(rdfType);
        } else {
            vector.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
        }

        if (label != null) {
            vector.setLabel(label);
        } else if (bindingSet.getValue(LABEL) != null ){
            vector.setLabel(bindingSet.getValue(LABEL).stringValue());
        }

        if (brand != null) {
            vector.setBrand(brand);
        } else {
            vector.setBrand(bindingSet.getValue(BRAND).stringValue());
        }

        if (inServiceDate != null) {
            vector.setInServiceDate(inServiceDate);
        } else if (bindingSet.getValue(IN_SERVICE_DATE) != null) {
            vector.setInServiceDate(bindingSet.getValue(IN_SERVICE_DATE).stringValue());
        }

        if (dateOfPurchase != null) {
            vector.setDateOfPurchase(dateOfPurchase);
        } else if (bindingSet.getValue(DATE_OF_PURCHASE) != null) {
            vector.setDateOfPurchase(bindingSet.getValue(DATE_OF_PURCHASE).stringValue());
        }

        return vector;
    }
    
    /**
     * search all the sensors corresponding to the search params given by the user
     * @return the list of the sensors which match the given search params.
     */
    public ArrayList<Vector> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Vector> vectors = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Vector vector = getVectorFromBindingSet(bindingSet);
                vectors.add(vector);
            }
        }
        return vectors;
    }
}
