//******************************************************************************
//                                       SpeciesDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 déc. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Species;

/**
 * Species access in the triplestore rdf4j
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SpeciesDAOSesame extends DAOSesame<Species> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeciesDAOSesame.class);

    public SpeciesDAOSesame() {
    }
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Generates the query to count the results for a specific filter.
     * @param filter
     * @param language
     * @example
     * SELECT DISTINCT  (COUNT(DISTINCT ?uri) as ?count) WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.opensilex.org/vocabulary/oeso#Species> . 
     *      OPTIONAL {
     *          ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label . 
     *      }
     *      FILTER ( (LANG(?label) = "" || LANGMATCHES(LANG(?label), "en")) ) 
     * }
     * @return The generated query
     */
    private SPARQLQueryBuilder prepareCountWithFilter(Species filter, String language) {
        SPARQLQueryBuilder query = prepareSearchWithFilterQuery(filter, language);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") as ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }

    /**
     * Count the query result with the given filter
     * @param filter
     * @param language
     * @return The number of results for a given filter.
     */
    public int countWithFilter(Species filter, String language) {
        SPARQLQueryBuilder prepareCount = prepareCountWithFilter(filter, language);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, prepareCount.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        return count;
    }
    
    /**
     * Generates a query with the given filters.
     * @param filter
     * @param language
     * @example
     * SELECT DISTINCT  ?uri  ?label WHERE {
     *      ?uri  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.opensilex.org/vocabulary/oeso#Species> . 
     *      OPTIONAL {
     *          ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label . 
     *      }
     *      FILTER ( (LANG(?label) = "" || LANGMATCHES(LANG(?label), "en")) ) 
     *  }
     *  LIMIT 20 
     *  OFFSET 0 
     * @return The generated query
     */
    protected SPARQLQueryBuilder prepareSearchWithFilterQuery(Species filter, String language) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendDistinct(Boolean.TRUE);
        
        // 1. Select URI
        String speciesUri;
        if (filter.getUri() != null) {
            speciesUri = "<" + filter.getUri() + ">";
        } else {
            speciesUri = "?" + URI;
            query.appendSelect(speciesUri);
        }
        
        //2. Add rdf type
        query.appendTriplet(speciesUri, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_SPECIES.toString(), Boolean.FALSE);
        
        //3. Filter with label and language if needed
        query.beginBodyOptional();
        query.appendSelect(" ?" + LABEL);
        query.appendToBody(speciesUri + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + LABEL + " . ");
        if (language != null) {
            query.appendFilter("LANG(?" + LABEL + ") = \"\" || LANGMATCHES(LANG(?" + LABEL + "), \"" + language + "\")");
        }
        if (filter.getLabel() != null){
            query.appendAndFilter("REGEX ( ?" + LABEL + ",\".*" + filter.getLabel() + ".*\",\"i\")");
        }
        query.endBodyOptional();

        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        
        return query;
    }
    
    /**
     * Get a species from a given binding set.
     * Assume that the following attributes exist :
     * uri, label
     * @param bindingSet a bindingSet from a search query
     * @return a species with data extracted from the given bindingSet
     */
    private Species getSpeciesFromBindingSet(Species filter, BindingSet bindingSet) {
        Species species = new Species();

        if (filter.getUri() != null) {
            species.setUri(filter.getUri());
        } else {
            species.setUri(bindingSet.getValue(URI).stringValue());
        }

        species.setLabel(bindingSet.getValue(LABEL).stringValue());
        
        return species;
    }
    
    /**
     * Search species with the given filter.
     * @param filter
     * @param language
     * @return The list of species corresponding to the result.
     */
    public ArrayList<Species> searchWithFilter(Species filter, String language) {
        SPARQLQueryBuilder query = prepareSearchWithFilterQuery(filter, language);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        ArrayList<Species> species;
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            species = new ArrayList<>();
            
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Species infrastructure = getSpeciesFromBindingSet(filter, bindingSet);
                species.add(infrastructure);
            }
        }
        
        return species;
    }
}
