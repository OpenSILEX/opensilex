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
import java.util.logging.Level;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Skos;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.resources.dto.MethodDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
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
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(Contexts.VARIABLES.toString());
        String methodUri;
        
        if (uri != null) {
            methodUri = "<" + uri + ">";
        } else {
            methodUri = "?uri";
            query.appendSelect("?uri");
        }
        
        query.appendTriplet(methodUri, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_METHOD.toString(), null);
        
        if (label != null) {
            query.appendTriplet(methodUri, Rdfs.RELATION_LABEL.toString(),"\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?" + LABEL);
            query.appendTriplet(methodUri, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        }
        
        if (comment != null) {
            query.appendTriplet(methodUri, Rdfs.RELATION_COMMENT.toString(), "\"" + comment + "\"", null);
        } else {
            query.appendSelect(" ?" + COMMENT);
            query.beginBodyOptional();
            query.appendToBody(methodUri + " <" + Rdfs.RELATION_COMMENT.toString() + "> " + "?" + COMMENT + " . ");
            query.endBodyOptional();
        }
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());

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
        
        //Vérification des méthodes
        for (MethodDTO methodDTO : methodsDTO) {
            //Vérification des relations d'ontologies de référence
            for (OntologyReference ontologyReference : methodDTO.getOntologiesReferences()) {
                if (!ontologyReference.getProperty().equals(Skos.RELATION_EXACT_MATCH.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_CLOSE_MATCH.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_NARROWER.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_BROADER.toString())) {
                    dataOk = false;
                    //SILEX:todo
                    //create a java beans validator for this check
                    //\SILEX:todo
                    
                    checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, 
                            "Bad property relation given. Must be one of the following : " + Skos.RELATION_EXACT_MATCH.toString()
                            + ", " + Skos.RELATION_CLOSE_MATCH.toString()
                            + ", " + Skos.RELATION_NARROWER.toString()
                            + ", " + Skos.RELATION_BROADER.toString()
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
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_METHOD.toString(), null);
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
    
    /**
     * Prepare update query for method
     * 
     * @param methodDTO
     * @return update request
     */
    private UpdateRequest prepareInsertQuery(MethodDTO methodDTO) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        
        Node methodConcept = NodeFactory.createURI(Oeso.CONCEPT_METHOD.toString());
        Resource methodUri = ResourceFactory.createResource(methodDTO.getUri());

        spql.addInsert(graph, methodUri, RDF.type, methodConcept);
        spql.addInsert(graph, methodUri, RDFS.label, methodDTO.getLabel());
        
        if (methodDTO.getComment() != null) {
            spql.addInsert(graph, methodUri, RDFS.comment, methodDTO.getComment());
        }
        
        for (OntologyReference ontologyReference : methodDTO.getOntologiesReferences()) {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addInsert(graph, methodUri, ontologyProperty, ontologyObject);
            Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
            spql.addInsert(graph, ontologyObject, RDFS.seeAlso, seeAlso);
        }
        
        return spql.buildRequest();
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
        
        final Iterator<MethodDTO> iteratorMethodDTO = methodsDTO.iterator();
        
        while (iteratorMethodDTO.hasNext() && annotationInsert) {
            MethodDTO methodDTO = iteratorMethodDTO.next();
            try {
                methodDTO.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_METHOD.toString(), null, null));
            } catch (Exception ex) { //In the method case, no exception should be raised
                annotationInsert = false;
            }
            
            //Enregistrement dans le triplestore
            UpdateRequest spqlInsert = prepareInsertQuery(methodDTO);
            
            try {
                //SILEX:test
                //Toute la notion de connexion au triplestore sera à revoir.
                //C'est un hot fix qui n'est pas propre
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
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
                    insertStatusList.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed insertion query: " + e.getMessage()));
            } 
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatusList;
        results.setCreatedResources(createdResourcesURI);
        if (resultState && !createdResourcesURI.isEmpty()) {
            results.createdResources = createdResourcesURI;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesURI.size() + " new resource(s) created."));
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
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(Contexts.VARIABLES.toString());
        
        if (ontologiesReferences.isEmpty()) {
            query.appendSelect(" ?property ?object ?seeAlso");
            query.appendTriplet(uri, "?property", "?object", null);
            query.appendOptional("{?object <" + Rdfs.RELATION_SEE_ALSO.toString() + "> ?seeAlso}");
            query.appendFilter("?property IN(<" + Skos.RELATION_CLOSE_MATCH.toString() + ">, <"
                                               + Skos.RELATION_EXACT_MATCH.toString() + ">, <"
                                               + Skos.RELATION_NARROWER.toString() + ">, <"
                                               + Skos.RELATION_BROADER.toString() + ">)");
        } else {
            for (OntologyReference ontologyReference : ontologiesReferences) {
                query.appendTriplet(uri, ontologyReference.getProperty(), ontologyReference.getObject(), null);
                query.appendTriplet(ontologyReference.getObject(), Rdfs.RELATION_SEE_ALSO.toString(), ontologyReference.getSeeAlso(), null);
            }
        }
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
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
    
    /**
     * Prepare delete query for method
     * 
     * @param method
     * @return delete request
     */
    private UpdateRequest prepareDeleteQuery(Method method) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        Resource methodUri = ResourceFactory.createResource(method.getUri());
        
        spql.addDelete(graph, methodUri, RDFS.label, method.getLabel());
        spql.addDelete(graph, methodUri, RDFS.comment, method.getComment());
        
        for (OntologyReference ontologyReference : method.getOntologiesReferences()) {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addDelete(graph, methodUri, ontologyProperty, ontologyObject);
            if (ontologyReference.getSeeAlso() != null) {
                Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
                spql.addDelete(graph, ontologyObject, RDFS.seeAlso, seeAlso);
            }
        }
                
        return spql.buildRequest();        
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
                UpdateRequest deleteQuery = prepareDeleteQuery(methodsCorresponding.get(0));

                //2. Insertion des nouvelles données
                UpdateRequest queryInsert = prepareInsertQuery(methodDTO);
                 try {
                        // début de la transaction : vérification de la requête
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        prepareDelete.execute();
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
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
