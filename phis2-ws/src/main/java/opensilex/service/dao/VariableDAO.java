//******************************************************************************
//                              VariableDAO.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 16 Nov. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.NotFoundException;
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
import opensilex.service.model.BrapiMethod;
import opensilex.service.model.BrapiScale;
import opensilex.service.model.BrapiVariable;
import opensilex.service.model.BrapiVariableTrait;
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
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.vocabulary.XSD;

/**
 * Variable DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Vincent Migot] 17 July 2019: Update getLastId method to fix bug and limitation in URI generation
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
    protected static final String SKOS_RELATION = "skosRelation";
    protected static final String OBJECT = "object";
    protected static final String SEE_ALSO = "seeAlso";
    protected static final String PROPERTY = "property";

    private static final String MAX_ID = "maxID";
        
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
     * @example
     * <pre>
     * SELECT ?maxID WHERE {
     *   ?uri a <http://www.opensilex.org/vocabulary/oeso#Variable>
     *   BIND(xsd:integer>(strafter(str(?uri), "http://www.opensilex.org/diaphen/id/variables/v")) AS ?maxID)
     * }
     * ORDER BY DESC(?maxID)
     * LIMIT 1
     * </pre>
     */
    private Query prepareGetLastId() {
        SelectBuilder query = new SelectBuilder();
        
        Var uri = makeVar(URI);
        Var maxID = makeVar(MAX_ID);
        
        // Select the highest identifier
        query.addVar(maxID);
        
        // Filter by variable
        Node methodConcept = NodeFactory.createURI(Oeso.CONCEPT_VARIABLE.toString());
        query.addWhere(uri, RDF.type, methodConcept);
        
        // Binding to extract the last part of the URI as a MAX_ID integer
        ExprFactory expr = new ExprFactory();
        Expr indexBinding =  expr.function(
            XSD.integer.getURI(), 
            ExprList.create(Arrays.asList(
                expr.strafter(expr.str(uri), UriGenerator.PLATFORM_URI_ID_VARIABLES))
            )
        );
        query.addBind(indexBinding, maxID);
        
        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID,  Query.ORDER_DESCENDING));
        query.setLimit(1);
        
        return query.build();
    }
    
    /**
     * Gets the higher id of the variables.
     * @return the id
     */
    public int getLastId() {
        Query query = prepareGetLastId();

        //get last variable uri ID inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            return Integer.valueOf(bindingSet.getValue(MAX_ID).stringValue());
        } else {
            return 0;
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
        
        final Iterator<VariableDTO> iteratorVariablesDTO = variablesDTO.iterator();      
        
        while (iteratorVariablesDTO.hasNext() && annotationInsert) {
            VariableDTO variableDTO = iteratorVariablesDTO.next();
            
            try {
                variableDTO.setUri(UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIABLE.toString(), null, null));
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
     * @example
     *  SELECT DISTINCT   ?property ?object ?seeAlso WHERE {
     *  GRAPH <http://www.phenome-fppn.fr/diaphen/variables> { 
     *      <http://www.phenome-fppn.fr/diaphen/id/variables/v001>  ?property  ?object  . 
     *      OPTIONAL {
     *          {?object <http://www.w3.org/2000/01/rdf-schema#seeAlso> ?seeAlso} 
     *      } 
     *      FILTER ( (?property IN(<http://www.w3.org/2008/05/skos#closeMatch>, <http://www.w3.org/2008/05/skos#exactMatch>, <http://www.w3.org/2008/05/skos#narrower>, <http://www.w3.org/2008/05/skos#broader>)) ) 
     *  }}
     * @return ontology references
     */
    private SPARQLQueryBuilder prepareSearchOntologiesReferencesQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(Contexts.VARIABLES.toString());
        
        query.appendSelect(" ?" + PROPERTY + " ?" + OBJECT + " ?" + SEE_ALSO);
        query.appendTriplet(uri, "?" + PROPERTY, "?" + OBJECT, null);
        query.appendOptional("{?" + OBJECT + " <" + Rdfs.RELATION_SEE_ALSO.toString() + "> ?" + SEE_ALSO + "}");
        query.appendFilter("?" + PROPERTY + " IN(<" + Skos.RELATION_CLOSE_MATCH.toString() + ">, <"
                                           + Skos.RELATION_EXACT_MATCH.toString() + ">, <"
                                           + Skos.RELATION_NARROWER.toString() + ">, <"
                                           + Skos.RELATION_BROADER.toString() + ">)");
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    public ArrayList<Variable> getAll(boolean usePagination, boolean withTraitMethodUnit) {
        SPARQLQueryBuilder query = prepareSearchQuery();
        
        if (!usePagination) {
            query.clearLimit();
            query.clearOffset();
        }
                
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
                
                if (withTraitMethodUnit) {
                    try {
                        //Get method informations
                        variable.setMethod(methodDao.findById(methodDao.uri));
                        
                        //Get unit informations
                        variable.setUnit(unitDao.findById(unitDao.uri));

                        //Get trait informations
                        variable.setTrait(traitDao.findById(traitDao.uri));

                    } catch (Exception ex) {
                        // Ignore trait method and units not found
                    }

                }
                variables.add(variable);
            }
        }
        
        return variables;
    }
    
    /**
     * @return objects found
     */
    public ArrayList<Variable> allPaginate() {
        return getAll(true, false);
    }
    
    /**
     * @return objects found
     */
    public ArrayList<Variable> allPaginateDetails() {
        return getAll(true, true);
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
        if (variable.getComment() != null) {
            spql.addDelete(graph, variableUri, RDFS.comment, variable.getComment());
        }
        
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
    
    private POSTResultsReturn updateAndReturnPOSTResult(List<VariableDTO> variablesDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        for (VariableDTO variableDTO : variablesDTO) {
            try {
                //1. Delete existing data
                //1.1 Get information to modify (to delete the right triplets)
                Variable variableCorresponding = findById(variableDTO.getUri());
                UpdateRequest deleteQuery = prepareDeleteQuery(variableCorresponding);

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
            } catch (Exception ex) {
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
            return updateAndReturnPOSTResult(variablesDTO);
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
    
    /**
     * Get the list of brapi variables from the the DAO
     * @return the list of brapi variables
     */
    public ArrayList<BrapiVariable> getBrapiVarData() {
        ArrayList<Variable> variablesList = this.allPaginate();
        ArrayList<BrapiVariable> varList = new ArrayList();
        for (Variable var:variablesList) {
            try {
                Variable details = findById(var.getUri());
                varList.add(getBrapiVariable(details)); 
            } catch (Exception ex) {
                // Ignore variable not found;
            }

        }
        return varList;    
    }
    
    public BrapiVariable getBrapiVariable(Variable var) {
        BrapiVariable brapiVar = new BrapiVariable();
        brapiVar.setObservationVariableDbId(var.getUri());
        brapiVar.setObservationVariableName(var.getLabel());
        brapiVar.setContextOfUse(new ArrayList());
        brapiVar.setSynonyms(new ArrayList());

        //trait 
        BrapiVariableTrait trait = new BrapiVariableTrait();
        trait.setTraitDbId(var.getTrait().getUri());
        trait.setTraitName(var.getTrait().getLabel());
        trait.setDescription(var.getTrait().getComment());
        trait.setAlternativeAbbreviations(new ArrayList());
        trait.setSynonyms(new ArrayList());
        brapiVar.setTrait(trait);

        //method
        BrapiMethod method = new BrapiMethod();
        method.setMethodDbId(var.getMethod().getUri());
        method.setMethodName(var.getMethod().getLabel());
        method.setDescription(var.getMethod().getComment());
        brapiVar.setMethod(method);

        //scale
        BrapiScale scale = new BrapiScale();
        scale.setScaleDbid(var.getUnit().getUri());
        scale.setScaleName(var.getUnit().getLabel());
        scale.setDataType("Numerical");
        brapiVar.setScale(scale);
        
        return brapiVar;
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
    
    private Variable getVariableFromBindingSet(BindingSet bindingSet) {
        Variable variable = new Variable();
        
        if (bindingSet.getValue(URI) != null) {
            variable.setUri(bindingSet.getValue(URI).stringValue());
        }
        
        if (bindingSet.getValue(LABEL) != null) {
            variable.setLabel(bindingSet.getValue(LABEL).stringValue());
        }
        
        if (bindingSet.getValue(COMMENT) != null) {
            variable.setComment(bindingSet.getValue(COMMENT).stringValue());
        }
        
        if (bindingSet.getValue(TRAIT) != null) {
            Trait foundedTrait = new Trait(bindingSet.getValue(TRAIT).stringValue());
            variable.setTrait(foundedTrait);
        }
        
        if (bindingSet.getValue(METHOD) != null) {
            Method foundedMethod = new Method(bindingSet.getValue(METHOD).stringValue());
            variable.setMethod(foundedMethod);
        }
        
        if (bindingSet.getValue(UNIT) != null) {
            Unit foundedUnit = new Unit(bindingSet.getValue(UNIT).stringValue());
            variable.setUnit(foundedUnit);
        }
        
        return variable;
    }
    
    /**
     * Return searchQuery for variable by uri
     * @example 
     *   SELECT DISTINCT   ?label ?comment ?trait ?method ?unit WHERE {
     *   GRAPH <http://www.phenome-fppn.fr/diaphen/variables> { 
     *      <http://www.phenome-fppn.fr/diaphen/id/variables/v004>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.opensilex.org/vocabulary/oeso#Variable> . 
     *      <http://www.phenome-fppn.fr/diaphen/id/variables/v004>  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . 
     *      OPTIONAL {
     *          <http://www.phenome-fppn.fr/diaphen/id/variables/v004> <http://www.w3.org/2000/01/rdf-schema#comment> ?comment . 
     *      }
     *      <http://www.phenome-fppn.fr/diaphen/id/variables/v004>  <http://www.opensilex.org/vocabulary/oeso#hasTrait>  ?trait  . 
     *      <http://www.phenome-fppn.fr/diaphen/id/variables/v004>  <http://www.opensilex.org/vocabulary/oeso#hasMethod>  ?method  . 
     *      <http://www.phenome-fppn.fr/diaphen/id/variables/v004>  <http://www.opensilex.org/vocabulary/oeso#hasUnit>  ?unit  . 
     * }}
     * @param uri
     * @return 
     */
    protected SPARQLQueryBuilder prepareSearchByUri(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendGraph(Contexts.VARIABLES.toString());
        
        String variableURI = "<" + uri + ">";
        query.appendTriplet(variableURI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_VARIABLE.toString(), null);
        
        query.appendSelect(" ?" + LABEL + " ?" + COMMENT + " ?" + TRAIT + " ?" + METHOD + " ?" + UNIT);
        
        //Label
        query.appendTriplet(variableURI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        
        //Comment
        query.beginBodyOptional();
        query.appendToBody(variableURI + " <" + Rdfs.RELATION_COMMENT.toString() + "> " + "?" + COMMENT + " . ");
        query.endBodyOptional();
                
        //Trait
        query.appendTriplet(variableURI, Oeso.RELATION_HAS_TRAIT.toString(), "?" + TRAIT, null);
        
        //Method
        query.appendTriplet(variableURI, Oeso.RELATION_HAS_METHOD.toString(), "?" + METHOD, null);
        
        //Unit
        query.appendTriplet(variableURI, Oeso.RELATION_HAS_UNIT.toString(), "?" + UNIT, null);
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;
    }
    
    /**
     * Get the ontology references of a given uri.
     * @param uri
     * @return 
     */
    private ArrayList<OntologyReference> getOntologyReferences(String uri) {
        ArrayList<OntologyReference> ontologyReferences = new ArrayList<>();
        
        SPARQLQueryBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(uri);
        TupleQuery tupleQueryOntologiesReferences = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryOntologiesReferences.toString());
        TupleQueryResult resultOntologiesReferences = tupleQueryOntologiesReferences.evaluate();
        while (resultOntologiesReferences.hasNext()) {
            BindingSet bindingSetOntologiesReferences = resultOntologiesReferences.next();
            if (bindingSetOntologiesReferences.getValue(OBJECT) != null
                    && bindingSetOntologiesReferences.getValue(PROPERTY) != null) {
                OntologyReference ontologyReference = new OntologyReference();
                ontologyReference.setObject(bindingSetOntologiesReferences.getValue(OBJECT).toString());
                ontologyReference.setProperty(bindingSetOntologiesReferences.getValue(PROPERTY).toString());
                if (bindingSetOntologiesReferences.getValue(SEE_ALSO) != null) {
                    ontologyReference.setSeeAlso(bindingSetOntologiesReferences.getValue(SEE_ALSO).toString());
                }

                ontologyReferences.add(ontologyReference);
            }
        }
        
        return ontologyReferences;
    }

    @Override
    public Variable findById(String id) throws DAOPersistenceException, Exception {
        SPARQLQueryBuilder query = prepareSearchByUri(id);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        Variable variable = new Variable();
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                variable = getVariableFromBindingSet(result.next());
                variable.setOntologiesReferences(getOntologyReferences(id));
                
                //Get method informations
                MethodDAO methodDAO = new MethodDAO();
                variable.setMethod(methodDAO.findById(variable.getMethod().getUri()));
                
                //Get unit informations
                UnitDAO unitDAO = new UnitDAO();
                variable.setUnit(unitDAO.findById(variable.getUnit().getUri()));
                
                //Get trait informations
                TraitDAO traitDAO = new TraitDAO();
                variable.setTrait(traitDAO.findById(variable.getTrait().getUri()));
                
            } else {
                throw new NotFoundException(id + " not found.");
            }
            
            variable.setUri(id);
        }
        return variable;
    }

    public BrapiVariable findBrapiVariableById(String id) throws Exception {
        Variable variable = this.findById(id);
        
        return getBrapiVariable(variable);
    }
    
    @Override
    public void validate(List<Variable> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
