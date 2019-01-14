//******************************************************************************
//                          EventDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12  nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.model.User;
import phis2ws.service.ontologies.Oeev;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Time;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.ConcernItem;
import phis2ws.service.view.model.phis.Event;

/**
 * Dao for Events
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventDAOSesame extends DAOSesame<Event> {
    final static Logger LOGGER = LoggerFactory.getLogger(EventDAOSesame.class);
    
    // constants used for SPARQL variables
    private static final String URI_VARIABLE_SPARQL = "?" + URI;
    private static final String TYPE_VARIABLE_SPARQL = "?" + RDF_TYPE;
        
    private static final String CONCERNED_ITEM_URI_VARIABLE = "concernedItemUri";
    private static final String CONCERNED_ITEM_URI_VARIABLE_SPARQL = "?" + CONCERNED_ITEM_URI_VARIABLE;
    private static final String CONCERNED_ITEM_TYPE_VARIABLE = "concernedItemType";
    private static final String CONCERNED_ITEM_TYPE_VARIABLE_SPARQL = "?" + CONCERNED_ITEM_TYPE_VARIABLE;
    private static final String CONCERNED_ITEM_LABEL_VARIABLE = "concernedItemLabel";
    private static final String CONCERNED_ITEM_LABEL_VARIABLE_SPARQL = "?" + CONCERNED_ITEM_LABEL_VARIABLE;
    private static final String CONCERNED_ITEM_LABELS_VARIABLE = "concernedItemLabels";
    private static final String CONCERNED_ITEM_LABELS_VARIABLE_SPARQL = "?" + CONCERNED_ITEM_LABELS_VARIABLE;
    
    private static final String TIME_VARIABLE = "time";
    private static final String TIME_VARIABLE_SPARQL = "?" + TIME_VARIABLE;
    
    private static final String DATETIMESTAMP_VARIABLE = "dateTimeStamp";
    private static final String DATETIMESTAMP_VARIABLE_SPARQL = "?" + DATETIMESTAMP_VARIABLE;
    
    /**
     * Set a search query to select an URI and to filter according to it 
     * if necessary
     * @return the URI SPARQL variable
     * @example SparQL filter added :
     * SELECT DISTINCT  ?uri
     * FILTER ( (regex (str(?uri), "http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1", "i"))
     * GROUP BY ?uri
     */
    private String prepareSearchQueryUri(SPARQLQueryBuilder query, String searchUri){
        query.appendSelect(URI_VARIABLE_SPARQL);
        query.appendGroupBy(URI_VARIABLE_SPARQL);
        if (searchUri != null) {
            query.appendAndFilter("regex (str(" + URI_VARIABLE_SPARQL + ")" + ", \"" + searchUri + "\", \"i\")");
        }
        return URI_VARIABLE_SPARQL;
    }
    
    /**
     * Set a search query to select a type and to filter according to it 
     * if necessary
     * @example SparQL filter added :
     *  SELECT DISTINCT ?rdfType
     *  ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.phenome-fppn.fr/vocabulary/2018/oeev#MoveFrom> . 
     *  GROUP BY ?rdfType
     */
    private void prepareSearchQueryType(SPARQLQueryBuilder query, String sparqlVariableUri, String searchType){
        query.appendSelect(TYPE_VARIABLE_SPARQL);
        query.appendGroupBy(TYPE_VARIABLE_SPARQL);
        query.appendTriplet(sparqlVariableUri, Rdf.RELATION_TYPE.toString(), TYPE_VARIABLE_SPARQL, null);
        if (searchType != null) {
            query.appendTriplet(TYPE_VARIABLE_SPARQL, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", searchType, null);
        } else {
            query.appendTriplet(TYPE_VARIABLE_SPARQL, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeev.CONCEPT_EVENT.toString(), null);
        }    
    }
    
    /**
     * Set a search query to applies the concerned items label filter. 
     * This function DOES NOT make the query return the events concerned items 
     * informations. This is done by another query further in the process.
     * @example SparQL filter added :
     *  ?uri  <http://www.phenome-fppn.fr/vocabulary/2018/oeev#concerns>  ?concernedItemUri  . 
     *  ?concernedItemUri  <http://www.w3.org/2000/01/rdf-schema#label>  ?concernedItemLabel  . 
     */
    private void prepareSearchQueryConcernedItemFilter(SPARQLQueryBuilder query, String sparqlVariableUri, String searchConcernedItemLabel, String searchConcernedItemUri){

        if (searchConcernedItemLabel != null || searchConcernedItemUri != null) {
            query.appendTriplet(sparqlVariableUri, Oeev.RELATION_CONCERNS.toString(), CONCERNED_ITEM_URI_VARIABLE_SPARQL, null);
            
            if (searchConcernedItemLabel != null){
                query.appendTriplet(CONCERNED_ITEM_URI_VARIABLE_SPARQL, Rdfs.RELATION_LABEL.toString(), CONCERNED_ITEM_LABEL_VARIABLE_SPARQL, null);
                
                query.appendAndFilter("regex(" + CONCERNED_ITEM_LABEL_VARIABLE_SPARQL + ", \"" + searchConcernedItemLabel + "\", \"i\")");
            }
            
            if (searchConcernedItemUri != null){
                query.appendAndFilter("regex (str(" + CONCERNED_ITEM_URI_VARIABLE_SPARQL + ")" + ", \"" + searchConcernedItemUri + "\", \"i\")");
            }
        }
    }

    /**
     * Set a search query to select a datetime from an instant and to filter 
     * according to it if necessary
     * @example SparQL filter added :
     * SELECT DISTINCT ?dateTimeStamp
     * ?uri  <http://www.w3.org/2006/time#hasTime>  ?time  . 
     * ?time  <http://www.w3.org/2006/time#inXSDDateTimeStamp>  ?dateTimeStamp  . 
     * BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str(?dateTimeStamp)) as ?dateTime) .
     * BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2017-09-10T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     * BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2017-09-12T12:00:00+01:00")) as ?dateRangeEndDateTime) .
     * GROUP BY ?dateTimeStamp
     */
    private void prepareSearchQueryDateTime(SPARQLQueryBuilder query, String sparqlVariableUri, String searchDateTimeRangeStartString, String searchDateTimeRangeEndString){  
        
        query.appendSelect(DATETIMESTAMP_VARIABLE_SPARQL);
        query.appendGroupBy(DATETIMESTAMP_VARIABLE_SPARQL);
        query.appendTriplet(sparqlVariableUri, Time.RELATION_HAS_TIME.toString(), TIME_VARIABLE_SPARQL, null);
        query.appendTriplet(TIME_VARIABLE_SPARQL, Time.RELATION_IN_XSD_DATETIMESTAMP.toString(), DATETIMESTAMP_VARIABLE_SPARQL, null);
        
        if (searchDateTimeRangeStartString != null || searchDateTimeRangeEndString != null) {
            filterSearchQueryWithDateRangeComparisonWithDateTimeStamp(query, DateFormat.YMDTHMSZZ.toString(), searchDateTimeRangeStartString, searchDateTimeRangeEndString, DATETIMESTAMP_VARIABLE_SPARQL);
        }
    }
    
    /**
     * Prepare the event search query
     * @param eventSearchParameters
     * @param searchConcernedItemLabel
     * @param searchConcernedItemUri
     * @param dateRangeStartString
     * @param dateRangeEndString
     * @param user
     * @return query
     * @example
     * SELECT DISTINCT  ?uri ?rdfType ?dateTimeStamp 
     * WHERE {
     *   ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *   ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.phenome-fppn.fr/vocabulary/2018/oeev#MoveFrom> . 
     *   ?uri  <http://www.phenome-fppn.fr/vocabulary/2018/oeev#concernd>  ?concernedItemUri  . 
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
     */
    protected SPARQLQueryBuilder prepareSearchQuery(Event eventSearchParameters, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString, User user) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String sparqlVariableUri = prepareSearchQueryUri(query, eventSearchParameters.getUri());
        prepareSearchQueryType(query, sparqlVariableUri, eventSearchParameters.getType());  
        prepareSearchQueryConcernedItemFilter(query, sparqlVariableUri, searchConcernedItemLabel, searchConcernedItemUri); 
        prepareSearchQueryDateTime(query, sparqlVariableUri, dateRangeStartString, dateRangeEndString); 
        
        query.appendLimit(getPageSize());
        query.appendOffset(getPage() * getPageSize());
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Prepare the query to search the concerned items of an event
     * @param eventUri
     * @return query
     * @example
     * SELECT DISTINCT  ?concernedItemUri ?concernedItemType 
     * (GROUP_CONCAT(DISTINCT ?concernedItemLabel; SEPARATOR=",") AS ?concernedItemLabels) 
     * WHERE {
     *  <http://www.phenome-fppn.fr/id/event/96e72788-6bdc-4f8e-abd1-ce9329371e8e>  <http://www.phenome-fppn.fr/vocabulary/2018/oeev#concerns>  ?concernedItemUri  . 
     *  ?concernedItemUri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?concernedItemType  . 
     *  ?concernedItemUri  <http://www.w3.org/2000/01/rdf-schema#label>  ?concernedItemLabel  . 
     * }
     *  GROUP BY  ?concernedItemUri ?concernedItemType 
     */
    protected SPARQLQueryBuilder prepareConcernedItemsSearchQuery(String eventUri) {
        
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String sparqlVariableUri = "<" + eventUri + ">";
        
        query.appendSelect(CONCERNED_ITEM_URI_VARIABLE_SPARQL);
        query.appendGroupBy(CONCERNED_ITEM_URI_VARIABLE_SPARQL);
        query.appendTriplet(sparqlVariableUri, Oeev.RELATION_CONCERNS.toString(), CONCERNED_ITEM_URI_VARIABLE_SPARQL, null);
        
        query.appendSelect(CONCERNED_ITEM_TYPE_VARIABLE_SPARQL);
        query.appendGroupBy(CONCERNED_ITEM_TYPE_VARIABLE_SPARQL);
        query.appendTriplet(CONCERNED_ITEM_URI_VARIABLE_SPARQL, Rdf.RELATION_TYPE.toString(), CONCERNED_ITEM_TYPE_VARIABLE_SPARQL, null);
         
        query.appendTriplet(CONCERNED_ITEM_URI_VARIABLE_SPARQL, Rdfs.RELATION_LABEL.toString(), CONCERNED_ITEM_LABEL_VARIABLE_SPARQL, null);
        
        query.appendSelectConcat(CONCERNED_ITEM_LABEL_VARIABLE_SPARQL, SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR, CONCERNED_ITEM_LABELS_VARIABLE_SPARQL);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Get an event from a given binding set.
     * @param bindingSet a binding set, result from a search query
     * @return an event target with data extracted from the given binding set
     */
    private Event getEventFromBindingSet(BindingSet bindingSet) {
          
        String eventUri = getValueOfVariableFromBindingSet(URI, bindingSet);
                
        String eventType = getValueOfVariableFromBindingSet(RDF_TYPE, bindingSet);
        
        String eventDateTimeString = getValueOfVariableFromBindingSet(DATETIMESTAMP_VARIABLE, bindingSet);    
        DateTime eventDateTime = null;
        if (eventDateTimeString != null) {
            eventDateTime = Dates.stringToDateTimeWithGivenPattern(eventDateTimeString, DateFormat.YMDTHMSZZ.toString());
        }
        
        return new Event(eventUri, eventType, new ArrayList<>(), eventDateTime, new ArrayList<>());
    }
    
    /**
     * Get a concerned item from a binding set
     * @param bindingSet
     * @return concerned item
     */
    private ConcernItem getConcernedItemFromBindingSet(BindingSet bindingSet){
                
        String concernedItemUri = getValueOfVariableFromBindingSet(CONCERNED_ITEM_URI_VARIABLE, bindingSet);
        String concernedItemType = getValueOfVariableFromBindingSet(CONCERNED_ITEM_TYPE_VARIABLE, bindingSet);
        
        String concernedItemLabelsConcatenated = getValueOfVariableFromBindingSet(CONCERNED_ITEM_LABELS_VARIABLE, bindingSet);
        ArrayList<String> concernedItemLabels = new ArrayList<>(Arrays.asList(concernedItemLabelsConcatenated.split(SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));

        return new ConcernItem(concernedItemUri, concernedItemType, concernedItemLabels);
    }
    
    /**
     * Search events stored
     * @param eventSearchParameters
     * @param searchConcernedItemLabel
     * @param searchConcernedItemUri
     * @param dateRangeStartString
     * @param dateRangeEndString
     * @param user
     * @param searchPage
     * @param searchPageSize
     * @return events
     */
    public ArrayList<Event> searchEvents(Event eventSearchParameters, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString, User user, int searchPage, int searchPageSize) {
        
        SPARQLQueryBuilder eventsQuery = prepareSearchQuery(eventSearchParameters, searchConcernedItemLabel, searchConcernedItemUri, dateRangeStartString, dateRangeEndString, user);
        
        setPage(searchPage);
        setPageSize(searchPageSize);
        
        // get events from storage
        TupleQuery eventsTupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, eventsQuery.toString());
        
        ArrayList<Event> events;
        // for each event, set its properties and concerned Items
        try (TupleQueryResult eventsResult = eventsTupleQuery.evaluate()) {
            events = new ArrayList<>();
            while (eventsResult.hasNext()) {
                Event event = getEventFromBindingSet(eventsResult.next());
                searchEventPropertiesAndSetThemToIt(event);
                searchEventConcernedItemsAndSetThemToIt(event);
                events.add(event);
            }
        }
        return events;
    }
    
    /**
     * Search event properties and set them to it
     * @param event 
     */
    private void searchEventPropertiesAndSetThemToIt(Event event){

        PropertyDAOSesame propertyDAO = new PropertyDAOSesame(event.getUri());
        propertyDAO.getAllPropertiesWithLabelsExceptThoseSpecified(
                event, null, new ArrayList(){
                    {
                        add(Rdf.RELATION_TYPE.toString());
                        add(Time.RELATION_HAS_TIME.toString());
                        add(Oeev.RELATION_CONCERNS.toString());
                    }});
    }
    
    /**
     * Search event concerned items and set them to it
     * @param event 
     */
    private void searchEventConcernedItemsAndSetThemToIt(Event event){
                
        SPARQLQueryBuilder concernedItemsQuery = prepareConcernedItemsSearchQuery(event.getUri());
        TupleQuery concernedItemsTupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, concernedItemsQuery.toString());

        try (TupleQueryResult concernedItemsTupleQueryResult = concernedItemsTupleQuery.evaluate()) {
            ConcernItem concernedItem;
            while(concernedItemsTupleQueryResult.hasNext()){
                concernedItem = getConcernedItemFromBindingSet(concernedItemsTupleQueryResult.next());
                event.addConcernedItem(concernedItem);
            }
        }
    }
    
    /**
     * Get the value of a variable from a binding set
     * @param variableName 
     * @param bindingSet 
     */
    private String getValueOfVariableFromBindingSet(String variableName, BindingSet bindingSet){ 
        Value selectedFieldValue = bindingSet.getValue(variableName);
        if (selectedFieldValue != null) {
            return selectedFieldValue.stringValue();
        }
        return null;
    }

    /**
     * Generate a query to count the results of the research with the 
     * searched parameters. 
     * @example 
     * SELECT DISTINCT  (COUNT(DISTINCT ?uri) AS ?count) 
     * WHERE {
     *  ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *  ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.phenome-fppn.fr/vocabulary/2018/oeev#MoveFrom> . 
     *  ?uri  <http://www.phenome-fppn.fr/vocabulary/2018/oeev#concerns>  ?concernedItemUri  . 
     *  ?concernedItemUri  <http://www.w3.org/2000/01/rdf-schema#label>  ?concernedItemLabel  . 
     *  ?uri  <http://www.w3.org/2006/time#hasTime>  ?time  . 
     *  ?time  <http://www.w3.org/2006/time#inXSDDateTimeStamp>  ?dateTimeStamp  . 
     *  BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str(?dateTimeStamp)) as ?dateTime) .
     *  BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2017-09-08T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *  BIND(<http://www.w3.org/2001/XMLSchema#dateTime>(str("2019-10-08T12:00:00+01:00")) as ?dateRangeEndDateTime) .
     *  FILTER ( (regex (str(?uri), "http://www.phenome-fppn.fr/id/event/96e72788-6bdc-4f8e-abd1-ce9329371e8e", "i")) 
     *   && (regex (?concernedItemLabel, "Plot Lavalette", "i")) 
     *   && (regex (str(?concernedItemUri), "http://www.phenome-fppn.fr/m3p/arch/2017/c17000242", "i")) 
     *   && (?dateRangeStartDateTime <= ?dateTime) && (?dateRangeEndDateTime >= ?dateTime) ) 
     *}
     */
    private SPARQLQueryBuilder prepareCountQuery(Event eventSearchParameters, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString, User user) {
        SPARQLQueryBuilder query = this.prepareSearchQuery(eventSearchParameters, searchConcernedItemLabel, searchConcernedItemUri, dateRangeStartString, dateRangeEndString, user);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT " + URI_VARIABLE_SPARQL + ") AS " + "?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }

    /**
     * Count the total number of events filtered with the search fields
     * @param eventSearchParameters
     * @param searchConcernedItemLabel
     * @param searchConcernedItemUri
     * @param dateRangeStartString
     * @param dateRangeEndString
     * @param user
     * @return results number
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public Integer count(Event eventSearchParameters, String searchConcernedItemLabel, String searchConcernedItemUri, String dateRangeStartString, String dateRangeEndString, User user) 
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        
        SPARQLQueryBuilder countQuery = prepareCountQuery(eventSearchParameters, searchConcernedItemLabel, searchConcernedItemUri, dateRangeStartString, dateRangeEndString, user);
        
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
    protected SPARQLQueryBuilder prepareSearchQuery() {
        return prepareSearchQuery(new Event(null, null, null, null, null), null, null, null, null, null);
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return count(new Event(null, null, null, null, null), null, null, null, null, null);
    }
}
