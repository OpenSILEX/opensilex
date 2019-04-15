//******************************************************************************
//                              DAOSesame.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.manager;

import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import static org.apache.jena.sparql.vocabulary.VocabTestQuery.query;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.authentication.TokenManager;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormPOST;

/**
 * DAO class to query the triplestore 
 * @update [Morgane Vidal] 04 Oct, 2018: Rename existObject to existUri and change the query of the method existUri.
 * @update [Andréas Garcia] 11 Jan, 2019: Add generic date time stamp comparison SparQL filter.
 * @update [Andréas Garcia] 5 March, 2019: 
 *   Move date related functions in TimeDAOSesame.java
 *   Add a generic function to get a string value from a binding set
 *   Add the max value of a page (to get all results of a service)
 * @param <T>
 * @author Arnaud Charleroy
 */
public abstract class DAOSesame<T> extends DAO<T> {

    final static Logger LOGGER = LoggerFactory.getLogger(DAOSesame.class);
    protected static final String PROPERTY_FILENAME = "sesame_rdf_config";
    
    /**
     * Page size max value used to get the highest number of results of an 
     * object when getting a list within a list (e.g to get all the concerned
     * items of all the events)
     * //SILEX:todo 
     * Pagination should be handled in this case too (i.e when getting a list
     * within a list)
     * For the moment we use only one page by taking the max value
     * //\SILEX:todo
     */    
    protected int pageSizeMaxValue = Integer.parseInt(PropertiesFileManager.getConfigFileProperty("service", "pageSizeMax"));
    
    //SILEX:test
    // For the full connection pool issue
    protected static final String SESAME_SERVER = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
    protected static final String REPOSITORY_ID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
    //\SILEX:test

    // used for logger
    protected static final String SPARQL_QUERY = "SPARQL query: ";
    
    protected static final String COUNT_ELEMENT_QUERY = "count";
    
    /**
     * The following constants are SPARQL variables name used for each subclass 
     * to query the triplestore.
     */
    protected static final String URI = "uri";
    protected static final String URI_SELECT_NAME_SPARQL = "?" + URI;
    protected static final String RDF_TYPE = "rdfType";
    protected static final String RDF_TYPE_SELECT_NAME_SPARQL = "?" + RDF_TYPE;
    protected static final String LABEL = "label";
    protected static final String COMMENT = "comment";
    
    protected final String DATETIMESTAMP_FORMAT_SPARQL = DateFormat.YMDTHMSZZ.toString();
    
    // Triplestore relations
    protected static final URINamespaces ONTOLOGIES = new URINamespaces();

    protected static Repository rep;
    private RepositoryConnection connection;

    protected static String resourceType;

    protected Integer page;
    protected Integer pageSize;

