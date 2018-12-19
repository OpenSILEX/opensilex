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
import java.util.HashMap;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.ontologies.Oeev;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Time;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Event;

/**
 * Dao for Events
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventDAOSesame extends DAOSesame<Event> {
    final static Logger LOGGER = LoggerFactory.getLogger(EventDAOSesame.class);
    
    private String searchUri;
    private String searchType;
    private String searchConcernsLabel;
    private String searchDateTimeRangeStartString;
    private String searchDateTimeRangeEndString;
        
    private static final String SELECT_URI = "uri";
    private static final String SELECT_TYPE = "type";
    private static final String SELECT_CONCERNS_URI = "concernsUri";
    private static final String SELECT_CONCERNS_LABEL = "concernsLabel";
    private static final String SELECT_CONCERNS_LABELS = "concernsLabels";
    private static final String SELECT_TIME = "time";
    private static final String SELECT_DATE_TIME_STAMP = "dateTimeStamp";
    
    /**
     * Set a search query to select an URI and to filter according to it 
     * if necessary
     * @return the URI SPARQL variable
     */
    private String prepareSearchQueryUri(SPARQLQueryBuilder query){
        String sparqlVariableUri = "?" + SELECT_URI;
        query.appendSelect(sparqlVariableUri);
        query.appendGroupBy(sparqlVariableUri);
        if (searchUri != null) {
            query.appendToBody("\nVALUES " + sparqlVariableUri 
                    +  "{<" + searchUri + ">}");
        }
        return sparqlVariableUri;
    }
    
    /**
     * Set a search query to select a type and to filter according to it 
     * if necessary
     */
    private void prepareSearchQueryType(SPARQLQueryBuilder query
            , String sparqlVariableUri){
        String sparqlVariableType = "?" + SELECT_TYPE;
        query.appendSelect(sparqlVariableType);
        query.appendGroupBy(sparqlVariableType);
        query.appendTriplet(sparqlVariableUri
            , Rdf.RELATION_TYPE.toString(), sparqlVariableType, null);
        if (searchType != null) {
            query.appendTriplet(sparqlVariableType
                , "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*"
                , searchType
                , null);
        } else {
            query.appendTriplet(sparqlVariableType
                , "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*"
                , Oeev.CONCEPT_EVENT.toString()
                , null);
        }    
    }
    
    /**
     * Set a search query to applies the "concerns" label filter. 
     * This function DOES NOT ensure that the query returns the events'concerns 
     * informations. This is done by another query further in the process.
     */
    private void prepareSearchQueryConcernsFilter(SPARQLQueryBuilder query
        , String sparqlVariableUri){

        if (searchConcernsLabel != null) {
            String sparqlVariableConcernsUri = "?" + SELECT_CONCERNS_URI;
            query.appendTriplet(
                sparqlVariableUri
                , Oeev.RELATION_CONCERNS.toString()
                , sparqlVariableConcernsUri, null);
            query.appendTriplet(
                sparqlVariableConcernsUri
                , Rdfs.RELATION_LABEL.toString()
                , "\"" + searchConcernsLabel + "\"", null);
        }
    }
    
    /**
     * Set a search query to select a datetime from an instant and to filter 
     * according to it if necessary
     */
    private void prepareSearchQueryDateTime(SPARQLQueryBuilder query
        , String sparqlVariableUri){  
        
        String sparqlVariableDateTimeStamp = "?" + SELECT_DATE_TIME_STAMP;
        String sparqlVariableTime = "?" + SELECT_TIME;
        
        query.appendSelect(sparqlVariableDateTimeStamp);
        query.appendGroupBy(sparqlVariableDateTimeStamp);
        query.appendTriplet(
                sparqlVariableUri
                , Time.RELATION_HAS_TIME.toString()
                , sparqlVariableTime, null);
        query.appendTriplet(
                sparqlVariableTime
                , Time.RELATION_IN_XSD_DATE_TIMESTAMP.toString()
                , sparqlVariableDateTimeStamp, null);
        query.appendDateTimeStampRangeFilter(
            DateFormat.YMDTHMSZZ.toString()
            , searchDateTimeRangeStartString 
            , searchDateTimeRangeEndString
            , sparqlVariableDateTimeStamp
        );
    }
    
    /**
     * Generates the search query
     * @return the query
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String sparqlVariableUri = prepareSearchQueryUri(query);
        prepareSearchQueryType(query, sparqlVariableUri);  
        prepareSearchQueryConcernsFilter(query, sparqlVariableUri); 
        prepareSearchQueryDateTime(query, sparqlVariableUri); 
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    protected SPARQLQueryBuilder prepareConcernsListSearchQuery(
            String eventUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String sparqlVariableConcernsUri = "?" + SELECT_CONCERNS_URI;
        String sparqlVariableConcernsLabel = "?" + SELECT_CONCERNS_LABEL;
        String sparqlVariableConcernsLabels = "?" + SELECT_CONCERNS_LABELS;
        String sparqlVariableUri = "<" + eventUri + ">";
        
        query.appendSelect(sparqlVariableConcernsUri);
        query.appendGroupBy(sparqlVariableConcernsUri);
        query.appendTriplet(sparqlVariableUri, 
                Oeev.RELATION_CONCERNS.toString()
                , sparqlVariableConcernsUri, null);
         
        query.appendTriplet(sparqlVariableConcernsUri, 
                Rdfs.RELATION_LABEL.toString()
                , sparqlVariableConcernsLabel, null);
        
        query.appendSelectConcat(sparqlVariableConcernsLabel
               , SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR
               , sparqlVariableConcernsLabels);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Get an event from a given binding set.
     * @param bindingSet a binding set, result from a search query
     * @return an event target with data extracted from the given binding set
     */
    private Event getEventFromBindingSet(BindingSet bindingSet) {
          
        String eventUri = getValueOfSelectFieldFromBindingSet(
                SELECT_URI, bindingSet);
                
        String eventType = getValueOfSelectFieldFromBindingSet(
                SELECT_TYPE, bindingSet);
        
        String eventDateTimeString = getValueOfSelectFieldFromBindingSet(
                SELECT_DATE_TIME_STAMP, bindingSet);    
        DateTime eventDateTime = null;
        if (eventDateTimeString != null) {
            eventDateTime = Dates.stringToDateTimeWithGivenPattern(
                    eventDateTimeString
                    , DateFormat.YMDTHMSZZ.toString());
        }
        
        return new Event(eventUri, eventType, new ArrayList<>(), eventDateTime
                , new ArrayList<>());
    }
    
    /**
     * Get an list of object "concerns" from a given binding set.
     * @param bindingSet a binding set, result from a search query
     * @return an list of object "concerns"
     */
    private HashMap<String, ArrayList<String>> getConcernsObjectFromBindingSet(
            BindingSet bindingSet) {
                
        String concernsUri = getValueOfSelectFieldFromBindingSet(
                SELECT_CONCERNS_URI, bindingSet);
        
        String eventConcernsLabelsConcatenated = 
                getValueOfSelectFieldFromBindingSet(SELECT_CONCERNS_LABELS
                    , bindingSet);
        ArrayList<String> eventConcernsLabels = 
                new ArrayList<>(Arrays.asList(eventConcernsLabelsConcatenated
                        .split(SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));

        return new HashMap<String, ArrayList<String>>(){
            {
                put(concernsUri, eventConcernsLabels);
            }
        };
    }
    
    /**
     * Get the events found in the triplestore.
     * @return the list of the events found
     */
    public ArrayList<Event> allPaginate() {
        SPARQLQueryBuilder eventsQuery = prepareSearchQuery();

        TupleQuery eventsTupleQuery = getConnection()
                .prepareTupleQuery(QueryLanguage.SPARQL, eventsQuery.toString());
        
        ArrayList<Event> events;
        try (TupleQueryResult eventsResult = eventsTupleQuery.evaluate()) {
            events = new ArrayList<>();
            
            while (eventsResult.hasNext()) {
                Event event = getEventFromBindingSet(eventsResult.next());
                
                PropertyDAOSesame propertyDAO = 
                        new PropertyDAOSesame(event.getUri());
                propertyDAO.getAllPropertiesExceptThoseSpecified(event, null
                        , new ArrayList(){
                            {
                                add(Rdf.RELATION_TYPE.toString());
                                add(Time.RELATION_HAS_TIME.toString());
                                add(Oeev.RELATION_CONCERNS.toString());
                            }});
                
                SPARQLQueryBuilder concernsListQuery = 
                        prepareConcernsListSearchQuery(event.getUri());
                
                TupleQuery concernsListTupleQuery = getConnection()
                        .prepareTupleQuery(QueryLanguage.SPARQL
                                , concernsListQuery.toString());
                
                try (TupleQueryResult concernsListTupleQueryResult = 
                        concernsListTupleQuery.evaluate()) {
                    
                    HashMap<String, ArrayList<String>> concerns;
                    while(concernsListTupleQueryResult.hasNext()){
                        concerns = getConcernsObjectFromBindingSet(
                                    concernsListTupleQueryResult.next());
                        if(concerns != null){
                            event.addConcerns(concerns);
                        }
                        
                                
                    }
                }

                events.add(event);
            }
        }
        return events;
    }
    
    String getValueOfSelectFieldFromBindingSet(String selectField
        , BindingSet bindingSet){ 
        Value selectedFieldValue = bindingSet.getValue(selectField);
        if (selectedFieldValue != null) {
            return selectedFieldValue.stringValue();
        }
        
        return null;
    }

    /**
     * Count query generated by the searched parameters. Must be done to 
     * get the total number of instances found in the triplestore using these 
     * search parameters because the query is paginated (reduce the amount of 
     * data retrieved and the time to process data before sending it to the 
     * client) 
     * 
     * PREFIX oeev: <http://www.phenome-fppn.fr/vocabulary/2018/oeev#>
     * SELECT (count(distinct ?uri) as ?count) 
     * WHERE { 
     *     ?uri rdf:type oeev:MoveTo 
     * }
     *
     * @return query generated with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount() {
        SPARQLQueryBuilder query = this.prepareSearchQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" 
                + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }

    @Override
    public Integer count() 
            throws RepositoryException
            , MalformedQueryException
            , QueryEvaluationException {
        SPARQLQueryBuilder prepareCount = prepareCount();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(
                QueryLanguage.SPARQL
                , prepareCount.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(
                        bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        return count;
    }

    public String getSearchUri() {
        return searchUri;
    }

    public void setSearchUri(String searchUri) {
        this.searchUri = searchUri;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getSearchConcerns() {
        return searchConcernsLabel;
    }

    public void setSearchConcernsLabel(String searchConcerns) {
        this.searchConcernsLabel = searchConcerns;
    }

    public String getSearchDateTimeRangeStartString() {
        return searchDateTimeRangeStartString;
    }

    public void setSearchDateTimeRangeStartString(
            String searchDateTimeRangeStartString) {
        this.searchDateTimeRangeStartString = searchDateTimeRangeStartString;
    }
    
    public String getSearchDateTimeRangeEndString() {
        return searchDateTimeRangeEndString;
    }

    public void setSearchDateTimeRangeEndString(
            String searchDateTimeRangeEndString) {
        this.searchDateTimeRangeEndString = searchDateTimeRangeEndString;
    }
}
