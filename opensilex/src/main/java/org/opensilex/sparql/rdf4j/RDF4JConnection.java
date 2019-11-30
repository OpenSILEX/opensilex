//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import java.net.URI;
import java.util.*;
import java.util.function.*;
import org.apache.jena.arq.querybuilder.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.http.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.exceptions.*;

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
    public List<SPARQLStatement> executeDescribeQuery(DescribeBuilder describe) throws SPARQLQueryException {
        GraphQuery describeQuery = rdf4JConnection.prepareGraphQuery(QueryLanguage.SPARQL, describe.buildString());
        GraphQueryResult results = describeQuery.evaluate();
        
        return statementsToSPARQLResultList(results);
    }

    @Override
    public List<SPARQLStatement> executeConstructQuery(ConstructBuilder construct) throws SPARQLQueryException {
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
    public void clearGraph(URI graph) throws SPARQLQueryException {
        rdf4JConnection.clear(SimpleValueFactory.getInstance().createIRI(graph.toString()));
    }

    @Override
    public void clear() throws SPARQLQueryException {
        rdf4JConnection.clear();
    }

    private List<SPARQLStatement> statementsToSPARQLResultList(QueryResult<Statement> queryResults) {
        List<SPARQLStatement> resultList = new ArrayList<>();

        while (queryResults.hasNext()) {
            Statement result = queryResults.next();
            resultList.add(new RDF4JStatement(result));
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
