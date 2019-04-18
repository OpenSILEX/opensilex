//******************************************************************************
//                            ConcernedItemDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 5 March 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.User;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.ConcernedItem;

/**
 * Concerned items DAO.
 * @author Andreas Garcia <andreas.garcia@inra.fr>
 */
public class ConcernedItemDAO extends Rdf4jDAO<ConcernedItem> {
    final static Logger LOGGER = LoggerFactory.getLogger(ConcernedItemDAO.class);
    
    // constants used for SPARQL names in the SELECT
    private static final String CONCERNED_ITEM_URI_SELECT_NAME = "concernedItemUri";
    private static final String CONCERNED_ITEM_URI_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_URI_SELECT_NAME;
    private static final String CONCERNED_ITEM_TYPE_SELECT_NAME = "concernedItemType";
    private static final String CONCERNED_ITEM_TYPE_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_TYPE_SELECT_NAME;
    private static final String CONCERNED_ITEM_LABEL_SELECT_NAME = "concernedItemLabel";
    private static final String CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_LABEL_SELECT_NAME;
    private static final String CONCERNED_ITEM_LABELS_SELECT_NAME = "concernedItemLabels";
    private static final String CONCERNED_ITEM_LABELS_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_LABELS_SELECT_NAME;

    public ConcernedItemDAO(User user) {
        super(user);
    }
    
    /**
     * Sets a search query to applies the concerned items label filter. 
     * This function DOES NOT make the query return concerned items.
     * The filter concerns the concerned items URI and labels
     * @example SparQL filter added:
     *  WHERE {
     *    ?uri  <http://www.opensilex.org/vocabulary/oeev#concerns>  ?concernedItemUri  . 
     *    ?concernedItemUri  <http://www.w3.org/2000/01/rdf-schema#label>  ?concernedItemLabel  . 
     *  }
     * @param query
     * @param objectUriSelectNameSparql URI SparQL variable of the concerning object
     * @param searchConcernedItemLabel
     * @param concernsRelationUri since "concerns" can designate various
     * relations in various vocabularies (e.g OESO or OEEV), the URI of the 
     * relation has to be 
     * @param searchConcernedItemUri
     */
    public static void prepareQueryWithConcernedItemFilters(SPARQLQueryBuilder query, String objectUriSelectNameSparql, String concernsRelationUri, String searchConcernedItemUri, String searchConcernedItemLabel) {
        if (objectUriSelectNameSparql != null) {
            query.appendTriplet(objectUriSelectNameSparql, concernsRelationUri, CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, null);
        }

        if (searchConcernedItemLabel != null) {
            query.appendTriplet(CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, Rdfs.RELATION_LABEL.toString(), CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL, null);

            query.appendAndFilter("regex(" + CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL + ", \"" + searchConcernedItemLabel + "\", \"i\")");
        }

        if (searchConcernedItemUri != null) {
            query.appendAndFilter("regex (str(" + CONCERNED_ITEM_URI_SELECT_NAME_SPARQL + ")" + ", \"" + searchConcernedItemUri + "\", \"i\")");
        }
    }
    
