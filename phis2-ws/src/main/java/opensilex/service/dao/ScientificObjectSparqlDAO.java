//******************************************************************************
//                          ScientificObjectSparqlDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Aug. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package opensilex.service.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.SparqlDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.GeoSPARQL;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.ScientificObjectDTO;
import opensilex.service.resource.dto.LayerDTO;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyPostDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.ResourcesUtils;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.ScientificObject;
import opensilex.service.model.Property;
import opensilex.service.model.Uri;

/**
 * Scientific object DAO for a triplestore.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ScientificObjectSparqlDAO extends SparqlDAO<ScientificObject> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ScientificObjectSparqlDAO.class);
    
    //The following attributes are used to search scientific objects in the triplestore
    //uri of the scientific object
    public String uri;
    
    //type of the scientific object
    public String rdfType;
    
    //experiment of the scientific object
    public String experiment;
    private final String EXPERIMENT = "experiment";
    
    //alias of the scientific object
    public String alias;
    private final String ALIAS = "alias";
    
    private final String PROPERTY = "property";
    private final String PROPERTY_RELATION = "propertyRelation";
    private final String PROPERTY_TYPE = "propertyType";
    private final String CHILD = "child";
    private final String RELATION = "relation";
    
    private static final String URI_CODE_SCIENTIFIC_OBJECT = "o";

    public ScientificObjectSparqlDAO() {
        super();
    }
    
    /**
     * Generates a query to get the last scientific object URI from the given year.
     * @param year 
     * @return the query to get the URI of the last inserted scientific object 
     *         for the given year
     * @example
     * SELECT ?uri WHERE {
     *      ?uri  rdf:type  ?type  . 
     *      ?type  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#ScientificObject> . 
     *      FILTER ( regex(str(?uri), ".*\/2018/.*") ) 
     * }
     * ORDER BY desc(?uri) 
     * LIMIT 1
     */
    private SPARQLQueryBuilder prepareGetLastScientificObjectUriFromYear(String year) {
        SPARQLQueryBuilder queryLastScientificObjectURi = new SPARQLQueryBuilder();
        queryLastScientificObjectURi.appendSelect("?" + URI);
        queryLastScientificObjectURi.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        queryLastScientificObjectURi.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString(), null);
        queryLastScientificObjectURi.appendFilter("regex(str(?" + URI + "), \".*/" + year + "/.*\")");
        queryLastScientificObjectURi.appendOrderBy("desc(?" + URI + ")");
        queryLastScientificObjectURi.appendLimit(1);
        
        LOGGER.debug(SPARQL_QUERY + queryLastScientificObjectURi.toString());
        return queryLastScientificObjectURi;
    }
    
    /**
     * Gets the last scientific object id for the given year. 
     * @param year the year of the wanted scientific object number.
     * @return the id of the last scientific object inserted in the triplestore for the given year
     */
    public int getLastScientificObjectIdFromYear(String year) {
        SPARQLQueryBuilder lastScientificObjectUriFromYearQuery = prepareGetLastScientificObjectUriFromYear(year);
        
        this.getConnection().begin();

        //get the last scientificObject URI inserted during the given year
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, lastScientificObjectUriFromYearQuery.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        getConnection().commit();
        getConnection().close();
        
        String uriScientificObject = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriScientificObject = bindingSet.getValue(URI).stringValue();
        }
        
        if (uriScientificObject == null) {
            return 0;
        } else {
            //2018 -> 18. to get /o18
            String split = "/" + URI_CODE_SCIENTIFIC_OBJECT + year.substring(2, 4);
            String[] parts = uriScientificObject.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }
    
    /**
     * Generates the ask query to know if a given alias exists in a given context.
     * @param alias
     * @param context
     * @return the query
     */
    private SPARQLQueryBuilder askExistAliasInContext(String alias, String context) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendGraph(context);
        query.appendAsk("");
        query.appendToBody("?x <" + Rdfs.RELATION_LABEL.toString() + "> \"" + alias + "\"");
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
        
    /**
     * Checks if the scientific objects are valid.
     * @param ScientificObjectsDTO
     * @return
     * @throws RepositoryException 
     */
    public POSTResultsReturn check(List<ScientificObjectDTO> ScientificObjectsDTO) throws RepositoryException {
        //Expected results
        POSTResultsReturn scientificObjectsCheck;
        //returned status list
        List<Status> checkStatusList = new ArrayList<>();
        
        boolean dataOk = true;
        for (ScientificObjectDTO scientificObject : ScientificObjectsDTO) {
            //Check if the types are present in the ontology
            UriDAO uriDao = new UriDAO();

            if (!uriDao.isSubClassOf(scientificObject.getRdfType(), Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString())) {
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Wrong scientific object type value. See ontology"));
            }
            
            //check if the uri of the isPartOf object exists
            if (scientificObject.getIsPartOf() != null) {
                if (existUri(scientificObject.getIsPartOf())) {
                    //1. get isPartOf object type
                    uriDao.uri = scientificObject.getIsPartOf();
                    ArrayList<Uri> typesResult = uriDao.getAskTypeAnswer();
                    if (!uriDao.isSubClassOf(typesResult.get(0).getRdfType(), Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString())) {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "is part of object type is not scientific object"));
                    }
                } else {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "unknown is part of uri"));
                }
            }

            //check properties
            boolean missingAlias = true;
            for (PropertyPostDTO property : scientificObject.getProperties()) {
                //check alias
                if (property.getRelation().equals(Rdfs.RELATION_LABEL.toString())) {
                    missingAlias = false;
                    //check unique alias in the experiment
                    if (scientificObject.getExperiment() != null) {
                        SPARQLQueryBuilder query = askExistAliasInContext(property.getValue(), scientificObject.getExperiment());
                        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
                        boolean result = booleanQuery.evaluate();

                        if (result) {
                            dataOk = false;
                            checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "already existing alias for the given experiment"));
                        }
                    }
                }                
                
                //check if property exists in the ontology Vocabulary --> see how to check rdfs                
                if (existUri(property.getRelation()) == false) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the property relation " + property.getRelation() + " doesn't exist in the ontology"));
                }
            }
        
            if (missingAlias) {
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.MISSING_FIELDS, StatusCodeMsg.ERR, "missing alias"));
            }
        }
        scientificObjectsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        scientificObjectsCheck.statusList = checkStatusList;
        return scientificObjectsCheck;        
    }
    
    /**
     * Creates objects. Objects must have been checked.
     * @param scientificObjects
     * @return 
     */
    public POSTResultsReturn insert(List<ScientificObjectDTO> scientificObjects) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURIList = new ArrayList<>(); 
        
        POSTResultsReturn results;
        
        boolean resultState = false; // Data are valid and well inserted
        boolean annotationInsert = true; // Insertion well done
        
        final Iterator<ScientificObjectDTO> iteratorScientificObjects = scientificObjects.iterator();
        
        UriGenerator uriGenerator = new UriGenerator();
        
        while (iteratorScientificObjects.hasNext() && annotationInsert) {
            ScientificObjectDTO scientificObjectDTO = iteratorScientificObjects.next();
            ScientificObject scientificObject = scientificObjectDTO.createObjectFromDTO();
            
            try {
                //1. generates a scientific object URI
                scientificObject.setUri(uriGenerator.generateNewInstanceUri(scientificObject.getRdfType(), scientificObjectDTO.getYear(), null));
            } catch (Exception ex) { //In the scientific object case, no exception should be raised
                annotationInsert = false;
            }
            
            //2. Register in triplestore
            UpdateBuilder spql = new UpdateBuilder();
            
            Resource scientificObjectUri = ResourceFactory.createResource(scientificObject.getUri());
            Node scientificObjectType = NodeFactory.createURI(scientificObject.getRdfType());
            
            Node graph;
            if (scientificObject.getUriExperiment() != null) {
                graph = NodeFactory.createURI(scientificObject.getUriExperiment());
                
                // Add participates in (scientific object participates in experiment)
                Node participatesIn = NodeFactory.createURI(Oeso.RELATION_PARTICIPATES_IN.toString());
                spql.addInsert(graph, scientificObjectUri, participatesIn, graph);
            } else {
                graph = NodeFactory.createURI(Contexts.SCIENTIFIC_OBJECTS.toString());
            }
            
            spql.addInsert(graph, scientificObjectUri, RDF.type, scientificObjectType);
            
            // Scientific objects properties
            for (Property property : scientificObject.getProperties()) {
                if (property.getRdfType() != null && !property.getRdfType().equals("")) {// Typed property
                    if (property.getRdfType().equals(Oeso.CONCEPT_VARIETY.toString())) {
                        
                        String propertyURI;
                        try {
                            propertyURI = uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIETY.toString(), null, property.getValue());
                            Node propertyNode = NodeFactory.createURI(propertyURI);
                            Node propertyType = NodeFactory.createURI(property.getRdfType());
                            org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                            
                            spql.addInsert(graph, propertyNode, RDF.type, propertyType);
                            spql.addInsert(graph, scientificObjectUri, propertyRelation, propertyNode);
                        } catch (Exception ex) { //In the variety case, no exception should be raised
                            annotationInsert = false;
                        }
                    } else {
                        Node propertyNode = NodeFactory.createURI(property.getValue());
                        Node propertyType = NodeFactory.createURI(property.getRdfType());
                        org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                        
                        spql.addInsert(graph, propertyNode, RDF.type, propertyType);
                        spql.addInsert(graph, scientificObjectUri, propertyRelation, propertyNode);
                    }
                } else {
                    Literal propertyLiteral = ResourceFactory.createStringLiteral(property.getValue());
                    org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());

                    spql.addInsert(graph, scientificObjectUri, propertyRelation, propertyLiteral);         
                }
                
            }
            
            if (scientificObject.getUriExperiment() != null) {
                Node experimentUri = NodeFactory.createURI(scientificObject.getUriExperiment());
                org.apache.jena.rdf.model.Property relationHasPlot = ResourceFactory.createProperty(Oeso.RELATION_HAS_PLOT.toString());
                
                spql.addInsert(graph, experimentUri, relationHasPlot, scientificObjectUri);  
            }
            
            // isPartOf: the object which has part the element must not be a plot    
            if (scientificObject.getIsPartOf()!= null) {
                Node agronomicalObjectPartOf = NodeFactory.createURI(scientificObject.getIsPartOf());
                org.apache.jena.rdf.model.Property relationIsPartOf = ResourceFactory.createProperty(Oeso.RELATION_HAS_PLOT.toString());
                
                spql.addInsert(graph, scientificObjectUri, relationIsPartOf, agronomicalObjectPartOf);  
            }
            
            try {
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spql.buildRequest().toString());
                LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + prepareUpdate.toString());
                prepareUpdate.execute();
                createdResourcesURIList.add(scientificObject.getUri());
               
                if (annotationInsert) {
                    resultState = true;
                    this.getConnection().commit();
                } else {
                    // retour en arrière sur la transaction
                    this.getConnection().rollback();
                }
            } catch (RepositoryException ex) {
                    LOGGER.error("Error during commit or rolleback Triplestore statements: ", ex);
            } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationInsert = false;
                    insertStatusList.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_CREATE_QUERY + e.getMessage()));
            } 
            
            //3. insert in postgresql
            ScientificObjectSQLDAO scientificObjectDAO = new ScientificObjectSQLDAO();
            ArrayList<ScientificObject> aos = new ArrayList<>();
            aos.add(scientificObject);
            POSTResultsReturn postgreInsertionResult = scientificObjectDAO.checkAndInsertListAO(aos);
            insertStatusList.addAll(postgreInsertionResult.statusList);
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatusList;
        if (resultState && !createdResourcesURIList.isEmpty()) {
            results.createdResources = createdResourcesURIList;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesURIList.size() + " new resource(s) created."));
        }

        return results;
    }
    
    /**
     * Check objects and create them.
     * @param scientificObjectsDTO
     * @return 
     */
    public POSTResultsReturn checkAndInsert(List<ScientificObjectDTO> scientificObjectsDTO) {
        POSTResultsReturn checkResult = check(scientificObjectsDTO);
        if (checkResult.statusList.size() > 0) {
            return checkResult;
        } else {
            return insert(scientificObjectsDTO);
        }
    }
    

    /**
     * @param experimentURI
     * @return the query enabling to retrieve the list of scientific objects participating in the experiment and the objects propertiesT
     * SILEX:TODO 
     * filter on the rdfs.type --> scientific object
     * \SILEX:TODO 
     */
    private SPARQLQueryBuilder prepareSearchExperimentScientificObjects(String experimentURI) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(experimentURI);
        sparqlQuery.appendSelect("?" + CHILD +" ?" + RDF_TYPE + " ?" + PROPERTY + " ?" + PROPERTY_RELATION + " ?" + PROPERTY_TYPE);
        
        sparqlQuery.appendTriplet("?" + CHILD, Oeso.RELATION_PARTICIPATES_IN.toString(),experimentURI, null);
        sparqlQuery.appendTriplet("?" + CHILD, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        sparqlQuery.appendTriplet("?" + CHILD, "?" + PROPERTY_RELATION, "?" + PROPERTY, null);
        
        sparqlQuery.beginBodyOptional();
        sparqlQuery.appendToBody("?" + PROPERTY +" <" + Rdf.RELATION_TYPE.toString() + "> ?" + PROPERTY_TYPE);
        sparqlQuery.endBodyOptional();
        
        LOGGER.debug(SPARQL_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }
    
    /**
     * Prepares the query to search children with contained (geo:contains) elements.
     * @param objectURI
     * @return the query to search children with contained elements
     */
    private SPARQLQueryBuilder prepareSearchChildrenWithContains(String objectURI, String objectType) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        if (objectType.equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
            sparqlQuery.appendGraph(objectURI);
        }
        sparqlQuery.appendSelect("?" + CHILD + " ?" + RDF_TYPE);
        sparqlQuery.appendTriplet(objectURI, GeoSPARQL.RELATION_CONTAINS_MULTIPLE.toString(), "?" + CHILD, null);
        sparqlQuery.appendTriplet("?" + CHILD, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        
        LOGGER.debug(SPARQL_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }
    
    /**
     * Prepares the query to search the first children contained (geo:contains).
     * @param objectURI
     * @return the query to search the first children contained (geo:contains).
     */
    private SPARQLQueryBuilder prepareSearchFirstChildrenWithContains(String objectURI) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendPrefix("geo", GeoSPARQL.NAMESPACE.toString());
        sparqlQuery.appendSelect("?" + CHILD + " ?" + RDF_TYPE);
        sparqlQuery.appendTriplet(objectURI, GeoSPARQL.RELATION_CONTAINS.toString(), "?" + CHILD, null);
        sparqlQuery.appendTriplet("?" + CHILD, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        
        LOGGER.debug(SPARQL_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }
     
    /**
     * Search the descendants of the layer.
     * @param layerDTO
     * @return the descendants of the layer. Returns all the descendants if 
     * layerDTO.getDepth == true. (key: URI, value: type)
     */
    public HashMap<String, ScientificObject> searchChildren(LayerDTO layerDTO) {
        HashMap<String, ScientificObject> children = new HashMap<>(); // uri (clé), type (valeur)
        
        /* If it is an experiment, the name of the link is not the same so we 
        start by getting the list of the direct children
        */
        if (layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
            //SILEX:test
            // For pool connection issues
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID);
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            
            SPARQLQueryBuilder sparqlQuery = prepareSearchExperimentScientificObjects(layerDTO.getObjectUri());
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            
            TupleQueryResult result = tupleQuery.evaluate();
                        
            //SILEX:test
            // For pool connection issues
            getConnection().close();
            //\SILEX:test
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                ScientificObject scientificObject = children.get(bindingSet.getValue(CHILD).stringValue());
                if (scientificObject != null) { // Add property 
                    Property property = new Property();
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    property.setRelation(bindingSet.getValue(PROPERTY_RELATION).stringValue());
                    if (bindingSet.getValue(PROPERTY_TYPE) != null) {
                        property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                    }
                    
                    scientificObject.addProperty(property);
                } else { // It is not in the list yet, it has to be added
                    scientificObject = new ScientificObject();
                    scientificObject.setUri(bindingSet.getValue(CHILD).stringValue());
                    scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                    
                    Property property = new Property();
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    property.setRelation(bindingSet.getValue(PROPERTY_RELATION).stringValue());
                    if (bindingSet.getValue(PROPERTY_TYPE) != null) {
                        property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                    }
                    scientificObject.addProperty(property);
                }
                
                children.put(scientificObject.getUri(), scientificObject);
            }
        }
        /*
        For the moment, we only get the properties of the first descendants of
        the experimentation, not the others.
        If all descendants are needed
        */
        if (ResourcesUtils.getStringBooleanValue(layerDTO.getDepth())) {
            // If they are descendants from a trial, a specific treatment is needed
            if (layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
                // We get all the children of the plots of the experimentation, recovered previously
                    //SILEX:test
                    // For pool connection issues
                    rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
                    rep.initialize();
                    setConnection(rep.getConnection());
                    //\SILEX:test
                for (Entry<String, ScientificObject> child : children.entrySet()) {
                   
                    SPARQLQueryBuilder sparqlQuery = prepareSearchChildrenWithContains(child.getKey(), child.getValue().getRdfType());
                    TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
                    TupleQueryResult result = tupleQuery.evaluate();
                   
                    
                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        if (!children.containsKey(bindingSet.getValue(CHILD).stringValue())) {
                            ScientificObject scientificObject = new ScientificObject();
                            scientificObject.setUri(bindingSet.getValue(CHILD).stringValue());
                            scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                            
                            children.put(bindingSet.getValue(CHILD).stringValue(), scientificObject);
                        }
                    }
                }
                 //SILEX:test
                 // For pool connection issues
                    getConnection().close();
                    //\SILEX:test
            } else { // If a classic object
                //SILEX:test
                // For pool connection issues
                rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID);
                rep.initialize();
                setConnection(rep.getConnection());
                //\SILEX:test
                SPARQLQueryBuilder sparqlQuery = prepareSearchChildrenWithContains(layerDTO.getObjectUri(), layerDTO.getObjectType());
                TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
                TupleQueryResult result = tupleQuery.evaluate();
                //SILEX:test
                // For pool connection issues
                getConnection().close();
                //\SILEX:test
                
                
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    ScientificObject scientificObject = new ScientificObject();
                    scientificObject.setUri(bindingSet.getValue(CHILD).stringValue());
                    scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());

                    children.put(bindingSet.getValue(CHILD).stringValue(), scientificObject);
                }
            }
            
        } else if (!layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) { 
            // If only direct children needed and not an experimentation
            //SILEX:test
            // For pool connection issues
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID);
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            SPARQLQueryBuilder sparqlQuery = prepareSearchFirstChildrenWithContains(layerDTO.getObjectUri());
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            TupleQueryResult result = tupleQuery.evaluate();
            //SILEX:test
            // For pool connection issues
            getConnection().close();
            //\SILEX:test
            
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                ScientificObject scientificObject = new ScientificObject();
                scientificObject.setUri(bindingSet.getValue(CHILD).stringValue());
                scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());

                children.put(bindingSet.getValue(CHILD).stringValue(), scientificObject);
            }
        }
        //\SILEX:INFO
        
        return children;
    }
    
    /**
     * @return scientific objects list, result of the user query, empty if no result
     */
    public ArrayList<ScientificObject> allPaginate() {
        try {
            SPARQLQueryBuilder sparqlQuery = prepareSearchQuery();
            //SILEX:test
            // For pool connection issues
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID);
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            Map<String, ScientificObject> foundedScientificObjects = new HashMap<>();
            
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    boolean alreadyFoundedUri = false;
                    
                    String actualUri = uri != null ? uri : bindingSet.getValue(URI).stringValue();
                    
                    if (foundedScientificObjects.containsKey(actualUri)) {
                        alreadyFoundedUri = true;
                    }
                    
                    ScientificObject scientificObject;
                    
                    Property property = new Property();
                    
                    property.setRelation(bindingSet.getValue(RELATION).stringValue());
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    
                    if (alreadyFoundedUri) {
                        scientificObject = foundedScientificObjects.get(actualUri);
                    } else {
                        scientificObject = new ScientificObject();
                        scientificObject.setUri(actualUri);
                        
                        if (experiment != null) {
                            scientificObject.setUriExperiment(experiment);
                        } else if (bindingSet.getValue(EXPERIMENT) != null) {
                            scientificObject.setExperiment(bindingSet.getValue(EXPERIMENT).stringValue());
                        }
                        
                        if (alias != null) {
                            scientificObject.setAlias(alias);
                        } else {
                            scientificObject.setAlias(bindingSet.getValue(ALIAS).stringValue());
                        }
                        
                        if (rdfType != null) {
                            scientificObject.setRdfType(rdfType);
                        } else {
                            scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                        }
                    }
                    
                    scientificObject.addProperty(property);
                    
                    foundedScientificObjects.put(actualUri, scientificObject);
                }
            }
            
            ArrayList<String> scientificObjectsUris = new ArrayList<>();
            ArrayList<ScientificObject> scientificObjects = new ArrayList<>();
            foundedScientificObjects.entrySet().forEach((entry) -> {
                scientificObjects.add(entry.getValue());
                scientificObjectsUris.add(entry.getKey());
            });
            
            //Get geometries in relational database
            ScientificObjectSQLDAO scientificObjectDao = new ScientificObjectSQLDAO();
            HashMap<String, String> geometries = scientificObjectDao.getGeometries(scientificObjectsUris);
            
            scientificObjects.forEach((scientificObject) -> {
                scientificObject.setGeometry(geometries.get(scientificObject.getUri()));
            });
            
            
            //SILEX:test
            // For pool connection issues
            getConnection().close();
            //\SILEX:test
            
            return scientificObjects;
        }   catch (SQLException ex) {
            java.util.logging.Logger.getLogger(ScientificObjectSparqlDAO.class.getName()).log(Level.SEVERE, null, ex);
            
            if (getConnection() != null) {
                getConnection().close();
            }
            
            return null;
        }
    }
    
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:todo
        // object properties will have to be added
        //\SILEX:todo
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        
        sparqlQuery.appendDistinct(true);
                
        String scientificObjectURI;
        
        if (uri != null ) {
            scientificObjectURI = "<" + uri + ">";
        } else {
            scientificObjectURI = "?" + URI;
            sparqlQuery.appendSelect(" ?" + URI);
        }
        
        if (experiment != null) {
              sparqlQuery.appendFrom("<" + Contexts.VOCABULARY.toString() + "> \n FROM <" + experiment + ">");
        } else {
            sparqlQuery.appendSelect("?" + EXPERIMENT);
            sparqlQuery.appendOptional(scientificObjectURI + " <" + Oeso.RELATION_PARTICIPATES_IN.toString() + "> " + "?" + EXPERIMENT);
        }
        
        if (alias != null) {
            sparqlQuery.appendTriplet(scientificObjectURI, Rdfs.RELATION_LABEL.toString(), "\"" + alias + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?" + ALIAS);
            sparqlQuery.appendTriplet(scientificObjectURI, Rdfs.RELATION_LABEL.toString(), "?" + ALIAS, null);
        }
        
        if (rdfType != null) {
            sparqlQuery.appendTriplet(scientificObjectURI, Rdf.RELATION_TYPE.toString(), rdfType, null);
        } else {
            sparqlQuery.appendSelect(" ?" + RDF_TYPE);
            sparqlQuery.appendTriplet(scientificObjectURI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
            sparqlQuery.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString(), null);
        }
        
        sparqlQuery.appendSelect(" ?" + RELATION + " ?" + PROPERTY);
        sparqlQuery.appendTriplet(scientificObjectURI, "?" + RELATION, "?" + PROPERTY, null);
        
        LOGGER.debug(SPARQL_QUERY + sparqlQuery.toString());
        
        return sparqlQuery;
    }
    
    /**
     * Checks if the scientific object exists.
     * @param uri
     * @return 0 if the object URI doesn't exist, 1 if it does exist
     */
    public boolean existScientificObject(String uri) {        
        SPARQLQueryBuilder query = askExistScientificObject(uri);
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        boolean result = booleanQuery.evaluate();    
        return result;
    }

    /**
     * Generates the ask query to know if the scientific object URI exists.
     * existing in a given context
     * @param uri
     * @return the query
     */
    private SPARQLQueryBuilder askExistScientificObject(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendAsk("");
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString(), null);

        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }

    @Override
    public List<ScientificObject> create(List<ScientificObject> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<ScientificObject> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ScientificObject> update(List<ScientificObject> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScientificObject find(ScientificObject object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScientificObject findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void checkBeforeCreation(List<ScientificObject> objects) throws DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
