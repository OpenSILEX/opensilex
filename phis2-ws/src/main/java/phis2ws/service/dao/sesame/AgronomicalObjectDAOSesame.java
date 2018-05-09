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
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.AgronomicalObjectDAO;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.AgronomicalObjectDTO;
import phis2ws.service.resources.dto.LayerDTO;
import phis2ws.service.resources.dto.PropertyDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.AgronomicalObject;
import phis2ws.service.view.model.phis.Property;
import phis2ws.service.view.model.phis.Uri;


public class AgronomicalObjectDAOSesame extends DAOSesame<AgronomicalObject> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(AgronomicalObjectDAOSesame.class);
    
    //The following attributes are used to search agronomical objects in the triplestore
    //uri of the agronomical object
    public String uri;
    private final String URI = "uri";
    
    //type of the agronomical object
    public String rdfType;
    private final String RDF_TYPE = "rdfType";
    
    //experiment of the agronomical object
    public String experiment;
    
    //alias of the agronomical object
    public String alias;
    
    private static final String PROPERTIES_SERVICE_FILE_NAME = "service";
    private static final String PROPERTIES_SERVICE_BASE_URI = "baseURI";
    
    private static final String URI_CODE_AGRONOMICAL_OBJECT = "o";
    
    private final static URINamespaces NAMESPACES = new URINamespaces();
    final static String TRIPLESTORE_CONCEPT_AGRONOMICAL_OBJECT = NAMESPACES.getObjectsProperty("cAgronomicalObject");
    final static String TRIPLESTORE_CONCEPT_VARIETY = NAMESPACES.getObjectsProperty("cVariety");
    final static String TRIPLESTORE_CONTEXT_AGRONOMICAL_OBJECTS = NAMESPACES.getContextsProperty("agronomicalObjects");
    final static String TRIPLESTORE_RELATION_HAS_ALIAS = NAMESPACES.getRelationsProperty("rHasAlias");
    final static String TRIPLESTORE_RELATION_HAS_PLOT = NAMESPACES.getRelationsProperty("rHasPlot");
    final static String TRIPLESTORE_RELATION_IS_PART_OF = NAMESPACES.getRelationsProperty("rIsPartOf");
    final static String TRIPLESTORE_RELATION_TYPE = NAMESPACES.getRelationsProperty("type");
    final static String TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE = NAMESPACES.getRelationsProperty("subClassOf*");
        
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
        queryLastAgronomicalObjectURi.appendTriplet("?" + URI, TRIPLESTORE_RELATION_TYPE, "?" + RDF_TYPE, null);
        queryLastAgronomicalObjectURi.appendTriplet("?" + RDF_TYPE, TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE, TRIPLESTORE_CONCEPT_AGRONOMICAL_OBJECT, null);
        queryLastAgronomicalObjectURi.appendFilter("regex(str(?" + URI + "), \".*/" + year + "/.*\")");
        queryLastAgronomicalObjectURi.appendOrderBy("desc(?" + URI + ")");
        queryLastAgronomicalObjectURi.appendLimit(1);
        
        LOGGER.debug("SPARQL query : " + queryLastAgronomicalObjectURi.toString());
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
        query.appendToBody("?x <" + TRIPLESTORE_RELATION_HAS_ALIAS + "> \"" + alias + "\"");
        
        LOGGER.debug(query.toString());
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
            //Vérification des agronomical objects
            if ((boolean) agronomicalObject.isOk().get(AbstractVerifiedClass.STATE)) { //Données attendues reçues
               //On vérifie que les types soient effectivement présents dans l'ontologie
                UriDaoSesame uriDao = new UriDaoSesame();
                
                if (!uriDao.isSubClassOf(agronomicalObject.getRdfType(), TRIPLESTORE_CONCEPT_AGRONOMICAL_OBJECT)) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Wrong agronomical object type value. See ontology"));
                }
                //SILEX:TODO
                //Il faudra aussi faire une vérification sur les properties de l'ao : est-ce que les types sont
                //bien présents dans l'ontologie ? Idem pour les relations
                //\SILEX:TODO
                
                //check isPartOf
                if (agronomicalObject.getIsPartOf() != null) {
                    if (existObject(agronomicalObject.getIsPartOf())) {
                        //1. get isPartOf object type
                        uriDao.uri = agronomicalObject.getIsPartOf();
                        ArrayList<Uri> typesResult = uriDao.getAskTypeAnswer();
                        if (!uriDao.isSubClassOf(typesResult.get(0).getRdfType(), TRIPLESTORE_CONCEPT_AGRONOMICAL_OBJECT)) {
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
                for (PropertyDTO property : agronomicalObject.getProperties()) {
                    //check alias
                    if (property.getRelation().equals(TRIPLESTORE_RELATION_HAS_ALIAS)) {
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
            } else {
                // Format des données non attendu par rapport au schéma demandé
                dataOk = false;
                agronomicalObject.isOk().remove(AbstractVerifiedClass.STATE);
                checkStatusList.add(new Status(StatusCodeMsg.BAD_DATA_FORMAT, StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(agronomicalObject.isOk()).toString()));
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
            agronomicalObject.setUri(uriGenerator.generateNewInstanceUri(agronomicalObject.getRdfType(), agronomicalObjectDTO.getYear()));
            
            //2. Register in triplestore
            SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
            
            String graphURI = agronomicalObject.getUriExperiment() != null ? agronomicalObject.getUriExperiment() 
                                                                      : TRIPLESTORE_CONTEXT_AGRONOMICAL_OBJECTS;
            spqlInsert.appendGraphURI(graphURI);
            spqlInsert.appendTriplet(agronomicalObject.getUri(), TRIPLESTORE_RELATION_TYPE, agronomicalObject.getRdfType(), null);
            
            //Propriétés associées à l'AO
            for (Property property : agronomicalObject.getProperties()) {
                if (property.getTypeProperty() != null && !property.getTypeProperty().equals("")) {//Propriété typée
                    if (property.getTypeProperty().equals(TRIPLESTORE_CONCEPT_VARIETY)) {
                        //On génère l'uri de la variété
                        //SILEX:TODO
                        //move the uri generation in the UriGenerator (#45)
                        //\SILEX:TODO
                        String propertyURI = NAMESPACES.getContextsProperty("pxPlatform") + "/v/" + property.getValue().toLowerCase();
                        spqlInsert.appendTriplet(propertyURI, TRIPLESTORE_RELATION_TYPE, property.getTypeProperty(), null);
                        spqlInsert.appendTriplet(agronomicalObject.getUri(), property.getRelation(), propertyURI, null);
                    } else {
                        spqlInsert.appendTriplet(property.getValue(), TRIPLESTORE_RELATION_TYPE, property.getTypeProperty(), null);
                        spqlInsert.appendTriplet(agronomicalObject.getUri(), property.getRelation(), property.getValue(), null);
                    }
                } else {
                    spqlInsert.appendTriplet(agronomicalObject.getUri(), property.getRelation(), "\"" + property.getValue() + "\"", null);
                }
            }
            
            if (agronomicalObject.getUriExperiment() != null) {
                spqlInsert.appendTriplet(agronomicalObject.getUriExperiment(), TRIPLESTORE_RELATION_HAS_PLOT, agronomicalObject.getUri(), null);
            }
            
            //isPartOf : the object which has part the element must not be a plot    
            if (agronomicalObject.getIsPartOf()!= null) {
                spqlInsert.appendTriplet(agronomicalObject.getUri(), TRIPLESTORE_RELATION_IS_PART_OF, agronomicalObject.getIsPartOf(), null);
            }
            
            try {
//                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
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
        sparqlQuery.appendSelect("?child ?type ?property ?propertyRelation ?propertyType");
        
        sparqlQuery.appendTriplet(experimentURI, TRIPLESTORE_RELATION_HAS_PLOT, "?child", null);
        sparqlQuery.appendTriplet("?child", TRIPLESTORE_RELATION_TYPE, "?type", null);
        sparqlQuery.appendTriplet("?child", "?propertyRelation", "?property", null);
        
        sparqlQuery.beginBodyOptional();
        sparqlQuery.appendToBody("?property " + TRIPLESTORE_RELATION_TYPE + " ?propertyType");
        sparqlQuery.endBodyOptional();
        
        LOGGER.debug("sparql select query : " + sparqlQuery.toString());

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
        sparqlQuery.appendPrefix("geo", NAMESPACES.getContextsProperty("pxGeoSPARQL"));
        if (objectType.equals(NAMESPACES.getObjectsProperty("cExperiment"))) {
            sparqlQuery.appendGraph(objectURI);
        }
        sparqlQuery.appendSelect("?child ?type");
        sparqlQuery.appendTriplet(objectURI, "geo:contains*", "?child", null);
        sparqlQuery.appendTriplet("?child", TRIPLESTORE_RELATION_TYPE, "?type", null);
        
        LOGGER.debug("sparql select query : " + sparqlQuery.toString());

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
        sparqlQuery.appendPrefix("geo", NAMESPACES.getContextsProperty("pxGeoSPARQL"));
        sparqlQuery.appendSelect("?child ?type");
        sparqlQuery.appendTriplet(objectURI, "geo:contains", "?child", null);
        sparqlQuery.appendTriplet("?child", TRIPLESTORE_RELATION_TYPE, "?type", null);
        
        LOGGER.debug("sparql select query : " + sparqlQuery.toString());

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
        if (layerDTO.getObjectType().equals(NAMESPACES.getObjectsProperty("cExperiment"))) {
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
                AgronomicalObject agronomicalObject = children.get(bindingSet.getValue("child").stringValue());
                if (agronomicalObject != null) { //Il suffit juste de lui ajouter la propriété. 
                    Property property = new Property();
                    property.setValue(bindingSet.getValue("property").stringValue());
                    property.setRelation(bindingSet.getValue("propertyRelation").stringValue());
                    if (bindingSet.getValue("propertyType") != null) {
                        property.setTypeProperty(bindingSet.getValue("propertyType").stringValue());
                    }
                    
                    agronomicalObject.addProperty(property);
                } else { //Il n'est pas encore dans la liste, il faut le rajouter
                    agronomicalObject = new AgronomicalObject();
                    agronomicalObject.setUri(bindingSet.getValue("child").stringValue());
                    agronomicalObject.setRdfType(bindingSet.getValue("type").stringValue());
                    
                    Property property = new Property();
                    property.setValue(bindingSet.getValue("property").stringValue());
                    property.setRelation(bindingSet.getValue("propertyRelation").stringValue());
                    if (bindingSet.getValue("propertyType") != null) {
                        property.setTypeProperty(bindingSet.getValue("propertyType").stringValue());
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
            if (layerDTO.getObjectType().equals(NAMESPACES.getObjectsProperty("cExperiment"))) {
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
                        if (!children.containsKey(bindingSet.getValue("child").stringValue())) {
                            AgronomicalObject agronomicalObject = new AgronomicalObject();
                            agronomicalObject.setUri(bindingSet.getValue("child").stringValue());
                            agronomicalObject.setRdfType(bindingSet.getValue("type").stringValue());
                            
                            children.put(bindingSet.getValue("child").stringValue(), agronomicalObject);
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
                    agronomicalObject.setUri(bindingSet.getValue("child").stringValue());
                    agronomicalObject.setRdfType(bindingSet.getValue("type").stringValue());

                    children.put(bindingSet.getValue("child").stringValue(), agronomicalObject);
                }
            }
            
        } else if (!layerDTO.getObjectType().equals(NAMESPACES.getObjectsProperty("cExperiment"))) { //S'il ne faut que les enfants directs et que ce n'est pas une expérimentation
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
                agronomicalObject.setUri(bindingSet.getValue("child").stringValue());
                agronomicalObject.setRdfType(bindingSet.getValue("type").stringValue());

                children.put(bindingSet.getValue("child").stringValue(), agronomicalObject);
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
                    
                    property.setRelation(bindingSet.getValue("relation").stringValue());
                    property.setValue(bindingSet.getValue("property").stringValue());
                    
                    if (alreadyFoundedUri) {
                        agronomicalObject = foundedAgronomicalObjects.get(actualUri);
                    } else {
                        agronomicalObject = new AgronomicalObject();
                        agronomicalObject.setUri(actualUri);
                        
                        if (experiment != null) {
                            agronomicalObject.setUriExperiment(experiment);
                        }
                        
                        if (alias != null) {
                            agronomicalObject.setAlias(alias);
                        } else {
                            agronomicalObject.setAlias(bindingSet.getValue("alias").stringValue());
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
            sparqlQuery.appendFrom("<" + NAMESPACES.getContextsProperty("pVoc2017") + "> \n FROM <" + experiment + ">");
        } 
        
        if (alias != null) {
            sparqlQuery.appendTriplet(agronomicalObjectURI, TRIPLESTORE_RELATION_HAS_ALIAS, "\"" + alias + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?alias");
            sparqlQuery.appendTriplet(agronomicalObjectURI, TRIPLESTORE_RELATION_HAS_ALIAS, "?alias", null);
        }
        
        if (rdfType != null) {
            sparqlQuery.appendTriplet(agronomicalObjectURI, TRIPLESTORE_RELATION_TYPE, rdfType, null);
        } else {
            sparqlQuery.appendSelect(" ?" + RDF_TYPE);
            sparqlQuery.appendTriplet(agronomicalObjectURI, TRIPLESTORE_RELATION_TYPE, "?" + RDF_TYPE, null);
            sparqlQuery.appendTriplet("?" + RDF_TYPE, TRIPLESTORE_RELATION_SUBCLASS_OF_MULTIPLE, TRIPLESTORE_CONCEPT_AGRONOMICAL_OBJECT, null);
        }
        
        sparqlQuery.appendSelect(" ?relation ?property");
        sparqlQuery.appendTriplet("?" + URI, "?relation", "?property", null);
        
        LOGGER.debug("sparql select query : " + sparqlQuery.toString());
        
        return sparqlQuery;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
