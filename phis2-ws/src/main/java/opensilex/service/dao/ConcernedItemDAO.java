//******************************************************************************
//                          ConcernedItemDAOS.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 5 March, 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import opensilex.service.dao.exception.UnknownUriException;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAODataErrorException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.model.User;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.model.ConcernedItem;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Concerned items DAO.
 * @author Andreas Garcia <andreas.garcia@inra.fr>
 */
public class ConcernedItemDAO extends Rdf4jDAO<ConcernedItem> {
    final static Logger LOGGER = LoggerFactory.getLogger(ConcernedItemDAO.class);
    
    /**
     * Graph in which the DAO will operate.
     * @example <http://www.opensilex.org/{instance}/set/events>, <http://www.opensilex.org/{instance}/documents>
     */
    private final String graphString;
    
    /**
     * Relation URI of "concerns"
     * @example oeev:concerns, oeso:concerns
     */
    private final String concernsRelationUri;
    
    // constants used for SPARQL names in the SELECT
    private static final String CONCERNED_ITEM_URI_SELECT_NAME = "concernedItemUri";
    private static final String CONCERNED_ITEM_URI_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_URI_SELECT_NAME;
    private static final String CONCERNED_ITEM_TYPE_SELECT_NAME = "concernedItemType";
    private static final String CONCERNED_ITEM_TYPE_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_TYPE_SELECT_NAME;
    private static final String CONCERNED_ITEM_LABEL_SELECT_NAME = "concernedItemLabel";
    private static final String CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_LABEL_SELECT_NAME;
    private static final String CONCERNED_ITEM_LABELS_SELECT_NAME = "concernedItemLabels";
    private static final String CONCERNED_ITEM_LABELS_SELECT_NAME_SPARQL = "?" + CONCERNED_ITEM_LABELS_SELECT_NAME;

    public ConcernedItemDAO(User user, String graph, String concernsRelationUri) {
        super(user);
        this.graphString = graph;
        this.concernsRelationUri = concernsRelationUri;
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
            query.appendTriplet(
                    objectUriSelectNameSparql, 
                    concernsRelationUri, 
                    CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, 
                    null);
        }

        if (searchConcernedItemLabel != null) {
            query.appendTriplet(
                    CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, 
                    Rdfs.RELATION_LABEL.toString(), 
                    CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL, 
                    null);

            query.appendAndFilter("regex(" + CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL + ", " 
                    + "\"" + searchConcernedItemLabel + "\", \"i\")");
        }

