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
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.ontologies.Oeev;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Time;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Event;
import phis2ws.service.view.model.phis.Property;

/**
 * Dao for Events
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventDAOSesame extends DAOSesame<Event> {
    final static Logger LOGGER = LoggerFactory.getLogger(EventDAOSesame.class);
    
    private String searchUri;
    private String searchType;
    private String searchConcernsUri;
    private String searchDateTimeRangeStartString;
    private String searchDateTimeRangeEndString;
        
    public static final String SELECT_URI = "uri";
    public static final String SELECT_TYPE = "type";
    public static final String SELECT_CONCERNS_URI = "concernsUri";
    public static final String SELECT_CONCERNS_Type = "concernsUri";
    public static final String SELECT_CONCERNS_LABEL = "concernsLabel";
    public static final String SELECT_CONCERNS_LABELS = "concernsLabels";
    public static final String SELECT_TIME = "time";
    public static final String SELECT_DATE_TIME_STAMP = "dateTimeStamp";
    public static final String SELECT_DATE_TIME = "dateTime";
    public static final String SELECT_DATE_RANGE_START_DATE_TIME 
            = "dateRangeStartDateTime";
    public static final String SELECT_DATE_RANGE_END_DATE_TIME 
            = "dateRangeEndDateTime";
    public static final String SELECT_FROM_URI = "fromUri";
    public static final String SELECT_FROM_TYPE = "fromType";
    public static final String SELECT_FROM_RELATION = "fromRelation";
    public static final String SELECT_TO_URI = "toUri";
    public static final String SELECT_TO_TYPE = "toType";
    public static final String SELECT_TO_RELATION = "toRelation";
    
    private String prepareSearchQueryUri(SPARQLQueryBuilder query){
        String sparkleVariableUri = "?" + SELECT_URI;
        query.appendSelect(sparkleVariableUri);
        query.appendGroupBy(sparkleVariableUri);
        if (searchUri != null) {
            query.appendToBody("\nVALUES " + sparkleVariableUri 
                    +  "{<" + searchUri + ">}");
        }
        return sparkleVariableUri;
    }
    
    private void prepareSearchQueryType(SPARQLQueryBuilder query
        , String sparkleVariableUri){
        String sparkleVariableType = "?" + SELECT_TYPE;
        query.appendSelect(sparkleVariableType);
        query.appendGroupBy(sparkleVariableType);
        query.appendTriplet(sparkleVariableUri
            , Rdf.RELATION_TYPE.toString(), sparkleVariableType, null);
        if (searchType != null) {
            query.appendTriplet(sparkleVariableType
                , "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*"
                , searchType
                , null);
        } else {
            query.appendTriplet(sparkleVariableType
                , "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*"
                , Oeev.CONCEPT_EVENT.toString()
                , null);
        }    
    }
    
    private void prepareSearchQueryConcerns(SPARQLQueryBuilder query
        , String sparkleVariableUri){

        String sparkleVariableConcernsUri = "?" + SELECT_CONCERNS_URI;
        String sparkleVariableConcernsLabel = "?" + SELECT_CONCERNS_LABEL;
        String sparkleVariableConcernsLabels = "?" + SELECT_CONCERNS_LABELS;
        query.appendTriplet(
            sparkleVariableUri
            , Oeev.RELATION_CONCERNS.toString()
            , sparkleVariableConcernsUri, null);

        if (searchConcernsUri != null) {
            query.appendToBody("\nVALUES " + sparkleVariableConcernsUri 
                    +  "{<" + searchConcernsUri + ">}");
        }
        
        query.appendSelectConcat(
                sparkleVariableConcernsLabel
                , SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR
                , sparkleVariableConcernsLabels);
    }
    
    private void prepareSearchQueryTime(SPARQLQueryBuilder query
        , String sparkleVariableUri){  
        
        String sparkleVariableDateTimeStamp = "?" + SELECT_DATE_TIME_STAMP;
        String sparkleVariableDateTime = "?" + SELECT_DATE_TIME;
        String sparkleVariableDateRangeStartDateTime = 
                "?" + SELECT_DATE_RANGE_START_DATE_TIME;
        String sparkleVariableDateRangeEndDateTime =
                "?" + SELECT_DATE_RANGE_END_DATE_TIME;
        String sparkleVariableTime = "?" + SELECT_TIME;
        
        query.appendSelect(sparkleVariableDateTimeStamp);
        query.appendGroupBy(sparkleVariableDateTimeStamp);
        query.appendTriplet(
                sparkleVariableUri
                , Time.RELATION_HAS_TIME.toString()
                , sparkleVariableTime, null);
        query.appendTriplet(
                sparkleVariableTime
                , Time.RELATION_IN_XSD_DATE_TIMESTAMP.toString()
                , sparkleVariableDateTimeStamp, null);
        if (searchDateTimeRangeStartString != null 
                || searchDateTimeRangeEndString != null) {
            query.appendToBody("\nBIND(xsd:dateTime(str(" 
                    + sparkleVariableDateTimeStamp 
                    + ")) as " + sparkleVariableDateTime
                        + ") .");
            if (searchDateTimeRangeStartString != null){
                DateTime dateRangeStartDateTime = 
                    Dates.stringToDateTimeWithGivenPattern(
                        searchDateTimeRangeStartString
                        , DateFormats.DATETIME_JSON_SERIALISATION_FORMAT);
                String dateRangeStartDateTimeString = DateTimeFormat
                            .forPattern(DateFormats.DATETIME_SPARQL_FORMAT)
                            .print(dateRangeStartDateTime);
        
                query.appendToBody("\nBIND(xsd:dateTime(str(\""
                        + dateRangeStartDateTimeString 
                        + "\")) as " + sparkleVariableDateRangeStartDateTime
                        + ") .");
                query.appendToBody("\nFILTER ("
                        + sparkleVariableDateTime + " >= "
                        + sparkleVariableDateRangeStartDateTime
                        + ") .");
            }
            if (searchDateTimeRangeEndString != null){
                DateTime dateRangeEndDateTime = 
                    Dates.stringToDateTimeWithGivenPattern(
                        searchDateTimeRangeEndString
                        , DateFormats.DATETIME_JSON_SERIALISATION_FORMAT);
                String dateRangeEndDateTimeString = DateTimeFormat
                            .forPattern(DateFormats.DATETIME_SPARQL_FORMAT)
                            .print(dateRangeEndDateTime);
                
                query.appendToBody("\nBIND(xsd:dateTime(str(\""
                        + dateRangeEndDateTimeString 
                        + "\")) as " + sparkleVariableDateRangeEndDateTime
                        + ") .");
                query.appendToBody("\nFILTER ("
                        + sparkleVariableDateTime + " <= "
                        + sparkleVariableDateRangeEndDateTime
                        + ") .");
            }
        }
    }
    
    private void prepareSearchQuerySubclassSpecificProperties(
            SPARQLQueryBuilder query, String sparkleVariableUri){   
        
        String sparkleVariableFromUri = "?" + SELECT_FROM_URI;
        query.appendSelect(sparkleVariableFromUri);
        query.appendGroupBy(sparkleVariableFromUri);
        
        String sparkleVariableFromType = "?" + SELECT_FROM_TYPE;
        query.appendSelect(sparkleVariableFromType);
        query.appendGroupBy(sparkleVariableFromType);
        
        String sparkleVariableFromRelation = "?" + SELECT_FROM_RELATION;
        query.appendSelect("(\"" + Oeev.RELATION_FROM.toString() 
                + "\" as "+ sparkleVariableFromRelation + ")");
        
        query.beginBodyOptional();
        query.appendTriplet(
                sparkleVariableUri
                , Oeev.RELATION_FROM.toString()
                , sparkleVariableFromUri, null);
        query.appendTriplet(
                sparkleVariableFromUri
                , Rdf.RELATION_TYPE.toString()
                , sparkleVariableFromType, null);
        query.endBodyOptional(); 
        
        String sparkleVariableTo = "?" + SELECT_TO_URI;
        query.appendSelect(sparkleVariableTo);
        query.appendGroupBy(sparkleVariableTo);
        query.beginBodyOptional();
        query.appendTriplet(
                sparkleVariableUri
                , Oeev.RELATION_TO.toString()
                , sparkleVariableTo, null);
        query.endBodyOptional(); 
    }
    
    /**
     * Generates the search query
     * @return the query
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String sparkleVariableUri = prepareSearchQueryUri(query);
        prepareSearchQueryType(query, sparkleVariableUri);  
        prepareSearchQueryConcerns(query, sparkleVariableUri); 
        prepareSearchQueryTime(query, sparkleVariableUri); 
        prepareSearchQuerySubclassSpecificProperties(query, sparkleVariableUri);
        
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        
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
        
        String eventConcernsLabelsConcatenated = 
                getValueOfSelectFieldFromBindingSet(SELECT_CONCERNS_LABELS
                    , bindingSet);
        ArrayList<HashMap<String, ArrayList<String>>> concernsList 
                = new ArrayList();
        ArrayList<String> eventConcernsLabels = 
                new ArrayList<>(Arrays.asList(eventConcernsLabelsConcatenated
                        .split(SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));
        
        String eventDateTimeString = getValueOfSelectFieldFromBindingSet(SELECT_DATE_TIME_STAMP, bindingSet);    
        DateTime eventDateTime = null;
        if (eventDateTimeString != null) {
            eventDateTime = Dates.stringToDateTimeWithGivenPattern(
                    eventDateTimeString
                    , DateFormats.DATETIME_SPARQL_FORMAT);
        }
        
        ArrayList<Property> eventSubclassSpecificProperties = new ArrayList<>();
        
        String eventFromUri = getValueOfSelectFieldFromBindingSet(
                SELECT_FROM_URI, bindingSet);
        String eventFromType = getValueOfSelectFieldFromBindingSet(
                SELECT_FROM_TYPE, bindingSet);
        String eventFromRelation = getValueOfSelectFieldFromBindingSet(
                SELECT_FROM_RELATION, bindingSet);
        if(eventFromUri != null){
            Property fromProperty = new Property(eventFromType, null
                    , eventFromRelation, null, eventFromUri, null, null, null);
            eventSubclassSpecificProperties.add(fromProperty);
        }
        
        String eventTo;
        eventTo = getValueOfSelectFieldFromBindingSet(SELECT_TO_URI, bindingSet); 

        
        return new Event(eventUri, eventType, concernsList, eventDateTime
                , eventSubclassSpecificProperties);
    }
    
    /**
     * Get the events found in the triplestore.
     * @return the list of the events found
     */
    public ArrayList<Event> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();

        TupleQuery tupleQuery = getConnection()
                .prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        ArrayList<Event> events;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            events = new ArrayList<>();
            
            while (result.hasNext()) {
                events.add(getEventFromBindingSet(result.next()));
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
        return searchConcernsUri;
    }

    public void setSearchConcerns(String searchConcerns) {
        this.searchConcernsUri = searchConcerns;
    }

    public String getSearchDateTime() {
        return searchDateTimeRangeStartString;
    }

    public void setSearchDateTimeRangeStartString(
            String searchDateTimeString) {
        this.searchDateTimeRangeStartString = searchDateTimeString;
    }
    
    public String getSearchDateTimeRangeEndString() {
        return searchDateTimeRangeEndString;
    }

    public void setSearchDateTimeRangeEndString(
            String searchDateTimeRangeEndString) {
        this.searchDateTimeRangeEndString = searchDateTimeRangeEndString;
    }
}
