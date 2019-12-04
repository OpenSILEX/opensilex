//******************************************************************************
//                                EventDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12  nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DateFormat;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAODataErrorException;
import opensilex.service.dao.exception.NotAnAdminException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.UnknownUriException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.model.User;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oa;
import opensilex.service.ontology.Oeev;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Time;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.model.Annotation;
import opensilex.service.model.Event;
import opensilex.service.model.Instant;
import opensilex.service.ontology.Xsd;
import opensilex.service.utils.date.Dates;
import opensilex.service.utils.sparql.SPARQLStringBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Events DAO.
 * @update [Andreas Garcia] 14 Feb. 2019: Add event detail service.
 * @update [Andreas Garcia] 5 Mar. 2019: Add events insertion service.
 * @update [Andréas Garcia] 5 Mar. 2019: 
 *      Move the generic function to get a string value from a binding set to mother class.
 *      Move concerned items accesses handling into a new ConcernedItemDAO class.
 * @update [Andréas Garcia] 8 Apr. 2019: Use DAO generic function create, update, checkBeforeCreation and use exceptions 
 * to handle errors.
 * @author Andreas Garcia <andreas.garcia@inra.fr>
 */
public class EventDAO extends Rdf4jDAO<Event> {
    final static Logger LOGGER = LoggerFactory.getLogger(EventDAO.class);
    
    private static final String INSTANT_SELECT_NAME = "instant";
    private static final String INSTANT_SELECT_NAME_SPARQL = "?" + INSTANT_SELECT_NAME;
    
    private static final String DATETIMESTAMP_SELECT_NAME = "dateTimeStamp";
    private static final String DATETIMESTAMP_SELECT_NAME_SPARQL = "?" + DATETIMESTAMP_SELECT_NAME;

    protected static final String DATETIME_SELECT_NAME = "dateTime";
    protected static final String DATETIME_SELECT_NAME_SPARQL = "?" + DATETIME_SELECT_NAME;
    
    protected static final String DATE_RANGE_START_DATETIME_SELECT_NAME = "dateRangeStartDateTime";
    protected static final String DATE_RANGE_START_DATETIME_SELECT_NAME_SPARQL = "?" + DATE_RANGE_START_DATETIME_SELECT_NAME;
    
    protected static final String DATE_RANGE_END_DATETIME_SELECT_NAME = "dateRangeEndDateTime";
    protected static final String DATE_RANGE_END_DATETIME_SELECT_NAME_SPARQL = "?" + DATE_RANGE_END_DATETIME_SELECT_NAME;
    
    public EventDAO(User user) {
        super(user);
    }
    
    /**
     * Sets a search query to select an URI and adds a filter according to it 
     * if necessary
     * @example SparQL filter added:
     *  SELECT DISTINCT  ?uri
     *  WHERE {
     *    FILTER ( (regex (str(?uri), "http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1", "i"))
     *  }
     *  GROUP BY ?uri
     * @param query
     * @param searchUri
     * @param inGroupBy
     * @return the value of the URI's value in the SELECT
     */
    private String prepareSearchQueryUri(SPARQLQueryBuilder query, String searchUri, boolean inGroupBy) {
        query.appendSelect(URI_SELECT_NAME_SPARQL);
        
        if (inGroupBy) {
            query.appendGroupBy(URI_SELECT_NAME_SPARQL);
        }
        if (searchUri != null) {
            query.appendAndFilter("regex (str(" + URI_SELECT_NAME_SPARQL + ")" + ", \"" + searchUri + "\", \"i\")");
        }
        return URI_SELECT_NAME_SPARQL;
    }
    
    /**
     * Sets a search query to select a type and to filter according to it 
     * if necessary
     * @example SparQL filter added:
     *  SELECT DISTINCT ?rdfType
     *  WHERE {
     *    ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeev#Event> .
     *    ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  .
     *  }
     *  GROUP BY ?rdfType
     * @param query
     * @param uriSelectNameSparql
     * @param searchType
     * @param inGroupBy
     */
    private void prepareSearchQueryType(SPARQLQueryBuilder query, String uriSelectNameSparql, String searchType, boolean inGroupBy) {
        query.appendSelect(RDF_TYPE_SELECT_NAME_SPARQL);
        if(inGroupBy){
            query.appendGroupBy(RDF_TYPE_SELECT_NAME_SPARQL);
        }
          if (searchType != null) {
            query.appendTriplet(
                    RDF_TYPE_SELECT_NAME_SPARQL, 
                    "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", 
                    searchType, 
                    null);
        } else {
            query.appendTriplet(
                    RDF_TYPE_SELECT_NAME_SPARQL, 
                    "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", 
                    Oeev.Event.getURI(), 
                    null);
        }
        query.appendTriplet(uriSelectNameSparql, Rdf.RELATION_TYPE.toString(), RDF_TYPE_SELECT_NAME_SPARQL, null);
     
    }
    