        if (searchConcernedItemUri != null) {
            query.appendAndFilter("regex (str(" + CONCERNED_ITEM_URI_SELECT_NAME_SPARQL + "), " 
                    + "\"" + searchConcernedItemUri + "\", \"i\")");
        }
    }
    
    /**
     * Prepares the query to search the concerned items of a object
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
    private SPARQLQueryBuilder prepareConcernedItemsSearchQuery(String objectUri, String searchUri, String searchLabel) {
        
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
        // concerned items labels and type are made optional FOR THE MOMENT
        // because various objects (like experiments, users, groups etc.) are 
        // stored in the triplestore and PostgreSQL. Types and labels are
        // currently stored in PostgreSQL and therefore are inaccessible
        // with a query on the triplestore.
        // Solutions possible:
        //     - short term: query PostgreSQL to get these properties
        //     - migrate these properties into the triplestore
        ///SILEX:todo
        query.beginBodyOptional();
        query.appendTriplet(
                CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, 
                Rdf.RELATION_TYPE.toString(), 
                CONCERNED_ITEM_TYPE_SELECT_NAME_SPARQL, 
                null);
        query.appendTriplet(
                CONCERNED_ITEM_URI_SELECT_NAME_SPARQL, 
                Rdfs.RELATION_LABEL.toString(), 
                CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL, 
                null);
        query.endBodyOptional();
        
        query.appendSelectConcat(
                CONCERNED_ITEM_LABEL_SELECT_NAME_SPARQL, 
                SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR, 
                CONCERNED_ITEM_LABELS_SELECT_NAME_SPARQL);
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Searches an object concerned items with filters.
     * @param objectUri 
     * @param searchUri 
     * @param searchLabel 
     * @param page 
     * @param pageSize 
     * @return  the object's concerned items
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    public ArrayList<ConcernedItem> find(String objectUri, String searchUri, String searchLabel, int page, int pageSize) 
            throws DAOPersistenceException {
        setPage(page);
        setPageSize(pageSize);
        
        ArrayList<ConcernedItem> concernedItems = new ArrayList<>();
        SPARQLQueryBuilder concernedItemsQuery = prepareConcernedItemsSearchQuery(
                objectUri, 
                searchUri, 
                searchLabel);
        TupleQuery concernedItemsTupleQuery = getConnection().prepareTupleQuery(
                QueryLanguage.SPARQL, 
                concernedItemsQuery.toString());

        try (TupleQueryResult concernedItemsTupleQueryResult = concernedItemsTupleQuery.evaluate()) {
            ConcernedItem concernedItem;
            while(concernedItemsTupleQueryResult.hasNext()) {
                concernedItem = getConcernedItemFromBindingSet(concernedItemsTupleQueryResult.next(), objectUri);
                if (concernedItem.getUri() != null) {
                    concernedItems.add(concernedItem);
                }
            }
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
        return concernedItems;
    }
    
    /**
     * Adds statements to an update builder to insert concerned items. 
     * @param updateBuilder
     * @param concernedItems
     * @example
     */
    public void addInsertToUpdateBuilder(UpdateBuilder updateBuilder, List<ConcernedItem> concernedItems) {
        Node graphNode = NodeFactory.createURI(this.graphString);
        Resource concernsRelation = ResourceFactory.createResource(concernsRelationUri);
        Resource objectLinkedResource;
        Resource concernedItemResource;
        for (ConcernedItem concernedItem : concernedItems) {
            objectLinkedResource = ResourceFactory.createResource(concernedItem.getObjectLinked());
            concernedItemResource = ResourceFactory.createResource(concernedItem.getUri());
            updateBuilder.addInsert(graphNode, objectLinkedResource, concernsRelation, concernedItemResource);
        }
    }
    
    /**
     * Generates an insert query for the links of the given concerned items links.
     * @param updateBuilder
     * @param graph
     * @param linkedResource
     * @param concernsRelationUri since "concerns" can designate various
     * relations in various vocabularies (e.g OESO or OEEV), the URI of the 
     * relation has to be
     * @param concernedItems
     */
    public static void addDeleteConcernedItemLinksToUpdateBuilder(UpdateBuilder updateBuilder, Node graph, Resource linkedResource, String concernsRelationUri, List<ConcernedItem> concernedItems) {
        org.apache.jena.rdf.model.Property  concernsJenaProperty = 
                ResourceFactory.createProperty(concernsRelationUri);
        for (ConcernedItem concernedItem : concernedItems) {
            Resource concernedItemResource = ResourceFactory.createResource(concernedItem.getUri());
            updateBuilder.addDelete(graph, linkedResource, concernsJenaProperty, concernedItemResource);
        }
    }
    
    /**
     * Checks the existence of the given list of concerned items.
     * @param concernedItems
     * @throws opensilex.service.dao.exception.DAODataErrorAggregateException
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    @Override
    public void validate(List<ConcernedItem> concernedItems) 
            throws DAODataErrorAggregateException, DAOPersistenceException {       
        ArrayList<DAODataErrorException> exceptions = new ArrayList<>(); 
        try {
            concernedItems.forEach((concernedItem) -> {
                String concernedItemUri = concernedItem.getUri();
                if (concernedItemUri != null) {
                    if (!existUri(concernedItem.getUri())) {
                        exceptions.add(new UnknownUriException(concernedItemUri, "the concerned item"));
                    }
                }
            });
            if (exceptions.size() > 0) {
                throw new DAODataErrorAggregateException(exceptions);
            }
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
    }
    
    /**
     * Gets a concerned item from a binding set.
     * @param bindingSet
     * @param objectUri
     * @return concerned item
     */
    public static ConcernedItem getConcernedItemFromBindingSet(BindingSet bindingSet, String objectUri) {
        String uri = getStringValueOfSelectNameFromBindingSet(CONCERNED_ITEM_URI_SELECT_NAME, bindingSet);
        String type = getStringValueOfSelectNameFromBindingSet(CONCERNED_ITEM_TYPE_SELECT_NAME, bindingSet);
        
        String labelsConcatenated = getStringValueOfSelectNameFromBindingSet(
                CONCERNED_ITEM_LABELS_SELECT_NAME, 
                bindingSet);
        ArrayList<String> labels = new ArrayList<>(Arrays.asList(labelsConcatenated.split(
                SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));

        return new ConcernedItem(uri, type, labels, objectUri);
    }

    @Override
    public List<ConcernedItem> create(List<ConcernedItem> objects) throws DAOPersistenceException, Exception {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        addInsertToUpdateBuilder(updateBuilder, objects);
        executeUpdateRequest(updateBuilder);
        return objects;
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
}
