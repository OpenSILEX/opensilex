//******************************************************************************
//                              VariableDAO.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 16 Nov. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
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
import opensilex.service.PropertiesFileManager;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Skos;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.VariableDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.Method;
import opensilex.service.model.OntologyReference;
import opensilex.service.model.Trait;
import opensilex.service.model.Unit;
import opensilex.service.model.Variable;

/**
 * Variable DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VariableDAO extends Rdf4jDAO<Variable> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(VariableDAO.class);
    
    public String trait;
    protected static final String TRAIT = "trait";
    public String method;
    protected static final String METHOD = "method";
    public String unit;
    protected static final String UNIT = "unit";
    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();
    public String traitSKosReference;

    public VariableDAO() {
        
    }
    
    /**
     * Generates the search query for the variables.
     * @return the generated query
     * @example
     * SELECT DISTINCT  ?uri  ?label  ?comment  ?trait  ?method  ?unit 
     * WHERE {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/variables> {
     *          ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.opensilex.org/vocabulary/oeso#Variable> . 
     *          ?uri  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . 
     *          OPTIONAL {
     *              ?uri <http://www.w3.org/2000/01/rdf-schema#comment> ?comment . 
     *          }
     *          ?uri  <http://www.opensilex.org/vocabulary/oeso#hasTrait>  ?trait  . 
     *          ?uri  <http://www.opensilex.org/vocabulary/oeso#hasMethod>  ?method  . 
     *          ?uri  <http://www.opensilex.org/vocabulary/oeso#hasUnit>  ?unit  . 
     *      }
     * }
     * LIMIT 20 
     * OFFSET 40 
     */
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:todo
        // Add search by ontology reference
        //\SILEX:todo
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendGraph(Contexts.VARIABLES.toString());
        
        String variableURI;
        if (uri != null) {
            variableURI = "<" + uri + ">";
        } else {
            variableURI = "?" + URI;
            query.appendSelect("?" + URI);
        }
        query.appendTriplet(variableURI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_VARIABLE.toString(), null);
        
        if (label != null) {
            query.appendTriplet(variableURI, Rdfs.RELATION_LABEL.toString(),"\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?label");
            query.appendTriplet(variableURI, Rdfs.RELATION_LABEL.toString(), "?label", null);
        }
        
        if (comment != null) {
            query.appendTriplet(variableURI, Rdfs.RELATION_COMMENT.toString(), "\"" + comment + "\"", null);
        } else {
            query.appendSelect(" ?" + COMMENT);
            query.beginBodyOptional();
            query.appendToBody(variableURI + " <" + Rdfs.RELATION_COMMENT.toString() + "> " + "?" + COMMENT + " . ");
            query.endBodyOptional();
        }
        
        if (trait != null) {
            query.appendTriplet(variableURI, Oeso.RELATION_HAS_TRAIT.toString(), trait, null);
        } else {
            query.appendSelect(" ?trait");
            query.appendTriplet(variableURI, Oeso.RELATION_HAS_TRAIT.toString(), "?trait", null);
        }
        
        if (traitSKosReference != null){
            if( trait != null) {
                query.appendTriplet(trait, "?skosRelation", traitSKosReference, null);
            } else {
                query.appendTriplet("?trait", "?skosRelation", traitSKosReference, null);
            }
            query.appendFilter("?skosRelation IN(<" + Skos.RELATION_CLOSE_MATCH.toString() + ">, <"
                                               + Skos.RELATION_EXACT_MATCH.toString() + ">, <"
                                               + Skos.RELATION_NARROWER.toString() + ">, <"
                                               + Skos.RELATION_BROADER.toString() + ">)");
        } 
        
        if (method != null) {
            query.appendTriplet(variableURI, Oeso.RELATION_HAS_METHOD.toString(), method, null);
        } else {
            query.appendSelect(" ?method");
            query.appendTriplet(variableURI, Oeso.RELATION_HAS_METHOD.toString(), "?method", null);
        }
        
        if (unit != null) {
            query.appendTriplet(variableURI, Oeso.RELATION_HAS_UNIT.toString(), unit, null);
        } else {
            query.appendSelect(" ?unit");
            query.appendTriplet(variableURI, Oeso.RELATION_HAS_UNIT.toString(), "?unit", null);
        }
        
        query.appendLimit(getPageSize());
        query.appendOffset(getPage() * getPageSize());
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Prepares a query to get the higher id of the variables.
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?uri", Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_VARIABLE.toString(), null);
        query.appendOrderBy("DESC(?" + URI + ")");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * Gets the higher id of the variables.
     * @return the id
     */
    public int getLastId() {
        SPARQLQueryBuilder query = prepareGetLastId();
        
        //SILEX:test
        //All the triplestore connection has to been checked and updated
        //This is an unclean hot fix
        String tripleStoreServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
        String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
        rep = new HTTPRepository(tripleStoreServer, repositoryID); //Stockage triplestore
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
            uriVariable = bindingSet.getValue(URI).stringValue();
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

    /**
     * Counts query generated by the searched parameters.
     * @example
     * SELECT DISTINCT  (COUNT(DISTINCT ?uri) AS ?count) 
     * WHERE {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/variables> { 
     *          ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.opensilex.org/vocabulary/oeso#Variable> . 
     *          ?uri  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . 
     *          OPTIONAL {
     *              ?uri <http://www.w3.org/2000/01/rdf-schema#comment> ?comment . 
     *          }
     *          ?uri  <http://www.opensilex.org/vocabulary/oeso#hasTrait>  ?trait  . 
     *          ?uri  <http://www.opensilex.org/vocabulary/oeso#hasMethod>  ?method  . 
     *          ?uri  <http://www.opensilex.org/vocabulary/oeso#hasUnit>  ?unit  . 
     *      }
     * }
     * @return Query generated to count the elements, with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount() {
        SPARQLQueryBuilder query = this.prepareSearchQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Counts the number of variables by the given searched parameters.
     * @return The number of variables 
     * @inheritdoc
     */
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        SPARQLQueryBuilder prepareCount = prepareCount();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, prepareCount.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        return count;
    }
    
    /**
     * Check if objects are valid.
     * @param variablesDTO
     * @return 
     */
    public POSTResultsReturn check(List<VariableDTO> variablesDTO)  {
        POSTResultsReturn variablesCheck;
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        
        for (VariableDTO variableDTO : variablesDTO) {
            // Check the method, unit and trait exist in the storage
            if (!existUri(variableDTO.getMethod()) 
                   || !existUri(variableDTO.getTrait())
                   || !existUri(variableDTO.getUnit())) {
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Unknown trait(" + variableDTO.getTrait() + ") or method (" + variableDTO.getMethod() + ") or unit (" + variableDTO.getUnit() + ")"));
            } else {
                // Check the ontology reference relations
                for (OntologyReference ontologyReference : variableDTO.getOntologiesReferences()) {
                    if (!ontologyReference.getProperty().equals(Skos.RELATION_EXACT_MATCH.toString())
                       && !ontologyReference.getProperty().equals(Skos.RELATION_CLOSE_MATCH.toString())
                       && !ontologyReference.getProperty().equals(Skos.RELATION_NARROWER.toString())
                       && !ontologyReference.getProperty().equals(Skos.RELATION_BROADER.toString())) {
                        dataOk = false;
                        checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, 
                                "Bad property relation given. Must be one of the following : " + Skos.RELATION_EXACT_MATCH.toString()
                                + ", " + Skos.RELATION_CLOSE_MATCH.toString()
                                + ", " + Skos.RELATION_NARROWER.toString()
                                + ", " + Skos.RELATION_BROADER.toString()
                                +". Given : " + ontologyReference.getProperty()));
                    }
                }
            }
        }
        
        variablesCheck = new POSTResultsReturn(dataOk, null, dataOk);
        variablesCheck.statusList = checkStatusList;
        return variablesCheck;
    }
    
    /**
     * Prepares a update query.
     * @param variable
     * @return update request
     */    
    private UpdateRequest prepareInsertQuery(VariableDTO variable) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());

        Node variableConcept = NodeFactory.createURI(Oeso.CONCEPT_VARIABLE.toString());
        Resource variableUri = ResourceFactory.createResource(variable.getUri());

        spql.addInsert(graph, variableUri, RDF.type, variableConcept);
        spql.addInsert(graph, variableUri, RDFS.label, variable.getLabel());
        
        if (variable.getComment() != null) {
            spql.addInsert(graph, variableUri, RDFS.comment, variable.getComment());
        }

        Property relationTrait = ResourceFactory.createProperty(Oeso.RELATION_HAS_TRAIT.toString());
        Property relationMethod = ResourceFactory.createProperty(Oeso.RELATION_HAS_METHOD.toString());
        Property relationUnit = ResourceFactory.createProperty(Oeso.RELATION_HAS_UNIT.toString());
        
        Node trait = NodeFactory.createURI(variable.getTrait());
        Node method = NodeFactory.createURI(variable.getMethod());
        Node unit = NodeFactory.createURI(variable.getUnit());
        
        spql.addInsert(graph, variableUri, relationTrait, trait);
        spql.addInsert(graph, variableUri, relationMethod, method);
        spql.addInsert(graph, variableUri, relationUnit, unit);
        
        variable.getOntologiesReferences().forEach((ontologyReference) -> {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addInsert(graph, variableUri, ontologyProperty, ontologyObject);
            Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
            spql.addInsert(graph, ontologyObject, RDFS.seeAlso, seeAlso);
        });
        
        return spql.buildRequest();
    }
    
    /**
     * Creates objects in the storage.
     * The integrity check has to be done previously.
     * @param variablesDTO
     * @return 
     */
    public POSTResultsReturn insert(List<VariableDTO> variablesDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURIList = new ArrayList<>();
        
        POSTResultsReturn results;
        
        boolean resultState = false;
        boolean annotationInsert = true;
        
        UriGenerator uriGenerator = new UriGenerator();
        final Iterator<VariableDTO> iteratorVariablesDTO = variablesDTO.iterator();      
        
        while (iteratorVariablesDTO.hasNext() && annotationInsert) {
            VariableDTO variableDTO = iteratorVariablesDTO.next();
            
            try {
                variableDTO.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIABLE.toString(), null, null));
            } catch (Exception ex) { //In the variable case, no exception should be raised
                annotationInsert = false;
            }

            // Register
            UpdateRequest spqlInsert = prepareInsertQuery(variableDTO);
            try {
                //SILEX:todo
                // storage connection to review: dirty hotfix
                String tripleStoreServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
                String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
                rep = new HTTPRepository(tripleStoreServer, repositoryID); //Stockage triplestore
                rep.initialize();
                this.setConnection(rep.getConnection());
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                prepareUpdate.execute();
                //\SILEX:todo

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
                    insertStatusList.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed insertion query: " + e.getMessage()));
            } 
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatusList;
        results.setCreatedResources(createdResourcesURIList);
        if (resultState && !createdResourcesURIList.isEmpty()) {
            results.createdResources = createdResourcesURIList;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesURIList.size() + " new resource(s) created."));
        }

        return results;
    }
    
    /**
     * Checks the data integrity and inserts them in the storage.
     * @param variablesDTO
     * @return the result.
     */
    public POSTResultsReturn checkAndInsert(List<VariableDTO> variablesDTO) {
        POSTResultsReturn checkResult = check(variablesDTO);
        if (checkResult.getDataState()) { 
            return insert(variablesDTO);
        } else { // incorrect data
            return checkResult;
        }
    }
    
    /**
     * @param uri
     * @return ontology references
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
     * @return objects found
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
                    variable.setUri(bindingSet.getValue(URI).stringValue());
                }
                
                if (label != null) {
                    variable.setLabel(label);
                } else {
                    variable.setLabel(bindingSet.getValue(LABEL).stringValue());
                }
                
                if (comment != null) {
                    variable.setComment(comment);
                } else if (bindingSet.getValue(COMMENT) != null) {
                    variable.setComment(bindingSet.getValue(COMMENT).stringValue());
                }
                
                TraitDAO traitDao = new TraitDAO();
                if (trait != null) {
                    traitDao.uri = trait;
                } else {
                    traitDao.uri = bindingSet.getValue(TRAIT).stringValue();
                }
                
                MethodDAO methodDao = new MethodDAO();
                if (method != null) {
                    methodDao.uri = method;
                } else {
                    methodDao.uri = bindingSet.getValue(METHOD).stringValue();
                }
                
                UnitDAO unitDao = new UnitDAO();
                if (unit != null) {
                    unitDao.uri = unit;
                } else {
                    unitDao.uri = bindingSet.getValue(UNIT).stringValue();
                }
                
                //Get ontology references 
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
                
                // Get the trait, method and unit information
                ArrayList<Trait> traits = traitDao.allPaginate();
                ArrayList<Method> methods = methodDao.allPaginate();
                ArrayList<Unit> units = unitDao.allPaginate();
                variable.setTrait(traits.get(0));
                variable.setMethod(methods.get(0));
                variable.setUnit(units.get(0));
                
                variables.add(variable);
            }
        }
        
        return variables;
    }
    
    /**
     * Prepares delete request.
     * @param variable
     * @return delete request
     */
    private UpdateRequest prepareDeleteQuery(Variable variable){
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        Resource variableUri = ResourceFactory.createResource(variable.getUri());
        
        spql.addDelete(graph, variableUri, RDFS.label, variable.getLabel());
        spql.addDelete(graph, variableUri, RDFS.comment, variable.getComment());
        
        Property relationTrait = ResourceFactory.createProperty(Oeso.RELATION_HAS_TRAIT.toString());
        Property relationMethod = ResourceFactory.createProperty(Oeso.RELATION_HAS_METHOD.toString());
        Property relationUnit = ResourceFactory.createProperty(Oeso.RELATION_HAS_UNIT.toString());
        
        Node traitUri = NodeFactory.createURI(variable.getTrait().getUri());
        Node methodUri = NodeFactory.createURI(variable.getMethod().getUri());
        Node unitUri = NodeFactory.createURI(variable.getUnit().getUri());        
        
        spql.addDelete(graph, variableUri, relationTrait, traitUri);
        spql.addDelete(graph, variableUri, relationMethod, methodUri);
        spql.addDelete(graph, variableUri, relationUnit, unitUri);
        
        variable.getOntologiesReferences().forEach((ontologyReference) -> {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addDelete(graph, variableUri, ontologyProperty, ontologyObject);
            if (ontologyReference.getSeeAlso() != null) {
                Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
                spql.addDelete(graph, ontologyObject, RDFS.seeAlso, seeAlso);
            }
        });
                
        return spql.buildRequest();        
    }    
    
    private POSTResultsReturn AndReturnPOSTResultsReturn(List<VariableDTO> variablesDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        for (VariableDTO variableDTO : variablesDTO) {
            //1. Delete existing data
            //1.1 Get information to modify (to delete the right triplets)
            uri = variableDTO.getUri();
            ArrayList<Variable> variablesCorresponding = allPaginate();
            if (variablesCorresponding.size() > 0) {
                UpdateRequest deleteQuery = prepareDeleteQuery(variablesCorresponding.get(0));

                //2. Insert the new data
                UpdateRequest queryInsert = prepareInsertQuery(variableDTO);
                 try {
                        // transaction start: check request
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery.toString());
                        LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        prepareDelete.execute();
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareUpdate.execute();

                        updatedResourcesURIList.add(variableDTO.getUri());
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        annotationUpdate = false;
                        updateStatusList.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
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
     * Checks the objects and updates them in the storage.
     * @param variablesDTO
     * @return the result
     */
    public POSTResultsReturn checkAndUpdate(List<VariableDTO> variablesDTO) {
        POSTResultsReturn checkResult = check(variablesDTO);
        if (checkResult.getDataState()) {
            return AndReturnPOSTResultsReturn(variablesDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }
    
    /**
     * Generates a query to know if a given object URI is a variable.
     * @param uri
     * @example 
     * ASK { 
     *   <http://www.phenome-fppn.fr/id/variables/v001>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *   ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#Variable> . 
     * }
     * @return the query
     */
    private SPARQLQueryBuilder prepareIsVariableQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_VARIABLE.toString(), null);
        
        query.appendAsk("");
        LOGGER.debug(query.toString());
        return query;
    }
    
    /**
     * Checks if a given URI is a variable.
     * @param uri
     * @return true if it is a variable
     *         false if not
     */
    private boolean isVariable(String uri) {
        SPARQLQueryBuilder query = prepareIsVariableQuery(uri);
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        
        return booleanQuery.evaluate();
    }
     
    /**
     * Checks if a given URI is a variable.
     * @param uri
     * @return true if the URI corresponds to a variable
     *         false if it does not exist or if it is not a variable
     */
    public boolean existAndIsVariable(String uri) {
        if (existUri(uri)) {
            return isVariable(uri);
            
        } else {
            return false;
        }
    }

    @Override
    public List<Variable> create(List<Variable> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Variable> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Variable> update(List<Variable> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Variable find(Variable object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Variable findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Variable> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