    /**
     * Prepares the event search query
     * @param uri
     * @param type
     * @example
     * SELECT DISTINCT  ?uri ?rdfType ?dateTimeStamp 
     * WHERE {
     *   ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *   ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeev#MoveFrom> . 
     *   ?uri  <http://www.opensilex.org/vocabulary/oeev#concerns>  ?concernedItemUri  . 
     *   ?concernedItemUri  <http://www.w3.org/2000/01/rdf-schema#label>  ?concernedItemLabel  . 
     *   ?uri  <http://www.w3.org/2006/time#hasTime>  ?time  . 
     *   ?time  <http://www.w3.org/2006/time#inXSDDateTimeStamp>  ?dateTimeStamp  . 
     *   BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str(?dateTimeStamp)) as ?dateTime) .
     *   BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2017-09-08T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *   BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2019-10-08T12:00:00+01:00")) as ?dateRangeEndDateTime) .
     *   FILTER ( (regex (str(?uri), "http://www.phenome-fppn.fr/id/event/96e72788-6bdc-4f8e-abd1-ce9329371e8e", "i")) 
     *    && (regex (?concernedItemLabel, "Plot Lavalette", "i")) 
     *    && (regex (str(?concernedItemUri), "http://www.phenome-fppn.fr/m3p/arch/2017/c17000242", "i")) 
     *    && (?dateRangeStartDateTime <= ?dateTime) && (?dateRangeEndDateTime >= ?dateTime) ) 
     *  }
     *  GROUP BY  ?uri ?rdfType ?dateTimeStamp 
     *  LIMIT 20 
     *  OFFSET 0 
     * @param searchConcernedItemLabel
     * @param searchConcernedItemUri
     * @param dateRangeStartString
     * @param dateRangeEndString
     * @return query
     */
    private SPARQLQueryBuilder prepareSearchQueryEvents(String uri, String type, String searchConcernedItemLabel, String searchConcernedItemUri, Date dateRangeStart, Date dateRangeEnd) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String uriSelectNameSparql = prepareSearchQueryUri(query, uri, true);
        prepareSearchQueryType(query, uriSelectNameSparql, type, true); 
        ConcernedItemDAO.prepareQueryWithConcernedItemFilters(
                query, 
                uriSelectNameSparql, 
                Oeev.concerns.getURI(), 
                searchConcernedItemUri, 
                searchConcernedItemLabel); 
        filterSearchQueryWithDateRangeComparisonWithDateTimeStamp(
                    query, 
                    uriSelectNameSparql,
                    INSTANT_SELECT_NAME_SPARQL,
                    dateRangeStart, 
                    dateRangeEnd, 
                    DATETIMESTAMP_SELECT_NAME_SPARQL,
                    true);
        
