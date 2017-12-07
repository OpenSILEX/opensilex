//**********************************************************************************************
//                               AgronomicalObjectDaoSesame.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 6 2017
// Subject: A specific DAO to retreive data on agronomical objects
//***********************************************************************************************

package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.AgronomicalObjectDTO;
import phis2ws.service.resources.dto.LayerDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.AgronomicalObject;
import phis2ws.service.view.model.phis.Property;


public class AgronomicalObjectDaoSesame extends DAOSesame<AgronomicalObject>{
    
    final static Logger LOGGER = LoggerFactory.getLogger(AgronomicalObjectDaoSesame.class);
       
    public String uri;
    public String typeAgronomicalObject;
    public String experiment;
    public String alias;
    
    public AgronomicalObjectDaoSesame() {
        super();
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
            if ((boolean) agronomicalObject.isOk().get("state")) { //Données attendues reçues
               //On vérifie que les types soient effectivement présents dans l'ontologie
                URINamespaces uriNamespaces = new URINamespaces();
                if (!uriNamespaces.objectsPropertyContainsValue(agronomicalObject.getTypeAgronomicalObject())) {
                    dataOk = false;
                    checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, "Wrong agronomical object type value. See ontology"));
                }
                //SILEX:TODO
                //Il faudra aussi faire une vérification sur les properties de l'ao : est-ce que les types sont
                //bien présents dans l'ontologie ? Idem pour les relations
                //\SILEX:TODO
            } else {
                // Format des données non attendu par rapport au schéma demandé
                dataOk = false;
                agronomicalObject.isOk().remove("state");
                checkStatusList.add(new Status("Bad data format", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSINGFIELDS).append(agronomicalObject.isOk()).toString()));
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
    public POSTResultsReturn insert(List<AgronomicalObject> agronomicalObjects) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURIList = new ArrayList<>(); 
        
        POSTResultsReturn results;
        
        boolean resultState = false; //Pour savoir si les données sont bonnes et ont bien été insérées
        boolean annotationInsert = true; // Si l'insertion a bien été effectuée
        
        final Iterator<AgronomicalObject> iteratorAgronomicalObjects = agronomicalObjects.iterator();
        
        while (iteratorAgronomicalObjects.hasNext() && annotationInsert) {
            AgronomicalObject agronomicalObject = iteratorAgronomicalObjects.next();
            
            //Enregistrement triplestore
            final URINamespaces uriNamespaces = new URINamespaces();
            SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
            
            String graphURI = agronomicalObject.getUriExperiment() != null ? agronomicalObject.getUriExperiment() 
                                                                      : uriNamespaces.getContextsProperty("agronomicalObjects");
            spqlInsert.appendGraphURI(graphURI);
            spqlInsert.appendTriplet(agronomicalObject.getUri(), "rdf:type", agronomicalObject.getTypeAgronomicalObject(), null);
            
            //Propriétés associées à l'AO
            for (Property property : agronomicalObject.getProperties()) {
                if (property.getTypeProperty() != null && !property.getTypeProperty().equals("")) {//Propriété typée
                    if (property.getTypeProperty().equals(uriNamespaces.getObjectsProperty("cVariety"))) {
                        //On génère l'uri de la variété
                        String propertyURI = uriNamespaces.getContextsProperty("pxPlatform") + "/v/" + property.getValue().toLowerCase();
                        spqlInsert.appendTriplet(propertyURI, "rdf:type", property.getTypeProperty(), null);
                        spqlInsert.appendTriplet(agronomicalObject.getUri(), property.getRelation(), propertyURI, null);
                    } else {
                        spqlInsert.appendTriplet(property.getValue(), "rdf:type", property.getTypeProperty(), null);
                        spqlInsert.appendTriplet(agronomicalObject.getUri(), property.getRelation(), property.getValue(), null);
                    }
                } else {
                    spqlInsert.appendTriplet(agronomicalObject.getUri(), property.getRelation(), "\"" + property.getValue() + "\"", null);
                }
            }
            
            if (agronomicalObject.getUriExperiment() != null) {
                spqlInsert.appendTriplet(agronomicalObject.getUriExperiment(),uriNamespaces.getRelationsProperty("rHasPlot"), agronomicalObject.getUri(), null);
            }
            try {
                //SILEX:test
                //Toute la notion de connexion au triplestore sera à revoir.
                //C'est un hot fix qui n'est pas propre
                String sesameServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
                String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
                rep = new HTTPRepository(sesameServer, repositoryID); //Stockage triplestore Sesame
                rep.initialize();
                this.setConnection(rep.getConnection());
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                prepareUpdate.execute();
                //\SILEX:test
                createdResourcesURIList.add(agronomicalObject.getUri());
               
             
                if (annotationInsert) {
                    resultState = true;

                    this.getConnection().commit();
                } else {
                    // retour en arrière sur la transaction
                    this.getConnection().rollback();
                }
                this.getConnection().close();
            } catch (RepositoryException ex) {
                    LOGGER.error("Error during commit or rolleback Triplestore statements: ", ex);
            } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationInsert = false;
                    insertStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed insertion query: " + e.getMessage()));
            } 
