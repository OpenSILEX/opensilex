//**********************************************************************************************
//                                       ConceptDaoSesame.java 

// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 8, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Decembre 8, 2017
// Subject: A Dao specific to concept insert into triplestore 
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
import static phis2ws.service.dao.sesame.UnitDaoSesame.LOGGER;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Concept;
import phis2ws.service.view.model.phis.Document;
import phis2ws.service.view.model.phis.Instance;
import phis2ws.service.view.model.phis.OntologyReference;


public class ConceptDaoSesame extends DAOSesame<Concept>{
    final static Logger LOGGER = LoggerFactory.getLogger(ConceptDaoSesame.class);
    public String uri;
    public String type;
    public Boolean deep;


    public ConceptDaoSesame() {
    }
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        final URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String contextURI;

        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        
        if (deep == true ) {
            query.appendSelect("?instance");
            query.appendSelect(" ?subclass");
            query.appendTriplet("?subclass", "rdfs:subClassOf*", contextURI, null);
            query.appendTriplet(  "?instance", "rdf:type", "?subclass", null);
        }else if (deep = false) {
            query.appendSelect(" ?instance");
            query.appendSelect(" ?subclass");
            query.appendTriplet("?subclass", "rdfs:subClassOf", contextURI, null);
            query.appendTriplet(  "?instance", "rdf:type", "?subclass", null);
        }
        LOGGER.trace("sparql select query : " + query.toString());
        return query;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  
    public ArrayList<Instance> allPaginate() {
        LOGGER.debug("bssfkhsedlkfhlisugh");
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Instance> instances = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            LOGGER.debug("bs2 =", result);
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                LOGGER.debug("uri = ", bindingSet.getValue("uri").stringValue());
                LOGGER.debug("type = ", bindingSet.getValue("type").stringValue());
                LOGGER.debug("all = ", bindingSet.getBindingNames());
                Instance instance = new Instance();
                if (uri != null) {
                    instance.setUri(uri);
                } else {
                    instance.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (type != null) {
                    instance.setType(type);
                } else {
                    instance.setType(bindingSet.getValue("Subclass").stringValue());
                }

                
                instances.add(instance);
            }
        }
        
        return instances;
    }

}