    public DAOSesame() {
        try {
            String sesameServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
            String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
            rep = new HTTPRepository(sesameServer, repositoryID); //Stockage triplestore Sesame
            rep.initialize();
            setConnection(rep.getConnection());
        } catch (Exception e) {
            ResponseFormPOST postForm = new ResponseFormPOST(new Status("Can't connect to triplestore", StatusCodeMsg.ERR, e.getMessage()));
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postForm).build());
        }
    }

    public DAOSesame(User user) {
        this();
        this.user = user;
    }

    public DAOSesame(String repositoryID) {
        try {
            String sesameServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
            rep = new HTTPRepository(sesameServer, repositoryID); //Stockage triplestore Sesame
            rep.initialize();
            setConnection(rep.getConnection());
        } catch (Exception e) {
            ResponseFormPOST postForm = new ResponseFormPOST(new Status("Can't connect to triplestore", StatusCodeMsg.ERR, e.getMessage()));
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postForm).build());
        }
    }

    public RepositoryConnection getConnection() {
        return connection;
    }

    public final void setConnection(RepositoryConnection connection) {
        this.connection = connection;
    }

    public static Repository getRepository() {
        return rep;
    }

    /**
     * Brapi API page starts at 0
     * @return current page number
     */
    public Integer getPage() {
        if (page == null || pageSize < 0) {
            return 0;
        }
        return page;
    }

    /**
     * Brapi page to be used for pagination in database
     * @return current page number + 1
     */
    public Integer getPageForDBQuery() {
        if (page == null || pageSize < 0) {
            return 1;
        }
        return page + 1;
    }

    /**
     * Page parameter
     * @param page
     */
    public void setPage(Integer page) {
        if (page < 0) {
            this.page = Integer.valueOf(DefaultBrapiPaginationValues.PAGE);
        }
        this.page = page;
    }

    /**
     * Page size parameter
     * @return
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 0) {
            return Integer.valueOf(DefaultBrapiPaginationValues.PAGE_SIZE);
        }
        return pageSize;
    }

    /**
     * Set page size parameter
     * @param pageSize
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Existence test method of a subject by triplet
     * @param subject
     * @param predicate
     * @param object
     * @return boolean
     */
    public boolean exist(String subject, String predicate, String object) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        boolean exist = false;
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect(null);
        query.appendTriplet(subject, predicate, object, null);
        query.appendParameters("LIMIT 1");
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                exist = true;
            }
        }
        return exist;
    }

    /**
     * Check if a given URI exist in the triplestore.
     * @param uri the uri to test
     * @example
     * ASK {
     *  VALUES (?r) { (<http://www.w3.org/2000/01/rdf-schema#Literal>) }
     *  { ?r ?p ?o }
     *  UNION
     *  { ?s ?r ?o }
     *  UNION
     *  { ?s ?p ?r }
     * }
     * @return true if the uri exist in the triplestore
     *         false if it does not exist
     */
    public boolean existUri(String uri) {
        if (uri == null) {
            return false;
        }
        try {
            //SILEX:warning
            //Remember to add rdf, rdfs and owl ontologies in your triplestore
            //\SILEX:warning
            SPARQLQueryBuilder query = new SPARQLQueryBuilder();
            query.appendAsk("VALUES (?r) { (<" + uri + ">) }\n" +
                        "    { ?r ?p ?o }\n" +
                        "    UNION\n" +
                        "    { ?s ?r ?o }\n" +
                        "    UNION\n" +
                        "    { ?s ?p ?r }\n");
            
            LOGGER.debug(SPARQL_QUERY + query.toString());
            BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
            return booleanQuery.evaluate();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a given URI exist in a given Graph in the triplestore.
     * @param uri the uri to test
     * @param graph
     * @example
     * ASK FROM <http://www.mygraph.com> {
     *  VALUES (?r) { (<http://www.w3.org/2000/01/rdf-schema#Literal>) }
     *  { ?r ?p ?o }
     *  UNION
     *  { ?s ?r ?o }
     *  UNION
     *  { ?s ?p ?r }
     * }
     * @return true if the uri exist in the graph
     *         false if it does not exist
     */
    public boolean existUriInGraph(String uri, String graph) {
        if (uri == null) {
            return false;
        }
        if (graph == null) {
            return false;
        }
        try {            
            String query = "ASK \n" +
                            "  FROM <" + graph + "> {\n" +
                            "\n" +
                            " VALUES (?r) { (<" + uri + ">) }\n" +
                            " { ?r ?p ?o }\n" +
                            " UNION\n" +
                            " { ?s ?r ?o }\n" +
                            " UNION\n" +
                            " { ?s ?p ?r }\n" +
                            "  \n" +
                            "}";
            
            LOGGER.debug(SPARQL_QUERY + query);
            BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query);
            return booleanQuery.evaluate();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Existence element recovery method by triplet
     * @param subject
     * @param predicate
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public String getValueFromPredicate(String subject, String predicate) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        String value = null;
        if (subject != null || predicate != null) {
            SPARQLQueryBuilder query = new SPARQLQueryBuilder();
            query.appendSelect("?x");
            query.appendTriplet(subject, predicate, "?x", null);
            query.appendParameters("LIMIT 1");
            LOGGER.trace(query.toString());
            TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                if (result.hasNext()) {
                    value = result.next().getBinding("x").getValue().stringValue();
                }
            }
            LOGGER.trace(value);
        }
        return value;
    }

    /**
     * Define of user object from an id
     * @param id
     */
    public void setUser(String id) {
        if (TokenManager.Instance().getSession(id).getUser() == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        } else {
            this.user = TokenManager.Instance().getSession(id).getUser();
        }
    }
    
    /**
     * Add object properties to a given object.
     * @param subjectUri the subject uri which will have the object properties uris
     * @param predicateUri the uri of the predicates
     * @param objectPropertiesUris the list of the object properties to link to the subject
     * @param graphUri
     * @example
     * INSERT DATA {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/sensors> { 
     *          <http://www.phenome-fppn.fr/diaphen/2018/s18533>  <http://www.opensilex.org/vocabulary/oeso#measures>  <http://www.phenome-fppn.fr/id/variables/v001> . 
     * }}
     * @return true if the insertion has been done
     *         false if an error occurred (see the error logs to get more details)
     */
    protected boolean addObjectProperties(String subjectUri, String predicateUri, List<String> objectPropertiesUris, String graphUri) {
        //Generates insert query
        UpdateBuilder spql = new UpdateBuilder();
        Node graph  = NodeFactory.createURI(graphUri);
        
        objectPropertiesUris.forEach((objectProperty) -> {
            Node subjectUriNode  = NodeFactory.createURI(subjectUri);
            Node predicateUriNode  = NodeFactory.createURI(predicateUri);
            Node objectPropertyNode  = NodeFactory.createURI(objectProperty);
            
            spql.addInsert(graph, subjectUriNode, predicateUriNode, objectPropertyNode);
        });
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        //Insert the properties in the triplestore
        Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, spql.buildRequest().toString());
        try {
            prepareUpdate.execute();
        } catch (UpdateExecutionException ex) {
            LOGGER.error("Add object properties error : " + ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Delete the given object properties.
     * @param subjectUri
     * @param predicateUri
     * @param objectPropertiesUris
     * @example
     * DELETE WHERE { 
     *  <http://www.phenome-fppn.fr/diaphen/2018/s18533> <http://www.opensilex.org/vocabulary/oeso#measures> <http://www.phenome-fppn.fr/id/variables/v001> .  
     * }
     * @return true if the object properties have been deleted
     *         false if the delete has not been done.
     */
    protected boolean deleteObjectProperties(String subjectUri, String predicateUri, List<String> objectPropertiesUris) {
        //1. Generates delete query
        UpdateBuilder query = new UpdateBuilder();
        
        Resource subject = ResourceFactory.createResource(subjectUri);
        Property predicate = ResourceFactory.createProperty(predicateUri);        
        
        for (String objectProperty : objectPropertiesUris) {
            Node object = NodeFactory.createURI(objectProperty);
            query.addWhere(subject, predicate, object);
        }
        
        UpdateDeleteWhere request = query.buildDeleteWhere();
        LOGGER.debug(request.toString());
        
        //2. Delete data in the triplestore
        Update prepareDelete = getConnection().prepareUpdate(QueryLanguage.SPARQL, request.toString());
        try {
            prepareDelete.execute();
        } catch (UpdateExecutionException ex) {
            LOGGER.error("Delete object properties error : " + ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the value of a name in the SELECT statement from a binding set
     * @param selectName 
     * @param bindingSet 
     * @return  the string value of the "selectName" variable in the binding set
     */
    protected String getStringValueOfSelectNameFromBindingSet(String selectName, BindingSet bindingSet) { 
        Value selectedFieldValue = bindingSet.getValue(selectName);
        if (selectedFieldValue != null) {
            return selectedFieldValue.stringValue();
        }
        return null;
    }
    
    @Override
    public void finalize() throws Throwable {
        try {
            if (getConnection() != null) {
                getConnection().close();
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    protected void initConnection() {
        getConnection().begin();    
    }

    @Override
    protected void closeConnection() {
        getConnection().close();
    }

    @Override
    protected void startTransaction() {
        // transactions are startes automatically in SPARQL.
    }

    @Override
    protected void commitTransaction() {
        getConnection().commit();
    }

    @Override
    protected void rollbackTransaction() {
        getConnection().rollback();
    }
}
