//******************************************************************************
//                                 TraitDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 17 Nov. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static opensilex.service.dao.VariableDAO.OBJECT;
import static opensilex.service.dao.VariableDAO.PROPERTY;
import static opensilex.service.dao.VariableDAO.SEE_ALSO;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.OntologyReference;
import opensilex.service.model.Trait;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Skos;
import opensilex.service.resource.dto.TraitDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trait DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Vincent Migot] 17 July 2019: Update getLastId method to fix bug and limitation in URI generation
 */
public class TraitDAO extends Rdf4jDAO<Trait> {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitDAO.class);

    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();
    
    private static final String VAR_URI = "varUri";
    private static final String MAX_ID = "maxID";
    
    public TraitDAO() {
    }

    public TraitDAO(String uri) {
        this.uri = uri;
    }
    
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:todo
        // Add the search by ontology reference
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
     * Prepares a query to get the higher id of the traits.
     * @example
     * <pre>
     * SELECT ?maxID WHERE {
     *   ?uri a <http://www.opensilex.org/vocabulary/oeso#Trait>
     *   BIND(xsd:integer>(strafter(str(?uri), "http://www.opensilex.org/diaphen/id/traits/t")) AS ?maxID)
     * }
     * ORDER BY DESC(?maxID)
     * LIMIT 1
     * </pre>
     * @return 
     */
    private Query prepareGetLastId() {
        SelectBuilder query = new SelectBuilder();
        
        Var uri = makeVar(URI);
        Var maxID = makeVar(MAX_ID);
        
        // Select the highest identifier
        query.addVar(maxID);
        
        // Filter by trait
        Node traitConcept = NodeFactory.createURI(Oeso.CONCEPT_TRAIT.toString());
        query.addWhere(uri, RDF.type, traitConcept);
        
        // Binding to extract the last part of the URI as a MAX_ID integer
        ExprFactory expr = new ExprFactory();
        Expr indexBinding =  expr.function(
            XSD.integer.getURI(), 
            ExprList.create(Arrays.asList(
                expr.strafter(expr.str(uri), UriGenerator.PLATFORM_URI_ID_TRAITS))
            )
        );
        query.addBind(indexBinding, maxID);
        
        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID,  Query.ORDER_DESCENDING));
        query.setLimit(1);
        
        return query.build();
    }
    
    /**
     * Gets the higher id of the traits.
     * @return the id
     */
    public int getLastId() {
       Query query = prepareGetLastId(); 

        //get last trait uri ID inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value maxId = bindingSet.getValue(MAX_ID);
            if (maxId != null) {
                return Integer.valueOf(maxId.stringValue());
            }
        } 
        
        return 0;
    }
    
    /**
     * Check traits.
     * @param traitsDTO
     * @return 
     */
    public POSTResultsReturn check(List<TraitDTO> traitsDTO) {
        //Résultats attendus
        POSTResultsReturn traitsCheck;
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
     * Prepares update query for trait.
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
        
        traitDTO.getOntologiesReferences().forEach((ontologyReference) -> {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addInsert(graph, traitUri, ontologyProperty, ontologyObject);
            Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
            spql.addInsert(graph, ontologyObject, RDFS.seeAlso, seeAlso);
        });
        
        return spql.buildRequest();
    }
    
    /**
     * Creates traits.The data check has to be done previously.
     * @param traitsDTO
     * @return Creation result
     */
    public POSTResultsReturn insert(List<TraitDTO> traitsDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURI = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false;
        boolean annotationInsert = true;
        
        final Iterator<TraitDTO> iteratorTraitDTO = traitsDTO.iterator();
        
        while (iteratorTraitDTO.hasNext() && annotationInsert) {
            TraitDTO traitDTO = iteratorTraitDTO.next();
            try {
                traitDTO.setUri(UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_TRAIT.toString(), null, null));
            } catch (Exception ex) {
                annotationInsert = false;
            }
            
            // Register
            UpdateRequest spqlInsert = prepareInsertQuery(traitDTO);
            
            try {
                /*//SILEX:todo
                Connection te review. Dirty hot fix.
                */
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
     * @param uri
     * @return ontology references list
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
     * @return traits found
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
                
                // Get ontology references list 
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
     * Prepares delete query for trait.
     * @param trait
     * @return delete request
     */
    private UpdateRequest prepareDeleteQuery(Trait trait) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        Resource traitUri = ResourceFactory.createResource(trait.getUri());
        
        spql.addDelete(graph, traitUri, RDFS.label, trait.getLabel());
        if (trait.getComment() != null) {
            spql.addDelete(graph, traitUri, RDFS.comment, trait.getComment());
        }
        
        trait.getOntologiesReferences().forEach((ontologyReference) -> {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addDelete(graph, traitUri, ontologyProperty, ontologyObject);
            if (ontologyReference.getSeeAlso() != null) {
                Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
                spql.addDelete(graph, ontologyObject, RDFS.seeAlso, seeAlso);
            }
        });
                
        return spql.buildRequest();        
    }
    
    private POSTResultsReturn updateAndReturnPOSTResultsReturn(List<TraitDTO> traitsDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        for (TraitDTO traitDTO : traitsDTO) {
            //1. Delete existing data
            //1.1 Get information that will be modified (to delete the right triplets)
            uri = traitDTO.getUri();
            ArrayList<Trait> traitsCorresponding = allPaginate();
            if (traitsCorresponding.size() > 0) {
                UpdateRequest deleteQuery = prepareDeleteQuery(traitsCorresponding.get(0));

                //2. Insert new data
                UpdateRequest queryInsert = prepareInsertQuery(traitDTO);
                 try {
                        // Transaction start: check request
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
            // Rollback
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
     * Check data and create them in the storage.
     * @param traitsDTO
     * @return Creation result
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
     * Check data and update them in the storage.
     * @param traitsDTO
     * @return Update result
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
     * Query generated by the searched parameter above (traitDbId).
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
     * Gets the variables associated to the traits.
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
    public List<Trait> create(List<Trait> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Trait> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Trait> update(List<Trait> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Trait find(Trait object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Prepare query to get a trait by it's URI
     * @example
     * SELECT DISTINCT   ?label ?comment ?property ?object ?seeAlso WHERE {
     * GRAPH <http://www.phenome-fppn.fr/diaphen/variables> { 
     *      <http://www.phenome-fppn.fr/diaphen/id/traits/t001>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.opensilex.org/vocabulary/oeso#Trait> . 
     *      <http://www.phenome-fppn.fr/diaphen/id/traits/t001>  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . 
     *      OPTIONAL {
     *          <http://www.phenome-fppn.fr/diaphen/id/traits/t001> <http://www.w3.org/2000/01/rdf-schema#comment> ?comment . 
     *      }
     *      OPTIONAL {
     *          <http://www.phenome-fppn.fr/diaphen/id/traits/t001> ?property ?object . 
     *          ?object <http://www.w3.org/2000/01/rdf-schema#seeAlso> ?seeAlso .  
     *          FILTER (?property IN(<http://www.w3.org/2008/05/skos#closeMatch>, <http://www.w3.org/2008/05/skos#exactMatch>, <http://www.w3.org/2008/05/skos#narrower>, <http://www.w3.org/2008/05/skos#broader>)) 
     *      } 
     *  }}
     * @param uri
     * @return 
     */
    private SPARQLQueryBuilder prepareSearchByUri(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendGraph(Contexts.VARIABLES.toString());
        
        String labelURI = "<" + uri + ">";
        query.appendTriplet(labelURI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_TRAIT.toString(), null);
        
        query.appendSelect(" ?" + LABEL + " ?" + COMMENT + " ?" + PROPERTY + " ?" + OBJECT + " ?" + SEE_ALSO);
        
        //Label
        query.appendTriplet(labelURI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        
        //Comment
        query.beginBodyOptional();
        query.appendToBody(labelURI + " <" + Rdfs.RELATION_COMMENT.toString() + "> " + "?" + COMMENT + " . ");
        query.endBodyOptional();
        
        //Ontologies references
        query.appendOptional(labelURI + " ?" + PROPERTY + " ?" + OBJECT + " . "                
                + "?" + OBJECT + " <" + Rdfs.RELATION_SEE_ALSO.toString() + "> ?" + SEE_ALSO + " . "
                + " FILTER (?" + PROPERTY + " IN(<" + Skos.RELATION_CLOSE_MATCH.toString() + ">, <"
                                           + Skos.RELATION_EXACT_MATCH.toString() + ">, <"
                                           + Skos.RELATION_NARROWER.toString() + ">, <"
                                           + Skos.RELATION_BROADER.toString() + ">))");
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;
    }

    /**
     * Map binding set value to OntologyReference object
     * @param bindingSet
     * @return 
     */
    private OntologyReference getOntologyReferenceFromBindingSet(BindingSet bindingSet) {
        if (bindingSet.getValue(OBJECT) != null
                    && bindingSet.getValue(PROPERTY) != null) {
            OntologyReference ontologyReference = new OntologyReference();
            ontologyReference.setObject(bindingSet.getValue(OBJECT).toString());
            ontologyReference.setProperty(bindingSet.getValue(PROPERTY).toString());
            if (bindingSet.getValue(SEE_ALSO) != null) {
                ontologyReference.setSeeAlso(bindingSet.getValue(SEE_ALSO).toString());
            }
            return ontologyReference;
        }
        return null;
    }
    
    /**
     * Return Trait corresponding to given id
     * 
     * @param id
     * @return
     * @throws DAOPersistenceException
     * @throws Exception 
     */
    @Override
    public Trait findById(String id) throws DAOPersistenceException, Exception {
        SPARQLQueryBuilder query = prepareSearchByUri(id);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        Trait trait = new Trait();
        trait.setUri(id);
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet row = result.next();

                if (trait.getLabel() == null && row.getValue(LABEL) != null) {
                    trait.setLabel(row.getValue(LABEL).stringValue());
                }

                if (trait.getComment() == null && row.getValue(COMMENT) != null) {
                    trait.setComment(row.getValue(COMMENT).stringValue());
                }

                OntologyReference ontologyReference = getOntologyReferenceFromBindingSet(row);
                if (ontologyReference != null) {
                    trait.addOntologyReference(ontologyReference);
                }
            }
        }
        return trait;
    }

    @Override
    public void validate(List<Trait> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
