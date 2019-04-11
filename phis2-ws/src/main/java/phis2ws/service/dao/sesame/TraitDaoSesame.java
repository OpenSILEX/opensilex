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
import phis2ws.service.resources.dto.TraitDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.OntologyReference;
import phis2ws.service.view.model.phis.Trait;

public class TraitDaoSesame extends DAOSesame<Trait> {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitDaoSesame.class);

    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();
    
    private static final String VAR_URI = "varUri";
    
    public TraitDaoSesame() {
    }

    public TraitDaoSesame(String uri) {
        this.uri = uri;
    }
    
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:todo
        //Ajouter la recherche par référence vers d'autres ontologies aussi
        //\SILEX:todo
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(Contexts.VARIABLES.toString());
        String traitURI;
        if (uri != null) {
            traitURI = "<" + uri + ">";
        } else {
            traitURI = "?" + URI;
            query.appendSelect("?" + URI);
        }
        query.appendTriplet(traitURI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_TRAIT.toString(), null);
        
        if (label != null) {
            query.appendTriplet(traitURI, Rdfs.RELATION_LABEL.toString(),"\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?label");
            query.appendTriplet(traitURI, Rdfs.RELATION_LABEL.toString(), "?label", null);
        }
        
        if (comment != null) {
            query.appendTriplet(traitURI, Rdfs.RELATION_COMMENT.toString(), "\"" + comment + "\"", null);
        } else {
            query.appendSelect(" ?" + COMMENT);
            query.beginBodyOptional();
            query.appendToBody(traitURI + " <" + Rdfs.RELATION_COMMENT.toString() + "> " + "?" + COMMENT + " . ");
            query.endBodyOptional();           
        }
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * prepare a query to get the higher id of the traits
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_TRAIT.toString(), null);
        query.appendOrderBy("DESC(?uri)");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * get the higher id of the traits
     * @return the id
     */
    public int getLastId() {
       SPARQLQueryBuilder query = prepareGetLastId(); 

        //get last trait uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();
        
        String uriTrait = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriTrait = bindingSet.getValue(URI).stringValue();
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
        
        //Vérification des traits
        for (TraitDTO traitDTO : traitsDTO) {
            //Vérification des relations d'ontologies de référence
            for (OntologyReference ontologyReference : traitDTO.getOntologiesReferences()) {
                if (!ontologyReference.getProperty().equals(Skos.RELATION_EXACT_MATCH.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_CLOSE_MATCH.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_NARROWER.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_BROADER.toString())) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
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
     * Prepare update query for trait
     * 
     * @param traitDTO
     * @return update request
     */
    private UpdateRequest prepareInsertQuery(TraitDTO traitDTO) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        
        Node traitConcept = NodeFactory.createURI(Oeso.CONCEPT_TRAIT.toString());
        Resource traitUri = ResourceFactory.createResource(traitDTO.getUri());

        spql.addInsert(graph, traitUri, RDF.type, traitConcept);
        spql.addInsert(graph, traitUri, RDFS.label, traitDTO.getLabel());
        
        if (traitDTO.getComment() != null) {
            spql.addInsert(graph, traitUri, RDFS.comment, traitDTO.getComment());
        }
        
        for (OntologyReference ontologyReference : traitDTO.getOntologiesReferences()) {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addInsert(graph, traitUri, ontologyProperty, ontologyObject);
            Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
            spql.addInsert(graph, ontologyObject, RDFS.seeAlso, seeAlso);
        }
        
        return spql.buildRequest();
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
        final Iterator<TraitDTO> iteratorTraitDTO = traitsDTO.iterator();
        
        while (iteratorTraitDTO.hasNext() && annotationInsert) {
            TraitDTO traitDTO = iteratorTraitDTO.next();
            try {
                traitDTO.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_TRAIT.toString(), null, null));
            } catch (Exception ex) { //In the traits case, no exception should be raised
                annotationInsert = false;
            }
            //Enregistrement dans le triplestore
            UpdateRequest spqlInsert = prepareInsertQuery(traitDTO);
            
            try {
                //SILEX:test
                //Toute la notion de connexion au triplestore sera à revoir.
                //C'est un hot fix qui n'est pas propre
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
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
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
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
                    trait.setUri(bindingSet.getValue(URI).stringValue());
                }
                
                if (label != null) {
                    trait.setLabel(label);
                } else {
                    trait.setLabel(bindingSet.getValue(LABEL).stringValue());
                }
                
                if (comment != null) {
                    trait.setComment(comment);
                } else if (bindingSet.getValue(COMMENT) != null) {
                    trait.setComment(bindingSet.getValue(COMMENT).stringValue());
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
    
    /**
     * Prepare delete query for trait
     * 
     * @param trait
     * @return delete request
     */
    private UpdateRequest prepareDeleteQuery(Trait trait) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        Resource traitUri = ResourceFactory.createResource(trait.getUri());
        
        spql.addDelete(graph, traitUri, RDFS.label, trait.getLabel());
        spql.addDelete(graph, traitUri, RDFS.comment, trait.getComment());
        
        for (OntologyReference ontologyReference : trait.getOntologiesReferences()) {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addDelete(graph, traitUri, ontologyProperty, ontologyObject);
            if (ontologyReference.getSeeAlso() != null) {
                Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
                spql.addDelete(graph, ontologyObject, RDFS.seeAlso, seeAlso);
            }
        }
                
        return spql.buildRequest();        
    }
    
    private POSTResultsReturn updateAndReturnPOSTResultsReturn(List<TraitDTO> traitsDTO) {
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
                UpdateRequest deleteQuery = prepareDeleteQuery(traitsCorresponding.get(0));

                //2. Insertion des nouvelles données
                UpdateRequest queryInsert = prepareInsertQuery(traitDTO);
                 try {
                        // début de la transaction : vérification de la requête
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery.toString());
                        LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        prepareDelete.execute();
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareUpdate.execute();

                        updatedResourcesURIList.add(traitDTO.getUri());
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        annotationUpdate = false;
                        updateStatusList.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
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
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, updatedResourcesURIList.size() + " resources updated"));
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
            return updateAndReturnPOSTResultsReturn(traitsDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }
    
    /**
     * Query generated by the searched parameter above (traitDbId) 
     * @example 
     * SELECT DISTINCT ?varUri
     * WHERE {
     * ?varUri <http://www.opensilex.org/vocabulary/oeso#hasTrait> http://www.phenome-fppn.fr/platform/id/traits/t001 .}
     *
     * @param traitURI
     * @return query generated with the searched parameter above
     */    
    protected SPARQLQueryBuilder prepareSearchQueryVariables(String traitURI) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + VAR_URI);
        query.appendTriplet("?" + VAR_URI, Oeso.RELATION_HAS_TRAIT.toString(),traitURI, null);   
        return query;
    }    
    
    /**
     * Get the Variables associated to the traits
     * @author Alice Boizet <alice.boizet@inra.fr>
     * @param trait 
     * @return traits list of traits
     */    
    public ArrayList<String> getVariableFromTrait(Trait trait) {                
        SPARQLQueryBuilder query = prepareSearchQueryVariables(trait.getUri());
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<String> varList = new ArrayList();
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                varList.add(bindingSet.getValue(VAR_URI).stringValue());
            }                    
        }
        return varList;
    } 

    @Override
    public List<Trait> create(List<Trait> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Trait> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Trait> update(List<Trait> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Trait find(Trait object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Trait findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
