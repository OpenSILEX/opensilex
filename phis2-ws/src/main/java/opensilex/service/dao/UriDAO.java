//******************************************************************************
//                                 UriDAO.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 26 Feb. 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.model.Ask;
import opensilex.service.model.Uri;

// SILEX:todo
// - Element: Instances
//   Method: instancesPaginate
//   Purpose: need to paginate the query instead of retreive all instances and then filtered them
// \SILEX:todo
/**
 * URI DAO.
 * @author Eloan Lagier
 */
public class UriDAO extends Rdf4jDAO<Uri> {

    public String uri;
    public String label;
    public String language;
    
    //used to query the triplestore
    final static String TRIPLESTORE_FIELDS_TYPE = "type";
    final static String TRIPLESTORE_FIELDS_CLASS = "class";
    final static String TRIPLESTORE_FIELDS_INSTANCE = "instance";
    final static String TRIPLESTORE_FIELDS_SUBCLASS = "subclass";

    final static Logger LOGGER = LoggerFactory.getLogger(UriDAO.class);
    public Boolean deep;

    /**
     * Prepares a query to get the triplets of an URI (given or not).
     * @example
     * SELECT DISTINCT ?class ?type 
     * WHERE {
     * <http://www.opensilex.org/vocabulary/oeso#Document> ?class ?type . 
     * }
     * @return the query 
     */
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

        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }

    /**
     * Builds a query to search a URI with the same label.
     * @return the query 
     * query example : 
     * SELECT ?class 
     * WHERE {
     * ?class rdfs:label contextName
     * }
     */
    protected SPARQLQueryBuilder prepareLabelSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String selectLabel;

        if (label != null) {
            selectLabel = label;
        } else {
            selectLabel = " ?label ";
            query.appendSelect(" ?label ");

        }

        query.appendSelect(" ?class ");
        query.appendTriplet(" ?class ", Rdfs.RELATION_LABEL.toString(), selectLabel, null);

        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }

    /**
     * Searches siblings of concept. 
     * @example 
     * SELECT DISTINCT ?class 
     * WHERE {
     *    contextURI rdfs:subClassOf ?parent .
     *    ?class rdfs:subClassOf ?parent 
     * }
     * @return SPARQLQueryBuilder
     */
    protected SPARQLQueryBuilder prepareSiblingsQuery() {
        //SILEX:warning
        //Siblings take ScientificDocument for exemple but it's different that all the other concept GET
        //where could this can be change?
        //\SILEX:warning

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
        query.appendTriplet(contextURI, Rdfs.RELATION_SUBCLASS_OF.toString(), " ?parent ", null);
        query.appendTriplet("?class", Rdfs.RELATION_SUBCLASS_OF.toString(), "?parent", null);
        LOGGER.debug(query.toString());
        return query;
    }

    /**
     * Asks if an URI is in the triplestore.
     * @return the ask query 
     * @example
     * ASK { 
     *   concept ?any1 ?any2 .
     * }
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

        query.appendAsk(contextURI + " ?any1 ?any2 "); //any = anything
        LOGGER.debug(query.toString());
        return query;
    }

    /**
     * Checks if the given URIs exists in the triplestore and return the results
     * @return a boolean saying if the URI exist
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
     * Searches instances by URI, concept
     * @return SPARQLQueryBuilder the query
     * query example :
     * SELECT DISTINCT ?instance ?subclass 
     * WHERE { 
     *    ?subclass rdfs:subClassOf* <http://www.w3.org/ns/oa#Motivation> . 
     *    ?instance rdf:type ?subclass . 
     * }
     */
    protected SPARQLQueryBuilder prepareInstanceSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String contextURI;

        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?" + URI;
            query.appendSelect("?" + URI);
        }

        query.appendSelect(" ?" + TRIPLESTORE_FIELDS_INSTANCE);
        query.appendSelect(" ?" + TRIPLESTORE_FIELDS_SUBCLASS);
        // if deep get descendents
        if (deep) {
            query.appendTriplet("?" + TRIPLESTORE_FIELDS_SUBCLASS, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", contextURI, null);
        } else {
            query.appendTriplet("?" + TRIPLESTORE_FIELDS_SUBCLASS, Rdfs.RELATION_SUBCLASS_OF.toString(), contextURI, null);
        }
        query.appendTriplet("?" + TRIPLESTORE_FIELDS_INSTANCE, Rdf.RELATION_TYPE.toString(), "?" + TRIPLESTORE_FIELDS_SUBCLASS, null);
        
        query.appendSelect(" ?" + LABEL);
        query.beginBodyOptional();
        query.appendToBody("?" + TRIPLESTORE_FIELDS_INSTANCE + " <" + Rdfs.RELATION_LABEL.toString() + "> ?" + LABEL);
        if (language != null) {
            query.appendFilter("LANG(?" + LABEL + ") = \"\" || LANGMATCHES(LANG(?" + LABEL + "), \"" + language + "\")");
        }
        query.endBodyOptional();
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }

    /**
     * Searches ancestors of a concept.
     * @return SPARQLQueryBuilder the query
     * @example
     * SELECT DISTINCT ?class 
     * WHERE {
     *    contextURI rdfs:subClassOf* ?class 
     * }
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
        query.appendTriplet(contextURI, Rdfs.RELATION_SUBCLASS_OF.toString(), " ?class ", null);
        LOGGER.debug(query.toString());
        return query;
    }

    /**
     * Searches descendants of a concept.
     * @return SPARQLQueryBuilder  the query 
     * @example : 
     * SELECT DISTINCT ?class 
     * WHERE {
     *    ?class rdfs:subClassOf* contextURI 
     * }
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
        query.appendTriplet(" ?class ", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", contextURI, null);
        LOGGER.debug(query.toString());

        return query;
    }

    /**
     * Creates the query that returns the type of an URI.
     * @return SPARQLQueryBuilder the query
     * @example 
     * SELECT DISTINCT ?type 
     * WHERE {
     *    concept rdf:type ?type . 
     * }
     */
    protected SPARQLQueryBuilder prepareGetUriType() {
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
        query.appendTriplet(contextURI, Rdf.RELATION_TYPE.toString(), " ?type ", null);
        LOGGER.debug(query.toString());
        return query;
    }

    /**
     * Returns all metadata for the URI given.
     * @return the list of the URIs corresponding to the search information
     */
    public ArrayList<Uri> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> uris = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                Uri uriFounded = new Uri();
                BindingSet bindingSet = result.next();
                uriFounded.setUri(this.uri);
                String classname = bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).stringValue();
                Value propertyType = bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE);
                //if its a litteral we look what's the language
                if (propertyType instanceof Literal) {
                    Literal literal = (Literal) bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE);
                    Optional<String> propertyLanguage = literal.getLanguage();
                    uriFounded.addAnnotation(classname.substring(classname.indexOf("#") + 1, classname.length()) + "_" + propertyLanguage.get(), bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).stringValue());
                } else {
                    uriFounded.addProperty(classname.substring(classname.indexOf("#") + 1, classname.length()), bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).stringValue());
                }
                uris.add(uriFounded);
            }
        }
        return uris;
    }

    /**
     * Searches the URIs which have the given label as label.
     * @return ArrayList
     */
    public ArrayList<Uri> labelsPaginate() {
        SPARQLQueryBuilder query = prepareLabelSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> uris = new ArrayList();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                Uri uriFounded = new Uri();
                BindingSet bindingSet = result.next();
                uriFounded.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).toString());
                uris.add(uriFounded);
            }
        }
        return uris;
    }

    /**
     * List the desired instances.
     * SILEX:todo
     * need to paginate the query instead of retrieving all instances and then filter them
     * \SILEX:todo
     * @return the list of the instances, corresponding to the search params
     * given
     */
    public ArrayList<Uri> instancesPaginate() {

        SPARQLQueryBuilder query = prepareInstanceSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        ArrayList<Uri> instances = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                Uri instance = new Uri();

                instance.setUri(bindingSet.getValue(TRIPLESTORE_FIELDS_INSTANCE).stringValue());
                instance.setRdfType(bindingSet.getValue(TRIPLESTORE_FIELDS_SUBCLASS).stringValue());
                instance.setLabel(bindingSet.getValue(LABEL).stringValue());
                instances.add(instance);
            }
        }
        return instances;
    }

    /**
     * Calls the query function for the ancestors GET.
     * @return the ancestors info all paginate
     */
    public ArrayList<Uri> ancestorsAllPaginate() {

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
     * Calls the query function for the siblings GET.
     * @return the siblings info all paginate
     */
    public ArrayList<Uri> siblingsAllPaginate() {

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
     * Calls the query function for the descendants GET.
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
     * Returns the type of the URI if it's in the triplestore.
     * @return a boolean or a type
     */
    public ArrayList<Uri> getAskTypeAnswer() {

        SPARQLQueryBuilder query = prepareAskQuery();
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Uri> uris = new ArrayList<>();
        boolean result = booleanQuery.evaluate();
        Ask ask = new Ask();
        ask.setExist(result);

        if (ask.getExist()) {
            Uri uriType = new Uri();
            query = prepareGetUriType();
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
            try (TupleQueryResult resultat = tupleQuery.evaluate()) {
                BindingSet bindingSet = resultat.next();
                uriType.setRdfType(bindingSet.getValue(TRIPLESTORE_FIELDS_TYPE).toString());
                uris.add(uriType);
            }
        }

        return uris;
    }

    /**
     * Generates an ask query to know if the given rdfSubType is a subclass of
     * rdfType.
     * @param rdfSubType
     * @param rdfType
     * @return the query. 
     * @example
     * ASK {
     *    <http://www.opensilex.org/vocabulary/oeso#HemisphericalCamera>  rdfs:subClassOf* <http://www.opensilex.org/vocabulary/oeso#SensingDevice> 
     * }
     */
    private SPARQLQueryBuilder prepareIsSubclassOf(String rdfSubType, String rdfType) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendTriplet("<" + rdfSubType + ">", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", "<" + rdfType + ">", null);

        query.appendAsk(""); //any = anything
        LOGGER.debug(query.toString());
        return query;
    }

    /**
     * Generates an ask query to know if the given instance URI is an instance
     * of rdfType.
     * @param instanceUri
     * @param rdfType
     * @return the query. 
     * @example
     * ASK { 
     *  <http://www.w3.org/ns/oa#commenting>  rdf:type  <  http://www.w3.org/ns/oa#Motivation>  .  
     * }
     */
    private SPARQLQueryBuilder prepareIsInstanceOf(String instanceUri, String rdfType) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String contextURI;

        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";

        }
        query.appendSelect(contextURI);
        query.appendTriplet("<" + instanceUri + ">", Rdf.RELATION_TYPE.toString(), "<" + rdfType + ">", null);

        query.appendAsk("");
        LOGGER.debug(query.toString());
        return query;
    }

    /**
     * Checks if the given rdfSubType is a sub class of the given rdfType.
     * @param rdfSubType
     * @param rdfType
     * @return true if it is a subclass 
     *         false if not
     */
    public boolean isSubClassOf(String rdfSubType, String rdfType) {
        SPARQLQueryBuilder query = prepareIsSubclassOf(rdfSubType, rdfType);

        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        return booleanQuery.evaluate();
    }

    /**
     * Checks if the given URI is an instance of the given rdfType.
     * @param instanceUri
     * @param rdfType
     * @return true if it is a subclass 
     *         false if not
     */
    public boolean isInstanceOf(String instanceUri, String rdfType) {
        if (instanceUri == null) {
            return false;
        }
        if (rdfType == null) {
            return false;
        }
        SPARQLQueryBuilder query = prepareIsInstanceOf(instanceUri, rdfType);
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
        return booleanQuery.evaluate();
    }

    /**
     * Generates a query to get the list of the labels of the URI attribute
     * @example
     * SELECT DISTINCT ?label 
     * WHERE {
     *  <http://www.opensilex.org/vocabulary/oeso#hasTechnicalContact> rdfs:label ?label . 
     * }
     * @return the generated query
     */
    protected SPARQLQueryBuilder prepareGetLabels() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendSelect("?" + LABEL);
        query.appendTriplet(uri, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);

        LOGGER.debug(SPARQL_QUERY + " " + query.toString());

        return query;
    }

    /**
     * Generates a query to get the list of the comments of the URI attribute.
     * @example
     * SELECT DISTINCT ?comment 
     * WHERE {
     *      <http://www.opensilex.org/vocabulary/oeso#hasTechnicalContact> rdfs:comment ?comment . 
     * }
     * @return the generated query
     */
    protected SPARQLQueryBuilder prepareGetComments() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendSelect("?" + COMMENT);
        query.appendTriplet(uri, Rdfs.RELATION_COMMENT.toString(), "?" + COMMENT, null);

        LOGGER.debug(SPARQL_QUERY + query.toString());

        return query;
    }

    /**
     * Gets the labels associated to the uri attribute in the triplestore.
     * @return the list of labels. The key is the language.
     * @example
     * [ 
     *    "fr" : "maison", 
     *    "en" : "home", 
     *    "none" : "dqfgdf" 
     * ]
     */
    public Multimap<String, String> getLabels() {
        SPARQLQueryBuilder query = prepareGetLabels();
        Multimap<String, String> labels = ArrayListMultimap.create();

        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Literal propertyLabel = (Literal) bindingSet.getValue(LABEL);
                if (propertyLabel.getLanguage().isPresent()) {
                    Optional<String> commentLanguage = propertyLabel.getLanguage();
                    labels.put(commentLanguage.get(), bindingSet.getValue(LABEL).stringValue());
                } else { //no language tag associated
                    labels.put("none", bindingSet.getValue(LABEL).stringValue());
                }
            }
        }
        return labels;
    }

    /**
     * Gets the comments associated to the uri attribute in the triplestore.
     * @return the list of comments. the key is the language 
     * @example
     * [ 
     *    "fr" : "maison", 
     *    "en" : "home", 
     *    "none" : "dqfgdf" 
     * ]
     */
    public Multimap<String, String> getComments() {
        SPARQLQueryBuilder query = prepareGetComments();
        Multimap<String, String> comments = ArrayListMultimap.create();

        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Literal propertyLabel = (Literal) bindingSet.getValue(COMMENT);
                if (propertyLabel.getLanguage().isPresent()) {
                    Optional<String> commentLanguage = propertyLabel.getLanguage();
                    comments.put(commentLanguage.get(), bindingSet.getValue(COMMENT).stringValue());
                } else { //no language tag associated
                    comments.put("none", bindingSet.getValue(COMMENT).stringValue());
                }
            }
        }
        return comments;
    }

    @Override
    public List<Uri> create(List<Uri> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Uri> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Uri> update(List<Uri> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Uri find(Uri object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Uri findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Uri> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
