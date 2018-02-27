//**********************************************************************************************
//                                       UriDaoSesame.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: Feb 26 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Feb 26, 2018
// Subject: A Dao specific to insert Uri into the triplestore
//***********************************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Optional;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Ask;
import phis2ws.service.view.model.phis.Uri;

/**
 * 
 * 
 * @author Eloan LAGIER
 */
public class UriDaoSesame extends DAOSesame<Uri>{

    public String uri;
    public String name;
    
    final static String TRIPLESTORE_FIELDS_TYPE = "type";
    final static String TRIPLESTORE_FIELDS_CLASS = "class";
    
    final static Logger LOGGER = LoggerFactory.getLogger(UriDaoSesame.class);
    public Boolean deep;

    URINamespaces uriNameSpace = new URINamespaces();
    
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
        query.appendTriplet(contextURI, "?class", "?type", null);

        LOGGER.debug("sparql select query : " + query.toString());
        return query;
    }
    
    /**
     * Searche uri with same label
     *
     * query example : SELECT ?class WHERE { ?class rdfs:label contextName }
     *
     * @return SPARQLQueryBuilder
     *
     *
     */
    protected SPARQLQueryBuilder prepareLabelSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String contextName;

        if (name != null) {
            contextName = name;
        } else {
            contextName = " ?label ";
            query.appendSelect(" ?label ");

        }

        query.appendSelect(" ?class ");
        query.appendTriplet(" ?class ", " rdfs:label ", contextName, null);

        LOGGER.debug(" sparql select query : " + query.toString());
        return query;
    }
    
    /**
     * Search siblings of concept query example : SELECT DISTINCT ?class WHERE {
     * contextURI rdfs:subClassOf ?parent . ?class rdfs:subClassOf ?parent }
     *
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
        query.appendTriplet(contextURI,uriNameSpace.getRelationsProperty("SubClassOf"), " ?parent ", null);
        query.appendTriplet("?class",uriNameSpace.getRelationsProperty("SubClassOf"), "?parent", null);
        LOGGER.debug(query.toString());
        return query;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Ask if an Uri is in the triplestore
     *
     * query exemple : ASK { concept ?any1 ?any2 .}
     *
     * @return SPARQLQueryBuilder
     */
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
        query.appendAsk(contextURI + " ?any1 ?any2 ");
        LOGGER.debug(query.toString());
        return query;
    }
    
    /**
     * call the query function for the ask-problemes
     * @return a boolean saying if the uri exist
     */
    public ArrayList<Ask> askUriExistance() {
        SPARQLQueryBuilder query = prepareAskQuery();
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Ask> uriExistancesResults = new ArrayList<>();
        boolean result = booleanQuery.evaluate();
        Ask ask = new Ask();
        ask.setExist(result);

        uriExistancesResults.add(ask);

        return uriExistancesResults;
    }
    
       /**
     * Search instances by uri, ... query example : SELECT
     * ?instance ?subclass WHERE {?subclass rdfs:subClassOf(*) context URI }
     *
     * @return SPARQLQueryBuilder
     */
    protected SPARQLQueryBuilder prepareInstanceSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String contextURI;

        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }

        query.appendSelect(" ?instance");
        query.appendSelect(" ?subclass");
        // if deep get descendents
        if (deep) {
            query.appendTriplet("?subclass", uriNameSpace.getRelationsProperty("rdfs:subClassOf*"), contextURI, null);
        } else {
            query.appendTriplet("?subclass", uriNameSpace.getRelationsProperty("rdfs:subClassOf"), contextURI, null);
        }
        query.appendTriplet("?instance", "rdf:type", "?subclass", null);
        LOGGER.debug("sparql select query : " + query.toString());
        return query;
    }
    
   /**
     * Search ancestors of concept query example : SELECT DISTINCT ?class WHERE
     * { contextURI rdfs:subClassOf* ?class }
     *
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
        query.appendTriplet(contextURI,uriNameSpace.getRelationsProperty("SubClassOf"), " ?class ", null);
        LOGGER.debug(query.toString());
        return query;
    }
    
    /**
     * Search descendants of concept query example : SELECT DISTINCT ?class
     * WHERE { ?class rdfs:subClassOf* contextURI }
     *
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
        query.appendTriplet(" ?class ",uriNameSpace.getRelationsProperty("SubClassOf*"), contextURI, null);
        LOGGER.debug(query.toString());

        return query;
    }
    
    
    /**
     * return the type of the uri given
     *
     *
     * SELECT DISTINCT ?type WHERE { concept rdf:type ?type . }
     *
     * @return SPARQLQueryBuilder
     */
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
        query.appendTriplet(contextURI, " rdf:type", " ?type ", null);
        LOGGER.debug(query.toString());
        return query;
    }

    
    
    
    /**
     * return all metadata for the uri given
     * @return Concept info all paginate
     */
    public ArrayList<Uri> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> uris = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            Uri uri = new Uri();
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                uri.setUri(this.uri);
                String classname = bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue();
                Value propertyType = bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE);
                //if its a litteral we look what's the language
                if (propertyType instanceof Literal) {
                    Literal literal = (Literal) bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE);
                    Optional<String> propertyLanguage = literal.getLanguage();
                    uri.addAnnotation(classname.substring(classname.indexOf("#") + 1, classname.length()) + "_" + propertyLanguage.get(), bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).stringValue());
                } else {
                    uri.addProperty(classname.substring(classname.indexOf("#") + 1, classname.length()), bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).stringValue());
                }
            }
            uris.add(uri);
        }
        return uris;
    }
    /**
     * paginate all the metadata of the query request
     *
     * @return ArrayList
     */
    public ArrayList<Uri> labelsPaginate() {
        SPARQLQueryBuilder query = prepareLabelSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> labels = new ArrayList();
        LOGGER.debug(query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                Uri label = new Uri();
                BindingSet bindingSet = result.next();
                label.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).toString());
                labels.add(label);
            }

        }
        return labels;
    }
    
    /**
     * return Sparql result paginated
     *
     * @return ArrayList<>
     */
    public ArrayList<Uri> instancesPaginate() {

        SPARQLQueryBuilder query = prepareInstanceSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        ArrayList<Uri> instances = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                Uri instance = new Uri();

                instance.setUri(bindingSet.getValue("instance").stringValue());
                instance.setRdfType(bindingSet.getValue("subclass").stringValue());

                instances.add(instance);
            }
        }

        return instances;
    }

   /**
     * call the query function for the ancestors GET
     * @return the ancestors info all paginate
     */
    public ArrayList<Uri> AncestorsAllPaginate() {

        SPARQLQueryBuilder query = prepareAncestorsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> concepts = new ArrayList();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                Uri concept = new Uri();
                BindingSet bindingSet = result.next();
                concept.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue());
                concepts.add(concept);
            }

        }
        return concepts;
    }
    
    /**
     * call the query function for the siblings GET
     * @return the siblings info all paginate
     */
    public ArrayList<Uri> SiblingsAllPaginate() {

        SPARQLQueryBuilder query = prepareSiblingsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> concepts = new ArrayList();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                Uri concept = new Uri();
                BindingSet bindingSet = result.next();
                concept.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue());
                concepts.add(concept);
            }
        }
        return concepts;
    }
    
    /**
     * call the query function for the descendants GET
     * @return the descendants info all paginate
     */
    public ArrayList<Uri> descendantsAllPaginate() {

        SPARQLQueryBuilder query = prepareDescendantsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> concepts = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                Uri concept = new Uri();
                BindingSet bindingSet = result.next();
                concept.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue());
                concepts.add(concept);
            }
        }
        return concepts;
    }
    
        /**
     * return the type of the uri if it's in the triplestore
     * @return a boolean or a type
     */
    public ArrayList<Uri> getAskTypeAnswer() {
        
        SPARQLQueryBuilder query = prepareAskQuery();
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> uris = new ArrayList<>();
        boolean result = booleanQuery.evaluate();
        Ask ask = new Ask();
        ask.setExist(result);

        Uri uriType = new Uri();
        if (ask.getExist().equals("true")) {
            
            query = prepareAskTypeQuery();
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
            TupleQueryResult resultat = tupleQuery.evaluate();
            BindingSet bindingSet = resultat.next();
            uriType.setRdfType(bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).toString());
            
        }
        uris.add(uriType);
        
        return uris;
    }
}
