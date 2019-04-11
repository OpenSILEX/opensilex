//******************************************************************************
//                          EventDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12  nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Oeev;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Time;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Event;
import phis2ws.service.view.model.phis.Property;

/**
 * DAO for Events
 * @update [Andreas Garcia] 14 Feb., 2019: Add event detail service
 * @update [Andreas Garcia] 5 March, 2019: 
 *      Add events insertion service
 *      Move the generic function to get a string value from a binding set to DAOSesame
 *      Move concerned items accesses handling into a new ConcernedItemDAOSesame class
 * @update [Andreas Garcia] 19 March, 2019: remove annotations handling
 * @author Andreas Garcia <andreas.garcia@inra.fr>
 */
public class EventDAOSesame extends DAOSesame<Event> {
    final static Logger LOGGER = LoggerFactory.getLogger(EventDAOSesame.class);
    
    private static final String TIME_SELECT_NAME = "time";
    private static final String TIME_SELECT_NAME_SPARQL = "?" + TIME_SELECT_NAME;
    
    private static final String DATETIMESTAMP_SELECT_NAME = "dateTimeStamp";
    private static final String DATETIMESTAMP_SELECT_NAME_SPARQL = "?" + DATETIMESTAMP_SELECT_NAME;

    public EventDAOSesame(User user) {
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
     *    ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeev#MoveFrom> . 
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
        query.appendTriplet(uriSelectNameSparql, Rdf.RELATION_TYPE.toString(), RDF_TYPE_SELECT_NAME_SPARQL, null);
        if (searchType != null) {
            query.appendTriplet(RDF_TYPE_SELECT_NAME_SPARQL, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", searchType, null);
        } else {
            query.appendTriplet(RDF_TYPE_SELECT_NAME_SPARQL, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeev.Event.getURI(), null);
        }    
    }

    /**
     * Sets a search query to select a datetime from an instant and to filter 
     * according to it if necessary
     * @example SparQL filter added:
     *  SELECT DISTINCT ?dateTimeStamp
     *  WHERE {
     *    ?uri  <http://www.w3.org/2006/time#hasTime>  ?time  . 
     *    ?time  <http://www.w3.org/2006/time#inXSDDateTimeStamp>  ?dateTimeStamp  . 
     *    BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str(?dateTimeStamp)) as ?dateTime) .
     *    BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2017-09-10T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *    BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2017-09-12T12:00:00+01:00")) as ?dateRangeEndDateTime) .
     *  }
     *  GROUP BY ?dateTimeStamp
     * @param query
     * @param uriSelectNameSparql
     * @param searchDateTimeRangeStartString
     * @param searchDateTimeRangeEndString
     * @param inGroupBy
     */
    private void prepareSearchQueryDateTime(SPARQLQueryBuilder query, String uriSelectNameSparql, String searchDateTimeRangeStartString, String searchDateTimeRangeEndString, boolean inGroupBy) {  
        
        query.appendSelect(DATETIMESTAMP_SELECT_NAME_SPARQL);
        if (inGroupBy) {
            query.appendGroupBy(DATETIMESTAMP_SELECT_NAME_SPARQL);
        }
        query.appendTriplet(uriSelectNameSparql, Time.hasTime.toString(), TIME_SELECT_NAME_SPARQL, null);
        query.appendTriplet(TIME_SELECT_NAME_SPARQL, Time.inXSDDateTimeStamp.toString(), DATETIMESTAMP_SELECT_NAME_SPARQL, null);
        
        if (searchDateTimeRangeStartString != null || searchDateTimeRangeEndString != null) {
            TimeDAOSesame timeDao = new TimeDAOSesame(this.user);
            timeDao.filterSearchQueryWithDateRangeComparisonWithDateTimeStamp(
                    query, 
                    DateFormat.YMDTHMSZZ.toString(), 
                    searchDateTimeRangeStartString, 
                    searchDateTimeRangeEndString, 
                    DATETIMESTAMP_SELECT_NAME_SPARQL);
        }
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
    protected SPARQLQueryBuilder prepareSearchQueryEvents(String uri, String type, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String uriSelectNameSparql = prepareSearchQueryUri(query, uri, true);
        prepareSearchQueryType(query, uriSelectNameSparql, type, true); 
        ConcernedItemDAOSesame.prepareQueryWithConcernedItemFilters(query, uriSelectNameSparql, Oeev.concerns.getURI(), searchConcernedItemUri, searchConcernedItemLabel); 
        prepareSearchQueryDateTime(query, uriSelectNameSparql, dateRangeStartString, dateRangeEndString, true); 
        
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
    protected SPARQLQueryBuilder prepareSearchQueryEventDetailed(String searchUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String uriSelectNameSparql = prepareSearchQueryUri(query, searchUri, false);
        prepareSearchQueryType(query, uriSelectNameSparql, null, false);  
        ConcernedItemDAOSesame.prepareQueryWithConcernedItemFilters(query, uriSelectNameSparql, Oeev.concerns.getURI(), null, null); 
        prepareSearchQueryDateTime(query, uriSelectNameSparql, null, null, false); 
        
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
        
        String eventDateTimeString = getStringValueOfSelectNameFromBindingSet(DATETIMESTAMP_SELECT_NAME, bindingSet);    
        DateTime eventDateTime = null;
        if (eventDateTimeString != null) {
            eventDateTime = Dates.stringToDateTimeWithGivenPattern(eventDateTimeString, DateFormat.YMDTHMSZZ.toString());
        }
        
        return new Event(eventUri, eventType, new ArrayList<>(), eventDateTime, new ArrayList<>(), null);
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
     */
    public ArrayList<Event> searchEvents(String searchUri, String searchType, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString, int searchPage, int searchPageSize) {
        
        setPage(searchPage);
        setPageSize(searchPageSize);
        
        SPARQLQueryBuilder eventsQuery = prepareSearchQueryEvents(searchUri, searchType, searchConcernedItemLabel, searchConcernedItemUri, dateRangeStartString, dateRangeEndString);
        
        // get events from storage
        TupleQuery eventsTupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, eventsQuery.toString());
        
        ArrayList<Event> events;
        // for each event, set its properties and concerned Items
        try (TupleQueryResult eventsResult = eventsTupleQuery.evaluate()) {
            events = new ArrayList<>();
            while (eventsResult.hasNext()) {
                Event event = getEventFromBindingSet(eventsResult.next());
                searchEventPropertiesAndSetThemToIt(event);
                ConcernedItemDAOSesame concernedItemDao = new ConcernedItemDAOSesame(user);
                event.setConcernedItems(concernedItemDao.searchConcernedItems(event.getUri(), Oeev.concerns.getURI(), null, null, 0, pageSizeMaxValue));
                events.add(event);
            }
        }
        return events;
    }
    
    /**
     * Generates an insert query for the given event
     * @param event
     * @return the query
     * @example
     */
    private UpdateRequest prepareInsertQuery(Event event) {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        
        // Event URI and simple attributes
        Node graph = NodeFactory.createURI(Contexts.EVENTS.toString());
        Resource eventResource = ResourceFactory.createResource(event.getUri());
        Node eventType = NodeFactory.createURI(event.getType());
        updateBuilder.addInsert(graph, eventResource, RDF.type, eventType);
        
        // Event's Instant
        TimeDAOSesame timeDao = new TimeDAOSesame(this.user);
        try {
            timeDao.addInsertToUpdateBuilderWithInstant(
                    updateBuilder,
                    graph,
                    eventResource,
                    event.getDateTime());
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage());
        }
        
        UpdateRequest query = updateBuilder.buildRequest();
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * Inserts the given events in the storage.
     * /!\ Prerequisite: data must have been checked before calling this method
     * @see EventDAOSesame#check(java.util.List) 
     * @param events
     * @return the insertion result, with the error list or the URI of the 
     *         events inserted
     */
    private POSTResultsReturn insert(List<Event> events) {
        List<Status> status = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false;
        boolean noInsertionError = true;
        
        ConcernedItemDAOSesame concernedItemDao = new ConcernedItemDAOSesame(user);
        AnnotationDAOSesame annotationDao = new AnnotationDAOSesame(user);
        PropertyDAOSesame propertyDao = new PropertyDAOSesame();
        
        UriGenerator uriGenerator = new UriGenerator();
        getConnection().begin();
        for (Event event : events) {
            try {
                // Generate uri
                event.setUri(uriGenerator.generateNewInstanceUri(Oeev.Event.getURI(), null, null));
                // Insert event
                UpdateRequest query = prepareInsertQuery(event);

                Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
                prepareUpdate.execute();
                createdResourcesUris.add(event.getUri());
                
                Resource eventResource = ResourceFactory.createResource(event.getUri());

                // Insert concerned items links
                concernedItemDao.insertLinksWithObject(Contexts.EVENTS.toString(), eventResource, Oeev.concerns.getURI(), event.getConcernedItems());
        
                // The annotation
                ArrayList<String> annotationTargets = new ArrayList<>();
                annotationTargets.add(event.getUri());
                event.getAnnotations().forEach(annotation -> {
                    annotation.setTargets(annotationTargets);
                });
                annotationDao.insert(event.getAnnotations());

                // The properties links
                ArrayList<Property> properties = event.getProperties();
                if (!properties.isEmpty()) {
                    propertyDao.insertLinksBetweenObjectAndProperties(eventResource, properties, Contexts.EVENTS.toString(), false);
                }
            } catch (RepositoryException ex) {
                    LOGGER.error(StatusCodeMsg.ERROR_WHILE_COMMITTING_OR_ROLLING_BACK_TRIPLESTORE_STATEMENT, ex);
                    noInsertionError = false;
            } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    noInsertionError = false;
                    status.add(new Status(
                            StatusCodeMsg.QUERY_ERROR, 
                            StatusCodeMsg.ERR, 
                            StatusCodeMsg.MALFORMED_CREATE_QUERY + " " + e.getMessage()));
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
                noInsertionError = false;
            }
        }
        
        if (noInsertionError) {
            resultState = true;
            getConnection().commit();
        } else {
            getConnection().rollback();
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        results = new POSTResultsReturn(resultState, noInsertionError, true);
        results.statusList = status;
        results.setCreatedResources(createdResourcesUris);
        if (resultState && !createdResourcesUris.isEmpty()) {
            results.createdResources = createdResourcesUris;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUris.size() + " " + StatusCodeMsg.RESOURCES_CREATED));
        }
        
        return results;
    }
    
    /**
     * Checks and eventually inserts events in the storage
     * @param events
     * @return the insertion result :
     *           Error message if errors found in data
     *           The list of the generated URIs events is inserted
     */
    public POSTResultsReturn checkAndInsert(List<Event> events) {
        POSTResultsReturn checkResult = check(events);
        if (checkResult.getDataState()) {
            return insert(events);
        } else {
            return checkResult;
        }
    }
    
    /**
     * Checks the given list of events
     * @param events
     * @return the result with the list of the found errors (empty if no error)
     */
    public POSTResultsReturn check(List<Event> events) {
        POSTResultsReturn checkResult;
        List<Status> status = new ArrayList<>();
        
        // 1. Check if user is admin
        UserDaoPhisBrapi userDAO = new UserDaoPhisBrapi();
        if (userDAO.isAdmin(user)) {
            ConcernedItemDAOSesame concernedItemDAO = new ConcernedItemDAOSesame(user);
            PropertyDAOSesame propertyDAO = new PropertyDAOSesame();
            AnnotationDAOSesame annotationDao = new AnnotationDAOSesame();
            for (Event event : events) {
                String eventUri = event.getUri();
                
                // Check the event URI if given (in case of an update)
                if (eventUri != null) {
                    if (searchEvents(eventUri, null, null, null, null, null, 0, pageSizeMaxValue).isEmpty()){
                        status.add(new Status(
                                StatusCodeMsg.UNKNOWN_URI, 
                                StatusCodeMsg.ERR, 
                                StatusCodeMsg.UNKNOWN_EVENT_URI + " " + eventUri));
                    }
                }
                
                // Check Type
                if (!existUri(event.getType())) {
                    status.add(new Status(
                            StatusCodeMsg.UNKNOWN_URI, 
                            StatusCodeMsg.ERR, 
                            StatusCodeMsg.UNKNOWN_TYPE + " " + event.getType()));
                }
                
                // Check concerned items
                status.addAll(concernedItemDAO.check(event.getConcernedItems()).getStatusList());
                
                // Check properties
                ArrayList<Property> properties = event.getProperties();
                POSTResultsReturn propertiesResult = propertyDAO.checkExistenceRangeDomain(properties, event.getType());
                status.addAll(propertiesResult.getStatusList());
                
                // Check annotations
                POSTResultsReturn annotationsResult = annotationDao.check(event.getAnnotations());
                status.addAll(annotationsResult.getStatusList());
            }
        } else {
            status.add(new Status(
                    StatusCodeMsg.ACCESS_DENIED, 
                    StatusCodeMsg.ERR, 
                    StatusCodeMsg.ADMINISTRATOR_ONLY));
        }
        
        boolean dataIsValid = status.isEmpty();
        checkResult = new POSTResultsReturn(dataIsValid, null, dataIsValid);
        checkResult.statusList = status;
        return checkResult;   
    }
    
    /**
     * Searches event properties and set them to it
     * @param event 
     */
    private void searchEventPropertiesAndSetThemToIt(Event event) {
        PropertyDAOSesame propertyDAO = new PropertyDAOSesame();
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
    private SPARQLQueryBuilder prepareCountQuery(String searchUri, String searchType, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString) {
        SPARQLQueryBuilder query = this.prepareSearchQueryEvents(
                searchUri, 
                searchType, 
                searchConcernedItemLabel, 
                searchConcernedItemUri, 
                dateRangeStartString, 
                dateRangeEndString);
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
     * @param dateRangeStartString
     * @param dateRangeEndString
     * @return results number
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public Integer count(String searchUri, String searchType, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString) 
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        
        SPARQLQueryBuilder countQuery = prepareCountQuery(
                searchUri, 
                searchType, 
                searchConcernedItemLabel, 
                searchConcernedItemUri, 
                dateRangeStartString, 
                dateRangeEndString);
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, countQuery.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        return count;
    }

    @Override
    public List<Event> create(List<Event> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Event> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Event> update(List<Event> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Event find(Event object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Event findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