        query.appendLimit(getPageSize());
        query.appendOffset(getPage() * getPageSize());
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Prepares the event search query
     * @example
     * SELECT  ?uri ?rdfType ?dateTimeStamp 
     * WHERE {
     *   ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *   ?uri  <http://www.w3.org/2006/time#hasTime>  ?time  . 
     *   ?time  <http://www.w3.org/2006/time#inXSDDateTimeStamp>  ?dateTimeStamp  . 
     *   FILTER (regex (str(?uri), "http://opensilex.org/id/event/96e72788-6bdc-4f8e-abd1-ce9329371e8e", "i"))
     *  }
     * @param searchUri
     * @return query
     */
    private SPARQLQueryBuilder prepareSearchQueryEvent(String searchUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String uriSelectNameSparql = prepareSearchQueryUri(query, searchUri, false);
        prepareSearchQueryType(query, uriSelectNameSparql, null, false);  
        ConcernedItemDAO.prepareQueryWithConcernedItemFilters(
                query, 
                uriSelectNameSparql, 
                Oeev.concerns.getURI(), 
                null, 
                null); 
        filterSearchQueryWithDateRangeComparisonWithDateTimeStamp(
                    query, 
                    uriSelectNameSparql,
                    INSTANT_SELECT_NAME_SPARQL,
                    null, 
                    null, 
                    DATETIMESTAMP_SELECT_NAME_SPARQL,
                    false);
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Gets an event from a given binding set.
     * @param bindingSet a binding set, result from a search query
     * @return an event target with data extracted from the given binding set
     */
    private Event getEventFromBindingSet(BindingSet bindingSet) {
          
        String eventUri = getStringValueOfSelectNameFromBindingSet(URI, bindingSet);
                
        String eventType = getStringValueOfSelectNameFromBindingSet(RDF_TYPE, bindingSet);
        
        return new Event(eventUri, eventType, new ArrayList<>(), null, new ArrayList<>(), null);
    }
    
    /**
     * Searches events stored
     * @param searchUri
     * @param searchType
     * @param searchConcernedItemLabel
     * @param searchConcernedItemUri 
     * @param dateRangeStartString
     * @param dateRangeEndString
     * @param searchPage
     * @param searchPageSize
     * @return events
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    public ArrayList<Event> find(String searchUri, String searchType, String searchConcernedItemLabel, String searchConcernedItemUri, Date dateRangeStart, Date dateRangeEnd, int searchPage, int searchPageSize) 
            throws DAOPersistenceException {
        
        setPage(searchPage);
        setPageSize(searchPageSize);
        
        SPARQLQueryBuilder eventsQuery = prepareSearchQueryEvents(
                searchUri, 
                searchType, 
                searchConcernedItemLabel, 
                searchConcernedItemUri, 
                dateRangeStart, 
                dateRangeEnd);
        
        // get events from storage
        TupleQuery eventsTupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, eventsQuery.toString());
        
        ArrayList<Event> events = new ArrayList<>();
        ConcernedItemDAO concernedItemDao = new ConcernedItemDAO(
                user, 
                Contexts.EVENTS.toString(), 
                Oeev.concerns.getURI());
        
        // for each event, set its properties and concerned Items
        try {
            TupleQueryResult eventsResult = eventsTupleQuery.evaluate();
            while (eventsResult.hasNext()) {
                BindingSet bindingSet = eventsResult.next();
                int i = bindingSet.size();
                    if (i > 0) { // Graphdb fix 
                    Event event = getEventFromBindingSet(bindingSet);

                    // Instant
                    event.setInstant(getInstantFromBindingSet(
                                bindingSet,
                                INSTANT_SELECT_NAME, 
                                DATETIMESTAMP_SELECT_NAME));

                    // Properties
                    setEventProperties(event);

                    // Concerned items
                    event.setConcernedItems(concernedItemDao.find(
                                event.getUri(), 
                                null, 
                                null, 
                                0, 
                                pageSizeMaxValue));

                    events.add(event);
                    }
                }
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
       
        return events;
    }
    
    /**
     * Searches an event by its URI.
     * @param searchUri
     * @return events
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    @Override
    public Event findById(String searchUri) throws DAOPersistenceException {        
        SPARQLQueryBuilder eventQuery = prepareSearchQueryEvent(searchUri);
        ConcernedItemDAO concernedItemDao = new ConcernedItemDAO(
                user, 
                Contexts.EVENTS.toString(), 
                Oeev.concerns.getURI());
        Event event = null;
        
        // Get event from storage
        TupleQuery eventsTupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, eventQuery.toString());
        
        try {
            TupleQueryResult eventsResult = eventsTupleQuery.evaluate();
            if (eventsResult.hasNext()) {
                BindingSet bindingSet = eventsResult.next();
                event = getEventFromBindingSet(bindingSet);

                // Instant
                event.setInstant(getInstantFromBindingSet(
                        bindingSet,
                        INSTANT_SELECT_NAME, 
                        DATETIMESTAMP_SELECT_NAME));

                // Properties
                setEventProperties(event);

                // Concerned items
                event.setConcernedItems(concernedItemDao.find(
                        event.getUri(), 
                        null, 
                        null, 
                        0, 
                        pageSizeMaxValue));

                // Annotations
                AnnotationDAO annotationDAO = new AnnotationDAO(this.user);
                event.setAnnotations(annotationDAO.find(
                        null, 
                        null, 
                        event.getUri(), 
                        null, 
                        null,
                        true,
                        0, 
                        pageSizeMaxValue));
            }
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
        return event;
    }
    
    public static void setNewUris (List<Event> events) throws Exception {
        for(Event event : events) {
            event.setUri(UriGenerator.generateNewInstanceUri(Oeev.Event.getURI(), null, null));
            
            event.getConcernedItems().forEach(concernedItem -> {
                concernedItem.setObjectLinked(event.getUri());
            });
        
            AnnotationDAO.setNewUris(event.getAnnotations());
            ArrayList<String> annotationTargets = new ArrayList<>();
            annotationTargets.add(event.getUri());
            event.getAnnotations().forEach(annotation -> {
                annotation.setTargets(annotationTargets);
            });
        }
    }
    
    /**
     * Generates an insert query for the given event.
     * @param updateBuilder
     * @param event
     * @throws java.lang.Exception
     * @example
     */
    public void addInsertToUpdateBuilder(UpdateBuilder updateBuilder, Event event) throws Exception {
        // Event URI and simple attributes
        Node graph = NodeFactory.createURI(Contexts.EVENTS.toString());
        Resource eventResource = ResourceFactory.createResource(event.getUri());
        Node eventType = NodeFactory.createURI(event.getType());
        updateBuilder.addInsert(graph, eventResource, RDF.type, eventType);
        
        addInsertInstantToUpdateBuilder(
                updateBuilder,
                graph,
                eventResource,
                event.getInstant());
        
        ConcernedItemDAO concernedItemDao = 
                new ConcernedItemDAO(user, Contexts.EVENTS.toString(), Oeev.concerns.getURI());
        concernedItemDao.addInsertToUpdateBuilder(updateBuilder, event.getConcernedItems());
        
        AnnotationDAO.addInsertToUpdateBuilder(updateBuilder, event.getAnnotations());
        PropertyDAO.addInsertLinksToUpdateBuilder(
                updateBuilder,
                eventResource,
                event.getProperties(),
                Contexts.EVENTS.toString(), 
                false);
    }
    
    /**
     * Inserts the given events in the storage.
     * @param events
     * @return the insertion result, with the error list or the URI of the events inserted
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    @Override
    public List<Event> create(List<Event> events) throws DAOPersistenceException, Exception {  
        setNewUris(events);
        for (Event event : events) {
            UpdateBuilder updateBuilder = new UpdateBuilder();
            addInsertToUpdateBuilder(updateBuilder, event);
            executeUpdateRequest(updateBuilder);
        }
        return events;
    }
    
    /**
     * Checks the given list of events.
     * @param events
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     * @throws opensilex.service.dao.exception.NotAnAdminException
     * @throws opensilex.service.dao.exception.DAODataErrorAggregateException
     */
    @Override
    public void validate(List<Event> events) 
            throws DAOPersistenceException, DAODataErrorAggregateException, NotAnAdminException {
            ArrayList<DAODataErrorException> exceptions = new ArrayList<>();

            // Check if user is admin
            UserDAO userDAO = new UserDAO();
            if (!userDAO.isAdmin(user)) {
                throw new NotAnAdminException();
            }
            else {
                ConcernedItemDAO concernedItemDao = 
                        new ConcernedItemDAO(user, Contexts.EVENTS.toString(), Oeev.concerns.getURI());
                PropertyDAO propertyDao = new PropertyDAO();
                AnnotationDAO annotationDao = new AnnotationDAO();
                try {
                    for (Event event : events) {

                        // Check the event URI if given (in case of an update)
                        if (event.getUri() != null) {
                            if (!existUri(event.getUri())){
                                exceptions.add(new UnknownUriException(event.getUri(), "the event"));
                            }
                        }

                        // Check Type
                        if (!existUri(event.getType())) {
                            exceptions.add(new UnknownUriException(event.getType(), "the event type"));
                        }

                        // Check concerned items
                        try {
                            concernedItemDao.validate(event.getConcernedItems());
                        }
                        catch (DAODataErrorAggregateException ex) {
                            exceptions.addAll(ex.getExceptions());
                        }

                        // Check properties
                        try {
                            propertyDao.checkExistenceRangeDomain(event.getUri(), event.getType(), event.getProperties());
                        }
                        catch (DAODataErrorAggregateException ex) {
                            exceptions.addAll(ex.getExceptions());
                        }

                        // Check annotations
                        try {
                            annotationDao.validate(event.getAnnotations());
                        }
                        catch (DAODataErrorAggregateException ex) {
                            exceptions.addAll(ex.getExceptions());
                        }
                    }
                } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
                    handleTriplestoreException(ex);
                }
            }

            if (exceptions.size() > 0) {
                throw new DAODataErrorAggregateException(exceptions);
            }
    }
    
