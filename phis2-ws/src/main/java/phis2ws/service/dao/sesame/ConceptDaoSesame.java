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
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Ask;
import phis2ws.service.view.model.phis.Concept;

/**
 * Represents the Data Access Object for the concepts
 *
 * @author Eloan Lagier
 */
public class ConceptDaoSesame extends DAOSesame<Concept> {

    final static Logger LOGGER = LoggerFactory.getLogger(ConceptDaoSesame.class);

    public String uri;

    final static String TRIPLESTORE_FIELDS_TYPE = "type";
    final static String TRIPLESTORE_FIELDS_CLASS = "class";

    URINamespaces uriNamespaces = new URINamespaces();
    /**
     * Search properties of concept by it's uri (ex : label, subclass.. )
     *
     * @return the search query. example : SELECT DISTINCT ?class ?info WHERE {
     * conceptURI ?class ?info }
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
        query.appendTriplet(contextURI, "?class", "?type", null);

        LOGGER.debug("sparql select query : " + query.toString());
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
        query.appendTriplet(" ?class ",uriNamespaces.getRelationsProperty("SubClassOf*"), contextURI, null);
        LOGGER.debug(query.toString());

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
        query.appendTriplet(contextURI,uriNamespaces.getRelationsProperty("SubClassOf*"), " ?class ", null);
        LOGGER.debug(query.toString());
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
        query.appendTriplet(contextURI,uriNamespaces.getRelationsProperty("SubClassOf"), " ?parent ", null);
        query.appendTriplet("?class",uriNamespaces.getRelationsProperty("SubClassOf"), "?parent", null);
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
    public ArrayList<Concept> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            Concept concept = new Concept();
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                concept.setUri(uri);
                String classname = bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue();
                Value propertyType = bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE);
                //if its a litteral we look what's the language
                if (propertyType instanceof Literal) {
                    Literal literal = (Literal) bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE);
                    Optional<String> propertyLanguage = literal.getLanguage();
                    concept.addProperty(classname.substring(classname.indexOf("#") + 1, classname.length()) + "_" + propertyLanguage.get(), bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).stringValue());
                } else {
                    concept.addProperty(classname.substring(classname.indexOf("#") + 1, classname.length()), bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).stringValue());
                }
            }
            concepts.add(concept);
        }
        return concepts;
    }

    /**
     * call the query function for the descendants GET
     * @return the descendants info all paginate
     */
    public ArrayList<Concept> descendantsAllPaginate() {

        SPARQLQueryBuilder query = prepareDescendantsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                Concept concept = new Concept();
                BindingSet bindingSet = result.next();
                concept.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue());
                concepts.add(concept);
            }
        }
        return concepts;
    }

    /**
     * call the query function for the ancestors GET
     * @return the ancestors info all paginate
     */
    public ArrayList<Concept> AncestorsAllPaginate() {

        SPARQLQueryBuilder query = prepareAncestorsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                Concept concept = new Concept();
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
    public ArrayList<Concept> SiblingsAllPaginate() {

        SPARQLQueryBuilder query = prepareSiblingsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Concept> concepts = new ArrayList();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                Concept concept = new Concept();
                BindingSet bindingSet = result.next();
                concept.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue());
                concepts.add(concept);
            }
        }
        return concepts;
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
        ask.setExist(String.valueOf(result));

        uriExistancesResults.add(ask);

        return uriExistancesResults;
    }

    /**
     * return the type of the uri if it's in the triplestore
     * @return a boolean or a type
     */
    public ArrayList<Ask> getAskTypeAnswer() {
        
        SPARQLQueryBuilder query = prepareAskQuery();
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Ask> answer = new ArrayList<>();
        boolean result = booleanQuery.evaluate();
        Ask ask = new Ask();
        ask.setExist(String.valueOf(result));

        if (ask.getExist().equals("true")) {
            query = prepareAskTypeQuery();
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
            TupleQueryResult resultat = tupleQuery.evaluate();
            BindingSet bindingSet = resultat.next();
            ask.setRdfType(bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).toString());
        }

        answer.add(ask);
        return answer;
    }
}
