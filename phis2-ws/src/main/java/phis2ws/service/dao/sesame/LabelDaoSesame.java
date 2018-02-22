//**********************************************************************************************
//                                       LabelDaoSesame.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: Feb 2 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Feb 22, 2018
// Subject: A Dao specific to label insert into triplestore 
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
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Label;

/**
 * Represents the Data Access Object for the labels
 *
 * @author Eloan LAGIER
 */
public class LabelDaoSesame extends DAOSesame<Label> {

    final static Logger LOGGER = LoggerFactory.getLogger(ConceptDaoSesame.class);
    public String name;

    final static String TRIPLESTORE_FIELDS_CLASS = "class";

    /**
     * Searche concept with same label
     *
     * query example : SELECT ?class WHERE { ?class rdfs:label contextName }
     *
     * @return SPARQLQueryBuilder
     *
     *
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
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

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * paginate all the metadata of the query request
     *
     * @return ArrayList
     */
    public ArrayList<Label> AllPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Label> labels = new ArrayList();
        LOGGER.debug(query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                Label label = new Label();
                BindingSet bindingSet = result.next();
                label.setName(bindingSet.getValue(TRIPLESTORE_FIELDS_CLASS).toString());
                labels.add(label);
            }

        }
        return labels;
    }

}
