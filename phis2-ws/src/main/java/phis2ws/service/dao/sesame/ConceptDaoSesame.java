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
    public String label;
    public String comment;
    public String deep;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();

    public ConceptDaoSesame() {
    }
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        final URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(uriNamespaces.getContextsProperty("pVoc2017"));
        String contextURI;

        if (uri != null) {
            contextURI = "<" + uri + ">";
        } else {
            contextURI = "?uri";
            query.appendSelect("?uri");
        }
        
        if (deep == "true" ) {
            query.appendSelect("?instance");
            query.appendSelect("?subclass");
            query.appendTriplet("?subclass", "rdfs:subClassOf*", contextURI, null);
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
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Instance> instances = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Instance instance = new Instance();
                
                if (uri != null) {
                    instance.setUri(uri);
                } else {
                    instance.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (label != null) {
                    instance.setLabel(label);
                } else {
                    instance.setLabel(bindingSet.getValue("label").stringValue());
                }
                
                if (comment != null) {
                    instance.setComment(comment);
                } else {
                    instance.setComment(bindingSet.getValue("comment").stringValue());
                }
                
                //On récupère maintenant la liste des références vers des ontologies... 
                SPARQLQueryBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(instance.getUri());
                TupleQuery tupleQueryOntologiesReferences = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryOntologiesReferences.toString());
                TupleQueryResult resultOntologiesReferences = tupleQueryOntologiesReferences.evaluate();
                while (resultOntologiesReferences.hasNext()) {
                    BindingSet bindingSetOntologiesReferences = resultOntologiesReferences.next();
                    if (bindingSetOntologiesReferences.getValue("object") != null
                            && bindingSetOntologiesReferences.getValue("property") != null) {
                        OntologyReference ontologyReference = new OntologyReference();
                        ontologyReference.setObject(bindingSetOntologiesReferences.getValue("object").toString());
                        ontologyReference.setProperty(bindingSetOntologiesReferences.getValue("property").toString());
                        if (bindingSetOntologiesReferences.getValue("seeAlso") != null) {
                            ontologyReference.setSeeAlso(bindingSetOntologiesReferences.getValue("seeAlso").toString());
                        }
                        
                        instance.addOntologyReference(ontologyReference);
                    }
                }
                
                instances.add(instance);
            }
        }
        
        return instances;
    }
    
        /**
     * 
     * @param uri
     * @return la liste des liens vers d'autres ontologies
     */
    private SPARQLQueryBuilder prepareSearchOntologiesReferencesQuery(String uri) {
        final URINamespaces uriNamespaces = new URINamespaces();
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(uriNamespaces.getContextsProperty("variables"));
        
        if (ontologiesReferences.isEmpty()) {
            query.appendSelect(" ?property ?object ?seeAlso");
            query.appendTriplet(uri, "?property", "?object", null);
            query.appendOptional("{?object rdfs:seeAlso ?seeAlso}");
            query.appendFilter("?property IN(<" + uriNamespaces.getRelationsProperty("rCloseMatch") + ">, <"
                                               + uriNamespaces.getRelationsProperty("rExactMatch") + ">, <"
                                               + uriNamespaces.getRelationsProperty("rNarrower") + ">, <"
                                               + uriNamespaces.getRelationsProperty("rBroader") + ">)");
        } else {
            for (OntologyReference ontologyReference : ontologiesReferences) {
                query.appendTriplet(uri, ontologyReference.getProperty(), ontologyReference.getObject(), null);
                query.appendTriplet(ontologyReference.getObject(), "rdfs:seeAlso", ontologyReference.getSeeAlso(), null);
            }
        }
        
        LOGGER.trace("SPARQL select query : " + query.toString());
        return query;
    }
}
