//**********************************************************************************************
//                               AgronomicalObjectDaoSesame.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 6 2017
// Subject: A specific DAO to retreive data on agronomical objects
//***********************************************************************************************

package phis2ws.service.dao.sesame;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
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
import phis2ws.service.dao.phis.AgronomicalObjectDAO;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.GeoSPARQL;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.resources.dto.AgronomicalObjectDTO;
import phis2ws.service.resources.dto.LayerDTO;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyPostDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.AgronomicalObject;
import phis2ws.service.view.model.phis.Property;
import phis2ws.service.view.model.phis.Uri;


public class AgronomicalObjectDAOSesame extends DAOSesame<AgronomicalObject> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(AgronomicalObjectDAOSesame.class);
    
    //The following attributes are used to search agronomical objects in the triplestore
    //uri of the agronomical object
    public String uri;
    
    //type of the agronomical object
    public String rdfType;
    
    //experiment of the agronomical object
    public String experiment;
    private final String EXPERIMENT = "experiment";
    
    //alias of the agronomical object
    public String alias;
    private final String ALIAS = "alias";
    
    private final String PROPERTY = "property";
    private final String PROPERTY_RELATION = "propertyRelation";
    private final String PROPERTY_TYPE = "propertyType";
    private final String CHILD = "child";
    private final String RELATION = "relation";
    
    private static final String URI_CODE_AGRONOMICAL_OBJECT = "o";

    public AgronomicalObjectDAOSesame() {
        super();
    }
    
    /**
     * generates a query to get the last agronomical object uri from the given year
     * @param year 
     * @return the query to get the uri of the last inserted agronomical object 
     *         for the given year
     * e.g
     * SELECT ?uri WHERE {
     *      ?uri  rdf:type  ?type  . 
     *      ?type  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#AgronomicalObject> . 
     *      FILTER ( regex(str(?uri), ".*\/2018/.*") ) 
     * }
     * ORDER BY desc(?uri) 
     * LIMIT 1
     */
    private SPARQLQueryBuilder prepareGetLastAgronomicalObjectUriFromYear(String year) {
        SPARQLQueryBuilder queryLastAgronomicalObjectURi = new SPARQLQueryBuilder();
        queryLastAgronomicalObjectURi.appendSelect("?" + URI);
        queryLastAgronomicalObjectURi.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        queryLastAgronomicalObjectURi.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_AGRONOMICAL_OBJECT.toString(), null);
        queryLastAgronomicalObjectURi.appendFilter("regex(str(?" + URI + "), \".*/" + year + "/.*\")");
        queryLastAgronomicalObjectURi.appendOrderBy("desc(?" + URI + ")");
        queryLastAgronomicalObjectURi.appendLimit(1);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + queryLastAgronomicalObjectURi.toString());
        return queryLastAgronomicalObjectURi;
    }
    
    /**
     * get the last agronomical object id for the given year. 
     * e.g : for http://www.phenome-fppn.fr/diaphen/
     * @param year the year of the wanted agronomical object number.
     * @return the id of the last agronomical object inserted in the triplestore for the given year
     */
    public int getLastAgronomicalObjectIdFromYear(String year) {
        SPARQLQueryBuilder lastAgronomicalObjectUriFromYearQuery = prepareGetLastAgronomicalObjectUriFromYear(year);
        
        this.getConnection().begin();

        //get last agronomicalObject uri inserted during the given year
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, lastAgronomicalObjectUriFromYearQuery.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        getConnection().commit();
        getConnection().close();
        
        String uriAgronomicalObject = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriAgronomicalObject = bindingSet.getValue(URI).stringValue();
        }
        
        if (uriAgronomicalObject == null) {
            return 0;
        } else {
            //2018 -> 18. to get /o18
            String split = "/" + URI_CODE_AGRONOMICAL_OBJECT + year.substring(2, 4);
            String[] parts = uriAgronomicalObject.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }
    
    /**
     * generates the sparql ask query to know if a given alias is already 
     * existing in a given context
     * @param alias
     * @param context
     * @return the query
     */
    private SPARQLQueryBuilder askExistAliasInContext(String alias, String context) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendGraph(context);
        query.appendAsk("");
        query.appendToBody("?x <" + Vocabulary.RELATION_HAS_ALIAS.toString() + "> \"" + alias + "\"");
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Vérifie si les agronomical objects sont corrects
     * @param agronomicalObjectsDTO
     * @return
     * @throws RepositoryException 
     */
    public POSTResultsReturn check(List<AgronomicalObjectDTO> agronomicalObjectsDTO) throws RepositoryException {
        //Résultats attendus
        POSTResultsReturn agronomicalObjectsCheck = null;
        //Liste des status retournés
        List<Status> checkStatusList = new ArrayList<>();
        
        boolean dataOk = true;
        for (AgronomicalObjectDTO agronomicalObject : agronomicalObjectsDTO) {
            //On vérifie que les types soient effectivement présents dans l'ontologie
             UriDaoSesame uriDao = new UriDaoSesame();

             if (!uriDao.isSubClassOf(agronomicalObject.getRdfType(), Vocabulary.CONCEPT_AGRONOMICAL_OBJECT.toString())) {
                 dataOk = false;
                 checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Wrong agronomical object type value. See ontology"));
             }
             //SILEX:TODO
             //Il faudra aussi faire une vérification sur les properties de l'ao : est-ce que les types sont
             //bien présents dans l'ontologie ? Idem pour les relations
             //\SILEX:TODO

             //check isPartOf
             if (agronomicalObject.getIsPartOf() != null) {
                 if (existUri(agronomicalObject.getIsPartOf())) {
                     //1. get isPartOf object type
                     uriDao.uri = agronomicalObject.getIsPartOf();
                     ArrayList<Uri> typesResult = uriDao.getAskTypeAnswer();
                     if (!uriDao.isSubClassOf(typesResult.get(0).getRdfType(), Vocabulary.CONCEPT_AGRONOMICAL_OBJECT.toString())) {
                         dataOk = false;
                         checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "is part of object type is not agronomical object"));
                     }
                 } else {
                     dataOk = false;
                     checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "unknown is part of uri"));
                 }
             }

             //check properties
             boolean missingAlias = true;
             for (PropertyPostDTO property : agronomicalObject.getProperties()) {
                 //check alias
                 if (property.getRelation().equals(Vocabulary.RELATION_HAS_ALIAS.toString())) {
                     missingAlias = false;
                     //check unique alias in the experiment
                     if (agronomicalObject.getExperiment() != null) {
                         SPARQLQueryBuilder query = askExistAliasInContext(property.getValue(), agronomicalObject.getExperiment());
                         BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
                         boolean result = booleanQuery.evaluate();

                         if (result) {
                             dataOk = false;
                             checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "already existing alias for the given experiment"));
                         }
                     }
                 }
             }

             if (missingAlias) {
                 dataOk = false;
                 checkStatusList.add(new Status(StatusCodeMsg.MISSING_FIELDS, StatusCodeMsg.ERR, "missing alias"));
             }
        }
        agronomicalObjectsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        agronomicalObjectsCheck.statusList = checkStatusList;
        return agronomicalObjectsCheck;        
    }
    
    /**
     * insère les données dans le triplestore. 
     * On suppose que la vérification de leur intégrité a été fait auparavent via l'appel à la méthode check()
     * @param agronomicalObjects
     * @return 
     */
    public POSTResultsReturn insert(List<AgronomicalObjectDTO> agronomicalObjects) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURIList = new ArrayList<>(); 
        
        POSTResultsReturn results;
        
        boolean resultState = false; //Pour savoir si les données sont bonnes et ont bien été insérées
        boolean annotationInsert = true; // Si l'insertion a bien été effectuée
        
        final Iterator<AgronomicalObjectDTO> iteratorAgronomicalObjects = agronomicalObjects.iterator();
        
        UriGenerator uriGenerator = new UriGenerator();
        
        while (iteratorAgronomicalObjects.hasNext() && annotationInsert) {
            AgronomicalObjectDTO agronomicalObjectDTO = iteratorAgronomicalObjects.next();
            AgronomicalObject agronomicalObject = agronomicalObjectDTO.createObjectFromDTO();
            
            //1. generates agronomical object uri
            agronomicalObject.setUri(uriGenerator.generateNewInstanceUri(agronomicalObject.getRdfType(), agronomicalObjectDTO.getYear(), null));
            
            //2. Register in triplestore
//            SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
            UpdateBuilder spql = new UpdateBuilder();
            
            Node graph = null;
            if (agronomicalObject.getUriExperiment() != null) {
                graph = NodeFactory.createURI(agronomicalObject.getUriExperiment() );
            } else {
                graph = NodeFactory.createURI(Contexts.AGONOMICAL_OBJECTS.toString());
            }
            
            Resource agronomicalObjectUri = ResourceFactory.createResource(agronomicalObject.getUri());
            Node agronomicalObjectType = NodeFactory.createURI(agronomicalObject.getRdfType());
            
            spql.addInsert(graph, agronomicalObjectUri, RDF.type, agronomicalObjectType);
            
            //Propriétés associées à l'AO
            for (Property property : agronomicalObject.getProperties()) {
                if (property.getRdfType() != null && !property.getRdfType().equals("")) {//Propriété typée
                    if (property.getRdfType().equals(Vocabulary.CONCEPT_VARIETY.toString())) {
                        //On génère l'uri de la variété
                        //SILEX:TODO
                        //move the uri generation in the UriGenerator (#45)
                        //\SILEX:TODO
                        String propertyURI = uriGenerator.generateNewInstanceUri(Vocabulary.CONCEPT_VARIETY.toString(), null, property.getValue());
                        Node propertyNode = NodeFactory.createURI(propertyURI);
                        Node propertyType = NodeFactory.createURI(property.getRdfType());
                        org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                        
                        spql.addInsert(graph, propertyNode, RDF.type, propertyType);
                        spql.addInsert(graph, agronomicalObjectUri, propertyRelation, propertyNode);
                    } else {
                        Node propertyNode = NodeFactory.createURI(property.getValue());
                        Node propertyType = NodeFactory.createURI(property.getRdfType());
                        org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
                        
                        spql.addInsert(graph, propertyNode, RDF.type, propertyType);
                        spql.addInsert(graph, agronomicalObjectUri, propertyRelation, propertyNode);
                    }
                } else {
                    Node propertyNode = NodeFactory.createURI(property.getValue());
                    org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());

                    spql.addInsert(graph, agronomicalObjectUri, propertyRelation, propertyNode);                    
                }
            }
            
            if (agronomicalObject.getUriExperiment() != null) {
                Node experimentUri = NodeFactory.createURI(agronomicalObject.getUriExperiment());
                org.apache.jena.rdf.model.Property relationHasPlot = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_PLOT.toString());
                
                spql.addInsert(graph, experimentUri, relationHasPlot, agronomicalObjectUri);  
            }
            
            //isPartOf : the object which has part the element must not be a plot    
            if (agronomicalObject.getIsPartOf()!= null) {
                Node agronomicalObjectPartOf = NodeFactory.createURI(agronomicalObject.getIsPartOf());
                org.apache.jena.rdf.model.Property relationIsPartOf = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_PLOT.toString());
                
                spql.addInsert(graph, agronomicalObjectUri, relationIsPartOf, agronomicalObjectPartOf);  
            }
            
            try {
//                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spql.build().toString());
                LOGGER.debug(getTraceabilityLogs() + SPARQL_SELECT_QUERY + prepareUpdate.toString());
                prepareUpdate.execute();
                createdResourcesURIList.add(agronomicalObject.getUri());
               
                if (annotationInsert) {
                    resultState = true;
                    this.getConnection().commit();
                } else {
                    // retour en arrière sur la transaction
                    this.getConnection().rollback();
                }
//                this.getConnection().close();
            } catch (RepositoryException ex) {
                    LOGGER.error("Error during commit or rolleback Triplestore statements: ", ex);
            } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationInsert = false;
                    insertStatusList.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_CREATE_QUERY + e.getMessage()));
            } 
            
            //3. insert in postgresql
            AgronomicalObjectDAO agronomicalObjectDAO = new AgronomicalObjectDAO();
            ArrayList<AgronomicalObject> aos = new ArrayList<>();
            aos.add(agronomicalObject);
            POSTResultsReturn postgreInsertionResult = agronomicalObjectDAO.checkAndInsertListAO(aos);
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
     * vérifie les données et les insère dans le triplestore
     * @param agronomicalObjectsDTO
     * @return 
     */
    public POSTResultsReturn checkAndInsert(List<AgronomicalObjectDTO> agronomicalObjectsDTO) {
        POSTResultsReturn checkResult = check(agronomicalObjectsDTO);
        if (checkResult.statusList.size() > 0) { //Les données ne sont pas bonnes
            return checkResult;
        } else { //Si les données sont bonnes
            return insert(agronomicalObjectsDTO);
        }
    }
    
    /**
     * 
     * @param experimentURI
     * @return la requête permettant de récupérer la liste des plots de l'experimentation ainsi que leurs propriétés
     */
    private SPARQLQueryBuilder prepareSearchExperimentPlots(String experimentURI) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(experimentURI);
        sparqlQuery.appendSelect("?" + CHILD +" ?" + RDF_TYPE + " ?" + PROPERTY + " ?" + PROPERTY_RELATION + " ?" + PROPERTY_TYPE);
        
        sparqlQuery.appendTriplet(experimentURI, Vocabulary.RELATION_HAS_PLOT.toString(), "?" + CHILD, null);
        sparqlQuery.appendTriplet("?" + CHILD, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        sparqlQuery.appendTriplet("?" + CHILD, "?" + PROPERTY_RELATION, "?" + PROPERTY, null);
        
        sparqlQuery.beginBodyOptional();
        sparqlQuery.appendToBody("?" + PROPERTY +" <" + Rdf.RELATION_TYPE.toString() + "> ?" + PROPERTY_TYPE);
        sparqlQuery.endBodyOptional();
        
        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }
    
    /**
     * 
     * @param objectURI
     * @return la requête permettant d'avoir tous les éléments contenus (geo:contains) dans objectURI.
     *         la requête permet d'avoir la liste de tous les descendants
     */
    private SPARQLQueryBuilder prepareSearchChildrenWithContains(String objectURI, String objectType) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendPrefix("geo", GeoSPARQL.NAMESPACE.toString());
        if (objectType.equals(Vocabulary.CONCEPT_EXPERIMENT.toString())) {
            sparqlQuery.appendGraph(objectURI);
        }
        sparqlQuery.appendSelect("?" + CHILD + " ?" + RDF_TYPE);
        sparqlQuery.appendTriplet(objectURI, GeoSPARQL.RELATION_CONTAINS_MULTIPLE.toString(), "?" + CHILD, null);
        sparqlQuery.appendTriplet("?" + CHILD, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }
    
    /**
     * 
     * @param objectURI
     * @return La liste des premiers enfants de l'entités (relation geo:contains)
     */
    private SPARQLQueryBuilder prepareSearchFirstChildrenWithContains(String objectURI) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendPrefix("geo", GeoSPARQL.NAMESPACE.toString());
        sparqlQuery.appendSelect("?" + CHILD + " ?" + RDF_TYPE);
        sparqlQuery.appendTriplet(objectURI, GeoSPARQL.RELATION_CONTAINS.toString(), "?" + CHILD, null);
        sparqlQuery.appendTriplet("?" + CHILD, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }
     
    /**
     * 
     * @param layerDTO
     * @return la liste des enfants de layerDTO.getObjectURI. Retourne tous les
     *         descendants si layerDTO.getDepth == true. (clé : uri, valeur : type)
     */
    public HashMap<String, AgronomicalObject> searchChildren(LayerDTO layerDTO) {
        HashMap<String, AgronomicalObject> children = new HashMap<>(); // uri (clé), type (valeur)
        
        //Si c'est une expérimentation, le nom du lien n'est pas le même donc, 
        //on commence par récupérer la liste des enfants directs
        if (layerDTO.getObjectType().equals(Vocabulary.CONCEPT_EXPERIMENT.toString())) {
            //SILEX:test
            //Pour les soucis de pool de connexion
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            
            SPARQLQueryBuilder sparqlQuery = prepareSearchExperimentPlots(layerDTO.getObjectUri());
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            
            TupleQueryResult result = tupleQuery.evaluate();
                        
            //SILEX:test
            //Pour les soucis de pool de connexion
            getConnection().close();
            //\SILEX:test
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                AgronomicalObject agronomicalObject = children.get(bindingSet.getValue(CHILD).stringValue());
                if (agronomicalObject != null) { //Il suffit juste de lui ajouter la propriété. 
                    Property property = new Property();
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    property.setRelation(bindingSet.getValue(PROPERTY_RELATION).stringValue());
                    if (bindingSet.getValue(PROPERTY_TYPE) != null) {
                        property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                    }
                    
                    agronomicalObject.addProperty(property);
                } else { //Il n'est pas encore dans la liste, il faut le rajouter
                    agronomicalObject = new AgronomicalObject();
                    agronomicalObject.setUri(bindingSet.getValue(CHILD).stringValue());
                    agronomicalObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                    
                    Property property = new Property();
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    property.setRelation(bindingSet.getValue(PROPERTY_RELATION).stringValue());
                    if (bindingSet.getValue(PROPERTY_TYPE) != null) {
                        property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                    }
                    agronomicalObject.addProperty(property);
                }
                
                children.put(agronomicalObject.getUri(), agronomicalObject);
            }
        }
        //SILEX:INFO
        //Pour l'instant, on ne récupère que les propriétés des AO de type plot, les premiers descendants de l'expérimentation.
        //Pas les autres
        //Si il faut aussi tous les descendants
        if (ResourcesUtils.getStringBooleanValue(layerDTO.getDepth())) {
            //Si c'est les descendants d'un essai, il y a un traitement particulier
            if (layerDTO.getObjectType().equals(Vocabulary.CONCEPT_EXPERIMENT.toString())) {
                //On recherche tous les fils des plots de l'experimentation, récupérés précédemment
                    //SILEX:test
                    //Pour les soucis de pool de connexion
                    rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
                    rep.initialize();
                    setConnection(rep.getConnection());
                    //\SILEX:test
                for (Entry<String, AgronomicalObject> child : children.entrySet()) {
                   
                    SPARQLQueryBuilder sparqlQuery = prepareSearchChildrenWithContains(child.getKey(), child.getValue().getRdfType());
                    TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
                    TupleQueryResult result = tupleQuery.evaluate();
                   
                    
                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        if (!children.containsKey(bindingSet.getValue(CHILD).stringValue())) {
                            AgronomicalObject agronomicalObject = new AgronomicalObject();
                            agronomicalObject.setUri(bindingSet.getValue(CHILD).stringValue());
                            agronomicalObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                            
                            children.put(bindingSet.getValue(CHILD).stringValue(), agronomicalObject);
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
                    AgronomicalObject agronomicalObject = new AgronomicalObject();
                    agronomicalObject.setUri(bindingSet.getValue(CHILD).stringValue());
                    agronomicalObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());

                    children.put(bindingSet.getValue(CHILD).stringValue(), agronomicalObject);
                }
            }
            
        } else if (!layerDTO.getObjectType().equals(Vocabulary.CONCEPT_EXPERIMENT.toString())) { //S'il ne faut que les enfants directs et que ce n'est pas une expérimentation
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
                AgronomicalObject agronomicalObject = new AgronomicalObject();
                agronomicalObject.setUri(bindingSet.getValue(CHILD).stringValue());
                agronomicalObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());

                children.put(bindingSet.getValue(CHILD).stringValue(), agronomicalObject);
            }
        }
        //\SILEX:INFO
        
        return children;
    }
    
    /**
     * 
     * @return liste d'objets agronomiques, résultat de la recherche, vide si pas de résultats
     */
    public ArrayList<AgronomicalObject> allPaginate() {
        try {
            SPARQLQueryBuilder sparqlQuery = prepareSearchQuery();
            //SILEX:test
            //Pour les soucis de pool de connexion
            rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
            rep.initialize();
            setConnection(rep.getConnection());
            //\SILEX:test
            
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            Map<String, AgronomicalObject> foundedAgronomicalObjects = new HashMap<>();
            
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    boolean alreadyFoundedUri = false;
                    
                    String actualUri = uri != null ? uri : bindingSet.getValue(URI).stringValue();
                    
                    if (foundedAgronomicalObjects.containsKey(actualUri)) {
                        alreadyFoundedUri = true;
                    }
                    
                    AgronomicalObject agronomicalObject = null;
                    
                    Property property = new Property();
                    
                    property.setRelation(bindingSet.getValue(RELATION).stringValue());
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    
                    if (alreadyFoundedUri) {
                        agronomicalObject = foundedAgronomicalObjects.get(actualUri);
                    } else {
                        agronomicalObject = new AgronomicalObject();
                        agronomicalObject.setUri(actualUri);
                        
                        if (experiment != null) {
                            agronomicalObject.setUriExperiment(experiment);
                        } else if (bindingSet.getValue(EXPERIMENT) != null) {
                            agronomicalObject.setExperiment(bindingSet.getValue(EXPERIMENT).stringValue());
                        }
                        
                        if (alias != null) {
                            agronomicalObject.setAlias(alias);
                        } else {
                            agronomicalObject.setAlias(bindingSet.getValue(ALIAS).stringValue());
                        }
                        
                        if (rdfType != null) {
                            agronomicalObject.setRdfType(rdfType);
                        } else {
                            agronomicalObject.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                        }
                    }
                    
                    agronomicalObject.addProperty(property);
                    
                    foundedAgronomicalObjects.put(actualUri, agronomicalObject);
                }
            }
            
            ArrayList<String> agronomicalObjectsUris = new ArrayList<>();
            ArrayList<AgronomicalObject> agronomicalObjects = new ArrayList<>();
            foundedAgronomicalObjects.entrySet().forEach((entry) -> {
                agronomicalObjects.add(entry.getValue());
                agronomicalObjectsUris.add(entry.getKey());
            });
            
            //Get geometries in relational database
            AgronomicalObjectDAO agronomicalObjectDao = new AgronomicalObjectDAO();
            HashMap<String, String> geometries = agronomicalObjectDao.getGeometries(agronomicalObjectsUris);
            
            agronomicalObjects.forEach((agronomicalObject) -> {
                agronomicalObject.setGeometry(geometries.get(agronomicalObject.getUri()));
            });
            
            
            //SILEX:test
            //Pour les soucis de pool de connexion
            getConnection().close();
            //\SILEX:test
            
            return agronomicalObjects;
        }   catch (SQLException ex) {
            java.util.logging.Logger.getLogger(AgronomicalObjectDAOSesame.class.getName()).log(Level.SEVERE, null, ex);
            
            if (getConnection() != null) {
                getConnection().close();
            }
            
            return null;
        }
    }
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:INFO
        //- il faudra par la suite pouvoir avoir plusieurs ao appartenant à une xp sans faire en fonction du type
        //- il faudra ajouter les propriétés de l'objet agronomique
        //\SILEX:INFO
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        
        sparqlQuery.appendDistinct(true);
                
        String agronomicalObjectURI;
        
        if (uri != null ) {
            agronomicalObjectURI = "<" + uri + ">";
        } else {
            agronomicalObjectURI = "?" + URI;
            sparqlQuery.appendSelect(" ?" + URI);
        }
        
        if (experiment != null) {
              sparqlQuery.appendFrom("<" + Contexts.VOCABULARY.toString() + "> \n FROM <" + experiment + ">");
        } else {
            sparqlQuery.appendSelect("?" + EXPERIMENT);
            sparqlQuery.appendOptional("?" + EXPERIMENT + " <" + Vocabulary.RELATION_HAS_PLOT.toString() + "> " + agronomicalObjectURI);
        }
        
        if (alias != null) {
            sparqlQuery.appendTriplet(agronomicalObjectURI, Vocabulary.RELATION_HAS_ALIAS.toString(), "\"" + alias + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?" + ALIAS);
            sparqlQuery.appendTriplet(agronomicalObjectURI, Vocabulary.RELATION_HAS_ALIAS.toString(), "?" + ALIAS, null);
        }
        
        if (rdfType != null) {
            sparqlQuery.appendTriplet(agronomicalObjectURI, Rdf.RELATION_TYPE.toString(), rdfType, null);
        } else {
            sparqlQuery.appendSelect(" ?" + RDF_TYPE);
            sparqlQuery.appendTriplet(agronomicalObjectURI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
            sparqlQuery.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_AGRONOMICAL_OBJECT.toString(), null);
        }
        
        sparqlQuery.appendSelect(" ?" + RELATION + " ?" + PROPERTY);
        sparqlQuery.appendTriplet(agronomicalObjectURI, "?" + RELATION, "?" + PROPERTY, null);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());
        
        return sparqlQuery;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
