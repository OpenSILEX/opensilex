//**********************************************************************************************
//                                       TraitDaoSesame.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 17 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 17 2017
// Subject: A specific DAO to retrieve data on traits
//***********************************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import phis2ws.service.resources.dto.TraitDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.OntologyReference;
import phis2ws.service.view.model.phis.Trait;

public class TraitDaoSesame extends DAOSesame<Trait> {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitDaoSesame.class);

    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();
    
    public TraitDaoSesame() {
    }

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:todo
        //Ajouter la recherche par référence vers d'autres ontologies aussi
        //\SILEX:todo
        final URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(uriNamespaces.getContextsProperty("variables"));
        String traitURI;
        if (uri != null) {
            traitURI = "<" + uri + ">";
        } else {
            traitURI = "?uri";
            query.appendSelect("?uri");
        }
        query.appendTriplet(traitURI, "rdf:type", uriNamespaces.getObjectsProperty("cTrait"), null);
        
        if (label != null) {
            query.appendTriplet(traitURI, "rdfs:label","\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?label");
            query.appendTriplet(traitURI, "rdfs:label", "?label", null);
        }
        
        if (comment != null) {
            query.appendTriplet(traitURI, "rdfs:comment", "\"" + comment + "\"", null);
        } else {
            query.appendSelect(" ?comment");
            query.appendTriplet(traitURI, "rdfs:comment", " ?comment", null);
        }
        
        LOGGER.trace("sparql select query : " + query.toString());
        return query;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * prepare a query to get the higher id of the traits
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        URINamespaces uriNamespace = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", uriNamespace.getRelationsProperty("type"), uriNamespace.getObjectsProperty("cTrait"), null);
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * get the higher id of the traits
     * @return the id
     */
    public int getLastId() {
       SPARQLQueryBuilder query = prepareGetLastId(); 
       
       //SILEX:test
        //All the triplestore connection has to been checked and updated
        //This is an unclean hot fix
        String sesameServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
        String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
        rep = new HTTPRepository(sesameServer, repositoryID); //Stockage triplestore Sesame
        rep.initialize();
        this.setConnection(rep.getConnection());
        this.getConnection().begin();
        //\SILEX:test

        //get last trait uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        //SILEX:test
        //For the pool connection problems
        getConnection().commit();
        getConnection().close();
        //\SILEX:test
        
        String uriTrait = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriTrait = bindingSet.getValue("uri").stringValue();
        }
        
        if (uriTrait == null) {
            return 0;
        } else {
            String split = "traits/t";
            String[] parts = uriTrait.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }
    
    /**
     * Vérifie si les traits sont corrects
     * @param traitsDTO
     * @return 
     */
    public POSTResultsReturn check(List<TraitDTO> traitsDTO) {
        //Résultats attendus
        POSTResultsReturn traitsCheck = null;
        //Liste des status retournés
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        URINamespaces uriNamespaces = new URINamespaces();
        
        //Vérification des traits
        for (TraitDTO traitDTO : traitsDTO) {
            if ((boolean) traitDTO.isOk().get("state")) { 
                //Vérification des relations d'ontologies de référence
                for (OntologyReference ontologyReference : traitDTO.getOntologiesReferences()) {
                        if (!ontologyReference.getProperty().equals(uriNamespaces.getRelationsProperty("rExactMatch"))
                           && !ontologyReference.getProperty().equals(uriNamespaces.getRelationsProperty("rCloseMatch"))
                           && !ontologyReference.getProperty().equals(uriNamespaces.getRelationsProperty("rNarrower"))
                           && !ontologyReference.getProperty().equals(uriNamespaces.getRelationsProperty("rBroader"))) {
                            dataOk = false;
                            checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, 
                                    "Bad property relation given. Must be one of the following : " + uriNamespaces.getRelationsProperty("rExactMatch")
                                    + ", " + uriNamespaces.getRelationsProperty("rCloseMatch")
                                    + ", " + uriNamespaces.getRelationsProperty("rNarrower")
                                    + ", " + uriNamespaces.getRelationsProperty("rBroader")
                                    +". Given : " + ontologyReference.getProperty()));
                        }
                    }
            } else { //Données attendues non reçues
                dataOk = false;
                traitDTO.isOk().remove("state");
                checkStatusList.add(new Status("Bad data format", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(traitDTO.isOk()).toString()));
            }
        }
        
        traitsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        traitsCheck.statusList = checkStatusList;
        return traitsCheck;
    }
    
    private SPARQLUpdateBuilder prepareInsertQuery(TraitDTO traitDTO) {
        SPARQLUpdateBuilder spql = new SPARQLUpdateBuilder();
        final URINamespaces uriNamespaces = new URINamespaces();
        
        spql.appendGraphURI(uriNamespaces.getContextsProperty("variables"));
        spql.appendTriplet(traitDTO.getUri(), "rdf:type", uriNamespaces.getObjectsProperty("cTrait"), null);
        spql.appendTriplet(traitDTO.getUri(), "rdfs:label", "\"" + traitDTO.getLabel() + "\"", null);
        spql.appendTriplet(traitDTO.getUri(), "rdfs:comment", "\"" + traitDTO.getComment() + "\"", null);
        
        for (OntologyReference ontologyReference : traitDTO.getOntologiesReferences()) {
            spql.appendTriplet(traitDTO.getUri(), ontologyReference.getProperty(), ontologyReference.getObject(), null);
            spql.appendTriplet(ontologyReference.getObject(), "rdfs:seeAlso", "\"" + ontologyReference.getSeeAlso() + "\"", null);
        }
        
        return spql;
    }
    
    /**
     * insère les données dans le triplestore
     * On suppose que la vérification de leur intégrité a été faite auparavent, via l'appel à la méthode check
     * @param traitsDTO
     * @return 
     */
    public POSTResultsReturn insert(List<TraitDTO> traitsDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURI = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false; //Pour savoir si les données sont bonnes et ont bien été insérées
        boolean annotationInsert = true; //Si l'insertion a bien été faite
        
        UriGenerator uriGenerator = new UriGenerator();
        URINamespaces uriNamespaces = new URINamespaces();
        final Iterator<TraitDTO> iteratorTraitDTO = traitsDTO.iterator();
        
        while (iteratorTraitDTO.hasNext() && annotationInsert) {
            TraitDTO traitDTO = iteratorTraitDTO.next();
            traitDTO.setUri(uriGenerator.generateNewInstanceUri(uriNamespaces.getObjectsProperty("cTrait"), null));
            //Enregistrement dans le triplestore
            SPARQLUpdateBuilder spqlInsert = prepareInsertQuery(traitDTO);
            
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

                createdResourcesURI.add(traitDTO.getUri());

                if (annotationInsert) {
                    resultState = true;
                    getConnection().commit();
                } else {
                    getConnection().rollback();
                }
            } catch (RepositoryException ex) {
                    LOGGER.error("Error during commit or rolleback Triplestore statements: ", ex);
            } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationInsert = false;
                    insertStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed insertion query: " + e.getMessage()));
            } 
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatusList;
        results.setCreatedResources(createdResourcesURI);
        if (resultState && !createdResourcesURI.isEmpty()) {
            results.createdResources = createdResourcesURI;
            results.statusList.add(new Status("Resources created", StatusCodeMsg.INFO, createdResourcesURI.size() + " new resource(s) created."));
        }
        
        return results;
    }
    
    /**
     * 
     * @param uri
     * @return la liste des liens vers d'autres ontologies
     */
    private SPARQLQueryBuilder prepareSearchOntologiesReferencesQuery(String uri) {
        final URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(uriNamespaces.getContextsProperty("variables"));
        
        if (ontologiesReferences.isEmpty()) {
            query.appendSelect(" ?property ?object ?seeAlso");
            query.appendTriplet(uri, "?property", "?object", null);
            query.appendOptional("{?object rdfs:seeAlso ?seeAlso}");
            query.appendFilter("?property IN(<" + uriNamespaces.getRelationsProperty("rCloseMatch") + ">, <"
                                               + uriNamespaces.getRelationsProperty("rExactMatch") + ">, <"
                                               + uriNamespaces.getRelationsProperty("rNarrower") + ">, <"
                                               + uriNamespaces.getRelationsProperty("rBroader") + ">)");
        } else {
            for (OntologyReference ontologyReference : ontologiesReferences) {
                query.appendTriplet(uri, ontologyReference.getProperty(), ontologyReference.getObject(), null);
                query.appendTriplet(ontologyReference.getObject(), "rdfs:seeAlso", ontologyReference.getSeeAlso(), null);
            }
        }
        
        LOGGER.trace("SPARQL select query : " + query.toString());
        return query;
    }
    
    /**
     * 
     * @return la liste des traits correspondant à la recherche
     */
    public ArrayList<Trait> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Trait> traits = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Trait trait = new Trait();
                
                if (uri != null) {
                    trait.setUri(uri);
                } else {
                    trait.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (label != null) {
                    trait.setLabel(label);
                } else {
                    trait.setLabel(bindingSet.getValue("label").stringValue());
                }
                
                if (comment != null) {
                    trait.setComment(comment);
                } else {
                    trait.setComment(bindingSet.getValue("comment").stringValue());
                }
                
                //On récupère maintenant la liste des références vers des ontologies... 
                SPARQLQueryBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(trait.getUri());
                TupleQuery tupleQueryOntologiesReferences = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryOntologiesReferences.toString());
                TupleQueryResult resultOntologiesReferences = tupleQueryOntologiesReferences.evaluate();
                while (resultOntologiesReferences.hasNext()) {
                    BindingSet bindingSetOntologiesReferences = resultOntologiesReferences.next();
                    if (bindingSetOntologiesReferences.getValue("object") != null
                            && bindingSetOntologiesReferences.getValue("property") != null) {
                        OntologyReference ontologyReference = new OntologyReference();
                        ontologyReference.setObject(bindingSetOntologiesReferences.getValue("object").toString());
                        ontologyReference.setProperty(bindingSetOntologiesReferences.getValue("property").toString());
                        if (bindingSetOntologiesReferences.getValue("seeAlso") != null) {
                            ontologyReference.setSeeAlso(bindingSetOntologiesReferences.getValue("seeAlso").toString());
                        }
                        
                        trait.addOntologyReference(ontologyReference);
                    }
                }
                
                traits.add(trait);
            }
        }
        
        return traits;
    }
    
    private String prepareDeleteQuery(Trait trait) {
        String deleteQuery;
        deleteQuery = "DELETE WHERE {"
                + "<" + trait.getUri() + "> rdfs:label \"" + trait.getLabel() + "\" . "
                + "<" + trait.getUri() + "> rdfs:comment \"" + trait.getComment() + "\" . ";

        for (OntologyReference ontologyReference : trait.getOntologiesReferences()) {
            deleteQuery += "<" + trait.getUri() + "> <" + ontologyReference.getProperty() + "> <" + ontologyReference.getObject() + "> . ";
            if (ontologyReference.getSeeAlso() != null) {
                deleteQuery += "<" + ontologyReference.getObject() + "> rdfs:seeAlso " + ontologyReference.getSeeAlso() + " . ";
            }
        }

        deleteQuery += "}";
                
        return deleteQuery;
    }
    
    private POSTResultsReturn update(List<TraitDTO> traitsDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true; // Si l'insertion a bien été réalisée
        boolean resultState = false; // Pour savoir si les données étaient bonnes et on bien été mises à jour
        
        for (TraitDTO traitDTO : traitsDTO) {
            //1. Suppression des données déjà existantes
            //1.1 Récupération des infos qui seront modifiées (pour supprimer les bons triplets)
            uri = traitDTO.getUri();
            ArrayList<Trait> traitsCorresponding = allPaginate();
            if (traitsCorresponding.size() > 0) {
                String deleteQuery = prepareDeleteQuery(traitsCorresponding.get(0));

                //2. Insertion des nouvelles données
                SPARQLUpdateBuilder queryInsert = prepareInsertQuery(traitDTO);
                 try {
                        // début de la transaction : vérification de la requête
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareDelete.execute();
                        prepareUpdate.execute();

                        updatedResourcesURIList.add(traitDTO.getUri());
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        annotationUpdate = false;
                        updateStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                    }   
            } else {
                annotationUpdate = false;
                updateStatusList.add(new Status("Unknown instance", StatusCodeMsg.ERR, "Unknown trait " + traitDTO.getUri()));
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
            // retour en arrière sur la transaction
            try {
                this.getConnection().rollback();
            } catch (RepositoryException ex) {
                LOGGER.error("Error during rollback Triplestore statements : ", ex);
            }
        }
        
        results = new POSTResultsReturn(resultState, annotationUpdate, true);
        results.statusList = updateStatusList;
        if (resultState && !updatedResourcesURIList.isEmpty()) {
            results.createdResources = updatedResourcesURIList;
            results.statusList.add(new Status("Resources updated", StatusCodeMsg.INFO, updatedResourcesURIList.size() + " resources updated"));
        }
        
        return results;
    }
    
    /**
     * Vérifie les données et les insère dans le triplestore.
     * @param traitsDTO
     * @return POSTResultsReturn le résultat de la tentative d'insertion
     */
    public POSTResultsReturn checkAndInsert(List<TraitDTO> traitsDTO) {
        POSTResultsReturn checkResult = check(traitsDTO);
        if (checkResult.getDataState()) {
            return insert(traitsDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }
    
    /**
     * Vérifie les données et met à jour le triplestore
     * @param traitsDTO
     * @return POSTResultsReturn le résultat de la tentative de modification des données
     */
    public POSTResultsReturn checkAndUpdate(List<TraitDTO> traitsDTO) {
        POSTResultsReturn checkResult = check(traitsDTO);
        if (checkResult.getDataState()) {
            return update(traitsDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }
}
