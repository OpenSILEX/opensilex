//**********************************************************************************************
//                                       ConceptDaoSesame.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: January 3 2018
// Contact: eloan.lager@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January 31, 2018
// Subject: A Dao specific to concept insert into triplestore 
//***********************************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Optional;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Ask;
import phis2ws.service.view.model.phis.Concept;

/**
 * Represents the Data Access Object for the concepts
 * @author Eloan Lagier
 */
public class ConceptDaoSesame extends DAOSesame<Concept>{
    final static Logger LOGGER = LoggerFactory.getLogger(ConceptDaoSesame.class);
    public String uri;
    public ArrayList<Double> infos;
    
     /**
     *  Search infos of concept (ex : label, subclass.. )
     * query example : 
     *     SELECT DISTINCT ?class ?info WHERE {
     *     conceptURI ?class ?info
     *      }
     * @param <>
     * @return SPARQLQueryBuilder
     */
         
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String contextURI;
        
        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        
        query.appendSelect(" ?class ?type");
        query.appendTriplet(contextURI,"?class", "?type", null);
 
        LOGGER.trace("sparql select query : " + query.toString());
        return query;
    }
    
     /**
     *  Search descendants of concept 
     * query example : 
     *     SELECT DISTINCT ?class WHERE {
     *      ?class  rdfs:subClassOf* contextURI
     *      }
     * @param <>
     * @return SPARQLQueryBuilder
     */
    protected SPARQLQueryBuilder prepareDescendantsQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String contextURI;
        
        
        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        query.appendSelect(" ?class ");
        query.appendTriplet(" ?class ", " rdfs:subClassOf* ",contextURI, null);
        
        return query;
    }
    

     /**
     *  Search ancestors of concept 
     * query example : 
     *     SELECT DISTINCT ?class WHERE {
     *       contextURI rdfs:subClassOf* ?class
     *      }
     * @param <>
     * @return SPARQLQueryBuilder
     */
    protected SPARQLQueryBuilder prepareAncestorsQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String contextURI;
        
        
        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        query.appendSelect(" ?class ");
        query.appendTriplet(contextURI, " rdfs:subClassOf* "," ?class ", null);
        
        return query;
    }
    
    /**
     *  Search siblings of concept 
     * query example : 
     *     SELECT DISTINCT ?class WHERE {
     *       contextURI rdfs:subClassOf ?parent .
     *       ?class rdfs:subClassOf ?parent
     *      }
     * @param <>
     * @return SPARQLQueryBuilder
     */
    
    /*probleme : Siblings take ScientificDocument for exemple but it's different that all the other concept GET
    where could this can be change?
    */
    protected SPARQLQueryBuilder prepareSiblingsQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String contextURI;
        
        
        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        query.appendSelect(" ?class ");
        query.appendTriplet(contextURI, " rdfs:subClassOf "," ?parent ", null);
        query.appendTriplet("?class", " rdfs:subClassOf ", "?parent", null);
        
        return query;
    }
    
    
    
    
    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    /* create the ask query */
    
    protected SPARQLQueryBuilder prepareAskQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String contextURI;
        
        
        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        //any = anything
        query.appendAsk(contextURI+" ?any1 ?any2 ");

        return query;
    }
  
    
    /* create the query that return the type of an URI if its in the Tupple */
    protected SPARQLQueryBuilder prepareAskTypeQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        String contextURI;
        
        
        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        query.appendSelect(" ?type ");
        query.appendTriplet(contextURI," rdf:type"," ?type ", null);
        LOGGER.debug(query.toString());
        return query;
    }
    
            
    /*
    return the concept info all paginate
    */
    
    public ArrayList<Concept> ConceptAllPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList<Concept>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            Concept concept = new Concept();
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                concept.setUri(uri);
                String classname = bindingSet.getValue("class").stringValue();
                Value label = bindingSet.getValue("type");
                if (label instanceof Literal) {
                    Literal literal = (Literal) bindingSet.getValue("type");
                    Optional<String> propertyLanguage = literal.getLanguage();
                    concept.addInfos(classname.substring(classname.indexOf("#")+1,classname.length())+"_"+propertyLanguage.get(), bindingSet.getValue("type").stringValue());
                }else{
                
                concept.addInfos(classname.substring(classname.indexOf("#")+1,classname.length()),bindingSet.getValue("type").stringValue());
                }
                
            }
            concepts.add(concept);
        }
        return concepts;
    } 
    
   /*
    return the descendants info all paginate
    */
    
    public ArrayList<Concept> DescendantsAllPaginate() {
        
        SPARQLQueryBuilder query = prepareDescendantsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList<Concept>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            
            while (result.hasNext()) {
                Concept concept = new Concept();
                BindingSet bindingSet = result.next();
                LOGGER.debug(bindingSet.getValue("class").stringValue());
                concept.setUri(bindingSet.getValue("class").stringValue()); 
                concepts.add(concept);
            }
            
        }
        return concepts;
    }
    
    /*
    return the ancestors info all paginate
    */
    public ArrayList<Concept> AncestorsAllPaginate() {
        
        SPARQLQueryBuilder query = prepareAncestorsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {

            
            while (result.hasNext()) {
                Concept concept = new Concept();
                BindingSet bindingSet = result.next();
                concept.setUri(bindingSet.getValue("class").stringValue()); 
                concepts.add(concept);
            }
            
        }
        return concepts;
    }
    
    /*
    return the siblings info all paginate
    */
    public ArrayList<Concept> SiblingsAllPaginate() {
        
        SPARQLQueryBuilder query = prepareSiblingsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList<Concept>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            
            while (result.hasNext()) {
                Concept concept = new Concept();
                BindingSet bindingSet = result.next();
                concept.setUri(bindingSet.getValue("class").stringValue()); 
                concepts.add(concept);
            }
            
        }
        return concepts;
    }
    
    /*
    return if an URI is in the tupple
    */
    public ArrayList<Ask> getAskAnswer() {
        SPARQLQueryBuilder query = prepareAskQuery();
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Ask> answer = new ArrayList<Ask>();
        boolean result = booleanQuery.evaluate();
        Ask ask = new Ask();
        ask.setResponse(String.valueOf(result));
        answer.add(ask);       
        
        return answer;
    }
    
    /*return the type of the uri concept if it is in the tupple */
    public ArrayList<Ask> getAskTypeAnswer() {
        SPARQLQueryBuilder query = prepareAskQuery();
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Ask> answer = new ArrayList<Ask>();;
        boolean result = booleanQuery.evaluate();
        Ask ask = new Ask();
        ask.setResponse(String.valueOf(result));
        if (ask.getResponse() == "true"){
            query = prepareAskTypeQuery();
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
            TupleQueryResult resultat = tupleQuery.evaluate();
            BindingSet bindingSet = resultat.next();
            ask.setType(bindingSet.getValue("type").toString());
            
        }
        answer.add(ask);
        return answer;
    }
    
}
