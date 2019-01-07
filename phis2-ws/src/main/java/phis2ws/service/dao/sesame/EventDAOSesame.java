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
    private static final String URI_VARIABLE = "uri";
    private static final String URI_VARIABLE_SPARQLE = "?" + URI_VARIABLE;
    private static final String TYPE_VARIABLE = "type";
    private static final String TYPE_VARIABLE_SPARQL = "?" + TYPE_VARIABLE;
        
    private static final String CONCERNS_ITEM_URI_VARIABLE = "concernsUri";
    private static final String CONCERNS_ITEM_URI_VARIABLE_SPARQL = 
            "?" + CONCERNS_ITEM_URI_VARIABLE;
    private static final String CONCERNS_ITEM_TYPE_VARIABLE = "concernsType";
    private static final String CONCERNS_ITEM_TYPE_VARIABLE_SPARQL = 
            "?" + CONCERNS_ITEM_TYPE_VARIABLE;
    private static final String CONCERNS_ITEM_LABEL_VARIABLE = "concernsLabel";
    private static final String CONCERNS_ITEM_LABEL_VARIABLE_SPARQL = 
            "?" + CONCERNS_ITEM_LABEL_VARIABLE;
    private static final String CONCERNS_ITEM_LABELS_VARIABLE = "concernsLabels";
    private static final String CONCERNS_ITEM_LABELS_VARIABLE_SPARQL = 
            "?" + CONCERNS_ITEM_LABELS_VARIABLE;
    
    private static final String TIME_VARIABLE = "time";
    private static final String TIME_VARIABLE_SPARQL = "?" + TIME_VARIABLE;
    
    private static final String DATETIMESTAMP_VARIABLE = "dateTimeStamp";
    private static final String DATETIMESTAMP_VARIABLE_SPARQL = 
            "?" + DATETIMESTAMP_VARIABLE;
    
    // search parameters
    private String searchUri;
    private String searchType;
    private String searchConcernsItemUri;
    private String searchConcernsItemLabel;
    private String searchDateTimeRangeStartString;
    private String searchDateTimeRangeEndString;
    
    /**
     * Set a search query to select an URI and to filter according to it 
     * if necessary
     * @return the URI SPARQL variable
     */
    private String prepareSearchQueryUri(SPARQLQueryBuilder query){
        query.appendSelect(URI_VARIABLE_SPARQLE);
        query.appendGroupBy(URI_VARIABLE_SPARQLE);
        if (searchUri != null) {
            query.appendAndFilter("regex (str("
                + URI_VARIABLE_SPARQLE + ")"
                + ", \"" + searchUri + "\", \"i\")");
        }
        return URI_VARIABLE_SPARQLE;
    }
    
    /**
     * Set a search query to select a type and to filter according to it 
     * if necessary
     */
    private void prepareSearchQueryType(SPARQLQueryBuilder query
            , String sparqlVariableUri){
        query.appendSelect(TYPE_VARIABLE_SPARQL);
        query.appendGroupBy(TYPE_VARIABLE_SPARQL);
        query.appendTriplet(sparqlVariableUri
            , Rdf.RELATION_TYPE.toString(), TYPE_VARIABLE_SPARQL, null);
        if (searchType != null) {
            query.appendTriplet(TYPE_VARIABLE_SPARQL
                , "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*"
                , searchType
                , null);
        } else {
            query.appendTriplet(TYPE_VARIABLE_SPARQL
                , "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*"
                , Oeev.CONCEPT_EVENT.toString()
                , null);
        }    
    }
    
    /**
     * Set a search query to applies the concerns items label filter. 
     * This function DOES NOT make the query return the events concerns items 
     * informations. This is done by another query further in the process.
     */
    private void prepareSearchQueryConcernsItemFilter(
            SPARQLQueryBuilder query, String sparqlVariableUri){

        if (searchConcernsItemLabel != null || searchConcernsItemUri != null) {
            query.appendTriplet(
                sparqlVariableUri
                , Oeev.RELATION_CONCERNS.toString()
                , CONCERNS_ITEM_URI_VARIABLE_SPARQL, null);
            
            if (searchConcernsItemLabel != null){
                query.appendTriplet(
                    CONCERNS_ITEM_URI_VARIABLE_SPARQL
                    , Rdfs.RELATION_LABEL.toString()
                    , CONCERNS_ITEM_LABEL_VARIABLE_SPARQL, null);
                
                query.appendAndFilter("regex ("
                    + CONCERNS_ITEM_LABEL_VARIABLE_SPARQL
                    + ", \"" + searchConcernsItemLabel + "\", \"i\")");
            }
            
            if (searchConcernsItemUri != null){
                query.appendAndFilter("regex (str("
                    + CONCERNS_ITEM_URI_VARIABLE_SPARQL + ")"
                    + ", \"" + searchConcernsItemUri + "\", \"i\")");
            }
        }
    }

    /**
     * Set a search query to select a datetime from an instant and to filter 
     * according to it if necessary
     */
    private void prepareSearchQueryDateTime(SPARQLQueryBuilder query
        , String sparqlVariableUri){  
        
        query.appendSelect(DATETIMESTAMP_VARIABLE_SPARQL);
        query.appendGroupBy(DATETIMESTAMP_VARIABLE_SPARQL);
        query.appendTriplet(
                sparqlVariableUri
                , Time.RELATION_HAS_TIME.toString()
                , TIME_VARIABLE_SPARQL, null);
        query.appendTriplet(
                TIME_VARIABLE_SPARQL
                , Time.RELATION_IN_XSD_DATE_TIMESTAMP.toString()
                , DATETIMESTAMP_VARIABLE_SPARQL, null);
        
        if (searchDateTimeRangeStartString != null 
                || searchDateTimeRangeEndString != null) {
            filterSearchQueryWithDateRangeComparisonWithDateTimeStamp(
                    query
                    , DateFormat.YMDTHMSZZ.toString()
                    , searchDateTimeRangeStartString 
                    , searchDateTimeRangeEndString
                    , DATETIMESTAMP_VARIABLE_SPARQL
            );
        }
    }
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String sparqlVariableUri = prepareSearchQueryUri(query);
        prepareSearchQueryType(query, sparqlVariableUri);  
        prepareSearchQueryConcernsItemFilter(query, sparqlVariableUri); 
        prepareSearchQueryDateTime(query, sparqlVariableUri); 
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    protected SPARQLQueryBuilder prepareConcernsItemsSearchQuery(
            String eventUri) {
        
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String sparqlVariableUri = "<" + eventUri + ">";
        
        query.appendSelect(CONCERNS_ITEM_URI_VARIABLE_SPARQL);
        query.appendGroupBy(CONCERNS_ITEM_URI_VARIABLE_SPARQL);
        query.appendTriplet(sparqlVariableUri, 
                Oeev.RELATION_CONCERNS.toString()
                , CONCERNS_ITEM_URI_VARIABLE_SPARQL, null);
        
        query.appendSelect(CONCERNS_ITEM_TYPE_VARIABLE_SPARQL);
        query.appendGroupBy(CONCERNS_ITEM_TYPE_VARIABLE_SPARQL);
        query.appendTriplet(CONCERNS_ITEM_URI_VARIABLE_SPARQL, 
                Rdf.RELATION_TYPE.toString()
                , CONCERNS_ITEM_TYPE_VARIABLE_SPARQL, null);
         
        query.appendTriplet(CONCERNS_ITEM_URI_VARIABLE_SPARQL, 
                Rdfs.RELATION_LABEL.toString()
                , CONCERNS_ITEM_LABEL_VARIABLE_SPARQL, null);
        
        query.appendSelectConcat(CONCERNS_ITEM_LABEL_VARIABLE_SPARQL
               , SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR
               , CONCERNS_ITEM_LABELS_VARIABLE_SPARQL);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Get an event from a given binding set.
     * @param bindingSet a binding set, result from a search query
     * @return an event target with data extracted from the given binding set
     */
    private Event getEventFromBindingSet(BindingSet bindingSet) {
          
        String eventUri = getValueOfVariableFromBindingSet(
                URI_VARIABLE, bindingSet);
                
        String eventType = getValueOfVariableFromBindingSet(
                TYPE_VARIABLE, bindingSet);
        
        String eventDateTimeString = getValueOfVariableFromBindingSet(
                DATETIMESTAMP_VARIABLE, bindingSet);    
        DateTime eventDateTime = null;
        if (eventDateTimeString != null) {
            eventDateTime = Dates.stringToDateTimeWithGivenPattern(
                    eventDateTimeString
                    , DateFormat.YMDTHMSZZ.toString());
        }
        
        return new Event(eventUri, eventType, new ArrayList<>(), eventDateTime
                , new ArrayList<>());
    }
    
    private ConcernItem getConcernsItemFromBindingSet(BindingSet bindingSet){
                
        String concernsItemUri = getValueOfVariableFromBindingSet(
                CONCERNS_ITEM_URI_VARIABLE, bindingSet);
        String concernsItemType = getValueOfVariableFromBindingSet(
                CONCERNS_ITEM_TYPE_VARIABLE, bindingSet);
        
        String eventConcernsItemLabelsConcatenated = 
                getValueOfVariableFromBindingSet(CONCERNS_ITEM_LABELS_VARIABLE
                    , bindingSet);
        ArrayList<String> eventConcernsItemLabels = 
                new ArrayList<>(Arrays.asList(eventConcernsItemLabelsConcatenated
                        .split(SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));

        return new ConcernItem(concernsItemUri, concernsItemType
                , eventConcernsItemLabels);
    }
    
    public ArrayList<Event> searchEvents() {
        
        SPARQLQueryBuilder eventsQuery = prepareSearchQuery();
        TupleQuery eventsTupleQuery = getConnection()
                .prepareTupleQuery(QueryLanguage.SPARQL, eventsQuery.toString());
        
        ArrayList<Event> events;
        try (TupleQueryResult eventsResult = eventsTupleQuery.evaluate()) {
            
            events = new ArrayList<>();
            while (eventsResult.hasNext()) {
                Event event = getEventFromBindingSet(eventsResult.next());
                searchEventPropertiesAndSetThemToIt(event);
                searchEventConcernsItemsAndSetThemToIt(event);
                events.add(event);
            }
        }
        return events;
    }
    
    private void searchEventPropertiesAndSetThemToIt(Event event){

        PropertyDAOSesame propertyDAO = new PropertyDAOSesame(event.getUri());
        propertyDAO.getRdfObjectPropertiesExceptThoseSpecifiedAndAddThemToIt(
                event, null, new ArrayList(){
                    {
                        add(Rdf.RELATION_TYPE.toString());
                        add(Time.RELATION_HAS_TIME.toString());
                        add(Oeev.RELATION_CONCERNS.toString());
                    }});
    }
    
    private void searchEventConcernsItemsAndSetThemToIt(Event event){
                
        SPARQLQueryBuilder concernsItemsQuery = 
                prepareConcernsItemsSearchQuery(event.getUri());
        TupleQuery concernsItemsTupleQuery = getConnection()
                .prepareTupleQuery(QueryLanguage.SPARQL
                        , concernsItemsQuery.toString());

        try (TupleQueryResult concernsItemsTupleQueryResult = 
                concernsItemsTupleQuery.evaluate()) {

            ConcernItem concernsItem;
            while(concernsItemsTupleQueryResult.hasNext()){
                concernsItem = getConcernsItemFromBindingSet(
                            concernsItemsTupleQueryResult.next());
                event.addConcernsItem(concernsItem);
            }
        }
    }
    
    private String getValueOfVariableFromBindingSet(String variableName
        , BindingSet bindingSet){ 
        Value selectedFieldValue = bindingSet.getValue(variableName);
        if (selectedFieldValue != null) {
            return selectedFieldValue.stringValue();
        }
        
        return null;
    }

    /**
     * Generate a query to count the results of the research with the 
     * searched parameters. 
     */
    private SPARQLQueryBuilder prepareCountSearchQuery() {
        SPARQLQueryBuilder query = this.prepareSearchQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT " + URI_VARIABLE_SPARQLE + ") AS "
                + "?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }

    @Override
    public Integer count() throws RepositoryException
            , MalformedQueryException, QueryEvaluationException {
        
        SPARQLQueryBuilder prepareCount = prepareCountSearchQuery();
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

    public String getSearchConcernsItemUri() {
        return searchConcernsItemUri;
    }

    public void setSearchConcernsItemUri(String searchConcernsItemUri) {
        this.searchConcernsItemUri = searchConcernsItemUri;
    }

    public String getSearchConcernsItemLabel() {
        return searchConcernsItemLabel;
    }

    public void setSearchConcernsItemLabel(String searchConcernsItemLabel) {
        this.searchConcernsItemLabel = searchConcernsItemLabel;
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