//            finally {
//                if (this.getConnection() != null) {
//                    this.getConnection().close();
//                }
//            }
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatusList;
        if (resultState && !createdResourcesURIList.isEmpty()) {
            results.createdResources = createdResourcesURIList;
            results.statusList.add(new Status("Resources created", StatusCodeMsg.INFO, createdResourcesURIList.size() + " new resource(s) created."));
        }

        return results;
    }
    
    /**
     * vérifie les données et les insère dans le triplestore
     * @param agronomicalObjects
     * @param agronomicalObjectsDTO
     * @return 
     */
    public POSTResultsReturn checkAndInsert(List<AgronomicalObject> agronomicalObjects, List<AgronomicalObjectDTO> agronomicalObjectsDTO) {
        POSTResultsReturn checkResult = check(agronomicalObjectsDTO);
        if (checkResult.statusList == null) { //Les données ne sont pas bonnes
            return checkResult;
        } else { //Si les données sont bonnes
            return insert(agronomicalObjects);
        }
    }
    
    /**
     * 
     * @param experimentURI
     * @return la requête permettant de récupérer la liste des plots de l'experimentation ainsi que leurs propriétés
     */
    private SPARQLQueryBuilder prepareSearchExperimentPlots(String experimentURI) {
        URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(experimentURI);
        sparqlQuery.appendSelect("?child ?type ?property ?propertyRelation ?propertyType");
        
        sparqlQuery.appendTriplet(experimentURI, uriNamespaces.getRelationsProperty("rHasPlot"), "?child", null);
        sparqlQuery.appendTriplet("?child", "rdf:type", "?type", null);
        sparqlQuery.appendTriplet("?child", "?propertyRelation", "?property", null);
        
        sparqlQuery.beginBodyOptional();
        sparqlQuery.appendToBody("?property rdf:type ?propertyType");
        sparqlQuery.endBodyOptional();
        
        LOGGER.trace("sparql select query : " + sparqlQuery.toString());

        return sparqlQuery;
    }
    
    /**
     * 
     * @param objectURI
     * @return la requête permettant d'avoir tous les éléments contenus (geo:contains) dans objectURI.
     *         la requête permet d'avoir la liste de tous les descendants
     */
    private SPARQLQueryBuilder prepareSearchChildrenWithContains(String objectURI, String objectType) {
        URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendPrefix("geo", uriNamespaces.getContextsProperty("pxGeoSPARQL"));
        if (objectType.equals(uriNamespaces.getObjectsProperty("cExperiment"))) {
            sparqlQuery.appendGraph(objectURI);
        }
        sparqlQuery.appendSelect("?child ?type");
        sparqlQuery.appendTriplet(objectURI, "geo:contains*", "?child", null);
        sparqlQuery.appendTriplet("?child", "rdf:type", "?type", null);
        
        LOGGER.debug("sparql select query : " + sparqlQuery.toString());

        return sparqlQuery;
    }
    
    /**
     * 
     * @param objectURI
     * @return La liste des premiers enfants de l'entités (relation geo:contains)
     */
    private SPARQLQueryBuilder prepareSearchFirstChildrenWithContains(String objectURI) {
        URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendPrefix("geo", uriNamespaces.getContextsProperty("pxGeoSPARQL"));
        sparqlQuery.appendSelect("?child ?type");
        sparqlQuery.appendTriplet(objectURI, "geo:contains", "?child", null);
        sparqlQuery.appendTriplet("?child", "rdf:type", "?type", null);

        
        LOGGER.trace("sparql select query : " + sparqlQuery.toString());

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
        URINamespaces uriNamespaces = new URINamespaces();
        
        //Si c'est une expérimentation, le nom du lien n'est pas le même donc, 
        //on commence par récupérer la liste des enfants directs
        if (layerDTO.getObjectType().equals(uriNamespaces.getObjectsProperty("cExperiment"))) {
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
                    agronomicalObject.setTypeAgronomicalObject(bindingSet.getValue("type").stringValue());
                    
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
            if (layerDTO.getObjectType().equals(uriNamespaces.getObjectsProperty("cExperiment"))) {
                //On recherche tous les fils des plots de l'experimentation, récupérés précédemment
                    //SILEX:test
                    //Pour les soucis de pool de connexion
                    rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
                    rep.initialize();
                    setConnection(rep.getConnection());
                    //\SILEX:test
                for (Entry<String, AgronomicalObject> child : children.entrySet()) {
                   
                    SPARQLQueryBuilder sparqlQuery = prepareSearchChildrenWithContains(child.getKey(), child.getValue().getTypeAgronomicalObject());
                    TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
                    TupleQueryResult result = tupleQuery.evaluate();
                   
                    
                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        if (!children.containsKey(bindingSet.getValue("child").stringValue())) {
                            AgronomicalObject agronomicalObject = new AgronomicalObject();
                            agronomicalObject.setUri(bindingSet.getValue("child").stringValue());
                            agronomicalObject.setTypeAgronomicalObject(bindingSet.getValue("type").stringValue());
                            
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
                    agronomicalObject.setTypeAgronomicalObject(bindingSet.getValue("type").stringValue());

                    children.put(bindingSet.getValue("child").stringValue(), agronomicalObject);
                }
            }
            
        } else if (!layerDTO.getObjectType().equals(uriNamespaces.getObjectsProperty("cExperiment"))) { //S'il ne faut que les enfants directs et que ce n'est pas une expérimentation
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
                agronomicalObject.setTypeAgronomicalObject(bindingSet.getValue("type").stringValue());

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
        SPARQLQueryBuilder sparqlQuery = prepareSearchQuery();
        
        //SILEX:test
        //Pour les soucis de pool de connexion
        rep = new HTTPRepository(SESAME_SERVER, REPOSITORY_ID); //Stockage triplestore Sesame
        rep.initialize();
        setConnection(rep.getConnection());
        //\SILEX:test
        
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
        ArrayList<AgronomicalObject> agronomicalObjects = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                AgronomicalObject agronomicalObject = new AgronomicalObject();
                
                if (uri != null) {
                    agronomicalObject.setUri(uri);
                } else {
                    agronomicalObject.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (experiment != null) {
                    agronomicalObject.setUriExperiment(experiment);
                } else {
                    agronomicalObject.setUriExperiment(bindingSet.getValue("experimentURI").stringValue());
                }
                
                if (alias != null) {
                    agronomicalObject.setAlias(alias);
                } else {
                    agronomicalObject.setAlias(bindingSet.getValue("alias").stringValue());
                }
                
                URINamespaces uriNamespaces = new URINamespaces();
                agronomicalObject.setTypeAgronomicalObject(uriNamespaces.getObjectsProperty("cPlot"));
                
                
                agronomicalObjects.add(agronomicalObject);
            }
        }
        
        //SILEX:test
        //Pour les soucis de pool de connexion
        getConnection().close();
        //\SILEX:test
        
        return agronomicalObjects;
    }
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:INFO
        //- il faudra par la suite pouvoir avoir plusieurs ao appartenant à une xp sans faire en fonction du type
        //- il faudra ajouter les propriétés de l'objet agronomique
        //\SILEX:INFO
        final URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        
        sparqlQuery.appendDistinct(true);
        String agronomicalObjectURI;
        
        if (uri != null ) {
            agronomicalObjectURI = "<" + uri + ">";
        } else {
            agronomicalObjectURI = "?uri";
            sparqlQuery.appendSelect(" ?uri");
        }
        
        if (experiment != null) {
            sparqlQuery.appendGraph(experiment);
        } else {
            sparqlQuery.appendSelect(" ?experimentURI");
            sparqlQuery.appendTriplet("?experimentURI", uriNamespaces.getRelationsProperty("rHasPlot"), agronomicalObjectURI, null);
        }
        
        if (alias != null) {
            sparqlQuery.appendTriplet(agronomicalObjectURI, uriNamespaces.getRelationsProperty("rHasAlias"), "\"" + alias + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?alias");
            sparqlQuery.appendTriplet(agronomicalObjectURI, uriNamespaces.getRelationsProperty("rHasAlias"), "?alias", null);
        }
        
        sparqlQuery.appendTriplet(agronomicalObjectURI, "rdf:type", uriNamespaces.getObjectsProperty("cPlot"), null);
        
        LOGGER.trace("sparql select query : " + sparqlQuery.toString());
        
        return sparqlQuery;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
