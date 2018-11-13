//******************************************************************************
//                          EventDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12  nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
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
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Event;

/**
 * Dao for Events
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventDAOSesame extends DAOSesame<Event> {
    final static Logger LOGGER = LoggerFactory.getLogger(EventDAOSesame.class);
    
    // Used to search in the triplestore
    public String uri;
    public String rdfType;
    public String label;
    
    /**
     * Generates the query to get the uri and the label of all the events
     * @example
     * SELECT DISTINCT  ?uri ?label WHERE {
     *      ?uri  rdf:type  
     * <http://www.phenome-fppn.fr/vocabulary/2018/oeev#Event> . 
     *      ?uri  rdfs:label  ?label  .
     * }
     * @return the query
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String select;
        if (uri != null) {
            select = "<" + uri + ">";
        } else {
            select = "?" + URI;
            query.appendSelect(select);
        }
        
        query.appendSelect("?" + LABEL);
        query.appendTriplet(select, Rdf.RELATION_TYPE.toString(), 
                Vocabulary.CONCEPT_EVENT.toString(), null);
        
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
    private Event getFromBindingSet(BindingSet bindingSet) {
        
        DateTime dateTime = null;//bindingSet.getValue(DATE).stringValue()
        
        return new Event(
                        bindingSet.getValue(URI).stringValue()
                        , bindingSet.getValue(LABEL).stringValue()
                        , dateTime);
    }
    
    /**
     * Get the events (uri, label) of the triplestore.
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
                BindingSet bindingSet = result.next();
                Event event = getFromBindingSet(bindingSet);
                events.add(event);
            }
        }
        return events;
    }

    @Override
    public Integer count() 
            throws RepositoryException
            , MalformedQueryException
            , QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); 
//To change body of generated methods, choose Tools | Templates.
    }
}
