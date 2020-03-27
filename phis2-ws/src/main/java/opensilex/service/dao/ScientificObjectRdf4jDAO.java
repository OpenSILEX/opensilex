///******************************************************************************
//                       ScientificObjectRdf4jDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Aug. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.germplasm.dal.GermplasmModel;
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
import org.eclipse.rdf4j.model.Value;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

/**
 * Allows CRUD methods of scientific objects in the triplestore.
 *
 * @update [Morgane Vidal] 29 March, 2019: add update scientific objects and
 * refactor to the new DAO conception.
 * @update [Renaud COLIN] 20 September, 2019: update
 * create(List<ScientificObject> scientificObjects) method to not create RDF
 * triple with oeso:isPartOf as property and a literal as object.
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
    private final String GERMPLASM = "germplasm";

    private static final String MAX_ID = "maxID";

    public ScientificObjectRdf4jDAO(SPARQLService sparql) {
        super(sparql);
    }

    /**
     * Generates a query to get the last scientific object uri from the given
     * year
     *
     * @param year
     * @return The query to get the uri of the last inserted scientific object
     * for the given year.
     * @example      <pre>
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
        Expr yearFilter = expr.regex(expr.str(uri), ".*/" + year + "/.*", "");
        query.addFilter(yearFilter);

        // Binding to extract the last part of the URI as a MAX_ID integer
        Expr indexBinding = expr.function(
                XSD.integer.getURI(),
                ExprList.create(Arrays.asList(
                        expr.strafter(expr.str(uri), UriGenerator.getScientificObjectUriPatternByYear(year)))
                )
        );
        query.addBind(indexBinding, maxID);

        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID, Query.ORDER_DESCENDING));
        query.setLimit(1);

        return query.build();
    }

    /**
     * Get the last scientific object id for the given year.
     *
     * @param year The year of the wanted scientific object number.
     * @return The ID of the last scientific object inserted in the triplestore
     * for the given year.
     */
    public int getLastScientificObjectIdFromYear(String year) {
        Query lastScientificObjectUriFromYearQuery = prepareGetLastScientificObjectUriFromYear(year);

        //Get the URI of the last scientific object inserted during the given year.
        TupleQuery tupleQuery = prepareRDF4JTupleQuery(lastScientificObjectUriFromYearQuery);
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value maxId = bindingSet.getValue(MAX_ID);
                if (maxId != null) {
                    return Integer.valueOf(maxId.stringValue());
                }
            }
        }
        
        return 0;
    }

    /**
     * Generates the SPARQL ask query to know if a given alias already exists in
     * a given context
     *
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
     *
     * @param scientificObjects
     * @return
     * @throws RepositoryException
     */
    public POSTResultsReturn check(List<ScientificObject> scientificObjects) throws RepositoryException, URISyntaxException, Exception {
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
            UriDAO uriDao = new UriDAO(sparql);

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
                String isPartOfUri = scientificObject.getIsPartOf();
                //the part of uri is not in the cache
                if (!isPartOfCache.contains(isPartOfUri)) {
                    //the part of uri doesn't exist
                    if (!existUri(isPartOfUri)) {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "unknown is part of uri"));
                    } else {
                        //1. Get isPartOf object type
                        uriDao.uri = isPartOfUri;
                        ArrayList<Uri> typesResult = uriDao.getAskTypeAnswer();
                        if (!uriDao.isSubClassOf(typesResult.get(0).getRdfType(), Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString())) {
                            dataOk = false;
                            checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "is part of object type is not scientific object"));
                        } else {
                            isPartOfCache.add(uriDao.uri);
                        }
                    }
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
                        BooleanQuery booleanQuery = prepareRDF4JBooleanQuery(query);
                        boolean result = booleanQuery.evaluate();

                        if (result) {
                            dataOk = false;
                            checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "already existing alias for the given experiment"));
                        }
                    }
                }
                
                //Check if the given germplasm exists
                if (property.getRelation().equals(Oeso.RELATION_HAS_GERMPLASM.toString())) {
                    if (property.getRdfType() != null) {
                        opensilex.service.germplasm.dal.GermplasmDAO germplasmDAO = new opensilex.service.germplasm.dal.GermplasmDAO(sparql);
                        GermplasmModel germplasm = germplasmDAO.get(new URI(property.getValue()));
                        if (germplasm != null) {
                            if (!germplasm.getType().toString().equals(property.getRdfType())) {
                                dataOk = false;
                                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "The given germplasm doesn't correspond to the given rdfType"));
                            }
                        } else {
                            dataOk = false;
                            checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "The given germplasm doesn't exist"));
                        }
                    } else {
                            dataOk = false;
                            checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "The property hasGermplasm requires to give rdfType"));
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
     *
     * @param scientificObjects
     * @return
     */
    public POSTResultsReturn checkAndInsert(List<ScientificObject> scientificObjects) throws URISyntaxException, Exception {
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
     * Generates the query to get the list of scientific objects which
     * participates in a given experiment.
     *
     * @param experimentURI
     * @return the query enabling to retrieve the list of scientific objects
     * participating in the experiment and the objects propertiesT SILEX:TODO
     * filter on the rdfs.type --> scientific object \SILEX:TODO
     */
    private SPARQLQueryBuilder prepareSearchExperimentScientificObjects(String experimentURI) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(experimentURI);
        sparqlQuery.appendSelect("?" + CHILD + " ?" + RDF_TYPE + " ?" + PROPERTY + " ?" + PROPERTY_RELATION + " ?" + PROPERTY_TYPE);

        sparqlQuery.appendTriplet("?" + CHILD, Oeso.RELATION_PARTICIPATES_IN.toString(), experimentURI, null);
        sparqlQuery.appendTriplet("?" + CHILD, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        sparqlQuery.appendTriplet("?" + CHILD, "?" + PROPERTY_RELATION, "?" + PROPERTY, null);

        sparqlQuery.beginBodyOptional();
        sparqlQuery.appendToBody("?" + PROPERTY + " <" + Rdf.RELATION_TYPE.toString() + "> ?" + PROPERTY_TYPE);
        sparqlQuery.endBodyOptional();

        LOGGER.debug(SPARQL_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }

    /**
     * Generates a query to get all the scientific objects contained in a given
     * scientific object (geo:contains).
     *
     * @param objectURI
     * @example SELECT DISTINCT ?child ?rdfType WHERE {
     * <http://www.opensilex.org/opensilex/2019/o19000030>
     * <http://www.opengis.net/ont/geosparql#contains*> ?child . ?child
     * <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?rdfType . }
     * @return the query enabling to select all contained element (geo:contains)
     * in objectURI. the query enabling to select all descendants
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
     * Generates the query to get the first scientific objects contained by a
     * given scientific object (geo:contains).
     *
     * @param objectURI
     * @return the first descendants of the element (geo:contains relation)
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
     * @param layerDTO
     * @return children of layerDTO.getObjectURI. Returns the descendants if
     * layerDTO.getDepth == true. (key: URI, value: type)
     */
    public HashMap<String, ScientificObject> searchChildren(LayerDTO layerDTO) {
        HashMap<String, ScientificObject> children = new HashMap<>(); // uri (clé), type (valeur)

        /* If it's an experiment, the link name isn't the same so we get the direct descendants first*/
        if (layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) {

            SPARQLQueryBuilder sparqlQuery = prepareSearchExperimentScientificObjects(layerDTO.getObjectUri());
            TupleQuery tupleQuery = prepareRDF4JTupleQuery(sparqlQuery);

            try (TupleQueryResult result = tupleQuery.evaluate()) {
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
                    } else { // not in the list, has to be added
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
        }
        //SILEX:INFO
        //For the moment, get only the first descendants properties, not the others
        // If all descendants needed
        if (ResourcesUtils.getStringBooleanValue(layerDTO.getDepth())) {
            // Particular treatment if descendants of a trial
            if (layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
                // Get all descendants of the plots of the previously retrieved experimentations
                for (Entry<String, ScientificObject> child : children.entrySet()) {

                    SPARQLQueryBuilder sparqlQuery = prepareSearchChildrenWithContains(
                            child.getKey(),
                            child.getValue().getRdfType());
                    TupleQuery tupleQuery = prepareRDF4JTupleQuery(sparqlQuery);
                    
                    try (TupleQueryResult result = tupleQuery.evaluate()) {
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
                }
            } else { // if standard object
                SPARQLQueryBuilder sparqlQuery
                        = prepareSearchChildrenWithContains(layerDTO.getObjectUri(), layerDTO.getObjectType());
                TupleQuery tupleQuery = prepareRDF4JTupleQuery(sparqlQuery);
                
                try (TupleQueryResult result = tupleQuery.evaluate()) {
                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        ScientificObject scientificObject = new ScientificObject();
                        scientificObject.setUri(bindingSet.getValue(CHILD).stringValue());
                        scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());

                        children.put(bindingSet.getValue(CHILD).stringValue(), scientificObject);
                    }
                }
            }

        } else if (!layerDTO.getObjectType().equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
            // If only direct descendants needed and not an experimentation
            SPARQLQueryBuilder sparqlQuery = prepareSearchFirstChildrenWithContains(layerDTO.getObjectUri());
            TupleQuery tupleQuery = prepareRDF4JTupleQuery(sparqlQuery);
            
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    ScientificObject scientificObject = new ScientificObject();
                    scientificObject.setUri(bindingSet.getValue(CHILD).stringValue());
                    scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());

                    children.put(bindingSet.getValue(CHILD).stringValue(), scientificObject);
                }
            }
        }
        //\SILEX:INFO

        return children;
    }

    /**
     * Generates the query to get the list of properties for a given scientific
     * object
     *
     * @param uri
     * @return
     * @example SELECT ?relation ?property ?propertyType WHERE {
     * <http://www.opensilex.org/opensilex/2019/o19000115> ?relation ?property .
     * OPTIONAL {?property <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
     * ?propertyType } }
     */
    public SPARQLQueryBuilder prepareSearchScientificObjectProperties(String uri, String experiment) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();

        query.appendGraph(experiment);
        query.appendSelect(" ?" + RELATION + " ?" + PROPERTY + " ?" + PROPERTY_TYPE);
        query.appendTriplet(uri, "?" + RELATION, "?" + PROPERTY, null);

        query.appendOptional("?" + PROPERTY + " <" + Rdf.RELATION_TYPE.toString() + "> ?" + PROPERTY_TYPE);

        LOGGER.debug(query.toString());

        return query;
    }

    /**
     * Get the properties of a given scientific object uri.
     *
     * @param uri
     * @return the list of properties
     */
    public ArrayList<Property> findScientificObjectProperties(String uri, String experiment) {
        SPARQLQueryBuilder queryProperties = prepareSearchScientificObjectProperties(uri, experiment);
        TupleQuery tupleQuery = prepareRDF4JTupleQuery(queryProperties);
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
        } catch (Exception ex) {
            LOGGER.error("Error while getting scientific object properties", ex);
        }
        return properties;
    }

    /**
     * Find scientific objects by the given list of search params
     *
     * @param page
     * @param uri
     * @param pageSize
     * @param rdfType
     * @param experiment
     * @param alias
     * @return scientific objects list, result of the user query, empty if no
     * result
     */
    public ArrayList<ScientificObject> find(
            Integer page, 
            Integer pageSize, 
            String uri, 
            String rdfType, 
            String experiment, 
            String alias, 
            Boolean withProperties, 
            String germplasmURI, 
            Boolean withAallRelatedGermplasm) throws Exception {
        SPARQLQueryBuilder sparqlQuery = prepareSearchQuery(false, page, pageSize, uri, rdfType, experiment, alias, germplasmURI, withAallRelatedGermplasm);

        TupleQuery tupleQuery = prepareRDF4JTupleQuery(sparqlQuery);
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
                        String expeURI = bindingSet.getValue(EXPERIMENT).stringValue();
                        scientificObject.setExperiment(SPARQLDeserializers.getExpandedURI(expeURI));
                    }

                    scientificObject.setLabel(bindingSet.getValue(ALIAS).stringValue());

                    if (rdfType != null) {
                        scientificObject.setRdfType(rdfType);
                    } else {
                        scientificObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                    }

                    foundedScientificObjects.put(actualUri, scientificObject);
                }
            }
        }

        if (withProperties) {
            foundedScientificObjects.forEach((soURI, so) -> {
                so.setProperties(findScientificObjectProperties(soURI, so.getUriExperiment()));
            });
        }

        return new ArrayList<>(foundedScientificObjects.values());
    }

    /**
     * Generates a query to search scientific objects by the given search
     * params.
     *
     * @param page
     * @param pageSize
     * @param uri
     * @param rdfType
     * @param experiment
     * @param alias
     * @param count true if the query will be used to count number of scientific
     * objects corresponding to the search result. False if not.
     * @example SELECT DISTINCT ?uri ?alias ?experiment ?rdfType WHERE {
     * OPTIONAL { ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?alias . }
     * ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?rdfType .
     * ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>
     *
     * <http://www.opensilex.org/vocabulary/oeso#ScientificObject> . OPTIONAL {
     * ?uri <http://www.opensilex.org/vocabulary/oeso#participatesIn>
     * ?experiment . } }
     * @return the generated query
     */
    protected SPARQLQueryBuilder prepareSearchQuery(
            boolean count, 
            Integer page, 
            Integer pageSize, 
            String uri, 
            String rdfType, 
            String experiment, 
            String alias, 
            String germplasmURI, 
            Boolean withAallRelatedGermplasm) throws URISyntaxException, Exception {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();

        sparqlQuery.appendDistinct(true);

        //URI filter
        sparqlQuery.appendSelect("?" + URI);
        if (uri != null) {
            sparqlQuery.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
        }

        //Rdf type filter
        if (rdfType != null) {
            sparqlQuery.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), rdfType, null);
        } else {
            sparqlQuery.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
            sparqlQuery.appendTriplet(
                    "?" + RDF_TYPE,
                    "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*",
                    Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString(), null);
            sparqlQuery.appendSelect(" ?" + RDF_TYPE);
        }

        //Label filter
        sparqlQuery.appendSelect("?" + ALIAS);
        if (alias == null && !count) {
            sparqlQuery.beginBodyOptional();
            sparqlQuery.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + ALIAS + " . ");
            sparqlQuery.endBodyOptional();
        } else if (alias != null) {
            sparqlQuery.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + ALIAS, null);
            sparqlQuery.appendAndFilter("REGEX ( str(?" + ALIAS + "),\".*" + alias + ".*\",\"i\")");
        }

        //Experiment filter
        if (experiment != null) {
            sparqlQuery.appendFrom("<" + Contexts.VOCABULARY.toString() + "> \n FROM <" + experiment + ">");
        } else {
            sparqlQuery.appendSelect("?" + EXPERIMENT);
            sparqlQuery.appendOptional("?" + URI + " <" + Oeso.RELATION_PARTICIPATES_IN.toString() + "> " + "?" + EXPERIMENT + " . ");
        }
        
        //germplasm filter
        if (germplasmURI != null) {
            //filter also on the linked germplasm (if the germplasm is a species, then we find also varieties of this species)
            if (withAallRelatedGermplasm) {
                opensilex.service.germplasm.dal.GermplasmDAO germplasmDAO = new opensilex.service.germplasm.dal.GermplasmDAO(sparql);
                GermplasmModel germplasm = germplasmDAO.get(new URI(germplasmURI));
                URI germplasmType = germplasm.getType();
                ListWithPagination<GermplasmModel> germplasmList = null;
                if (germplasmType.equals(Oeso.CONCEPT_SPECIES)) {
                    germplasmList = germplasmDAO.search(null, null, null, new URI(germplasmURI), null, null, null, 0, 5000);
                } else if (germplasmType.equals(Oeso.CONCEPT_VARIETY)) {
                    germplasmList = germplasmDAO.search(null, null, null, null, new URI(germplasmURI), null, null, 0, 5000);
                } else if (germplasmType.equals(Oeso.CONCEPT_ACCESSION)) {
                    germplasmList = germplasmDAO.search(null, null, null, null, null, new URI(germplasmURI), null, 0, 5000);
                }
                
                String filter = "<" + germplasmURI + ">";
                if (!germplasmList.getList().isEmpty()) {
                    for (GermplasmModel germpl:germplasmList.getList()) {
                        String germURI = SPARQLDeserializers.getExpandedURI(germpl.getUri().toString());
                        filter = filter + ", <"+ germURI + ">";
                    } 
                }                 
                sparqlQuery.appendSelect("?" + GERMPLASM);
                sparqlQuery.appendTriplet("?" + URI, Oeso.RELATION_HAS_GERMPLASM.toString(), "?" + GERMPLASM, null);
                sparqlQuery.appendAndFilter("?" + GERMPLASM + " IN (" + filter + ")");
            
            //filter only on this specific germplasm
            } else {
                sparqlQuery.appendTriplet("?" + URI, Oeso.RELATION_HAS_GERMPLASM.toString(), "<" + germplasmURI + ">", null); 
            }
        }

        if (page != null && pageSize != null) {
            sparqlQuery.appendLimit(pageSize);
            sparqlQuery.appendOffset(page * pageSize);
        }
        
        LOGGER.debug(SPARQL_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }

    /**
     * Checks if the scientific object exists.
     *
     * @param uri
     * @return 0 if the object URI doesn't exist. 1 if it does exist.
     */
    public boolean existScientificObject(String uri) {
        SPARQLQueryBuilder query = askExistScientificObject(uri);
        BooleanQuery booleanQuery = prepareRDF4JBooleanQuery(query);
        boolean result = booleanQuery.evaluate();
        return result;
    }

    /**
     * Generates the SPARQL ASK query to know if the scientific object URI
     * exists.
     *
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
     *
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
     *
     * @param scientificObjecstSortedByYear
     * @return the list of the scientific objects with their uris. SILEX:warning
     * There are risks of collision due to insertion time for the generated
     * URIs. \SILEX:warning
     */
    private ArrayList<ScientificObject> generateUrisByYear(HashMap<Integer, ArrayList<ScientificObject>> scientificObjecstSortedByYear) throws Exception {
        ArrayList<ScientificObject> scientificObjects = new ArrayList<>();
        for (Entry<Integer, ArrayList<ScientificObject>> entry : scientificObjecstSortedByYear.entrySet()) {            
            List<String> scientificObjectUris = UriGenerator.generateScientificObjectUris(sparql, Integer.toString(entry.getKey()), entry.getValue().size());

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
    public List<ScientificObject> create(List<ScientificObject> scientificObjects) throws Exception {

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

                graph = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(scientificObject.getUriExperiment()));

                // Add participates in (scientific object participates in experiment)
                Node participatesIn = NodeFactory.createURI(Oeso.RELATION_PARTICIPATES_IN.toString());
                spql.addInsert(graph, scientificObjectUri, participatesIn, graph);
            } else {
                graph = NodeFactory.createURI(Contexts.SCIENTIFIC_OBJECTS.toString());
            }

            spql.addInsert(graph, scientificObjectUri, RDF.type, scientificObjectType);

            for (Property property : scientificObject.getProperties()) {
                if (property.getRdfType() != null && !property.getRdfType().equals("")) {//Typed properties
                    if (property.getRdfType().equals(Oeso.CONCEPT_VARIETY.toString())) {

                        String propertyURI;
                        try {
                            propertyURI = UriGenerator.generateNewInstanceUri(sparql, Oeso.CONCEPT_VARIETY.toString(), null, property.getValue());
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
                } else if (Oeso.RELATION_IS_PART_OF.toString().equals(property.getRelation())) {
                    continue; // Oeso:isPartOf relation will be handled just after
                } else {
                    Literal propertyLiteral = ResourceFactory.createStringLiteral(property.getValue());
                    org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());

                    spql.addInsert(graph, scientificObjectUri, propertyRelation, propertyLiteral);
                }
            }

            //isPartOf : the object which has part the element must not be a plot    
            if (scientificObject.getIsPartOf() != null) {
                Node agronomicalObjectPartOf = NodeFactory.createURI(scientificObject.getIsPartOf());
                org.apache.jena.rdf.model.Property relationIsPartOf = ResourceFactory.createProperty(Oeso.RELATION_IS_PART_OF.toString());

                spql.addInsert(graph, scientificObjectUri, relationIsPartOf, agronomicalObjectPartOf);
            }
        }

        Update prepareUpdate = prepareRDF4JUpdateQuery(spql.buildRequest());
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + prepareUpdate.toString());
        prepareUpdate.execute();

        if (annotationInsert) {
            resultState = true;

            //3. insert in postgresql
            ScientificObjectMongoDAO scientificObjectDAO = new ScientificObjectMongoDAO();
            scientificObjectDAO.checkAndInsertListAO(scientificObjectsReadyToInsert);
        }

        if (resultState) {
            return scientificObjectsReadyToInsert;
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Check the given scientific object and context and update the scientific
     * object in the given context.
     *
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
    public void delete(List<ScientificObject> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Get a scientific object by its URI in a given context.
     *
     * @param uri
     * @param context
     * @return the scientific object if it exist, null if this scientific object
     * does not exist.
     */
    public ScientificObject getScientificObjectInContext(String uri, String context) throws Exception {
        ArrayList<ScientificObject> scientificObjects = find(null, null, uri, null, context, null, true, null, false);
        if (!scientificObjects.isEmpty()) {
            return scientificObjects.get(0);
        } else {
            return null;
        }
    }

    /**
     * Delete the given scientific object's data in the given context.
     *
     * @param scientificObject
     * @param context
     * @example DELETE DATA { GRAPH
     * <http://www.opensilex.org/opensilex/DMO2018-1> {
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
     * <http://www.opensilex.org/vocabulary/oeso#Plot> .
     * <http://www.opensilex.org/vocabulary/oeso#Plot>
     * <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
     * <http://www.w3.org/2002/07/owl#Class> .
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.w3.org/2000/01/rdf-schema#label> "LA23" .
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.opensilex.org/vocabulary/oeso#isPartOf>
     * <http://www.opensilex.org/opensilex/2019/o19000102> .
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.opensilex.org/vocabulary/oeso#participatesIn>
     * <http://www.opensilex.org/opensilex/DMO2018-1> . } }
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
                } else {
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
     *
     * @param scientificObject
     * @param context
     * @example INSERT DATA { GRAPH
     * <http://www.opensilex.org/opensilex/DMO2018-1> {
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
     * <http://www.opensilex.org/vocabulary/oeso#Plot> .
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.opensilex.org/vocabulary/oeso#participatesIn>
     * <http://www.opensilex.org/opensilex/DMO2018-1> .
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.opensilex.org/vocabulary/oeso#isPartOf>
     * <http://www.opensilex.org/opensilex/2019/o19000102> .
     * <http://www.opensilex.org/opensilex/2019/o19000106>
     * <http://www.w3.org/2000/01/rdf-schema#label> "LA23" . } }
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
     * Update the metadata about the scientific object. The rdfType and geometry
     * are global metadata and are updated for each context. The others metadata
     * of the scientific object are updated for the given context. /!\
     * prerequisite: data must have been checked before.
     *
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
                property.setValue(UriGenerator.generateNewInstanceUri(sparql, Oeso.CONCEPT_VARIETY.toString(), null, property.getValue()));
            }
        }
        //2.1.2b Insert data
        UpdateRequest insertQuery = prepareInsertOneInContextQuery(scientificObject, context);
        try {
            if (deleteQuery != null) {
                Update prepareDelete = prepareRDF4JUpdateQuery(deleteQuery);
                prepareDelete.execute();
            }

            Update prepareUpdate = prepareRDF4JUpdateQuery(insertQuery);
            prepareUpdate.execute();

            //2.2 Relational database data
            ScientificObjectMongoDAO scientificObjectDAO = new ScientificObjectMongoDAO();
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

        } catch (MalformedQueryException e) { //an error occurred, rollback
            throw new MalformedQueryException(e.getMessage());
        }

        return scientificObject;
    }

    @Override
    public List<ScientificObject> update(List<ScientificObject> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScientificObject find(ScientificObject object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScientificObject findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<ScientificObject> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Generates the query to count the number of scientific objects
     * corresponding to the search params given.
     *
     * @param uri
     * @param rdfType
     * @param experimentURI
     * @param alias
     * @return The generated query
     * @example SELECT DISTINCT (COUNT(DISTINCT ?uri) AS ?count) WHERE {
     * OPTIONAL { ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?alias . }
     * ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?rdfType .
     * ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>
     *
     * <http://www.opensilex.org/vocabulary/oeso#ScientificObject> . OPTIONAL {
     * ?uri <http://www.opensilex.org/vocabulary/oeso#participatesIn>
     * ?experiment . } }
     */
    private SPARQLQueryBuilder prepareCount(String uri, String rdfType, String experimentURI, String alias, String germplasmURI, Boolean withAllRelatedGermplasm) throws Exception {
        SPARQLQueryBuilder query = prepareSearchQuery(true, null, null, uri, rdfType, experimentURI, alias, germplasmURI, withAllRelatedGermplasm);
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
     *
     * @param uri
     * @param rdfType
     * @param experimentURI
     * @param alias
     * @return The number of scientific objects.
     */
    public Integer count(String uri, String rdfType, String experimentURI, String alias, String germplasmURI, Boolean withAallRelatedGermplasm) throws Exception {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, rdfType, experimentURI, alias, germplasmURI, withAallRelatedGermplasm);
        TupleQuery tupleQuery = prepareRDF4JTupleQuery(prepareCount);
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
