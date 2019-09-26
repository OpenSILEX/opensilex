//******************************************************************************
//                                FactorDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 12 août 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/charlero/GIT/GITLAB/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static opensilex.service.dao.MethodDAO.LOGGER;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Factor;
import opensilex.service.model.OntologyReference;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Skos;
import opensilex.service.resource.dto.factor.FactorDTO;
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
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
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
import org.slf4j.LoggerFactory;

/**
 *
 * @author charlero
 */
public class FactorDAO extends Rdf4jDAO<Factor> {

    final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FactorDAO.class);
    private static final String MAX_ID = "maxID";

    /**
     * Search all the factors corresponding to the search params given.
     *
     * @param uri
     * @param label
     * @param language
     * @return the list of the factors.
     */
    public ArrayList<Factor> findAll(String uri, String label, String language) {
        SelectBuilder query = null;
        try {
            query = prepareSearchQuery(uri, label, language);

        } catch (ParseException ex) {
            Logger.getLogger(FactorDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        ArrayList<Factor> factors = new ArrayList<>();

        if (query == null) {
            return factors;
        }
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.buildString());
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + query.buildString());

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Factor factor = getFactorFromBindingSet(bindingSet, uri, label);
                if (uri != null) {
                    factor.setUri(uri);
                }
                factors.add(factor);
            }
        }
        return factors;
    }

    private ArrayList<OntologyReference> getOntologyReferences(String uri) {
        ArrayList<OntologyReference> ontologyReferences = new ArrayList<>();

        SelectBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(uri);
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

    /**
     * @param uri
     * @return the ontology references links
     */
    private SelectBuilder prepareSearchOntologiesReferencesQuery(String uri) {
        SelectBuilder query = new SelectBuilder();

        query.setDistinct(true);
        Node graph = NodeFactory.createURI(Contexts.FACTORS.toString());

        query.addVar(PROPERTY_SELECT_NAME_SPARQL);
        query.addVar(OBJECT_SELECT_NAME_SPARQL);
        query.addVar(SEE_ALSO_SELECT_NAME_SPARQL);
        query.addGraph(graph, NodeFactory.createURI(uri), PROPERTY_SELECT_NAME_SPARQL, OBJECT_SELECT_NAME_SPARQL);
        query.addOptional(OBJECT_SELECT_NAME_SPARQL, RDFS.seeAlso, SEE_ALSO_SELECT_NAME_SPARQL);

        try {
            query.addFilter("?property IN(<" + Skos.RELATION_CLOSE_MATCH.toString() + ">, <"
                    + Skos.RELATION_EXACT_MATCH.toString() + ">, <"
                    + Skos.RELATION_NARROWER.toString() + ">, <"
                    + Skos.RELATION_BROADER.toString() + ">)");
        } catch (ParseException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        LOGGER.debug(SPARQL_QUERY  + query.toString());
        return query;
    }

    /**
     * Generates an insert query for factors.
     *
     * @example INSERT DATA { GRAPH
     * <http://www.opensilex.org/opensilex/set/factors> {
     * <http://www.opensilex.org/opensilex/2019/f001> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opensilex.org/vocabulary/oeso#Factor> .
     * <http://www.opensilex.org/opensilex/2019/f001> <http://www.w3.org/2000/01/rdf-schema#label> "Water exposure" .
     * <http://www.opensilex.org/opensilex/2019/f001> <http://www.w3.org/2000/01/rdf-schema#comment> "Change frequency of irrigation" . 
     * } }
     * @param factor
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(FactorDTO factor) {
        UpdateBuilder spql = new UpdateBuilder();

        Node graph = NodeFactory.createURI(Contexts.FACTORS.toString());

        Resource factorUri = ResourceFactory.createResource(factor.getUri());

        Node factorType = NodeFactory.createURI(Oeso.CONCEPT_FACTOR.toString());

        spql.addInsert(graph, factorUri, RDF.type, factorType);
        spql.addInsert(graph, factorUri, RDFS.label, factor.getLabel());
        
        if (factor.getComment() != null) {
             spql.addInsert(graph, factorUri, RDFS.comment, factor.getComment());
        }
     
        factor.getOntologiesReferences().forEach((ontologyReference) -> {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addInsert(graph, factorUri, ontologyProperty, ontologyObject);
            Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
            spql.addInsert(graph, ontologyObject, RDFS.seeAlso, seeAlso);
        });
        
        UpdateRequest query = spql.buildRequest();
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + query.toString());
        return query;
    }

    protected SelectBuilder searchQueryTemplate(String uri, String label, String language) throws ParseException {
        SelectBuilder query = new SelectBuilder();

        query.setDistinct(true);

        //URI filter
        if (uri == null) {
            query.addWhere("?" + URI, RDF.type, NodeFactory.createURI(Oeso.CONCEPT_FACTOR.toString()));
        } else {
            query.addWhere(NodeFactory.createURI(uri), RDF.type, NodeFactory.createURI(Oeso.CONCEPT_FACTOR.toString()));
        }

        //Label filter
        if (label == null) {
            query.addOptional("?" + URI, RDFS.label, "?" + LABEL);
        } else {
            query.addWhere("?" + URI, RDFS.label, "?" + LABEL);
            query.addFilter("REGEX ( str(?" + LABEL + "),\".*" + label + ".*\",\"i\")");
        }
        query.addFilter("LANG(?" + LABEL + ") = \"\" || LANGMATCHES(LANG(?" + LABEL + "), \"" + language + "\")");
        query.addFilter("LANG(?" + COMMENT + ") = \"\" || LANGMATCHES(LANG(?" + COMMENT + "), \"" + language + "\")");

        if (this.page != null && this.pageSize != null) {
            query.setLimit(this.pageSize);
            query.setOffset(this.page * this.pageSize);
        }

        return query;
    }

    protected SelectBuilder prepareSearchQuery(String uri, String label, String language) throws ParseException {
        SelectBuilder query = searchQueryTemplate(uri, label, language);
        if (uri == null) {
            query.addVar("?" + URI);
        }
        query.addVar("?" + LABEL);
        query.addVar("?" + COMMENT);
        query.addWhere("?" + URI, RDFS.comment, "?" + COMMENT);
        LOGGER.debug(SPARQL_QUERY + query.buildString());
        return query;
    }

    protected SelectBuilder prepareCountQuery(String uri, String label, String language) throws ParseException {
        SelectBuilder query = searchQueryTemplate(uri, label, language);
        query.addVar("count(*)", "?" + COUNT_ELEMENT_QUERY);
        LOGGER.debug(SPARQL_QUERY + query.buildString());
        return query;
    }

    /**
     * Get an factor from a binding set given resulting from a query to the
     * triplestore.
     *
     * @param bindingSet
     * @param uri
     * @param label
     * @return
     */
    private Factor getFactorFromBindingSet(BindingSet bindingSet, String uri, String label) {
        Factor factor = new Factor();

        if (bindingSet.getValue(URI) != null) {
            factor.setUri(bindingSet.getValue(URI).stringValue());
            ArrayList<OntologyReference> ontologyReferences = getOntologyReferences(factor.getUri());
            factor.setOntologiesReferences(ontologyReferences);
        }

        if (bindingSet.getValue(LABEL) != null) {
            factor.setLabel(bindingSet.getValue(LABEL).stringValue());
        }
        if (bindingSet.getValue(COMMENT) != null) {
            factor.setComment(bindingSet.getValue(COMMENT).stringValue());
        }
        return factor;
    }

    /**
     * Counts the query result with the given filter.
     *
     * @param uri
     * @param label
     * @param language
     * @return The number of results for a given filter.
     */
    public int countWithFilter(String uri, String label, String language) {
        Integer count = 0;
        SelectBuilder query = null;
        try {
            query = prepareCountQuery(uri, label, language);
        } catch (ParseException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        if (query != null) {
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.buildString());

            try (TupleQueryResult result = tupleQuery.evaluate()) {
                if (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
                }
            }
        }
        return count;
    }

    /**
     * Checks the integrity of the objects and create them in the storage.
     *
     * @param factorDTO
     * @return the result
     */
    public POSTResultsReturn checkAndInsert(List<FactorDTO> factorDTO) {
        POSTResultsReturn checkResult = check(factorDTO);
        if (checkResult.getDataState()) {
            return insert(factorDTO);
        } else {
            return checkResult;
        }
    }

    /**
     * Check if the objects are valid.
     *
     * @param factorsDTO
     * @return
     */
    public POSTResultsReturn check(List<FactorDTO> factorsDTO) {
        //Résultats attendus
        POSTResultsReturn traitsCheck;
        //Liste des status retournés
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;

        //Vérification des unités
        for (FactorDTO factorDTO : factorsDTO) {
            //Vérification des relations d'ontologies de référence
            for (OntologyReference ontologyReference : factorDTO.getOntologiesReferences()) {
                if (!ontologyReference.getProperty().equals(Skos.RELATION_EXACT_MATCH.toString())
                        && !ontologyReference.getProperty().equals(Skos.RELATION_CLOSE_MATCH.toString())
                        && !ontologyReference.getProperty().equals(Skos.RELATION_NARROWER.toString())
                        && !ontologyReference.getProperty().equals(Skos.RELATION_BROADER.toString())) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR,
                            "Bad property relation given. Must be one of the following : "
                            + Skos.RELATION_EXACT_MATCH.toString()
                            + ", " + Skos.RELATION_CLOSE_MATCH.toString()
                            + ", " + Skos.RELATION_NARROWER.toString()
                            + ", " + Skos.RELATION_BROADER.toString()
                            + ". Given : " + ontologyReference.getProperty()));
                }
            }
        }

        traitsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        traitsCheck.statusList = checkStatusList;
        return traitsCheck;
    }

    /**
     * Create objects. The objects integrity must have been checked previously.
     *
     * @param factorsDTO
     * @return
     */
    public POSTResultsReturn insert(List<FactorDTO> factorsDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURI = new ArrayList<>();

        POSTResultsReturn results;
        boolean resultState = false;
        boolean annotationInsert = true;

        final Iterator<FactorDTO> iteratorFactorDTO = factorsDTO.iterator();

        while (iteratorFactorDTO.hasNext() && annotationInsert) {
            FactorDTO factorDTO = iteratorFactorDTO.next();
            try {
                factorDTO.setUri(UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_FACTOR.toString(), null, factorDTO.getLabel()));
            } catch (Exception ex) { //In the unit case, no exception should be raised
                annotationInsert = false;
            }
            // Register
            UpdateRequest spqlInsert = prepareInsertQuery(factorDTO);

            try {
                //SILEX:todo
                // Connection to review. Dirty hotfix.
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                prepareUpdate.execute();
                //\SILEX:todo

                createdResourcesURI.add(factorDTO.getUri());

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
                insertStatusList.add(new Status(
                        StatusCodeMsg.QUERY_ERROR,
                        StatusCodeMsg.ERR,
                        "Malformed insertion query: " + e.getMessage()));
            }
        }

        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatusList;
        results.setCreatedResources(createdResourcesURI);
        if (resultState && !createdResourcesURI.isEmpty()) {
            results.createdResources = createdResourcesURI;
            results.statusList.add(new Status(
                    StatusCodeMsg.RESOURCES_CREATED,
                    StatusCodeMsg.INFO,
                    createdResourcesURI.size() + " new resource(s) created."));
        }

        return results;
    }

    /**
     * Gets the higher id of the variables.
     *
     * @return the id
     */
    public int getLastId() {
        Query query = prepareGetLastId();

        //get last variable uri ID inserted
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
     * Prepares a query to get the higher id of the variables.
     *
     * @return
     * @example      <pre>
     * SELECT ?maxID WHERE {
     *   ?uri a <http://www.opensilex.org/vocabulary/oeso#Factor>
     *   BIND(xsd:integer>(strafter(str(?uri), "http://www.opensilex.org/diaphen/id/factor/f")) AS ?maxID)
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
        Node methodConcept = NodeFactory.createURI(Oeso.CONCEPT_FACTOR.toString());
        query.addWhere(uri, RDF.type, methodConcept);

        // Binding to extract the last part of the URI as a MAX_ID integer
        ExprFactory expr = new ExprFactory();
        Expr indexBinding = expr.function(
                XSD.integer.getURI(),
                ExprList.create(Arrays.asList(
                        expr.strafter(expr.str(uri), UriGenerator.PLATFORM_URI_ID_FACTORS))
                )
        );
        query.addBind(indexBinding, maxID);

        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID, Query.ORDER_DESCENDING));
        query.setLimit(1);

        return query.build();
    }

    @Override
    public List<Factor> create(List<Factor> factors) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Factor> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Factor> update(List<Factor> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Factor find(Factor object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Factor findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Factor> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
