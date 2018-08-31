//******************************************************************************
//                                       BrapiTraitDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import static phis2ws.service.dao.sesame.TraitDaoSesame.LOGGER;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.BrapiTrait;

/**
 * Get all traits available in the system according to brapi specifications
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiTraitDAO extends DAOSesame<BrapiTrait> {
   
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        final URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(uriNamespaces.getContextsProperty("variables"));

        query.appendSelect("?uri");
        query.appendTriplet("?uri", "rdf:type", uriNamespaces.getObjectsProperty("cTrait"), null);

        query.appendSelect(" ?label");
        query.appendTriplet("?uri", "rdfs:label", "?label", null);

        query.appendSelect(" ?comment");
        query.appendTriplet("?uri", "rdfs:comment", "?comment", null);
                
        LOGGER.trace("sparql select query : " + query.toString());
        return query;
    }
    
    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    /**
     * Get the Variables associated to the traits
     * @author Alice Boizet <alice.boizet@inra.fr>
     * @return traits list of traits
     */    
    private ArrayList<BrapiTrait> getVariables(ArrayList<BrapiTrait> traits){
        final URINamespaces uriNamespaces = new URINamespaces();
        
        for (BrapiTrait bt:traits) {
            SPARQLQueryBuilder query = new SPARQLQueryBuilder();
            query.appendSelect("?varUri");
            query.appendTriplet("?varUri", uriNamespaces.getRelationsProperty("rHasTrait"),bt.getTraitDbId(), null);   
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
            ArrayList<String> varList = new ArrayList();
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    varList.add(bindingSet.getValue("varUri").stringValue());
                }                    
            }
            bt.setObservationVariables(varList);
        }
        return traits; 
    }    

    /**
     * Collect the list of traits
     * @author Alice Boizet <alice.boizet@inra.fr>
     * @return traits list of traits
     */
    public ArrayList<BrapiTrait> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<BrapiTrait> traits = new ArrayList();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                BrapiTrait trait = new BrapiTrait();
                trait.setTraitDbId(bindingSet.getValue("uri").stringValue());
                trait.setDescription(bindingSet.getValue("comment").stringValue());
                trait.setName(bindingSet.getValue("label").stringValue());
               
                traits.add(trait);
            }
        }
        traits = getVariables(traits);
        
        return traits;
    }    
}
