/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResult;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.exceptions.SPARQLTransactionException;
import org.opensilex.sparql.SPARQLConnection;
import org.opensilex.sparql.SPARQLResult;

/**
 *
 * @author vincent
 */
public class RDF4JConnection implements SPARQLConnection {

    private RepositoryConnection rdf4JConnection;
    private RDF4JConfig config;
    
    public RDF4JConnection(RDF4JConfig config) {
        this.config = config;
    }

    public RDF4JConnection(RepositoryConnection connection) {
        this.rdf4JConnection = connection;
    }

    @Override
    public void startup() {
        if (rdf4JConnection == null) {
            HTTPRepository repository = new HTTPRepository(config.serverURI(), config.repository());
            rdf4JConnection = repository.getConnection();
        }

    }

    @Override
    public void shutdown() {
        if (rdf4JConnection != null) {
            rdf4JConnection.close();
        }
    }

    @Override
    public boolean executeAskQuery(AskBuilder ask) throws SPARQLQueryException {
        BooleanQuery askQuery = rdf4JConnection.prepareBooleanQuery(QueryLanguage.SPARQL, ask.buildString());
        return askQuery.evaluate();
    }

    @Override
    public List<SPARQLResult> executeDescribeQuery(DescribeBuilder describe) throws SPARQLQueryException {
        GraphQuery describeQuery = rdf4JConnection.prepareGraphQuery(QueryLanguage.SPARQL, describe.buildString());
        GraphQueryResult results = describeQuery.evaluate();

        return statementsToSPARQLResultList(results);
    }

    @Override
    public List<SPARQLResult> executeConstructQuery(ConstructBuilder construct) throws SPARQLQueryException {
        GraphQuery constructQuery = rdf4JConnection.prepareGraphQuery(QueryLanguage.SPARQL, construct.buildString());
        GraphQueryResult results = constructQuery.evaluate();

        return statementsToSPARQLResultList(results);
    }

    @Override
    public List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLQueryException {
        TupleQuery selectQuery = rdf4JConnection.prepareTupleQuery(QueryLanguage.SPARQL, select.buildString());
        TupleQueryResult results = selectQuery.evaluate();

        return bindingSetsToSPARQLResultList(results, resultHandler);
    }

    @Override
    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLQueryException {
        Update updateQuery = rdf4JConnection.prepareUpdate(QueryLanguage.SPARQL, update.buildRequest().toString());
        updateQuery.execute();
    }

    @Override
    public void executeDeleteQuery(UpdateBuilder update) throws SPARQLQueryException {
        Update updateQuery = rdf4JConnection.prepareUpdate(QueryLanguage.SPARQL, update.buildRequest().toString());
        updateQuery.execute();
    }

    @Override
    public void startTransaction() throws SPARQLTransactionException {
        rdf4JConnection.begin();
    }

    @Override
    public void commitTransaction() throws SPARQLTransactionException {
        rdf4JConnection.commit();
    }

    @Override
    public void rollbackTransaction() throws SPARQLTransactionException {
        rdf4JConnection.rollback();
    }

    @Override
    public void clearGraph(Node graph) throws SPARQLQueryException {
        rdf4JConnection.clear(SimpleValueFactory.getInstance().createIRI(graph.toString()));
    }

    @Override
    public void clear() throws SPARQLQueryException {
        rdf4JConnection.clear();
    }

    private List<SPARQLResult> statementsToSPARQLResultList(QueryResult<Statement> queryResults) {
        List<SPARQLResult> resultList = new ArrayList<>();

        while (queryResults.hasNext()) {
            Statement result = queryResults.next();
            resultList.add(new RDF4JResult(result));
        }

        return resultList;
    }

    private List<SPARQLResult> bindingSetsToSPARQLResultList(QueryResult<BindingSet> queryResults, Consumer<SPARQLResult> resultHandler) {
        List<SPARQLResult> resultList = new ArrayList<>();

        while (queryResults.hasNext()) {
            RDF4JResult result = new RDF4JResult(queryResults.next());
            if (resultHandler != null) {
                resultHandler.accept(result);
            }

            resultList.add(result);
        }

        return resultList;
    }
}