    /**
     * Prepares the query to search the concerned items of a object.
     * @param searchLabel
     * @param searchUri
     * @param concernsRelationUri since "concerns" can designate various
     * relations in various vocabularies (e.g OESO or OEEV), the URI of the 
     * relation has to be 
     * @example
     * SELECT DISTINCT  ?concernedItemUri ?concernedItemType 
     * (GROUP_CONCAT(DISTINCT ?concernedItemLabel; SEPARATOR=",") AS ?concernedItemLabels) 
     * WHERE {
     *   <http://opensilex.org/id/event/96e72788-6bdc-4f8e-abd1-ce9329371e8e>  <http://www.opensilex.org/vocabulary/oeev#concerns>  ?concernedItemUri  . 
     *   
     *   OPTIONAL {
     *     ?concernedItemUri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?concernedItemType  . 
     *     ?concernedItemUri  <http://www.w3.org/2000/01/rdf-schema#label>  ?concernedItemLabel  . 
     *   }
     * }
     * GROUP BY  ?concernedItemUri ?concernedItemType 
     * @param objectUri
     * @return query
     */
    protected static SPARQLQueryBuilder prepareConcernedItemsSearchQuery(String objectUri, String concernsRelationUri, String searchUri, String searchLabel) {
        
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String uriSelectNameSparql = null;
        if (objectUri != null) {
            uriSelectNameSparql = "<" + objectUri + ">";
        }
        
        prepareQueryWithConcernedItemFilters(query, uriSelectNameSparql, concernsRelationUri, searchUri, searchLabel);
        
        query.appendSelect(CONCERNED_ITEM_URI_SELECT_NAME_SPARQL);
        query.appendGroupBy(CONCERNED_ITEM_URI_SELECT_NAME_SPARQL);
        
        query.appendSelect(CONCERNED_ITEM_TYPE_SELECT_NAME_SPARQL);
        query.appendGroupBy(CONCERNED_ITEM_TYPE_SELECT_NAME_SPARQL);
        
        //\SILEX:todo 
        // concerned items' labels and type are made optional FOR THE MOMENT
        // because various objects (like experiments, users, groups etc.) are 
        // stored in the triplestore and PostgreSQL. Types and labels are
        // currently stored in PostgreSQL and therefore are inaccessible
        // with a query on the triplestore.
        // Solutions possible:
        //     - short term: query PostgreSQL to get these properties
        //     - migrate these properties into the triplestore
        ///SILEX:todo
        query.beginBodyOptional();
        query.appendTriplet(CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, Rdf.RELATION_TYPE.toString(), CONCERNED_ITEM_TYPE_SELECT_NAME_SPARQL, null);
        query.appendTriplet(CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, Rdfs.RELATION_LABEL.toString(), CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL, null);
        query.endBodyOptional();
        
        query.appendSelectConcat(CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL, SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR, CONCERNED_ITEM_LABELS_SELECT_NAME_SPARQL);
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Searches an object's concerned items with filters.
     * @param objectUri 
     * @param concernsRelationUri since "concerns" can designate various
     * relations in various vocabularies (e.g OESO or OEEV), the URI of the 
     * relation has to be 
     * @param searchUri 
     * @param searchLabel 
     * @param page 
     * @param pageSize 
     * @return  the object's concerned items
     */
    public ArrayList<ConcernedItem> searchConcernedItems(String objectUri, String concernsRelationUri, String searchUri, String searchLabel, int page, int pageSize) {
        setPage(page);
        setPageSize(pageSize);
        
        ArrayList<ConcernedItem> concernedItems = new ArrayList<>();
        SPARQLQueryBuilder concernedItemsQuery = prepareConcernedItemsSearchQuery(objectUri, concernsRelationUri, searchUri, searchLabel);
        TupleQuery concernedItemsTupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, concernedItemsQuery.toString());

        try (TupleQueryResult concernedItemsTupleQueryResult = concernedItemsTupleQuery.evaluate()) {
            ConcernedItem concernedItem;
            while(concernedItemsTupleQueryResult.hasNext()) {
                concernedItem = getConcernedItemFromBindingSet(concernedItemsTupleQueryResult.next());
                if (concernedItem.getUri() != null) {
                    concernedItems.add(concernedItem);
                }
            }
        }
        return concernedItems;
    }
    
