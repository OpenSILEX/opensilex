//**********************************************************************************************
//                                       VariableDaoSesame.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 16 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 16 2017
// Subject: A specific DAO to retreive data on variables
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
import phis2ws.service.resources.dto.VariableDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Method;
import phis2ws.service.view.model.phis.OntologyReference;
import phis2ws.service.view.model.phis.Trait;
import phis2ws.service.view.model.phis.Unit;
import phis2ws.service.view.model.phis.Variable;

public class VariableDaoSesame extends DAOSesame<Variable> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(VariableDaoSesame.class);
    
    public String trait;
    public String method;
    public String unit;
    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();

    public VariableDaoSesame() {
        
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
        String variableURI;
        if (uri != null) {
            variableURI = "<" + uri + ">";
        } else {
            variableURI = "?uri";
            query.appendSelect("?uri");
        }
        query.appendTriplet(variableURI, "rdf:type", uriNamespaces.getObjectsProperty("cVariable"), null);
        
        if (label != null) {
            query.appendTriplet(variableURI, "rdfs:label","\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?label");
            query.appendTriplet(variableURI, "rdfs:label", "?label", null);
        }
        
        if (comment != null) {
            query.appendTriplet(variableURI, "rdfs:comment", "\"" + comment + "\"", null);
        } else {
            query.appendSelect(" ?comment");
            query.appendTriplet(variableURI, "rdfs:comment", " ?comment", null);
        }
        
        if (trait != null) {
            query.appendTriplet(variableURI, uriNamespaces.getRelationsProperty("rHasTrait"), trait, null);
        } else {
            query.appendSelect(" ?trait");
            query.appendTriplet(variableURI, uriNamespaces.getRelationsProperty("rHasTrait"), "?trait", null);
        }
        
        if (method != null) {
            query.appendTriplet(variableURI, uriNamespaces.getRelationsProperty("rHasMethod"), method, null);
        } else {
            query.appendSelect(" ?method");
            query.appendTriplet(variableURI, uriNamespaces.getRelationsProperty("rHasMethod"), "?method", null);
        }
        
        if (unit != null) {
            query.appendTriplet(variableURI, uriNamespaces.getRelationsProperty("rHasUnit"), unit, null);
        } else {
            query.appendSelect(" ?unit");
            query.appendTriplet(variableURI, uriNamespaces.getRelationsProperty("rHasUnit"), "?unit", null);
        }
        
        LOGGER.trace("sparql select query : " + query.toString());
        return query;
    }
    
    /**
     * prepare a query to get the higher id of the variables
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        URINamespaces uriNamespace = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", uriNamespace.getRelationsProperty("type"), uriNamespace.getObjectsProperty("cVariable"), null);
        query.appendOrderBy("desc(?uri)");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * get the higher id of the variables
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

        //get last variable uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        //SILEX:test
        //For the pool connection problems
        getConnection().commit();
        getConnection().close();
        //\SILEX:test
        
        String uriVariable = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriVariable = bindingSet.getValue("uri").stringValue();
        }
        
        if (uriVariable == null) {
            return 0;
        } else {
            String split = "variables/v";
            String[] parts = uriVariable.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Vérifie si les variables sont correctes
     * @param variablesDTO
     * @return 
     */
    public POSTResultsReturn check(List<VariableDTO> variablesDTO)  {
        //Résultats attendus
        POSTResultsReturn variablesCheck = null;
        //Liste des status retournés
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        
        URINamespaces uriNamespaces = new URINamespaces();
        
        for (VariableDTO variableDTO : variablesDTO) {
            //Vérification des variables
            if ((boolean) variableDTO.isOk().get("state")) { //Données attendues reçues
                //On vérifie que le trait, la méthode et l'unité sont bien dans la base de données
                if (!existObject(variableDTO.getMethod()) 
                       || !existObject(variableDTO.getTrait())
                       || !existObject(variableDTO.getUnit())) {
                    dataOk = false;
                    checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, "Unknown trait(" + variableDTO.getTrait() + ") or method (" + variableDTO.getMethod() + ") or unit (" + variableDTO.getUnit() + ")"));
                } else {
                    //Vérification des relations d'ontologies reference
                    for (OntologyReference ontologyReference : variableDTO.getOntologiesReferences()) {
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
            } else {
                dataOk = false;
                variableDTO.isOk().remove("state");
                checkStatusList.add(new Status("Bad data format", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(variableDTO.isOk()).toString()));
            }
        }
        
        variablesCheck = new POSTResultsReturn(dataOk, null, dataOk);
        variablesCheck.statusList = checkStatusList;
        return variablesCheck;
    }
    
    private SPARQLUpdateBuilder prepareInsertQuery(VariableDTO variable) {
        SPARQLUpdateBuilder spql = new SPARQLUpdateBuilder();
        final URINamespaces uriNamespaces = new URINamespaces();
        
        spql.appendGraphURI(uriNamespaces.getContextsProperty("variables"));
        spql.appendTriplet(variable.getUri(), "rdf:type", uriNamespaces.getObjectsProperty("cVariable"), null);
        spql.appendTriplet(variable.getUri(), "rdfs:label", "\"" + variable.getLabel() + "\"", null);
        spql.appendTriplet(variable.getUri(), "rdfs:comment", "\"" + variable.getComment() + "\"", null);
        spql.appendTriplet(variable.getUri(), uriNamespaces.getRelationsProperty("rHasTrait"), variable.getTrait(), null);
        spql.appendTriplet(variable.getUri(), uriNamespaces.getRelationsProperty("rHasMethod"), variable.getMethod(), null);
        spql.appendTriplet(variable.getUri(), uriNamespaces.getRelationsProperty("rHasUnit"), variable.getUnit(), null);
        
        for (OntologyReference ontologyReference : variable.getOntologiesReferences()) {
            spql.appendTriplet(variable.getUri(), ontologyReference.getProperty(), ontologyReference.getObject(), null);
            spql.appendTriplet(ontologyReference.getObject(), "rdfs:seeAlso", "\"" + ontologyReference.getSeeAlso() + "\"", null);
        }
        
        return spql;
    }
    
    /**
     * insère les données dans le triplestore.
     * On suppose que la vérification de leur intégrité a été faite auparavent, via l'appel à la méthode check
     * @param variablesDTO
     * @return 
     */
    public POSTResultsReturn insert(List<VariableDTO> variablesDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURIList = new ArrayList<>();
        
        POSTResultsReturn results;
        
        boolean resultState = false; //Pour savoir si les données sont bonnes et ont bien été insérées
        boolean annotationInsert = true; //Si l'insertion a bien été effectuée
        
        UriGenerator uriGenerator = new UriGenerator();
        URINamespaces uriNamespaces = new URINamespaces();
        final Iterator<VariableDTO> iteratorVariablesDTO = variablesDTO.iterator();      
        
        while (iteratorVariablesDTO.hasNext() && annotationInsert) {
            VariableDTO variableDTO = iteratorVariablesDTO.next();
            
            variableDTO.setUri(uriGenerator.generateNewInstanceUri(uriNamespaces.getObjectsProperty("cVariable"), null, null));
            
            //Enregistrement dans le triplestore
            SPARQLUpdateBuilder spqlInsert = prepareInsertQuery(variableDTO);
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

                createdResourcesURIList.add(variableDTO.getUri());

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
        results.setCreatedResources(createdResourcesURIList);
        if (resultState && !createdResourcesURIList.isEmpty()) {
            results.createdResources = createdResourcesURIList;
            results.statusList.add(new Status("Resources created", StatusCodeMsg.INFO, createdResourcesURIList.size() + " new resource(s) created."));
        }

        return results;
    }
    
    /**
     * Vérifie les données et les insère dans le triplestore
     * @param variablesDTO
     * @return POSTResultsReturn le résultat de la tentative d'insertion
     */
    public POSTResultsReturn checkAndInsert(List<VariableDTO> variablesDTO) {
        POSTResultsReturn checkResult = check(variablesDTO);
        if (checkResult.getDataState()) { 
            return insert(variablesDTO);
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
     * @return la liste des variables correspondant à la recherche
     */
    public ArrayList<Variable> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Variable> variables = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Variable variable = new Variable();
                
                if (uri != null) {
                    variable.setUri(uri);
                } else {
                    variable.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (label != null) {
                    variable.setLabel(label);
                } else {
                    variable.setLabel(bindingSet.getValue("label").stringValue());
                }
                
                if (comment != null) {
                    variable.setComment(comment);
                } else {
                    variable.setComment(bindingSet.getValue("comment").stringValue());
                }
                
                TraitDaoSesame traitDaoSesame = new TraitDaoSesame();
                if (trait != null) {
                    traitDaoSesame.uri = trait;
                } else {
                    traitDaoSesame.uri = bindingSet.getValue("trait").stringValue();
                }
                
                MethodDaoSesame methodDaoSesame = new MethodDaoSesame();
                if (method != null) {
                    methodDaoSesame.uri = method;
                } else {
                    methodDaoSesame.uri = bindingSet.getValue("method").stringValue();
                }
                
                UnitDaoSesame unitDaoSesame = new UnitDaoSesame();
                if (unit != null) {
                    unitDaoSesame.uri = unit;
                } else {
                    unitDaoSesame.uri = bindingSet.getValue("unit").stringValue();
                }
                
                //On récupère maintenant la liste des références vers des ontologies... 
                SPARQLQueryBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(variable.getUri());
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
                        
                        variable.addOntologyReference(ontologyReference);
                    }
                }
                
                //On récupère les informations du trait, de la méthode et de l'unité
                ArrayList<Trait> traits = traitDaoSesame.allPaginate();
                ArrayList<Method> methods = methodDaoSesame.allPaginate();
                ArrayList<Unit> units = unitDaoSesame.allPaginate();
                variable.setTrait(traits.get(0));
                variable.setMethod(methods.get(0));
                variable.setUnit(units.get(0));
                
                variables.add(variable);
            }
        }
        
        return variables;
    }
    
    private String prepareDeleteQuery(Variable variable) {
        URINamespaces uriNamespaces = new URINamespaces();
        String deleteQuery;
        deleteQuery = "DELETE WHERE {"
                + "<" + variable.getUri() + "> rdfs:label \"" + variable.getLabel() + "\" . "
                + "<" + variable.getUri() + "> rdfs:comment \"" + variable.getComment() + "\" . "
                + "<" + variable.getUri() + "> <" + uriNamespaces.getRelationsProperty("rHasTrait") + "> <" + variable.getTrait() + "> . "
                + "<" + variable.getUri() + "> <" + uriNamespaces.getRelationsProperty("rHasMethod") + "> <" + variable.getMethod() + "> . "
                + "<" + variable.getUri() + "> <" + uriNamespaces.getRelationsProperty("rHasUnit") + "> <" + variable.getUnit() + "> . ";

        for (OntologyReference ontologyReference : variable.getOntologiesReferences()) {
            deleteQuery += "<" + variable.getUri() + "> <" + ontologyReference.getProperty() + "> <" + ontologyReference.getObject() + "> . ";
            if (ontologyReference.getSeeAlso() != null) {
                deleteQuery += "<" + ontologyReference.getObject() + "> rdfs:seeAlso " + ontologyReference.getSeeAlso() + " . ";
            }
        }

        deleteQuery += "}";
                
        return deleteQuery;
    }
    
    private POSTResultsReturn update(List<VariableDTO> variablesDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true; // Si l'insertion a bien été réalisée
        boolean resultState = false; // Pour savoir si les données étaient bonnes et on bien été mises à jour
        
        for (VariableDTO variableDTO : variablesDTO) {
            //1. Suppression des données déjà existantes
            //1.1 Récupération des infos qui seront modifiées (pour supprimer les bons triplets)
            uri = variableDTO.getUri();
            ArrayList<Variable> variablesCorresponding = allPaginate();
            if (variablesCorresponding.size() > 0) {
                String deleteQuery = prepareDeleteQuery(variablesCorresponding.get(0));

                //2. Insertion des nouvelles données
                SPARQLUpdateBuilder queryInsert = prepareInsertQuery(variableDTO);
                 try {
                        // début de la transaction : vérification de la requête
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareDelete.execute();
                        prepareUpdate.execute();

                        updatedResourcesURIList.add(variableDTO.getUri());
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        annotationUpdate = false;
                        updateStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                    }   
            } else {
                annotationUpdate = false;
                updateStatusList.add(new Status("Unknown instance", StatusCodeMsg.ERR, "Unknown variable " + variableDTO.getUri()));
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
     * @param variablesDTO
     * @return POSTResultsReturn le résultat de la tentative de modification des données
     */
    public POSTResultsReturn checkAndUpdate(List<VariableDTO> variablesDTO) {
        POSTResultsReturn checkResult = check(variablesDTO);
        if (checkResult.getDataState()) {
            return update(variablesDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }
}