    /**
     * Searches event properties and set them to it
     * @param event 
     */
    private void setEventProperties(Event event) throws DAOPersistenceException {
        PropertyDAO propertyDAO = new PropertyDAO();
        propertyDAO.getAllPropertiesWithLabelsExceptThoseSpecified(
            event, null, new ArrayList() {
                {
                    add(Rdf.RELATION_TYPE.toString());
                    add(Time.hasTime.getURI());
                    add(Oeev.concerns.getURI());
                }});
    }

    /**
     * Generates a query to count the results of the research with the 
     * searched parameters. 
     * @example 
     * SELECT DISTINCT  (COUNT(DISTINCT ?uri) AS ?count) 
     * WHERE {
     *   ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *   ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeev#MoveFrom> . 
     *   ?uri  <http://www.opensilex.org/vocabulary/oeev#concerns>  ?concernedItemUri  . 
     *   ?concernedItemUri  <http://www.w3.org/2000/01/rdf-schema#label>  ?concernedItemLabel  . 
     *   ?uri  <http://www.w3.org/2006/time#hasTime>  ?time  . 
     *   ?time  <http://www.w3.org/2006/time#inXSDDateTimeStamp>  ?dateTimeStamp  . 
     *   BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str(?dateTimeStamp)) as ?dateTime) .
     *   BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2017-09-08T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *   BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2019-10-08T12:00:00+01:00")) as ?dateRangeEndDateTime) .
     *   FILTER ( (regex (str(?uri), "http://www.phenome-fppn.fr/id/event/96e72788-6bdc-4f8e-abd1-ce9329371e8e", "i")) 
     *     && (regex (?concernedItemLabel, "Plot Lavalette", "i")) 
     *     && (regex (str(?concernedItemUri), "http://www.phenome-fppn.fr/m3p/arch/2017/c17000242", "i")) 
     *     && (?dateRangeStartDateTime <= ?dateTime) && (?dateRangeEndDateTime >= ?dateTime) ) 
     * }
     */
    private SPARQLQueryBuilder prepareCountQuery(String searchUri, String searchType, String searchConcernedItemLabel, String searchConcernedItemUri, Date dateRangeStart, Date dateRangeEnd) {
        SPARQLQueryBuilder query = this.prepareSearchQueryEvents(
                searchUri, 
                searchType, 
                searchConcernedItemLabel, 
                searchConcernedItemUri, 
                dateRangeStart, 
                dateRangeEnd);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT " + URI_SELECT_NAME_SPARQL + ") AS " + "?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }

    /**
     * Counts the total number of events filtered with the search fields
     * @param searchUri
     * @param searchType
     * @param searchConcernedItemLabel
     * @param searchConcernedItemUri
     * @param dateRangeStart
     * @param dateRangeEnd
     * @return results number
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    public Integer count(String searchUri, String searchType, String searchConcernedItemLabel, String searchConcernedItemUri, Date dateRangeStart, Date dateRangeEnd) 
            throws DAOPersistenceException, Exception {
        
        SPARQLQueryBuilder countQuery = prepareCountQuery(
                searchUri, 
                searchType, 
                searchConcernedItemLabel, 
                searchConcernedItemUri, 
                dateRangeStart, 
                dateRangeEnd);
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, countQuery.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        catch (QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
        catch (NumberFormatException ex) {
            handleCountValueNumberFormatException(ex);
        }
        return count;
    } 
    
    /**
     * Generates an delete query for the given event in the case of an update.
     * @param updateBuilder
     * @param event
     * @throws java.lang.Exception
     */
    public void addDeleteWhenUpdatingToUpdateBuilder(UpdateBuilder updateBuilder, Event event) throws Exception {        
        Node graph = NodeFactory.createURI(Contexts.EVENTS.toString());
        Resource eventResource = ResourceFactory.createResource(event.getUri());
        Resource eventTypeResource = ResourceFactory.createResource(event.getType());
        
        updateBuilder.addDelete(graph, eventResource, RDF.type, eventTypeResource);
        
        addDeleteInstantToUpdateBuilder(updateBuilder, graph, eventResource, event.getInstant());
        PropertyDAO.addDeletePropertyLinksToUpdateBuilder(updateBuilder, graph, eventResource, event.getProperties());
        ConcernedItemDAO.addDeleteConcernedItemLinksToUpdateBuilder(
                updateBuilder, 
                graph, 
                eventResource, 
                Oeev.concerns.getURI(),
                event.getConcernedItems());
    }

    @Override
    /**
     * @throws IllegalArgumentException 
     */
    public void delete(List<Event> events) throws DAOPersistenceException, Exception, IllegalArgumentException {
    	
    	ArrayList<String> uris = events.stream().map(event -> event.getUri()) 
    						.collect(Collectors.toCollection(ArrayList::new));
    	checkAndDeleteAll(uris);
    }
    
    /**
     * @apiNote
     * WARNING : delete an event trigger the deletion of all annotation which only have the event as target . 
     */
    @Override
    protected void deleteAll(List<String> uris) throws Exception, RepositoryException, UpdateExecutionException {
    	
    	AnnotationDAO annotationDAO = new AnnotationDAO(user);
    	annotationDAO.setConnection(connection); // make sure the two DAO use the same connection

    	for(String eventUri : uris) {
    		List<String> annotationUris = getAllAnnotationUrisWithEventAsTarget(eventUri);
    		
    		UpdateBuilder deleteEventQuery = deleteEventTriples(eventUri);
    		Update deleteEventUpdate = connection.prepareUpdate(QueryLanguage.SPARQL,deleteEventQuery.build().toString());
    		deleteEventUpdate.execute();
    		
    		if(! annotationUris.isEmpty())
    			annotationDAO.deleteAll(annotationUris);        	
    	}
    }
    
    /**
     * @return the {@link List} of {@link Annotation} which only have the given event uri as target.
     * @example
     * select distinct ?a <br>
 	 * where {  <br>
     * 		?a oa:hasTarget "http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1". <br>
     * 		?a oa:hasTarget ?t2. <br>
	 * FILTER ( 
	 * 		"http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1" = ?t2) <br>
	 * } <br> 
     * @param eventUri
     */
    protected List<String> getAllAnnotationUrisWithEventAsTarget(String eventUri) {
    	
    	Node a = NodeFactory.createVariable("a"), 
    		 t = NodeFactory.createVariable("t"),
    		 oaTargetPred = NodeFactory.createURI(Oa.RELATION_HAS_TARGET.toString()),
    		 eventNode = NodeFactory.createURI(eventUri),
    		 annotationGraph = NodeFactory.createURI(Contexts.ANNOTATIONS.toString());
    	
    	String removeAnnotationQuery = new SelectBuilder()
			.addVar(a)
			.addGraph(annotationGraph, new WhereBuilder() // specify the graph to query	
				.addWhere(a,oaTargetPred,eventNode)
				.addMinus(new WhereBuilder()
					.addWhere(a,oaTargetPred,t)
					.addFilter(new ExprFactory().ne(eventNode, t)))
			)
			.buildString();
    	
    	
    	List<String> annotationUris = new LinkedList<>();
    	TupleQuery getAnnotationQuery = getConnection().prepareTupleQuery(removeAnnotationQuery);
    	TupleQueryResult res = getAnnotationQuery.evaluate();
    	
    	while(res.hasNext()) {
    		BindingSet bs = res.next();
    		annotationUris.add(bs.getValue("a").stringValue());
    	}
    	return annotationUris;
    }
    
    /**
     * @return an {@link UpdateBuilder} producing a SPARQL query which remove all event triples
     * @example
     * PREFIX oeso: <http://www.opensilex.org/vocabulary/oeso#>
     * PREFIX time: <http://www.w3.org/2006/time#>
     * 
     * DELETE {
     * 		http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1> ?p ?o .
     * 	    ?s ?p1 <http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1> .
     *  	?time ?time_pred ?time_object 
     *  
     * }  WHERE { 
     * 			?s ?p1 <http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1>
     *         <http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1> ?p ?o ;
     *          																time:hasTime ?time. 
     *         ?time ?time_pred ?time_object .
     *     }   
     * }
     * @param eventUri : the URI of the {@link Event} to delete
     */
    protected UpdateBuilder deleteEventTriples(String eventUri) {
    	
    	Node  p = NodeFactory.createVariable("p"),  // Query variables 
         	  o = NodeFactory.createVariable("o"), 
         	  s = NodeFactory.createVariable("s"),
              p1 = NodeFactory.createVariable("o1"),
         	  time = NodeFactory.createVariable("time"), 
         	  timePred = NodeFactory.createVariable("time_pred"),
         	  timeObj = NodeFactory.createVariable("time_object");
                 	  
		 Node hasTimePred = NodeFactory.createURI(Time.hasTime.getURI()), // Query uris 
     		  eventNode = NodeFactory.createURI(eventUri);
     		 // eventGraph = NodeFactory.createURI(Contexts.EVENTS.toString());
      	
      	return new UpdateBuilder()   		
      		.addDelete(eventNode,p,o)
      		.addDelete(time,timePred,timeObj)
      		.addDelete(s,p1,eventNode)
			.addWhere(eventNode, p, o) // add the <s,p,annotation_uri> UNION <annotation_uri,p,o>
  			.addWhere(eventNode,hasTimePred,time)
  			.addWhere(time,timePred,timeObj) 
  			.addUnion(new WhereBuilder()
  				.addWhere(s,p1,eventNode)
  			);  
    }

    @Override
    public List<Event> update(List<Event> events) throws Exception {
        UpdateBuilder updateBuilder;
        for(Event event : events) {
            updateBuilder = new UpdateBuilder();
            Event oldEvent = findById(event.getUri());
            addDeleteWhenUpdatingToUpdateBuilder(updateBuilder, oldEvent);
            executeUpdateRequest(updateBuilder);
            
            updateBuilder = new UpdateBuilder();
            addInsertToUpdateBuilder(updateBuilder, event);   
            executeUpdateRequest(updateBuilder);
        }
        return events;
    }

    @Override
    public Event find(Event object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /** 
     * Adds a filter to the search query comparing a SPARQL dateTimeStamp 
     * variable to a date. 
     * SPARQL dateTimeStamp dates have to be handled in a specific way as 
     * the comparison operators (<, >, etc.) aren't available for dateTimeStamp
     * objects.
     * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">
     * SparQL Operator Mapping
     * </a>
     * @param query
     * @param filterDate
     * @param filterDateSparqlVariable SPARQL variable (?abc format)
     * @param comparisonSign e.g >, >=, <, <= 
     * @param dateTimeStampToCompareSparqlVariable the SPARQL variable 
     * (?abc format) of the dateTimeStamp to which the date has to be compared
     * @example SparQL code added to the query :
     *   BIND(xsd:dateTime(str("2017-09-10T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *   FILTER ( (?dateRangeStartDateTime <= ?dateTime) ) 
     */
    public static void filterSearchQueryWithDateTimeStampComparison(SPARQLStringBuilder query, Date filterDate, String filterDateSparqlVariable, String comparisonSign, String dateTimeStampToCompareSparqlVariable){
        
        String filterDateStringInSparqlDateTimeStampFormat = 
                DateTimeFormat.forPattern(DATETIMESTAMP_FORMAT_SPARQL).print(new DateTime(filterDate));

        query.appendToBody(
                "\nBIND(<" + Xsd.FUNCTION_DATETIME.toString() + ">(str(\"" 
                + filterDateStringInSparqlDateTimeStampFormat + "\")) as " + filterDateSparqlVariable + ") .");
        
        query.appendAndFilter(filterDateSparqlVariable + comparisonSign + dateTimeStampToCompareSparqlVariable);
    }

    /**
     * Appends a filter to select only the results whose datetime is included in 
     * the date range in parameter.
     * @param query
     * @param filterRangeStartDate
     * @param filterRangeEndDate
     * @param dateTimeStampToCompareSparqlName the SPARQL variable (?abc 
     * format) of the dateTimeStamp to compare to the range
     * @example SparQL code added to the query :
     *   BIND(xsd:dateTime(str(?dateTimeStamp)) as ?dateTime) .
     *   BIND(xsd:dateTime(str("2017-09-10T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *   BIND(xsd:dateTime(str("2017-09-12T12:00:00+01:00")) as ?dateRangeEndDateTime) .
     *   FILTER ( (?dateRangeStartDateTime <= ?dateTime) && (?dateRangeEndDateTime >= ?dateTime) ) 
     */
    public static void filterSearchQueryWithDateRangeComparisonWithDateTimeStamp(SPARQLQueryBuilder query, String objectUriLinkedToInstant, String instantSparqlName, Date filterRangeStartDate, Date filterRangeEndDate, String dateTimeStampToCompareSparqlName, boolean inGroupBy) {
        
        query.appendSelect(instantSparqlName);
        query.appendSelect(dateTimeStampToCompareSparqlName);
        if (inGroupBy) {
            query.appendGroupBy(instantSparqlName);
            query.appendGroupBy(dateTimeStampToCompareSparqlName);
        }
        query.appendTriplet(objectUriLinkedToInstant, Time.hasTime.toString(), instantSparqlName, null);
        query.appendTriplet(instantSparqlName, Time.inXSDDateTimeStamp.toString(), dateTimeStampToCompareSparqlName, null);
        
        query.appendToBody("\nBIND(<" + Xsd.FUNCTION_DATETIME.toString() + ">(str(" + dateTimeStampToCompareSparqlName + ")) as " + DATETIME_SELECT_NAME_SPARQL + ") .");
        
        if (filterRangeStartDate != null) {

            filterSearchQueryWithDateTimeStampComparison(
                query, 
                filterRangeStartDate, 
                DATE_RANGE_START_DATETIME_SELECT_NAME_SPARQL, 
                " <= ", 
                DATETIME_SELECT_NAME_SPARQL);
        }
        
        if (filterRangeEndDate != null){
            
            filterSearchQueryWithDateTimeStampComparison(
                    query, 
                    filterRangeEndDate, 
                    DATE_RANGE_END_DATETIME_SELECT_NAME_SPARQL, 
                    " >= ", 
                    DATETIME_SELECT_NAME_SPARQL);
        }
    }
    
    /**
     * Inserts an Instant linked to the given URI in the given graph with the given date value.
     * @param updateBuilder
     * @param graph
     * @param resourceLinkedToInstant
     * @param instant
     * @throws java.lang.Exception
     */
    public static void addInsertInstantToUpdateBuilder(UpdateBuilder updateBuilder, Node graph, Resource resourceLinkedToInstant, Instant instant) 
            throws Exception {
        // Add insert instant URI with type
        String instantUri = UriGenerator.generateNewInstanceUri(Time.Instant.toString(), null, null);
        Resource instantResource = ResourceFactory.createResource(instantUri);
        updateBuilder.addInsert(graph, instantResource, RDF.type, Time.Instant);

        // Add date time stamp to instant
        Literal dateTimeLiteral = getLiteralFromDateTime(instant.getDateTime());
        updateBuilder.addInsert(graph, instantResource, Time.inXSDDateTimeStamp, dateTimeLiteral);

        // Link resource to instant
        updateBuilder.addInsert(graph, resourceLinkedToInstant, Time.hasTime, instantResource);
    }
    
    /**
     * Adds a delete statement to an update builder for an Instant linked to the given URI in the given graph. 
     * @param updateBuilder
     * @param graph
     * @param linkedResource
     * @param instant
     * @throws java.lang.Exception
     */
    public static void addDeleteInstantToUpdateBuilder(UpdateBuilder updateBuilder, Node graph, Resource linkedResource, Instant instant) 
            throws Exception {
        Resource instantResource = ResourceFactory.createResource(instant.getUri());
        Literal dateTimeLiteral = getLiteralFromDateTime(instant.getDateTime());
        updateBuilder.addDelete(graph, instantResource, RDF.type, Time.Instant);
        updateBuilder.addDelete(graph, instantResource, Time.inXSDDateTimeStamp, dateTimeLiteral);
        updateBuilder.addDelete(graph, linkedResource, Time.hasTime, instantResource);
    }
    
    /**
     * Return instant from BindingSet string value
     * @param bindingSet
     * @param instantUriSelectName
     * @param dateTimeStampSelectName
     * @return 
     */
    public static Instant getInstantFromBindingSet(BindingSet bindingSet, String instantUriSelectName, String dateTimeStampSelectName) {
        String instantDateTimeString = getStringValueOfSelectNameFromBindingSet(dateTimeStampSelectName, bindingSet);    
        String instantUri = getStringValueOfSelectNameFromBindingSet(instantUriSelectName, bindingSet);    
        DateTime InstantDateTime = 
                Dates.stringToDateTimeWithGivenPattern(instantDateTimeString, DateFormat.YMDTHMSZZ.toString());
        return new Instant(instantUri, InstantDateTime);
    }
    
    /**
     * Builds a Literal date from a Datetime date.
     * @param datetime
     * @return 
     */
    public static Literal getLiteralFromDateTime(DateTime datetime) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DATETIMESTAMP_FORMAT_SPARQL);
        return ResourceFactory.createTypedLiteral(datetime.toString(formatter), XSDDatatype.XSDdateTime);
    }
}
