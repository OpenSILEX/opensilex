//**********************************************************************************************
//                                       VariableDaoSesame.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: November, 16 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Skos;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.resources.dto.VariableDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Method;
import phis2ws.service.view.model.phis.OntologyReference;
import phis2ws.service.view.model.phis.Trait;
import phis2ws.service.view.model.phis.Unit;
import phis2ws.service.view.model.phis.Variable;

/**
 * This class is a DAO for annotation.
 * It manages operation on variables in the triplestore.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VariableDaoSesame extends DAOSesame<Variable> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(VariableDaoSesame.class);
    
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

    public VariableDaoSesame() {
        
    }
    
    /**
     * Generates the search query for the variables. Search by uri, label, 
     * comment, trait uri, method uri and unit uri
     * @return the generated query
     * @example
     * SELECT DISTINCT  ?uri  ?label  ?comment  ?trait  ?method  ?unit 
     * WHERE {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/variables> {
     *          ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.phenome-fppn.fr/vocabulary/2017#Variable> . 
     *          ?uri  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . 
     *          OPTIONAL {
     *              ?uri <http://www.w3.org/2000/01/rdf-schema#comment> ?comment . 
     *          }
     *          ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasTrait>  ?trait  . 
     *          ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasMethod>  ?method  . 
     *          ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasUnit>  ?unit  . 
     *      }
     * }
     * LIMIT 20 
     * OFFSET 40 
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:todo
        //Ajouter la recherche par référence vers d'autres ontologies aussi
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
        query.appendTriplet(variableURI, Rdf.RELATION_TYPE.toString(), Vocabulary.CONCEPT_VARIABLE.toString(), null);
        
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
            query.appendTriplet(variableURI, Vocabulary.RELATION_HAS_TRAIT.toString(), trait, null);
        } else {
            query.appendSelect(" ?trait");
            query.appendTriplet(variableURI, Vocabulary.RELATION_HAS_TRAIT.toString(), "?trait", null);
        }
        
        if (method != null) {
            query.appendTriplet(variableURI, Vocabulary.RELATION_HAS_METHOD.toString(), method, null);
        } else {
            query.appendSelect(" ?method");
            query.appendTriplet(variableURI, Vocabulary.RELATION_HAS_METHOD.toString(), "?method", null);
        }
        
        if (unit != null) {
            query.appendTriplet(variableURI, Vocabulary.RELATION_HAS_UNIT.toString(), unit, null);
        } else {
            query.appendSelect(" ?unit");
            query.appendTriplet(variableURI, Vocabulary.RELATION_HAS_UNIT.toString(), "?unit", null);
        }
        
        query.appendLimit(getPageSize());
        query.appendOffset(getPage() * getPageSize());
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * prepare a query to get the higher id of the variables
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?uri", Rdf.RELATION_TYPE.toString(), Vocabulary.CONCEPT_VARIABLE.toString(), null);
        query.appendOrderBy("DESC(?" + URI + ")");
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
     * Count query generated by the searched parameters : uri, label, comment, unit uri, trait uri, method uri
     * @example
     * SELECT DISTINCT  (COUNT(DISTINCT ?uri) AS ?count) 
     * WHERE {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/variables> { 
     *          ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.phenome-fppn.fr/vocabulary/2017#Variable> . 
     *          ?uri  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . 
     *          OPTIONAL {
     *              ?uri <http://www.w3.org/2000/01/rdf-schema#comment> ?comment . 
     *          }
     *          ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasTrait>  ?trait  . 
     *          ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasMethod>  ?method  . 
     *          ?uri  <http://www.phenome-fppn.fr/vocabulary/2017#hasUnit>  ?unit  . 
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
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Count the number of variables by the given searched params : uri, label, 
     * comment, unit uri, trait uri, method uri
     * @return The number of variables 
     * @inheritdoc
     */
    @Override
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
        
        for (VariableDTO variableDTO : variablesDTO) {
            //On vérifie que le trait, la méthode et l'unité sont bien dans la base de données
            if (!existUri(variableDTO.getMethod()) 
                   || !existUri(variableDTO.getTrait())
                   || !existUri(variableDTO.getUnit())) {
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Unknown trait(" + variableDTO.getTrait() + ") or method (" + variableDTO.getMethod() + ") or unit (" + variableDTO.getUnit() + ")"));
            } else {
                //Vérification des relations d'ontologies reference
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
     * Prepare update query for variable
     * 
     * @param variable
     * @return update request
     */    
    private UpdateRequest prepareInsertQuery(VariableDTO variable) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());

        Node variableConcept = NodeFactory.createURI(Vocabulary.CONCEPT_VARIABLE.toString());
        Resource variableUri = ResourceFactory.createResource(variable.getUri());

        spql.addInsert(graph, variableUri, RDF.type, variableConcept);
        spql.addInsert(graph, variableUri, RDFS.label, variable.getLabel());
        
        if (variable.getComment() != null) {
            spql.addInsert(graph, variableUri, RDFS.comment, variable.getComment());
        }

        Property relationTrait = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_TRAIT.toString());
        Property relationMethod = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_METHOD.toString());
        Property relationUnit = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_UNIT.toString());
        
        Node trait = NodeFactory.createURI(variable.getTrait());
        Node method = NodeFactory.createURI(variable.getMethod());
        Node unit = NodeFactory.createURI(variable.getUnit());
        
        spql.addInsert(graph, variableUri, relationTrait, trait);
        spql.addInsert(graph, variableUri, relationMethod, method);
        spql.addInsert(graph, variableUri, relationUnit, unit);
        
        for (OntologyReference ontologyReference : variable.getOntologiesReferences()) {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addInsert(graph, variableUri, ontologyProperty, ontologyObject);
            Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
            spql.addInsert(graph, ontologyObject, RDFS.seeAlso, seeAlso);
        }
        
        return spql.buildRequest();
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
        final Iterator<VariableDTO> iteratorVariablesDTO = variablesDTO.iterator();      
        
        while (iteratorVariablesDTO.hasNext() && annotationInsert) {
            VariableDTO variableDTO = iteratorVariablesDTO.next();
            
            variableDTO.setUri(uriGenerator.generateNewInstanceUri(Vocabulary.CONCEPT_VARIABLE.toString(), null, null));

            //Enregistrement dans le triplestore
            UpdateRequest spqlInsert = prepareInsertQuery(variableDTO);
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
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
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
                
                TraitDaoSesame traitDaoSesame = new TraitDaoSesame();
                if (trait != null) {
                    traitDaoSesame.uri = trait;
                } else {
                    traitDaoSesame.uri = bindingSet.getValue(TRAIT).stringValue();
                }
                
                MethodDaoSesame methodDaoSesame = new MethodDaoSesame();
                if (method != null) {
                    methodDaoSesame.uri = method;
                } else {
                    methodDaoSesame.uri = bindingSet.getValue(METHOD).stringValue();
                }
                
                UnitDaoSesame unitDaoSesame = new UnitDaoSesame();
                if (unit != null) {
                    unitDaoSesame.uri = unit;
                } else {
                    unitDaoSesame.uri = bindingSet.getValue(UNIT).stringValue();
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
    
    /**
     * Prepare delete query for variable
     * 
     * @param variable
     * @return delete request
     */
    private UpdateRequest prepareDeleteQuery(Variable variable){
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        Resource variableUri = ResourceFactory.createResource(variable.getUri());
        
        spql.addDelete(graph, variableUri, RDFS.label, variable.getLabel());
        spql.addDelete(graph, variableUri, RDFS.comment, variable.getComment());
        
        Property relationTrait = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_TRAIT.toString());
        Property relationMethod = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_METHOD.toString());
        Property relationUnit = ResourceFactory.createProperty(Vocabulary.RELATION_HAS_UNIT.toString());
        
        Node traitUri = NodeFactory.createURI(variable.getTrait().getUri());
        Node methodUri = NodeFactory.createURI(variable.getMethod().getUri());
        Node unitUri = NodeFactory.createURI(variable.getUnit().getUri());        
        
        spql.addDelete(graph, variableUri, relationTrait, traitUri);
        spql.addDelete(graph, variableUri, relationMethod, methodUri);
        spql.addDelete(graph, variableUri, relationUnit, unitUri);
        
        for (OntologyReference ontologyReference : variable.getOntologiesReferences()) {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addDelete(graph, variableUri, ontologyProperty, ontologyObject);
            if (ontologyReference.getSeeAlso() != null) {
                Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
                spql.addDelete(graph, ontologyObject, RDFS.seeAlso, seeAlso);
            }
        }
                
        return spql.buildRequest();        
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
                UpdateRequest deleteQuery = prepareDeleteQuery(variablesCorresponding.get(0));

                //2. Insertion des nouvelles données
                UpdateRequest queryInsert = prepareInsertQuery(variableDTO);
                 try {
                        // début de la transaction : vérification de la requête
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
    
    /**
     * Generates a query to know if a given object uri is a variable.
     * @param uri
     * @example 
     * ASK { 
     *   <http://www.phenome-fppn.fr/id/variables/v001>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *   ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.phenome-fppn.fr/vocabulary/2017#Variable> . 
     * }
     * @return the query
     */
    private SPARQLQueryBuilder prepareIsVariableQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_VARIABLE.toString(), null);
        
        query.appendAsk("");
        LOGGER.debug(query.toString());
        return query;
    }
    
    /**
     * Check if a given uri is a variable.
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
     * Check if a given uri is a variable
     * @param uri
     * @return true if the uri corresponds to a variable
     *         false if it does not exist or if it is not a variable
     */
    public boolean existAndIsVariable(String uri) {
        if (existUri(uri)) {
            return isVariable(uri);
            
        } else {
            return false;
        }
    }
}
