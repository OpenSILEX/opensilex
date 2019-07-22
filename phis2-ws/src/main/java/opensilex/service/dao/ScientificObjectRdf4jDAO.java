///******************************************************************************
//                       ScientificObjectRdf4jDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Aug. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.GeoSPARQL;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.LayerDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.ResourcesUtils;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.ScientificObject;
import opensilex.service.model.Property;
import opensilex.service.model.Uri;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDFS;
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
 * Allows CRUD methods of scientific objects in the triplestore.
 * @update [Morgane Vidal] 29 March, 2019: add update scientific objects and refactor to the new DAO conception.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ScientificObjectRdf4jDAO extends Rdf4jDAO<ScientificObject> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ScientificObjectRdf4jDAO.class);
    
    //The following attributes are used to search scientific objects in the triplestore
    private final String EXPERIMENT = "experiment";
    private final String ALIAS = "alias";
    private final String PROPERTY = "property";
    private final String PROPERTY_RELATION = "propertyRelation";
    private final String PROPERTY_TYPE = "propertyType";
    private final String CHILD = "child";
    private final String RELATION = "relation";
    
    private static final String URI_CODE_SCIENTIFIC_OBJECT = "o";
    
    private static final String MAX_ID = "maxID";
    
    public ScientificObjectRdf4jDAO() {
        super();
    }
    
    /**
     * Generates a query to get the last scientific object uri from the given year
     * @param year 
     * @return The query to get the uri of the last inserted scientific object 
     *         for the given year.
     * @example
     * <pre>
     * SELECT  ?maxID WHERE {
     *      ?uri a ?type .
     *      ?type (rdfs:subClassOf)* <http://www.opensilex.org/vocabulary/oeso#ScientificObject>
     *      FILTER regex(str(?uri), ".* /2019/.*", "")
     *      BIND(xsd:integer(strafter(str(?uri), "http://www.opensilex.org/diaphen/2019/s19")) AS ?maxID)
     * }
     * ORDER BY DESC(?maxID)
     * LIMIT 1
     * </pre>
     */
    private Query prepareGetLastScientificObjectUriFromYear(String year) {
        SelectBuilder query = new SelectBuilder();
        
        Var uri = makeVar(URI);
        Var type = makeVar(RDF_TYPE);
        Var maxID = makeVar(MAX_ID);
        
        // Select the highest identifier
        query.addVar(maxID);
        
        // Get sensor type
        query.addWhere(uri, RDF.type, type);
        // Filter by type subclass of scientific object
        Node scientificObjectConcept = NodeFactory.createURI(Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString());
        query.addWhere(type, PathFactory.pathZeroOrMore1(PathFactory.pathLink(RDFS.subClassOf.asNode())), scientificObjectConcept);
        
        ExprFactory expr = new ExprFactory();
        
        // Filter by year prefix
        Expr yearFilter =  expr.regex(expr.str(uri), ".*/" + year + "/.*", "");
        query.addFilter(yearFilter);
        
        // Binding to extract the last part of the URI as a MAX_ID integer
        Expr indexBinding =  expr.function(
            XSD.integer.getURI(), 
            ExprList.create(Arrays.asList(
                expr.strafter(expr.str(uri), UriGenerator.getScientificObjectUriPatternByYear(year)))
            )
        );
        query.addBind(indexBinding, maxID);
        
        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID,  Query.ORDER_DESCENDING));
        query.setLimit(1);
        
        return query.build();
    }
    
    /**
     * Get the last scientific object id for the given year. 
     * @param year The year of the wanted scientific object number.
     * @return The ID of the last scientific object inserted in the triplestore for the given year.
     */
    public int getLastScientificObjectIdFromYear(String year) {
        Query lastScientificObjectUriFromYearQuery = prepareGetLastScientificObjectUriFromYear(year);
        
        this.getConnection().begin();

        //Get the URI of the last scientific object inserted during the given year.
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, lastScientificObjectUriFromYearQuery.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        getConnection().commit();
        getConnection().close();
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            return Integer.valueOf(bindingSet.getValue(MAX_ID).stringValue());
        } else {
            return 0;
        }
    }
    
    /**
     * Generates the sparql ask query to know if a given alias is already 
     * existing in a given context.
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
     * Check if the scientific objects are accurates.
     * @param scientificObjects
     * @return
     * @throws RepositoryException 
     */
    public POSTResultsReturn check(List<ScientificObject> scientificObjects) throws RepositoryException {
        //Expected results
        POSTResultsReturn scientificObjectsCheck = null;
        //Returned status list
        List<Status> checkStatusList = new ArrayList<>();
        
        //Caches
        List<String> scientificObjectTypesCache = new ArrayList<>();
        List<String> isPartOfCache = new ArrayList<>();
        List<String> propertyUriCache = new ArrayList<>();
        
        boolean dataOk = true;
        for (ScientificObject scientificObject : scientificObjects) {
            //Check if the types are present in the ontology
            UriDAO uriDao = new UriDAO();

            if (!scientificObjectTypesCache.contains(scientificObject.getRdfType())) {
                if (!uriDao.isSubClassOf(scientificObject.getRdfType(), Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString())) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Wrong scientific object type value. See ontology"));
                } else {
                    scientificObjectTypesCache.add(scientificObject.getRdfType());
                }
            }
            
            //Check if the uri of the isPartOf object exists
            if (scientificObject.getIsPartOf() != null) {
                if (!isPartOfCache.contains(scientificObject.getIsPartOf()) && existUri(scientificObject.getIsPartOf())) {
                    //1. Get isPartOf object type
                    uriDao.uri = scientificObject.getIsPartOf();
                    ArrayList<Uri> typesResult = uriDao.getAskTypeAnswer();
                    if (!uriDao.isSubClassOf(typesResult.get(0).getRdfType(), Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString())) {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "is part of object type is not scientific object"));
                    } else {
                        isPartOfCache.add(scientificObject.getIsPartOf());
                    }
                } else {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "unknown is part of uri"));
                }
            }

            //Check properties
            boolean missingLabel = scientificObject.getLabel() == null; // if there is no label, it is missing so we check the properties.
            for (Property property : scientificObject.getProperties()) {
                //Check alias
                if (property.getRelation().equals(Rdfs.RELATION_LABEL.toString())) {
                    missingLabel = false;
                    //Check unique alias in the experiment
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
                
                //Check if property exists in the ontology Vocabulary --> see how to check rdfs
                if (!propertyUriCache.contains(property.getRelation())) {
                    if (existUri(property.getRelation()) == false) {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the property relation " + property.getRelation() + " doesn't exist in the ontology"));
                    } else {
                        propertyUriCache.add(property.getRelation());
                    }
                }
            }
        
            if (missingLabel) {
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.MISSING_FIELDS, StatusCodeMsg.ERR, "missing alias"));
            }
        }
        scientificObjectsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        scientificObjectsCheck.statusList = checkStatusList;
        return scientificObjectsCheck;        
    }
    
    /**
     * Check the given scientific objects and insert them in the triplestore.
     * @param scientificObjects
     * @return 
     */
    public POSTResultsReturn checkAndInsert(List<ScientificObject> scientificObjects) {
        POSTResultsReturn checkResult = check(scientificObjects);
        if (checkResult.statusList.size() > 0) { // Errors founded in the given scientific objects.
            return checkResult;
        } else { // No error founded, we can insert them.
              try {
                  List<ScientificObject> createdScientificObjects = create(scientificObjects);
                  ArrayList<String> createdScientificObjectsUris = new ArrayList<>();
                  for (ScientificObject scientificObject : createdScientificObjects) {
                      createdScientificObjectsUris.add(scientificObject.getUri());
                  }
                  
                  POSTResultsReturn creationResult = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
                  creationResult.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdScientificObjects.size() + " new resource(s) created."));
                  creationResult.createdResources = createdScientificObjectsUris;
                  
                  return creationResult;
              } catch (Exception ex) {
                  POSTResultsReturn creationResult = new POSTResultsReturn(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
                  creationResult.statusList.add(new Status(StatusCodeMsg.ERR, StatusCodeMsg.ERR, ex.getMessage()));
                  
                  return creationResult;
              }
        }
    }
    
    /**
     * Generates the query to get the list of scientific objects which participates in a given experiment.
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
     * Generates a query to get all the scientific objects contained in a given scientific object (geo:contains).
     * @param objectURI
     * @example
     * SELECT DISTINCT  ?child ?rdfType 
     * WHERE {
     *      <http://www.opensilex.org/opensilex/2019/o19000030>  <http://www.opengis.net/ont/geosparql#contains*>  ?child  . 
     *      ?child  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     * }
     * @return the query enabling to select all contained element (geo:contains) in objectURI.
     *         the query enabling to select all descendants
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
     * Generates the query to get the first scientific objects contained by a given scientific object (geo:contains).
     * @param objectURI
     * @return The generated query.
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
     * 
     * @param layerDTO
     * @return la liste des enfants de layerDTO.getObjectURI. Retourne tous les
     *         descendants si layerDTO.getDepth == true. (clé : uri, valeur : type)
     */
    public HashMap<String, ScientificObject> searchChildren(LayerDTO layerDTO) {
        HashMap<String, ScientificObject> children = new HashMap<>(); // uri (clé), type (valeur)
        
        //Si c'est une expérimentation, le nom du lien n'est pas le même donc, 
        //on commence par récupérer la liste des enfants directs
        if (layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
            //SILEX:test
            //Pour les soucis de pool de connexion
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            
            SPARQLQueryBuilder sparqlQuery = prepareSearchExperimentScientificObjects(layerDTO.getObjectUri());
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            
            TupleQueryResult result = tupleQuery.evaluate();
                        
            //SILEX:test
            //Pour les soucis de pool de connexion
            getConnection().close();
            //\SILEX:test
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                ScientificObject scientificObject = children.get(bindingSet.getValue(CHILD).stringValue());
                if (scientificObject != null) { //Il suffit juste de lui ajouter la propriété. 
                    Property property = new Property();
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    property.setRelation(bindingSet.getValue(PROPERTY_RELATION).stringValue());
                    if (bindingSet.getValue(PROPERTY_TYPE) != null) {
                        property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                    }
                    
                    scientificObject.addProperty(property);
                } else { //Il n'est pas encore dans la liste, il faut le rajouter
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
        //SILEX:INFO
        //Pour l'instant, on ne récupère que les propriétés des premiers descendants de l'expérimentation.
        //Pas les autres
        //Si il faut aussi tous les descendants
        if (ResourcesUtils.getStringBooleanValue(layerDTO.getDepth())) {
            //Si c'est les descendants d'un essai, il y a un traitement particulier
            if (layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
                //On recherche tous les fils des plots de l'experimentation, récupérés précédemment
                    //SILEX:test
                    //Pour les soucis de pool de connexion
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
                    //Pour les soucis de pool de connexion
                    getConnection().close();
                    //\SILEX:test
            } else { //Si c'est un objet classique
                //SILEX:test
                //Pour les soucis de pool de connexion
                rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
                rep.initialize();
                setConnection(rep.getConnection());
                //\SILEX:test
                SPARQLQueryBuilder sparqlQuery = prepareSearchChildrenWithContains(layerDTO.getObjectUri(), layerDTO.getObjectType());
                TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
                TupleQueryResult result = tupleQuery.evaluate();
                //SILEX:test
                //Pour les soucis de pool de connexion
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
            // If only direct descendants needed and not an experimentation
            //SILEX:test
            //Pour les soucis de pool de connexion
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            SPARQLQueryBuilder sparqlQuery = prepareSearchFirstChildrenWithContains(layerDTO.getObjectUri());
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            TupleQueryResult result = tupleQuery.evaluate();
            //SILEX:test
            //Pour les soucis de pool de connexion
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
     * Generates the query to get the list of properties for a given scientific object
     * @param uri
     * @return 
     * @example
     * SELECT   ?relation ?property ?propertyType 
     * WHERE {
     *      <http://www.opensilex.org/opensilex/2019/o19000115>  ?relation  ?property  . 
     *      OPTIONAL {?property <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?propertyType } 
     * }
     */
    public SPARQLQueryBuilder prepareSearchScientificObjectProperties(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect(" ?" + RELATION + " ?" + PROPERTY + " ?" + PROPERTY_TYPE);
        query.appendTriplet(uri, "?" + RELATION, "?" + PROPERTY, null);
        
        query.appendOptional("?" + PROPERTY + " <" + Rdf.RELATION_TYPE.toString() + "> ?" + PROPERTY_TYPE);
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Get the properties of a given scientific object uri.
     * @param uri
     * @return the list of properties
     */
    public ArrayList<Property> findScientificObjectProperties(String uri) {
        SPARQLQueryBuilder queryProperties = prepareSearchScientificObjectProperties(uri);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryProperties.toString());
        List<String> foundedProperties = new ArrayList<>();
        ArrayList<Property> properties = new ArrayList<>();
            
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    
                    if (!foundedProperties.contains(bindingSet.getValue(PROPERTY).stringValue())) {
                        Property property = new Property();
                    
                        property.setRelation(bindingSet.getValue(RELATION).stringValue());
                        property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                        if (bindingSet.getValue(PROPERTY_TYPE) != null) {
                            property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                        }

                        properties.add(property);
                        foundedProperties.add(bindingSet.getValue(PROPERTY).stringValue());
                    };
                }
            }
        return properties;
    }
    
    /**
     * Find scientific objects by the given list of search params
     * @param page
     * @param uri
     * @param pageSize
     * @param rdfType
     * @param experiment
     * @param alias
     * @return scientific objects list, result of the user query, empty if no result
     */
    public ArrayList<ScientificObject> find(Integer page, Integer pageSize, String uri, String rdfType, String experiment, String alias) {
        try {
            SPARQLQueryBuilder sparqlQuery = prepareSearchQuery(false, page, pageSize, uri, rdfType, experiment, alias);
            //SILEX:test
            //Pour les soucis de pool de connexion
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            Map<String, ScientificObject> foundedScientificObjects = new HashMap<>();
            
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    boolean alreadyFoundedUri = false;
                    
                    String actualUri = bindingSet.getValue(URI).stringValue();
                    
                    if (foundedScientificObjects.containsKey(actualUri)) {
                        alreadyFoundedUri = true;
                    }
                    
                    ScientificObject scientificObject = null;
                                        
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
                        
                        scientificObject.setLabel(bindingSet.getValue(ALIAS).stringValue());
                        
                        if (rdfType != null) {
                            scientificObject.setRdfType(rdfType);
                        } else {
                            scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                        }
                    }
                    
                    //Get scientific object properties
                    scientificObject.setProperties(findScientificObjectProperties(actualUri));
                    
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
            
            return scientificObjects;
        }   catch (SQLException ex) {
            java.util.logging.Logger.getLogger(ScientificObjectRdf4jDAO.class.getName()).log(Level.SEVERE, null, ex);
            
            if (getConnection() != null) {
                getConnection().close();
            }
            
            return null;
        }
    }

    /**
     * Generates a query to search scientific objects by the given search params.
     * @param page
     * @param pageSize
     * @param uri
     * @param rdfType
     * @param experiment
     * @param alias
     * @param count true if the query will be used to count number of scientific objects corresponding to the search result. False if not.
     * @example 
     * SELECT DISTINCT  ?uri ?alias ?experiment  ?rdfType 
     * WHERE {
     *      OPTIONAL {
     *          ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?alias . 
     *      }
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *      ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#ScientificObject> . 
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#participatesIn> ?experiment .  
     *      } 
     * }
     * @return the generated query
     */
    protected SPARQLQueryBuilder prepareSearchQuery(boolean count, Integer page, Integer pageSize, String uri, String rdfType, String experiment, String alias) {    
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        
        sparqlQuery.appendDistinct(true);
                
        //URI filter
        sparqlQuery.appendSelect("?" + URI);
        if (uri != null) {
            sparqlQuery.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
        }

        //Label filter
        sparqlQuery.appendSelect("?" + ALIAS);
        if (alias == null && !count) {
            sparqlQuery.beginBodyOptional();
            sparqlQuery.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + ALIAS + " . ");
            sparqlQuery.endBodyOptional();
        } else if (!count) {
            sparqlQuery.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + ALIAS, null);
            sparqlQuery.appendAndFilter("REGEX ( str(?" + ALIAS + "),\".*" + alias + ".*\",\"i\")");
        }
        
        //Experiment filter
        if (experiment != null) {
              sparqlQuery.appendFrom("<" + Contexts.VOCABULARY.toString() + "> \n FROM <" + experiment + ">");
        } else if (!count) {
            sparqlQuery.appendSelect("?" + EXPERIMENT);
            sparqlQuery.appendOptional("?" + URI + " <" + Oeso.RELATION_PARTICIPATES_IN.toString() + "> " + "?" + EXPERIMENT + " . ");
        }
        
        //Rdf type filter
        if (rdfType != null) {
            sparqlQuery.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), rdfType, null);
        } else {
            sparqlQuery.appendSelect(" ?" + RDF_TYPE);
            sparqlQuery.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
            sparqlQuery.appendTriplet(
                    "?" + RDF_TYPE, 
                    "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", 
                    Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString(), null);
        }
        
        if (page != null && pageSize != null) {
            sparqlQuery.appendLimit(pageSize);
            sparqlQuery.appendOffset(page * pageSize);
        }
        
        LOGGER.debug(SPARQL_QUERY + sparqlQuery.toString());
        
        return sparqlQuery;
    }
    
    /**
     * checks if the scientific object exists
     * @param uri
     * @return 0 if the object uri doesn't exist, 1 if it does exist
     */
    public boolean existScientificObject(String uri) {        
        SPARQLQueryBuilder query = askExistScientificObject(uri);
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        boolean result = booleanQuery.evaluate();    
        return result;
    }

    /**
     * generates the sparql ask query to know if the scientific object uri exists
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
    
    /**
     * Sort the given scientific objects per year.
     * @param scientificObjects the scientific objects to sort by year.
     * @return the list of the scientific objects sorted per year.
     */
    private HashMap<Integer, ArrayList<ScientificObject>> sortByYear(List<ScientificObject> scientificObjects) {
        HashMap<Integer, ArrayList<ScientificObject>> sortedScientificObjects = new HashMap<>();
        
        final Iterator<ScientificObject> iteratorScientificObjects = scientificObjects.iterator();
        while (iteratorScientificObjects.hasNext()) {
            ScientificObject scientificObject = iteratorScientificObjects.next();
            Integer year = Calendar.getInstance().get(Calendar.YEAR);
            //If the scientific object has no year, it is the current year.
            if (scientificObject.getYear() != null) {
                year = Integer.getInteger(scientificObject.getYear());
            }
            ArrayList<ScientificObject> newScientificObjectList;
            //If no scientific object for the year have already been founded, add a new year in the map.
            if (!sortedScientificObjects.containsKey(year)) {
                newScientificObjectList = new ArrayList<>();
            } else {
                newScientificObjectList = sortedScientificObjects.get(year);
            }
            //Add the scientific object in the map for its year.
            newScientificObjectList.add(scientificObject);
            sortedScientificObjects.put(year, newScientificObjectList);
        }
        
        return sortedScientificObjects;
    }
    
    /**
     * Generates uris for the given scientific objects, per year.
     * @param scientificObjecstSortedByYear
     * @return the list of the scientific objects with their uris.
     * SILEX:warning
     * There are risks of collision due to insertion time for the generated URIs.
     * \SILEX:warning
     */
    private ArrayList<ScientificObject> generateUrisByYear(HashMap<Integer, ArrayList<ScientificObject>> scientificObjecstSortedByYear) {
        ArrayList<ScientificObject> scientificObjects = new ArrayList<>();
        for (Entry<Integer, ArrayList<ScientificObject>> entry : scientificObjecstSortedByYear.entrySet()) {
            List<String> scientificObjectUris = UriGenerator.generateScientificObjectUris(Integer.toString(entry.getKey()), entry.getValue().size());
            
            int numSo = 0;
            for (ScientificObject scientificObject : entry.getValue()) {
                scientificObject.setUri(scientificObjectUris.get(numSo));
                scientificObjects.add(scientificObject);
                numSo++;
            }
        }
        
        return scientificObjects;
    }
    
    @Override
    public List create(List<ScientificObject> scientificObjects) throws Exception {
        
        boolean resultState = false; // To know if the data are ok and have been inserted.
        boolean annotationInsert = true; // True if the insertion have been done.

        //1. Generate Uris For all the scientific objects
        //SILEX:warning
        //See the SILEX:warning about collisions of generateUrisByYear(scientificObjecstSortedByYear).
        //\SILEX:warning
        ArrayList<ScientificObject> scientificObjectsReadyToInsert = generateUrisByYear(sortByYear(scientificObjects));
        
        final Iterator<ScientificObject> iteratorScientificObjects = scientificObjectsReadyToInsert.iterator();
        
        //2. Register in triplestore
        UpdateBuilder spql = new UpdateBuilder();
        
        while (iteratorScientificObjects.hasNext() && annotationInsert) {
            ScientificObject scientificObject = iteratorScientificObjects.next();
            
            Resource scientificObjectUri = ResourceFactory.createResource(scientificObject.getUri());
            Node scientificObjectType = NodeFactory.createURI(scientificObject.getRdfType());
            
            Node graph = null;
            if (scientificObject.getUriExperiment() != null) {
                graph = NodeFactory.createURI(scientificObject.getUriExperiment());
                
                // Add participates in (scientific object participates in experiment)
                Node participatesIn = NodeFactory.createURI(Oeso.RELATION_PARTICIPATES_IN.toString());
                spql.addInsert(graph, scientificObjectUri, participatesIn, graph);
            } else {
                graph = NodeFactory.createURI(Contexts.SCIENTIFIC_OBJECTS.toString());
            }
            
            spql.addInsert(graph, scientificObjectUri, RDF.type, scientificObjectType);
            
            // Properties associated to the scientific object
            for (Property property : scientificObject.getProperties()) {
                if (property.getRdfType() != null && !property.getRdfType().equals("")) {//Typed properties
                    if (property.getRdfType().equals(Oeso.CONCEPT_VARIETY.toString())) {
                        
                        String propertyURI;
                        try {
                            propertyURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIETY.toString(), null, property.getValue());
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
            
            //isPartOf : the object which has part the element must not be a plot    
            if (scientificObject.getIsPartOf()!= null) {
                Node agronomicalObjectPartOf = NodeFactory.createURI(scientificObject.getIsPartOf());
                org.apache.jena.rdf.model.Property relationIsPartOf = ResourceFactory.createProperty(Oeso.RELATION_IS_PART_OF.toString());
                
                spql.addInsert(graph, scientificObjectUri, relationIsPartOf, agronomicalObjectPartOf);  
            }
        }
        
        this.getConnection().begin();
        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spql.buildRequest().toString());
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + prepareUpdate.toString());
        prepareUpdate.execute();

        if (annotationInsert) {
            resultState = true;
            this.getConnection().commit();
            
            //3. insert in postgresql
            ScientificObjectSQLDAO scientificObjectDAO = new ScientificObjectSQLDAO();
            scientificObjectDAO.checkAndInsertListAO(scientificObjectsReadyToInsert);
        } else {
            // Rollback on the transaction.
            this.getConnection().rollback();
        }
        
        if (resultState) {
            return scientificObjectsReadyToInsert;
        } else {
            return new ArrayList<>();
        }
    }
    
    /**
     * Check the given scientific object and context and update the scientific object in the given context.
     * @param scientificObject
     * @param context
     * @return the update result.
     * @throws Exception 
     */
    public POSTResultsReturn checkAndUpdateInContext(ScientificObject scientificObject, String context) throws Exception {
        POSTResultsReturn updateResult;
        //1. Check the new data for the scientific object
        ArrayList<ScientificObject> scientificObjects = new ArrayList<>();
        scientificObjects.add(scientificObject);
        updateResult = check(scientificObjects);
        if (updateResult.statusList.isEmpty()) { // No error founded, we can update the scientific object.
            //2. Update the scientific object.
            try {
                ScientificObject updatedScientificObject = updateOneInContext(scientificObject, context);

                ArrayList<String> updatedScientificObjectsUris = new ArrayList<>();
                updatedScientificObjectsUris.add(updatedScientificObject.getUri());

                updateResult = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
                updateResult.statusList.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, updatedScientificObjectsUris.size() + " updated resource(s)."));
                updateResult.createdResources = updatedScientificObjectsUris; 
            } catch (Exception ex) {
                updateResult = new POSTResultsReturn(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
                String errorMessage = "";
                for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
                    errorMessage += stackTraceElement.toString() + " ";
                }
                updateResult.statusList.add(new Status(ex.getClass().toString(), StatusCodeMsg.ERR, errorMessage));
            }
        }
        return updateResult;
    }

    @Override
    public void delete(List<ScientificObject> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Get a scientific object by its URI in a given context.
     * @param uri
     * @param context
     * @return the scientific object if it exist,
     *         null if this scientific object does not exist.
     */
    public ScientificObject getScientificObjectInContext(String uri, String context) {
        ArrayList<ScientificObject> scientificObjects = find(null, null, uri, null, context, null);
        if (!scientificObjects.isEmpty()) {
            return scientificObjects.get(0);
        } else {
            return null;
        }
    }
    
    /**
     * Delete the given scientific object's data in the given context.
     * @param scientificObject
     * @param context
     * @example
     * DELETE DATA {
     *      GRAPH <http://www.opensilex.org/opensilex/DMO2018-1> {
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#Plot> .
     *          <http://www.opensilex.org/vocabulary/oeso#Plot> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.w3.org/2000/01/rdf-schema#label> "LA23" .
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.opensilex.org/vocabulary/oeso#isPartOf> <http://www.opensilex.org/opensilex/2019/o19000102> .
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.opensilex.org/vocabulary/oeso#participatesIn> <http://www.opensilex.org/opensilex/DMO2018-1> .
     *      }
     *  }
     * @return the generated query
     */
    private UpdateRequest prepareDeleteOneInContextQuery(ScientificObject scientificObject, String context) {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(context);
        Resource scientificObjectUri = ResourceFactory.createResource(scientificObject.getUri());
        
        for (Property property : scientificObject.getProperties()) {
            if (property.getValue() != null) {
                org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                
                if (property.getRdfType() != null) { //If there is a Rdf Type, it is an URI
                    Node propertyValue = NodeFactory.createURI(property.getValue());
                    Node propertyRdfType = NodeFactory.createURI(property.getRdfType());
                    updateBuilder.addDelete(graph, scientificObjectUri, propertyRelation, propertyValue);
                    updateBuilder.addDelete(graph, propertyValue, RDF.type, propertyRdfType);
                } else  {
                    boolean propertyIsUrl = true;
                    try { 
                        new URL(property.getValue()).toURI();
                    } catch (MalformedURLException | URISyntaxException e) { 
                        propertyIsUrl = false; 
                    } 
                    if (propertyIsUrl && existUri(property.getValue())) { //If there is no Rdf type but the value is an existing URI in the triplestore, it is an object URI.
                        Node propertyValue = NodeFactory.createURI(property.getValue());
                        updateBuilder.addDelete(graph, scientificObjectUri, propertyRelation, propertyValue);
                    } else { //The value is a literal
                        Literal propertyValue = ResourceFactory.createStringLiteral(property.getValue());
                        updateBuilder.addDelete(graph, scientificObjectUri, propertyRelation, propertyValue);
                    }
                }
            }
        }
        
        UpdateRequest request = updateBuilder.buildRequest();
        LOGGER.debug(request.toString());
        
        return request;
    }
    
    /**
     * Generates the query to create a scientific object in the given context.
     * @param scientificObject
     * @param context
     * @example 
     * INSERT DATA {
     *      GRAPH <http://www.opensilex.org/opensilex/DMO2018-1> {
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#Plot> .
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.opensilex.org/vocabulary/oeso#participatesIn> <http://www.opensilex.org/opensilex/DMO2018-1> .
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.opensilex.org/vocabulary/oeso#isPartOf> <http://www.opensilex.org/opensilex/2019/o19000102> .
     *          <http://www.opensilex.org/opensilex/2019/o19000106> <http://www.w3.org/2000/01/rdf-schema#label> "LA23" .
     *      }
     * }
     * @return the generated query
     */
    private UpdateRequest prepareInsertOneInContextQuery(ScientificObject scientificObject, String context) {
        UpdateBuilder insertBuilder = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(context);
        Resource scientificObjectURI = ResourceFactory.createResource(scientificObject.getUri());
        
        //rdf type
        Node rdfType = NodeFactory.createURI(scientificObject.getRdfType());
        insertBuilder.addInsert(graph, scientificObjectURI, RDF.type, rdfType);
        
        //participates in (in the case where it is a new participates in an experiment)
        Node participatesIn = NodeFactory.createURI(Oeso.RELATION_PARTICIPATES_IN.toString());
        insertBuilder.addInsert(graph, scientificObjectURI, participatesIn, graph);
        
        //is part of
        if (scientificObject.getIsPartOf() != null) {
            Node isPartOf = NodeFactory.createURI(Oeso.RELATION_IS_PART_OF.toString());
            Resource scientificObjectPartOf = ResourceFactory.createResource(scientificObject.getIsPartOf());
            insertBuilder.addInsert(graph, scientificObjectURI, isPartOf, scientificObjectPartOf);
        }
        
        //label
        Literal label = ResourceFactory.createStringLiteral(scientificObject.getLabel());
        insertBuilder.addInsert(graph, scientificObjectURI, RDFS.label, label);
        
        //properties
        for (Property property : scientificObject.getProperties()) {
            if (property.getValue() != null) {
                org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                
                if (property.getRdfType() != null) {
                    Node propertyValue = NodeFactory.createURI(property.getValue());
                    insertBuilder.addInsert(graph, scientificObjectURI, propertyRelation, propertyValue);
                    insertBuilder.addInsert(graph, propertyValue, RDF.type, property.getRdfType());
                } else {
                    Literal propertyValue = ResourceFactory.createStringLiteral(property.getValue());
                    insertBuilder.addInsert(graph, scientificObjectURI, propertyRelation, propertyValue);
                }
            }
        }
        UpdateRequest query = insertBuilder.buildRequest();
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    /**
     * Update the metadata about the scientific object.
     * The rdfType and geometry are global metadata and are updated for each context.
     * The others metadata of the scientific object are updated for the given context.
     * /!\ prerequisite: data must have been checked before. 
     * @throws java.lang.Exception
     * @see ScientificObjectDAOSesame#check(java.util.List)
     * @param scientificObject
     * @param context
     * @return the updated scientific object
     */
    public ScientificObject updateOneInContext(ScientificObject scientificObject, String context) throws Exception {
        //1. Get the old scientific object data in the given experiment
        ScientificObject oldScientificObject = getScientificObjectInContext(scientificObject.getUri(), context);
        
        //2. Update the scientific object
        //2.1. Triplestore data
        //2.1.1 Delete old data if the scientific object is in the context
        UpdateRequest deleteQuery = null;
        if (existUriInGraph(scientificObject.getUri(), context)) {
            deleteQuery = prepareDeleteOneInContextQuery(oldScientificObject, context);
        }
        
        //2.1.2 Insert new data
        //2.1.2a Generate variety URI if needed
        for (Property property : scientificObject.getProperties()) {
            if (property.getRdfType() != null && property.getRdfType().equals(Oeso.CONCEPT_VARIETY.toString())) {
                property.setValue(UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIETY.toString(), null, property.getValue()));
            }
        }
        //2.1.2b Insert data
        UpdateRequest insertQuery = prepareInsertOneInContextQuery(scientificObject, context);
        getConnection().begin();
        try {
            if (deleteQuery != null) {
                Update prepareDelete = getConnection().prepareUpdate(deleteQuery.toString());
                prepareDelete.execute();
            }
            
            Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
            prepareUpdate.execute();
            
            //2.2 Relational database data
            ScientificObjectSQLDAO scientificObjectDAO = new ScientificObjectSQLDAO();
            ScientificObject scientificObjectToSearchInDB = new ScientificObject();
            scientificObjectToSearchInDB.setGeometry(scientificObject.getGeometry());
            scientificObjectToSearchInDB.setUri(scientificObject.getUri());
            //2.2.1 Check if it exist in the relational database
            if (scientificObjectDAO.existInDB(scientificObjectToSearchInDB)) {
                //2.2.1a Update old data
                scientificObjectDAO.updateOneGeometry(scientificObject.getUri(), scientificObject.getGeometry(), scientificObject.getRdfType(), scientificObject.getExperiment());
            } else {
                //2.2.1b Add new entry in database
                ArrayList<ScientificObject> scientificObjects = new ArrayList<>();
                scientificObjects.add(scientificObject);
                scientificObjectDAO.checkAndInsertListAO(scientificObjects);
            }
            
            this.getConnection().commit();
        } catch (MalformedQueryException e) { //an error occurred, rollback
            this.getConnection().rollback();
            throw new MalformedQueryException(e.getMessage());
        } catch (SQLException e) { //an error occurred, rollback
            this.getConnection().rollback();
            throw new SQLException(e.getMessage());
        }
        
        return scientificObject;  
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
    public void validate(List<ScientificObject> objects) throws DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Generates the query to count the number of scientific objects corresponding to the search params given.
     * @param uri
     * @param rdfType
     * @param experimentURI
     * @param alias
     * @return The generated query
     * @example
     * SELECT DISTINCT  (COUNT(DISTINCT ?uri) AS ?count) 
     * WHERE {
     *      OPTIONAL {
     *          ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?alias . 
     *      }
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *      ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#ScientificObject> . 
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#participatesIn> ?experiment .  
     *      } 
     * }
     */
    private SPARQLQueryBuilder prepareCount(String uri, String rdfType, String experimentURI, String alias) {
        SPARQLQueryBuilder query = prepareSearchQuery(true, null, null, uri, rdfType, experimentURI, alias);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Count the number of scientific objects by the given search parameters.
     * @param uri
     * @param rdfType
     * @param experimentURI
     * @param alias
     * @return The number of scientific objects.
     */
    public Integer count(String uri, String rdfType, String experimentURI, String alias) {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, rdfType, experimentURI, alias);
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
}