    /**
     * Generates an insert query for the links of the given concerned items.
     * @param graphString
     * @param objectResource the concerning object's URI
     * @param concernsRelationUri since "concerns" can designate various
     * relations in various vocabularies (e.g OESO or OEEV), the URI of the 
     * relation has to be 
     * @param concernedItem
     * @return the query
     * @example
     */
    private UpdateRequest prepareInsertLinkQuery(String graphString, Resource objectResource, String concernsRelationUri, ArrayList<ConcernedItem> concernedItems) {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        Node graph = NodeFactory.createURI(graphString);
        Resource concernsRelation = ResourceFactory.createResource(concernsRelationUri);
        for (ConcernedItem concernedItem : concernedItems) {
            Resource concernedItemResource = ResourceFactory.createResource(concernedItem.getUri());
            updateBuilder.addInsert(graph, objectResource, concernsRelation, concernedItemResource);
        }
        UpdateRequest query = updateBuilder.buildRequest();
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * Checks the existence of the given list of concerned items.
     * @param concernedItems
     * @return the result with the list of the found errors (empty if no error)
     */
    public POSTResultsReturn check(List<ConcernedItem> concernedItems) {
        POSTResultsReturn checkResult;
        List<Status> status = new ArrayList<>();
        
        for (ConcernedItem concernedItem : concernedItems) {
            String concernedItemUri = concernedItem.getUri();
            if (concernedItemUri != null) {

                // Check the URI if given (in case of an update)
                if (!existUri(concernedItem.getUri())){
                    status.add(new Status(
                            StatusCodeMsg.UNKNOWN_URI, 
                            StatusCodeMsg.ERR, 
                            String.format(StatusCodeMsg.UNKNOWN_CONCERNED_ITEM_URI, concernedItemUri)));
                }
            } 
        }
            
        boolean dataIsValid = status.isEmpty();
        checkResult = new POSTResultsReturn(dataIsValid, null, dataIsValid);
        checkResult.statusList = status;
        return checkResult;   
    }
    
    /**
     * Inserts the given concerned items in the storage. 
     * /!\ Prerequisite: data must have been checked before calling this method
     * @param graph 
     * @param objectResource
     * @param concernsRelationUri since "concerns" can designate various
     * relations in various vocabularies (e.g OESO or OEEV), the URI of the 
     * relation has to be 
     * @param concernedItems
     * @return the insertion result
     */
    public POSTResultsReturn insertLinksWithObject(String graph, Resource objectResource, String concernsRelationUri, ArrayList<ConcernedItem> concernedItems) {
        List<Status> status = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false;
        boolean linksInserted = true;
        
        getConnection().begin();
            
        // Insert links
        UpdateRequest query = prepareInsertLinkQuery(graph, objectResource, concernsRelationUri, concernedItems);
            
        try {
            Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
        } catch (RepositoryException ex) {
                LOGGER.error(StatusCodeMsg.ERROR_WHILE_COMMITTING_OR_ROLLING_BACK_TRIPLESTORE_STATEMENT, ex);
        } catch (MalformedQueryException e) {
                LOGGER.error(e.getMessage(), e);
                linksInserted = false;
                status.add(new Status(
                        StatusCodeMsg.QUERY_ERROR, 
                        StatusCodeMsg.ERR, 
                        StatusCodeMsg.MALFORMED_CREATE_QUERY + " " + e.getMessage()));
        }
        
        if (linksInserted) {
            resultState = true;
            getConnection().commit();
        } else {
            getConnection().rollback();
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        results = new POSTResultsReturn(resultState, linksInserted, true);
        results.statusList = status;
        results.setCreatedResources(createdResourcesUris);
        if (resultState && !createdResourcesUris.isEmpty()) {
            results.createdResources = createdResourcesUris;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUris.size() + " " + StatusCodeMsg.RESOURCES_CREATED));
        }
        
        return results;
    }
    
    /**
     * Gets a concerned item from a binding set.
     * @param bindingSet
     * @return concerned item
     */
    private ConcernedItem getConcernedItemFromBindingSet(BindingSet bindingSet) {
                
        String concernedItemUri = getStringValueOfSelectNameFromBindingSet(CONCERNED_ITEM_URI_SELECT_NAME, bindingSet);
        String concernedItemType = getStringValueOfSelectNameFromBindingSet(CONCERNED_ITEM_TYPE_SELECT_NAME, bindingSet);
        
        String concernedItemLabelsConcatenated = getStringValueOfSelectNameFromBindingSet(CONCERNED_ITEM_LABELS_SELECT_NAME, bindingSet);
        ArrayList<String> concernedItemLabels = new ArrayList<>(Arrays.asList(concernedItemLabelsConcatenated.split(SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));

        return new ConcernedItem(concernedItemUri, concernedItemType, concernedItemLabels);
    }

    @Override
    public List<ConcernedItem> create(List<ConcernedItem> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<ConcernedItem> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ConcernedItem> update(List<ConcernedItem> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ConcernedItem find(ConcernedItem object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ConcernedItem findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<ConcernedItem> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
