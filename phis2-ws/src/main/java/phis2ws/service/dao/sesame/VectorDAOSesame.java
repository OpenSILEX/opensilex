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
import java.util.List;
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
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.resources.dto.VectorDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
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
    //type uri of the sensor(s)
    public String rdfType;
    //alias of the sensor(s)
    public String label;
    //brand of the sensor(s)
    public String brand;
    private final String BRAND = "brand";
    //serial number of the sensor
    public String serialNumber;
    private final String SERIAL_NUMBER = "serialNumber";
    //service date of the sensor(s)
    public String inServiceDate;
    private final String IN_SERVICE_DATE = "inServiceDate";
    //date of purchase of the sensor(s)
    public String dateOfPurchase;
    private final String DATE_OF_PURCHASE = "dateOfPurchase";
    //email of the person in charge of the sensor
    public String personInCharge;
    private final String PERSON_IN_CHARGE = "personInCharge";
        
    /**
     * generates the query to get the number of vectors in the triplestore for 
     * a specific year
     * @param the 
     * @return query of number of vectors
     */
    private SPARQLQueryBuilder prepareGetVectorsNumber(String year) {
        SPARQLQueryBuilder queryNumberVectors = new SPARQLQueryBuilder();
        queryNumberVectors.appendSelect("(COUNT(DISTINCT ?vector) AS ?count)");
        queryNumberVectors.appendTriplet("?vector", "<" + Rdf.RELATION_TYPE.toString() + ">", "?rdfType", null);
        queryNumberVectors.appendTriplet("?rdfType", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", "<" + Oeso.CONCEPT_VECTOR.toString() + ">", null);
        queryNumberVectors.appendFilter("REGEX(STR(?vector), \".*/" + year + "/.*\")");
        
        LOGGER.debug(SPARQL_QUERY + queryNumberVectors.toString());
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
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Vector> . 
     *      OPTIONAL {
     *          ?uri rdfs:label ?label . 
     *      }
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#hasBrand>  ?brand  . 
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> ?dateOfPurchase . 
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
            query.appendTriplet(sensorUri, "<" + Rdf.RELATION_TYPE.toString() + ">", rdfType, null);
        } else {
            query.appendSelect("?" + RDF_TYPE);
            query.appendTriplet(sensorUri, "<" + Rdf.RELATION_TYPE.toString() + ">", "?" + RDF_TYPE, null);
            query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", "<" + Oeso.CONCEPT_VECTOR.toString() + ">", null);
        }        

        if (label != null) {
            query.appendTriplet(sensorUri, "<" + Rdfs.RELATION_LABEL.toString() + ">", "\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?" + LABEL);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + LABEL + " . ");
            query.endBodyOptional();
        }

        if (brand != null) {
            query.appendTriplet(sensorUri, Oeso.RELATION_HAS_BRAND.toString(), "\"" + brand + "\"", null);
        } else {
            query.appendSelect(" ?" + BRAND);
            query.appendTriplet(sensorUri, Oeso.RELATION_HAS_BRAND.toString(), "?" + BRAND, null);
        }
        
        if (serialNumber != null) {
            query.appendTriplet(sensorUri, Oeso.RELATION_HAS_SERIAL_NUMBER.toString(), "\"" + serialNumber + "\"", null);
        } else {
            query.appendSelect(" ?" + SERIAL_NUMBER);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Oeso.RELATION_HAS_SERIAL_NUMBER.toString() + "> " + "?" + SERIAL_NUMBER + " .");
            query.endBodyOptional();
        }

        if (inServiceDate != null) {
            query.appendTriplet(sensorUri, Oeso.RELATION_IN_SERVICE_DATE.toString(), "\"" + inServiceDate + "\"", null);
        } else {
            query.appendSelect(" ?" + IN_SERVICE_DATE);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Oeso.RELATION_IN_SERVICE_DATE.toString() + "> " + "?" + IN_SERVICE_DATE + " . ");
            query.endBodyOptional();
        }

        if (dateOfPurchase != null) {
            query.appendTriplet(sensorUri, Oeso.RELATION_DATE_OF_PURCHASE.toString(), "\"" + dateOfPurchase + "\"", null);
        } else {
            query.appendSelect("?" + DATE_OF_PURCHASE);
            query.beginBodyOptional();
            query.appendToBody(sensorUri + " <" + Oeso.RELATION_DATE_OF_PURCHASE.toString() + "> " + "?" + DATE_OF_PURCHASE + " . ");
            query.endBodyOptional();
        }
        
        if (personInCharge != null) {
            query.appendTriplet(sensorUri, Oeso.RELATION_PERSON_IN_CHARGE.toString(), personInCharge, null);
        } else {
            query.appendSelect("?" + PERSON_IN_CHARGE);
            query.appendTriplet(sensorUri, Oeso.RELATION_PERSON_IN_CHARGE.toString(), "?" + PERSON_IN_CHARGE, null);
        }
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage()* this.getPageSize());

        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }

    /**
     * Count query generated by the searched parameters : uri, rdfType, 
     * label, brand, variable, inServiceDate, dateOfPurchase, dateOfLastCalibration
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) WHERE {
     *      ?uri  ?0  ?rdfType  . 
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Vector> . 
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
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#personInCharge>  ?personInCharge  . 
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
     * Count the number of vectors by the given searched params : uri, rdfType, 
     * label, brand, variable, inServiceDate, dateOfPurchase, dateOfLastCalibration
     * @return The number of vectors 
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
     * Count query to get the number of UAV in the triplestore
     * @example
     * SELECT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#UAV> . 
     *      ?uri  rdf:type  ?rdfType  . 
     *      ?uri  rdfs:label  ?label  . 
     * }
     * @return Query generated to count the elements
     */
    private SPARQLQueryBuilder prepareCountUAVs() {
        SPARQLQueryBuilder query = this.prepareSearchUAVsQuery();
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
     * Count the number of uavs.
     * @example
     * SELECT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#UAV> . 
     *      ?uri  rdf:type  ?rdfType  . 
     *      ?uri  rdfs:label  ?label  . 
     * }
     * @return The number of uavs 
     * @inheritdoc
     */
    public Integer countUAVs() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        SPARQLQueryBuilder prepareCount = prepareCountUAVs();
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
     * prepare a query to get the higher id of the vector 
     * @return the generated query
     * @example
     * SELECT ?uri WHERE {
     *  ?uri  rdf:type  ?type  . 
     *  ?type  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Vector> . 
     *  FILTER ( regex(str(?uri), ".*\/2018/.*") ) 
     * }
     * ORDER BY desc(?uri)
     */
    private SPARQLQueryBuilder prepareGetLastIdFromYear(String year) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?type", null);
        query.appendTriplet("?type", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_VECTOR.toString(), null);
        query.appendFilter("regex(str(?uri), \".*/" + year + "/.*\")");
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * get the higher id of the vector
     * @param year
     * @return the id
     */
    public int getLastIdFromYear(String year) {
        SPARQLQueryBuilder query = prepareGetLastIdFromYear(year);

        //get last vector uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        getConnection().close();
        
        String uriVector = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriVector = bindingSet.getValue(URI).stringValue();
        }
        
        if (uriVector == null) {
            return 0;
        } else {
            //2018 -> 18. to get /v18
            String split = "/v" + year.substring(2, 4);
            String[] parts = uriVector.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
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
        } else if (bindingSet.getValue(URI) != null) {
            vector.setUri(bindingSet.getValue(URI).stringValue());
        }

        if (rdfType != null) {
            vector.setRdfType(rdfType);
        } else if (bindingSet.getValue(RDF_TYPE) != null) {
            vector.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
        }

        if (label != null) {
            vector.setLabel(label);
        } else if (bindingSet.getValue(LABEL) != null) {
            vector.setLabel(bindingSet.getValue(LABEL).stringValue());
        }

        if (brand != null) {
            vector.setBrand(brand);
        } else if (bindingSet.getValue(BRAND) != null) {
            vector.setBrand(bindingSet.getValue(BRAND).stringValue());
        }

        if (serialNumber != null) {
            vector.setSerialNumber(serialNumber);
        } else if (bindingSet.getValue(SERIAL_NUMBER) != null) {
            vector.setSerialNumber(bindingSet.getValue(SERIAL_NUMBER).stringValue());
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
        
        if (personInCharge != null) {
            vector.setPersonInCharge(personInCharge);
        } else if (bindingSet.getValue(PERSON_IN_CHARGE) != null) {
            vector.setPersonInCharge(bindingSet.getValue(PERSON_IN_CHARGE).stringValue());
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
    
    /**
     * check the given vectors's metadata
     * @param vectorsDTO
     * @return the result with the list of the errors founded (empty if no errors)
     */
    public POSTResultsReturn check(List<VectorDTO> vectorsDTO) {
        POSTResultsReturn vectorsCheck = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;

        //1. check if user is an administrator
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
        if (userDao.isAdmin(user)) {
            //2. check data
            for (VectorDTO vectorDTO : vectorsDTO) {
                try {
                    //2.1 check type (subclass of Vector)
                    UriDaoSesame uriDaoSesame = new UriDaoSesame();
                    if (!uriDaoSesame.isSubClassOf(vectorDTO.getRdfType(), Oeso.CONCEPT_VECTOR.toString())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Bad vector type given"));
                    }

                    //2.2 check if person in charge exist
                    User u = new User(vectorDTO.getPersonInCharge());
                    if (!userDao.existInDB(u)) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person in charge email"));
                    }
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(VectorDAOSesame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            dataOk = false;
            checkStatus.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, StatusCodeMsg.ADMINISTRATOR_ONLY));
        }
        
        vectorsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        vectorsCheck.statusList = checkStatus;
        return vectorsCheck;
    }
    
    /**
     * generates an insert query for vector. 
     * e.g.
     * INSERT DATA {
     *  GRAPH <http://www.phenome-fppn.fr/diaphen/vectors> { 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdf:type  <http://www.opensilex.org/vocabulary/oeso#UAV> . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdfs:label  "par03_p"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#hasBrand>  "Skye Instruments"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#inServiceDate>  "2017-06-15"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#personInCharge>  "morgane.vidal@inra.fr"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber>  "A1E345F32"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase>  "2017-06-15"  . 
     *  }
     * }
     * @param vector
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(Vector vector) {
        UpdateBuilder query = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VECTORS.toString());
        Resource vectorUri = ResourceFactory.createResource(vector.getUri());

        Node vectorType = NodeFactory.createURI(vector.getRdfType());
        query.addInsert(graph, vectorUri, RDF.type, vectorType);
                
        query.addInsert(graph, vectorUri, RDFS.label, vector.getLabel());

        Property relationHasBrand = ResourceFactory.createProperty(Oeso.RELATION_HAS_BRAND.toString());
        Property relationInServiceDate = ResourceFactory.createProperty(Oeso.RELATION_IN_SERVICE_DATE.toString());
        Property relationPersonInCharge = ResourceFactory.createProperty(Oeso.RELATION_PERSON_IN_CHARGE.toString());
        
        query.addInsert(graph, vectorUri, relationHasBrand, vector.getBrand());
        query.addInsert(graph, vectorUri, relationInServiceDate, vector.getInServiceDate());
        query.addInsert(graph, vectorUri, relationPersonInCharge, vector.getPersonInCharge());
        
        if (vector.getSerialNumber() != null) {
            Property relationSerialNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_SERIAL_NUMBER.toString());
            query.addInsert(graph, vectorUri, relationSerialNumber, vector.getSerialNumber());
        }
        if (vector.getDateOfPurchase() != null) {
            Property relationDateOfPurchase = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_PURCHASE.toString());
            query.addInsert(graph, vectorUri, relationDateOfPurchase, vector.getDateOfPurchase());
        }
        
        UpdateRequest request = query.buildRequest();
        LOGGER.debug(getTraceabilityLogs() + " query : " + request.toString());
        
        return request;
    }
    
    /**
     * insert the given vectors in the triplestore
     * @param vectors
     * @return the insertion result, with the errors list or the uri of the 
     *         inserted vectors
     */
    public POSTResultsReturn insert(List<VectorDTO> vectors) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false; //to know if data has been well inserted
        boolean annotationInsert = true; //
        
        UriGenerator uriGenerator = new UriGenerator();
        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        vectors.stream().map((vectorDTO) -> vectorDTO.createObjectFromDTO()).map((vector) -> {
            try {
                vector.setUri(uriGenerator.generateNewInstanceUri(vector.getRdfType(), null, null));
            } catch (Exception ex) { //In the vectors case, no exception should be raised
                java.util.logging.Logger.getLogger(VectorDAOSesame.class.getName()).log(Level.SEVERE, null, ex);
            }
            return vector;            
        }).forEachOrdered((vector) -> {
            UpdateRequest query = prepareInsertQuery(vector);
            Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
            
            createdResourcesUri.add(vector.getUri());
        });
        
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
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUri.size() + " new resource(s) created."));
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        return results;
    }
    
    /**
     * prepare a delete query of the triplets corresponding to the given vector
     * e.g.
     * DELETE DATA { 
     *  GRAPH <http://www.phenome-fppn.fr/diaphen/vectors> { 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142> rdf:type <http://www.opensilex.org/vocabulary/oeso#UAV> . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142> rdfs:label "par03_p" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142> <http://www.opensilex.org/vocabulary/oeso#hasBrand> "Skye Instruments" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142> <http://www.opensilex.org/vocabulary/oeso#inServiceDate> "2017-06-15" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142> <http://www.opensilex.org/vocabulary/oeso#personInCharge> "morgane.vidal@inra.fr" . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142> <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> "A1E345F32" .
     *   }
     * }
     * @param vector
     * @return 
     */
    private UpdateRequest prepareDeleteQuery(Vector vector) {
        UpdateBuilder query = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VECTORS.toString());
        Resource vectorUri = ResourceFactory.createResource(vector.getUri());

        Node vectorType = NodeFactory.createURI(vector.getRdfType());
        query.addDelete(graph, vectorUri, RDF.type, vectorType);
                
        query.addDelete(graph, vectorUri, RDFS.label, vector.getLabel());

        Property relationHasBrand = ResourceFactory.createProperty(Oeso.RELATION_HAS_BRAND.toString());
        Property relationInServiceDate = ResourceFactory.createProperty(Oeso.RELATION_IN_SERVICE_DATE.toString());
        Property relationPersonInCharge = ResourceFactory.createProperty(Oeso.RELATION_PERSON_IN_CHARGE.toString());
        
        query.addDelete(graph, vectorUri, relationHasBrand, vector.getBrand());
        query.addDelete(graph, vectorUri, relationInServiceDate, vector.getInServiceDate());
        query.addDelete(graph, vectorUri, relationPersonInCharge, vector.getPersonInCharge());
        
        if (vector.getSerialNumber() != null) {
            Property relationSerialNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_SERIAL_NUMBER.toString());
            query.addDelete(graph, vectorUri, relationSerialNumber, vector.getSerialNumber());
        }
        if (vector.getDateOfPurchase() != null) {
            Property relationDateOfPurchase = ResourceFactory.createProperty(Oeso.RELATION_DATE_OF_PURCHASE.toString());
            query.addDelete(graph, vectorUri, relationDateOfPurchase, vector.getDateOfPurchase());
        }
        
        UpdateRequest request = query.buildRequest();
        LOGGER.debug(getTraceabilityLogs() + " query : " + request.toString());
        
        return request;        
    }
    
    /**
     * update a list of vectors. The vectors data must have been checked before
     * @see VectorDAOSesame#check(java.util.List) 
     * @param vectors
     * @return the update results
     */
    private POSTResultsReturn update(List<VectorDTO> vectors) {
        List<Status> updateStatus = new ArrayList<>();
        List<String> updatedResourcesUri = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        for (VectorDTO vectorDTO : vectors) {
            //1. delete already existing data
            //1.1 get informations that will be updated (to delete the right triplets)
            uri = vectorDTO.getUri();
            ArrayList<Vector> vectorsCorresponding = allPaginate();
            if (vectorsCorresponding.size() > 0) {
                UpdateRequest deleteQuery = prepareDeleteQuery(vectorsCorresponding.get(0));
                
                //2. insert new data
                UpdateRequest insertQuery = prepareInsertQuery(vectorDTO.createObjectFromDTO());
                
                try {
                    this.getConnection().begin();
                    Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery.toString());
                    LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                    prepareDelete.execute();
                    Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
                    LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                    prepareUpdate.execute();
                    updatedResourcesUri.add(vectorDTO.getUri());

                    updatedResourcesUri.add(vectorDTO.getUri());
                } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationUpdate = false;
                    updateStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                } 
                
            } else {
                annotationUpdate = false;
                updateStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown vector " + uri));
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
     * check and insert the given vectors in the triplestore
     * @param vectors
     * @return the insertion result. Message error if errors founded in data,
     *         the list of the generated uri of the vectors if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<VectorDTO> vectors) {
        POSTResultsReturn checkResult = check(vectors);
        if (checkResult.getDataState()) {
            return insert(vectors);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * check and update the given vectors in the triplestore
     * @see VectorDAOSesame#check(java.util.List) 
     * @see VectorDAOSesame#update(java.util.List) 
     * @param vectors 
     * @return the update result. Message error if errors founded in data,
     *         the list of the updated vector's uri if the updated has been correcty done
     */
    public POSTResultsReturn checkAndUpdate(List<VectorDTO> vectors) {
        POSTResultsReturn checkResult = check(vectors);
        if (checkResult.getDataState()) {
            return update(vectors);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * Generates the query to get the uri, label and rdf type of all the uav
     * @example
     * SELECT DISTINCT  ?uri ?label ?rdfType WHERE {
     *      ?uri  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#UAV> . 
     *      ?uri rdf:type ?rdfType .
     *      ?uri  rdfs:label  ?label  .
     * }
     * @return the query
     */
    private SPARQLQueryBuilder prepareSearchUAVsQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI + " ?" + RDF_TYPE + " ?" + LABEL );
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_UAV.toString(), null);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendOrderBy("desc(?" + LABEL + ")");
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage()* this.getPageSize());
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Get the uav (type, label, uri) of the triplestore.
     * @return The list of the uav
     */
    public ArrayList<Vector> getUAVs() {
        SPARQLQueryBuilder query = prepareSearchUAVsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Vector> uavs = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Vector uav = getVectorFromBindingSet(bindingSet);
                uavs.add(uav);
            }
        }
        return uavs;
    }
}
