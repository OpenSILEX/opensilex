//**********************************************************************************************
//                                       ConceptDaoSesame.java 
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 8, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Decembre 8, 2017
// Subject: A Dao specific to instance insert into triplestore 
//***********************************************************************************************
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
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Instance;

/**
 * Represents the Data Access Object for the instances
 *
 * @author Eloan Lagier
 */
public class InstanceDaoSesame extends DAOSesame<Instance> {

    final static Logger LOGGER = LoggerFactory.getLogger(InstanceDaoSesame.class);
    public String uri;
    public Boolean deep;
    
    final static String TRIPLESTORE_FIELDS_INSTANCE = "instance";
    final static String TRIPLESTORE_FIELDS_SUBCLASS = "subclass";
    
    URINamespaces uriNameSpace = new URINamespaces();

    public InstanceDaoSesame() {
    }

    /**
     * Search instances by concept type, uri, ... query example : SELECT
     * ?instance ?subclass WHERE {?subclass rdfs:subClassOf(*) context URI }
     *
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

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * return Sparql result paginated
     *
     * @return ArrayList<>
     */
    public ArrayList<Instance> allPaginate() {

        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        ArrayList<Instance> instances = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                Instance instance = new Instance();

                instance.setUri(bindingSet.getValue("instance").stringValue());
                instance.setType(bindingSet.getValue("subclass").stringValue());

                instances.add(instance);
            }
        }

        return instances;
    }

}
