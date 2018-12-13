//******************************************************************************
//                          EventDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12  nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Date;
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
    private String searchConcerns;
    private String searchDateTimeRangeStartString;
    private String searchDateTimeRangeEndString;
        
    public static final String SELECT_CONCERNS = "concerns";
    public static final String SELECT_TIME = "time";
    public static final String SELECT_DATE_TIME = "dateTime";
    
    /**
     * Generates the search query
     * @return the query
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String eventUri;
        if (searchUri != null) {
            eventUri = "<" + searchUri + ">";
        } else {
            eventUri = "?" + URI;
            query.appendSelect(eventUri);
        }
        
        if (searchType != null) {
            query.appendTriplet(eventUri, Rdf.RELATION_TYPE.toString()
                    , searchType, null);
        } else {
            query.appendSelect("?" + RDF_TYPE);
            query.appendTriplet(eventUri
                    , Rdf.RELATION_TYPE.toString()
                    , "?" + RDF_TYPE
                    , null);
            query.appendTriplet("?" + RDF_TYPE
                    , "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*"
                    , Oeev.CONCEPT_EVENT.toString()
                    , null);
        }       

        if (searchConcerns != null) {
            query.appendTriplet(eventUri
                    , Oeev.RELATION_CONCERNS.toString()
                    , searchConcerns
                    , null);
        } else {
            query.appendSelect(" ?" + SELECT_CONCERNS);
            query.appendTriplet(eventUri
                    , Oeev.RELATION_CONCERNS.toString()
                    , "?" + SELECT_CONCERNS
                    , null);
        }  
        
        query.appendSelect("?" + SELECT_DATE_TIME);
        query.appendTriplet(eventUri
                , Time.RELATION_HAS_TIME.toString()
                , "?" + SELECT_TIME
                , null);
        query.appendTriplet("?" + SELECT_TIME
                , Time.RELATION_IN_XSD_DATE_TIMESTAMP.toString()
                , "?" + SELECT_DATE_TIME
                , null);
        //TODO search by date
        
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
        
        String eventUri = null;
        String eventType = null;
        String eventConcerns = null;
        DateTime eventDateTime = null;
        
        
        Value bindingSetValueEventUri = bindingSet.getValue(URI);
        if (bindingSetValueEventUri != null) {
            eventUri = bindingSetValueEventUri.stringValue();
        } 
        
        Value bindingSetValueEventType = bindingSet.getValue(RDF_TYPE);
        if (bindingSetValueEventType != null) {
            eventType = bindingSetValueEventType.stringValue();
        } 
        
        Value bindingSetValueEventConcerns = 
                bindingSet.getValue(SELECT_CONCERNS);
        if (bindingSetValueEventConcerns != null) {
            eventConcerns = bindingSetValueEventConcerns.stringValue();
        }         
        
        Value bindingSetValueEventDateTime = bindingSet.getValue(SELECT_TIME);
        if (bindingSetValueEventDateTime != null) {
            eventDateTime = Dates.stringToDateTimeWithGivenPattern(
                    bindingSetValueEventDateTime.toString()
                    , DateFormats.DATETIME_SPARQL_XSD_FORMAT);
        }
        
        return new Event(eventUri, eventType, eventConcerns, eventDateTime);
    }
    
    /**
     * Get the events found in the triplestore.
     * @return the list of the events found
     */
    public ArrayList<Event> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        /*TupleQuery tupleQuery = getConnection()
                .prepareTupleQuery(QueryLanguage.SPARQL, "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
"PREFIX oeev: <http://www.phenome-fppn.fr/vocabulary/2018/oeev#>\n" +
"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
"\n" +
"SELECT ?uri\n" +
"WHERE { \n" +
"  ?uri rdf:type ?type .\n" +
"  ?type rdfs:subClassOf* oeev:Event \n" +
"}");
        */
        TupleQuery tupleQuery = getConnection()
                .prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Event> events;

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            events = new ArrayList<>();
            
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Event event = getEventFromBindingSet(bindingSet);
                events.add(event);
            }
        }
        return events;
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
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
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
        return searchConcerns;
    }

    public void setSearchConcerns(String searchConcerns) {
        this.searchConcerns = searchConcerns;
    }

    public String getSearchDateTime() {
        return searchDateTimeRangeStartString;
    }

    public void setSearchDateTimeRangeStartString(String searchDateTimeString) {
        this.searchDateTimeRangeStartString = searchDateTimeString;
    }
    
    public String getSearchDateTimeRangeEndString() {
        return searchDateTimeRangeEndString;
    }

    public void setSearchDateTimeRangeEndString(String searchDateTimeRangeEndString) {
        this.searchDateTimeRangeEndString = searchDateTimeRangeEndString;
    }
}
