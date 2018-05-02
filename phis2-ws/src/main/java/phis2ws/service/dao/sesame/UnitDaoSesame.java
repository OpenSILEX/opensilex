//**********************************************************************************************
//                                       UnitDaoSesame.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 18 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 18 2017
// Subject: A specific DAO to retrieve data on units
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
import phis2ws.service.resources.dto.UnitDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.OntologyReference;
import phis2ws.service.view.model.phis.Unit;

public class UnitDaoSesame extends DAOSesame<Unit> {
    final static Logger LOGGER = LoggerFactory.getLogger(UnitDaoSesame.class);
    
    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();

    public UnitDaoSesame() {
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
        query.appendTriplet(traitURI, "rdf:type", uriNamespaces.getObjectsProperty("cUnit"), null);
        
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
     * Vérifie si les unités sont correctes
     * @param unitsDTO
     * @return 
     */
    public POSTResultsReturn check(List<UnitDTO> unitsDTO) {
        //Résultats attendus
        POSTResultsReturn traitsCheck = null;
        //Liste des status retournés
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        URINamespaces uriNamespaces = new URINamespaces();
        
        //Vérification des unités
        for (UnitDTO unitDTO : unitsDTO) {
            if ((boolean) unitDTO.isOk().get("state")) { 
                //Vérification des relations d'ontologies de référence
                for (OntologyReference ontologyReference : unitDTO.getOntologiesReferences()) {
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
                unitDTO.isOk().remove("state");
                checkStatusList.add(new Status("Bad data format", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(unitDTO.isOk()).toString()));
            }
        }
        
        traitsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        traitsCheck.statusList = checkStatusList;
        return traitsCheck;
    }
    
    /**
     * prepare a query to get the higher id of the units
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        URINamespaces uriNamespace = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", uriNamespace.getRelationsProperty("type"), uriNamespace.getObjectsProperty("cUnit"), null);
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * get the higher id of the units
     * @return the id
     */
    public int getLastId() {
        SPARQLQueryBuilder query = prepareGetLastId();

        //get last unit uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();
        
        String uriUnit = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriUnit = bindingSet.getValue("uri").stringValue();
        }
        
        if (uriUnit == null) {
            return 0;
        } else {
            String split = "units/u";
            String[] parts = uriUnit.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }
    
    private SPARQLUpdateBuilder prepareInsertQuery(UnitDTO unitDTO) {
        SPARQLUpdateBuilder spql = new SPARQLUpdateBuilder();
        final URINamespaces uriNamespaces = new URINamespaces();
        
        spql.appendGraphURI(uriNamespaces.getContextsProperty("variables"));
        spql.appendTriplet(unitDTO.getUri(), "rdf:type", uriNamespaces.getObjectsProperty("cUnit"), null);
        spql.appendTriplet(unitDTO.getUri(), "rdfs:label", "\"" + unitDTO.getLabel() + "\"", null);
        spql.appendTriplet(unitDTO.getUri(), "rdfs:comment", "\"" + unitDTO.getComment() + "\"", null);
        
        for (OntologyReference ontologyReference : unitDTO.getOntologiesReferences()) {
            spql.appendTriplet(unitDTO.getUri(), ontologyReference.getProperty(), ontologyReference.getObject(), null);
            spql.appendTriplet(ontologyReference.getObject(), "rdfs:seeAlso", "\"" + ontologyReference.getSeeAlso() + "\"", null);
        }
        
        return spql;
    }
    
    /**
     * insère les données dans le triplestore
     * On suppose que la vérification de leur intégrité a été faite auparavent, via l'appel à la méthode check
     * @param unitsDTO
     * @return 
     */
    public POSTResultsReturn insert(List<UnitDTO> unitsDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURI = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false; //Pour savoir si les données sont bonnes et ont bien été insérées
        boolean annotationInsert = true; //Si l'insertion a bien été faite
        
        UriGenerator uriGenerator = new UriGenerator();
        URINamespaces uriNamespaces = new URINamespaces();
        final Iterator<UnitDTO> iteratorUnitDTO = unitsDTO.iterator();
        
        while (iteratorUnitDTO.hasNext() && annotationInsert) {
            UnitDTO unitDTO = iteratorUnitDTO.next();
            unitDTO.setUri(uriGenerator.generateNewInstanceUri(uriNamespaces.getObjectsProperty("cUnit"), null));
            
            //Enregistrement dans le triplestore
            SPARQLUpdateBuilder spqlInsert = prepareInsertQuery(unitDTO);
            
            try {
                //SILEX:test
                //Toute la notion de connexion au triplestore sera à revoir.
                //C'est un hot fix qui n'est pas propre
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                prepareUpdate.execute();
                //\SILEX:test

                createdResourcesURI.add(unitDTO.getUri());

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
     * Vérifie les données et les insère dans le triplestore.
     * @param unitsDTO
     * @return POSTResultsReturn le résultat de la tentative d'insertion
     */
    public POSTResultsReturn checkAndInsert(List<UnitDTO> unitsDTO) {
        POSTResultsReturn checkResult = check(unitsDTO);
        if (checkResult.getDataState()) {
            return insert(unitsDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
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
     * @return la liste des unités correspondant à la recherche
     */
    public ArrayList<Unit> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Unit> units = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Unit unit = new Unit();
                
                if (uri != null) {
                    unit.setUri(uri);
                } else {
                    unit.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (label != null) {
                    unit.setLabel(label);
                } else {
                    unit.setLabel(bindingSet.getValue("label").stringValue());
                }
                
                if (comment != null) {
                    unit.setComment(comment);
                } else {
                    unit.setComment(bindingSet.getValue("comment").stringValue());
                }
                
                //On récupère maintenant la liste des références vers des ontologies... 
                SPARQLQueryBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(unit.getUri());
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
                        
                        unit.addOntologyReference(ontologyReference);
                    }
                }
                
                units.add(unit);
            }
        }
        
        return units;
    }
    
    private String prepareDeleteQuery(Unit unit) {
        String deleteQuery;
        deleteQuery = "DELETE WHERE {"
                + "<" + unit.getUri() + "> rdfs:label \"" + unit.getLabel() + "\" . "
                + "<" + unit.getUri() + "> rdfs:comment \"" + unit.getComment() + "\" . ";

        for (OntologyReference ontologyReference : unit.getOntologiesReferences()) {
            deleteQuery += "<" + unit.getUri() + "> <" + ontologyReference.getProperty() + "> <" + ontologyReference.getObject() + "> . ";
            if (ontologyReference.getSeeAlso() != null) {
                deleteQuery += "<" + ontologyReference.getObject() + "> rdfs:seeAlso " + ontologyReference.getSeeAlso() + " . ";
            }
        }

        deleteQuery += "}";
                
        return deleteQuery;
    }
    
    private POSTResultsReturn update(List<UnitDTO> unitsDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true; // Si l'insertion a bien été réalisée
        boolean resultState = false; // Pour savoir si les données étaient bonnes et on bien été mises à jour
        
        for (UnitDTO unitDTO : unitsDTO) {
            //1. Suppression des données déjà existantes
            //1.1 Récupération des infos qui seront modifiées (pour supprimer les bons triplets)
            uri = unitDTO.getUri();
            ArrayList<Unit> unitsCorresponding = allPaginate();
            if (unitsCorresponding.size() > 0) {
                String deleteQuery = prepareDeleteQuery(unitsCorresponding.get(0));

                //2. Insertion des nouvelles données
                SPARQLUpdateBuilder queryInsert = prepareInsertQuery(unitDTO);
                 try {
                        // début de la transaction : vérification de la requête
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareDelete.execute();
                        prepareUpdate.execute();

                        updatedResourcesURIList.add(unitDTO.getUri());
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        annotationUpdate = false;
                        updateStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                    }   
            } else {
                annotationUpdate = false;
                updateStatusList.add(new Status("Unknown instance", StatusCodeMsg.ERR, "Unknown unit " + unitDTO.getUri()));
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
     * Vérifie les données et met à jour le triplestore
     * @param unitsDTO
     * @return POSTResultsReturn le résultat de la tentative de modification des données
     */
    public POSTResultsReturn checkAndUpdate(List<UnitDTO> unitsDTO) {
        POSTResultsReturn checkResult = check(unitsDTO);
        if (checkResult.getDataState()) {
            return update(unitsDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }
}

