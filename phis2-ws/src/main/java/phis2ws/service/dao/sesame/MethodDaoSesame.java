//**********************************************************************************************
//                                       MethodDaoSesame.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 17 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 17 2017
// Subject: A specific DAO to retrieve data on methods
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.MethodDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Method;
import phis2ws.service.view.model.phis.OntologyReference;

public class MethodDaoSesame extends DAOSesame<Method> {

    final static Logger LOGGER = LoggerFactory.getLogger(MethodDaoSesame.class);
    
    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();

    public MethodDaoSesame() {
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
        String methodUri;
        if (uri != null) {
            methodUri = "<" + uri + ">";
        } else {
            methodUri = "?uri";
            query.appendSelect("?uri");
        }
        query.appendTriplet(methodUri, "rdf:type", uriNamespaces.getObjectsProperty("cMethod"), null);
        
        if (label != null) {
            query.appendTriplet(methodUri, "rdfs:label","\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?label");
            query.appendTriplet(methodUri, "rdfs:label", "?label", null);
        }
        
        if (comment != null) {
            query.appendTriplet(methodUri, "rdfs:comment", "\"" + comment + "\"", null);
        } else {
            query.appendSelect(" ?" + COMMENT);
            query.beginBodyOptional();
            query.appendToBody(methodUri + " " + TRIPLESTORE_RELATION_COMMENT + " " + "?" + COMMENT + " . ");
            query.endBodyOptional();
        }
        
        LOGGER.debug("sparql select query : " + query.toString());
        return query;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Vérifie si les méthodes sont corrects
     * @param methodsDTO
     * @return 
     */
    public POSTResultsReturn check(List<MethodDTO> methodsDTO) {
        //Résultats attendus
        POSTResultsReturn traitsCheck = null;
        //Liste des status retournés
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        URINamespaces uriNamespaces = new URINamespaces();
        
        //Vérification des méthodes
        for (MethodDTO methodDTO : methodsDTO) {
            //Vérification des relations d'ontologies de référence
            for (OntologyReference ontologyReference : methodDTO.getOntologiesReferences()) {
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
        }
        
        traitsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        traitsCheck.statusList = checkStatusList;
        return traitsCheck;
    }
    
    /**
     * prepare a query to get the higher id of the methods
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        URINamespaces uriNamespace = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", uriNamespace.getRelationsProperty("type"), uriNamespace.getObjectsProperty("cMethod"), null);
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * get the higher id of the methods
     * @return the id
     */
    public int getLastId() {
        SPARQLQueryBuilder query = prepareGetLastId();

        //get last method uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();
        
        String uriMethod = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriMethod = bindingSet.getValue("uri").stringValue();
        }
        
        if (uriMethod == null) {
            return 0;
        } else {
            String split = "methods/m";
            String[] parts = uriMethod.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }
    
    private SPARQLUpdateBuilder prepareInsertQuery(MethodDTO methodDTO) {
        SPARQLUpdateBuilder spql = new SPARQLUpdateBuilder();
        final URINamespaces uriNamespaces = new URINamespaces();
        
        spql.appendGraphURI(uriNamespaces.getContextsProperty("variables"));
        spql.appendTriplet(methodDTO.getUri(), "rdf:type", uriNamespaces.getObjectsProperty("cMethod"), null);
        spql.appendTriplet(methodDTO.getUri(), "rdfs:label", "\"" + methodDTO.getLabel() + "\"", null);
        if (methodDTO.getComment() != null) {
           spql.appendTriplet(methodDTO.getUri(), "rdfs:comment", "\"" + methodDTO.getComment() + "\"", null); 
        }
        
        for (OntologyReference ontologyReference : methodDTO.getOntologiesReferences()) {
            spql.appendTriplet(methodDTO.getUri(), ontologyReference.getProperty(), ontologyReference.getObject(), null);
            spql.appendTriplet(ontologyReference.getObject(), "rdfs:seeAlso", "\"" + ontologyReference.getSeeAlso() + "\"", null);
        }
        
        return spql;
    }
    
    /**
     * insère les données dans le triplestore
     * On suppose que la vérification de leur intégrité a été faite auparavent, via l'appel à la méthode check
     * @param methodsDTO
     * @return 
     */
    public POSTResultsReturn insert(List<MethodDTO> methodsDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURI = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false; //Pour savoir si les données sont bonnes et ont bien été insérées
        boolean annotationInsert = true; //Si l'insertion a bien été faite
        
        UriGenerator uriGenerator = new UriGenerator();
        URINamespaces uriNamespaces = new URINamespaces();
        
        final Iterator<MethodDTO> iteratorMethodDTO = methodsDTO.iterator();
        
        while (iteratorMethodDTO.hasNext() && annotationInsert) {
            MethodDTO methodDTO = iteratorMethodDTO.next();
            methodDTO.setUri(uriGenerator.generateNewInstanceUri(uriNamespaces.getObjectsProperty("cMethod"), null, null));
            
            //Enregistrement dans le triplestore
            SPARQLUpdateBuilder spqlInsert = prepareInsertQuery(methodDTO);
            
            try {
                //SILEX:test
                //Toute la notion de connexion au triplestore sera à revoir.
                //C'est un hot fix qui n'est pas propre
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                prepareUpdate.execute();
                //\SILEX:test

                createdResourcesURI.add(methodDTO.getUri());

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
     * @param methodsDTO
     * @return POSTResultsReturn le résultat de la tentative d'insertion
     */
    public POSTResultsReturn checkAndInsert(List<MethodDTO> methodsDTO) {
        POSTResultsReturn checkResult = check(methodsDTO);
        if (checkResult.getDataState()) {
            return insert(methodsDTO);
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
     * @return la liste des méthodes correspondant à la recherche
     */
    public ArrayList<Method> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Method> methods = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Method method = new Method();
                
                if (uri != null) {
                    method.setUri(uri);
                } else {
                    method.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (label != null) {
                    method.setLabel(label);
                } else {
                    method.setLabel(bindingSet.getValue("label").stringValue());
                }
                
                if (comment != null) {
                    method.setComment(comment);
                } else if (bindingSet.getValue(COMMENT) != null) {
                    method.setComment(bindingSet.getValue(COMMENT).stringValue());
                }
                
                //On récupère maintenant la liste des références vers des ontologies... 
                SPARQLQueryBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(method.getUri());
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
                        
                        method.addOntologyReference(ontologyReference);
                    }
                }
                
                methods.add(method);
            }
        }
        
        return methods;
    }
    
    private String prepareDeleteQuery(Method method) {
        String deleteQuery;
        deleteQuery = "DELETE WHERE {"
                + "<" + method.getUri() + "> rdfs:label \"" + method.getLabel() + "\" . "
                + "<" + method.getUri() + "> rdfs:comment \"" + method.getComment() + "\" . ";

        for (OntologyReference ontologyReference : method.getOntologiesReferences()) {
            deleteQuery += "<" + method.getUri() + "> <" + ontologyReference.getProperty() + "> <" + ontologyReference.getObject() + "> . ";
            if (ontologyReference.getSeeAlso() != null) {
                deleteQuery += "<" + ontologyReference.getObject() + "> rdfs:seeAlso " + ontologyReference.getSeeAlso() + " . ";
            }
        }

        deleteQuery += "}";
                
        return deleteQuery;
    }
    
    private POSTResultsReturn update(List<MethodDTO> methodsDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true; // Si l'insertion a bien été réalisée
        boolean resultState = false; // Pour savoir si les données étaient bonnes et on bien été mises à jour
        
        for (MethodDTO methodDTO : methodsDTO) {
            //1. Suppression des données déjà existantes
            //1.1 Récupération des infos qui seront modifiées (pour supprimer les bons triplets)
            uri = methodDTO.getUri();
            ArrayList<Method> methodsCorresponding = allPaginate();
            if (methodsCorresponding.size() > 0) {
                String deleteQuery = prepareDeleteQuery(methodsCorresponding.get(0));

                //2. Insertion des nouvelles données
                SPARQLUpdateBuilder queryInsert = prepareInsertQuery(methodDTO);
                 try {
                        // début de la transaction : vérification de la requête
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareDelete.execute();
                        prepareUpdate.execute();

                        updatedResourcesURIList.add(methodDTO.getUri());
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        annotationUpdate = false;
                        updateStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                    }   
            } else {
                annotationUpdate = false;
                updateStatusList.add(new Status("Unknown instance", StatusCodeMsg.ERR, "Unknown method " + methodDTO.getUri()));
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
     * @param methodsDTO
     * @return POSTResultsReturn le résultat de la tentative de modification des données
     */
    public POSTResultsReturn checkAndUpdate(List<MethodDTO> methodsDTO) {
        POSTResultsReturn checkResult = check(methodsDTO);
        if (checkResult.getDataState()) {
            return update(methodsDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }
}
