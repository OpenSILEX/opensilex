//******************************************************************************
//                                VectorDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 9 March 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.User;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Oeso;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.model.Vector;
import opensilex.service.ontology.Contexts;
import opensilex.service.resource.dto.VectorDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.rdf4j.query.Update;

/**
 * Vector DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Vincent Migot] 17 July 2019: Update getLastIdFromYear method to fix bug and limitation in URI generation
 */
public class VectorDAO extends Rdf4jDAO<Vector> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAO.class);

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
        
    private static final String MAX_ID = "maxID";
    
    /**
     * Generates the query to get the number of vectors in the triplestore for 
     * a specific year.
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
     * Gets the number of vectors in the triplestore, for a given year
     * @param year
     * @see VectorDAO#prepareGetVectorsNumber() 
     * @return the number of vectors in the triplestore
     */
    public int getNumberOfVectors(String year) {
        SPARQLQueryBuilder queryNumberVectors = prepareGetVectorsNumber(year);
        //SILEX:test
        //for the pool connection problems. 
        //WARNING this is a quick fix
        rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore
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
     * Generates a search query.
     * @return the query to execute.
     * @example 
     * SELECT DISTINCT  ?rdfType ?uri ?label ?brand ?serialNumber ?inServiceDate 
     * ?dateOfPurchase ?personInCharge 
     * WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *      ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#Vector> . 
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
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> ?dateOfPurchase . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#personInCharge> ?personInCharge . 
     *      }
     *      FILTER ( (REGEX ( str(?uri),".*op.*","i")) ) 
     * }
     * LIMIT 20 
     * OFFSET 0
     */
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        //RDF Type filter
        if (rdfType == null) {
            rdfType = "";
        }
        query.appendSelect("?" + RDF_TYPE);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_VECTOR.toString(), null);
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
     * Count query generated by the searched parameters.
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
     * Counts the number of vectors by the given searched parameters.
     * @return The number of vectors 
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
     * Count query to get the number of UAV in the triplestore.
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
     * Counts the number of UAVs.
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
     * Prepares a query to get the higher id of the vector.
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
        Var type = makeVar(RDF_TYPE);
        Var maxID = makeVar(MAX_ID);
        
        // Select the highest identifier
        query.addVar(maxID);
        
        // Get sensor type
        query.addWhere(uri, RDF.type, type);
        // Filter by type subclass of vector
        Node sensorConcept = NodeFactory.createURI(Oeso.CONCEPT_VECTOR.toString());
        query.addWhere(type, PathFactory.pathZeroOrMore1(PathFactory.pathLink(RDFS.subClassOf.asNode())), sensorConcept);
        
        ExprFactory expr = new ExprFactory();
        
        // Filter by year prefix
        Expr yearFilter =  expr.regex(expr.str(uri), ".*/" + year + "/.*", "");
        query.addFilter(yearFilter);
        
        // Binding to extract the last part of the URI as a MAX_ID integer
        Expr indexBinding =  expr.function(
            XSD.integer.getURI(), 
            ExprList.create(Arrays.asList(
                expr.strafter(expr.str(uri), UriGenerator.getVectorUriPatternByYear(year)))
            )
        );
        query.addBind(indexBinding, maxID);
        
        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID,  Query.ORDER_DESCENDING));
        query.setLimit(1);
        
        return query.build();
    }
    
    /**
     * Gets the highest id of the vector.
     * @param year
     * @return the id
     */
    public int getLastIdFromYear(String year) {
        Query query = prepareGetLastIdFromYear(year);

        //get last vector uri inserted
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
     * Gets a vector from a given binding set.
     * Assumes that the following attributes exist :
     * URI, rdfType, label, brand, variable, inServiceDate, dateOfPurchase
     * @param bindingSet a bindingSet from a search query
     * @return a sensor with data extracted from the given bindingSet
     */
    private Vector getVectorFromBindingSet(BindingSet bindingSet) {
        Vector vector = new Vector();

        if (bindingSet.getValue(URI) != null) {
            vector.setUri(bindingSet.getValue(URI).stringValue());
        }

        if (bindingSet.getValue(RDF_TYPE) != null) {
            vector.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
        }

        if (bindingSet.getValue(LABEL) != null) {
            vector.setLabel(bindingSet.getValue(LABEL).stringValue());
        }

        if (bindingSet.getValue(BRAND) != null) {
            vector.setBrand(bindingSet.getValue(BRAND).stringValue());
        }

        if (bindingSet.getValue(SERIAL_NUMBER) != null) {
            vector.setSerialNumber(bindingSet.getValue(SERIAL_NUMBER).stringValue());
        }
        
        if (bindingSet.getValue(IN_SERVICE_DATE) != null) {
            vector.setInServiceDate(bindingSet.getValue(IN_SERVICE_DATE).stringValue());
        }

        if (bindingSet.getValue(DATE_OF_PURCHASE) != null) {
            vector.setDateOfPurchase(bindingSet.getValue(DATE_OF_PURCHASE).stringValue());
        }
        
        if (bindingSet.getValue(PERSON_IN_CHARGE) != null) {
            vector.setPersonInCharge(bindingSet.getValue(PERSON_IN_CHARGE).stringValue());
        }

        return vector;
    }
    
    /**
     * Searches all the vectors corresponding to the search parameters given.
     * @return the list of the sensors which match the given search parameters.
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
     * Checks the given vectors' metadata.
     * @param vectorsDTO
     * @return the result with the list of the errors founded (empty if no errors)
     */
    public POSTResultsReturn check(List<VectorDTO> vectorsDTO) {
        POSTResultsReturn vectorsCheck = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;

        //1. check if user is an administrator
        UserDAO userDao = new UserDAO();
        if (userDao.isAdmin(user)) {
            //2. check data
            for (VectorDTO vectorDTO : vectorsDTO) {
                try {
                    //2.1 check type (subclass of Vector)
                    UriDAO uriDao = new UriDAO();
                    if (!uriDao.isSubClassOf(vectorDTO.getRdfType(), Oeso.CONCEPT_VECTOR.toString())) {
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
                    java.util.logging.Logger.getLogger(VectorDAO.class.getName()).log(Level.SEVERE, null, ex);
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
     * Generates an insert query. 
     * @example
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
     * Inserts the given vectors in the storage.
     * @param vectors
     * @return the insertion result, with the errors list or the URI of the 
     *         inserted vectors
     */
    public POSTResultsReturn insert(List<VectorDTO> vectors) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false; //to know if data has been well inserted
        boolean annotationInsert = true; //
        
        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        vectors.stream().map((vectorDTO) -> vectorDTO.createObjectFromDTO()).map((vector) -> {
            try {
                vector.setUri(UriGenerator.generateNewInstanceUri(vector.getRdfType(), null, null));
            } catch (Exception ex) { //In the vectors case, no exception should be raised
                java.util.logging.Logger.getLogger(VectorDAO.class.getName()).log(Level.SEVERE, null, ex);
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
     * Prepares a delete query of the triplets corresponding to the given vector.
     * @example
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
     * Updates a list of vectors. The vectors data must have been checked before.
     * @see VectorDAO#check(java.util.List) 
     * @param vectors
     * @return the update results
     */
    private POSTResultsReturn updateAndReturnPOSTResultsReturn(List<VectorDTO> vectors) {
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
     * Checks and inserts the given vectors in the storage.
     * @param vectors
     * @return the insertion result. Message error if errors founded in data,
     *         the list of the generated URI of the vectors if the insertion has been done
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
     * Checks and updates the given vectors in the triplestore.
     * @see VectorDAO#check(java.util.List) 
     * @see VectorDAO#updateAndReturnPOSTResultsReturn(java.util.List) 
     * @param vectors 
     * @return the update result. Message error if errors founded in data,
     *         the list of the updated vector's uri if the updated has been correcty done
     */
    public POSTResultsReturn checkAndUpdate(List<VectorDTO> vectors) {
        POSTResultsReturn checkResult = check(vectors);
        if (checkResult.getDataState()) {
            return updateAndReturnPOSTResultsReturn(vectors);
        } else { //errors founded in data
            return checkResult;
        }
    }
    
    /**
     * Generates the query to get the URI, label and rdf type of all the UAV.
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
     * Gets the UAV (type, label, uri) of the triplestore.
     * @return The list of the UAV
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

    @Override
    public List<Vector> create(List<Vector> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Vector> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Vector> update(List<Vector> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector find(Vector object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Vector> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
