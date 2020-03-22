//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.riot.Lang;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLConnection;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.utils.SHACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class RDF4JConnection implements SPARQLConnection {

    public static final int TIMEOUT = 20;

    private final static Logger LOGGER = LoggerFactory.getLogger(RDF4JConnection.class);

    private final RepositoryConnection rdf4JConnection;

    private static AtomicInteger connectionCount = new AtomicInteger(0);

    public RDF4JConnection(RepositoryConnection rdf4JConnection) {
        this.rdf4JConnection = rdf4JConnection;
        LOGGER.debug("Acquire RDF4J sparql connection: " + this.rdf4JConnection.hashCode() + " (" + RDF4JConnection.connectionCount.incrementAndGet() + ")");

    }

    @Override
    public void shutdown() throws Exception {
        LOGGER.debug("Release RDF4J sparql connection: " + this.rdf4JConnection.hashCode() + " (" + RDF4JConnection.connectionCount.decrementAndGet() + ")");
        this.rdf4JConnection.close();
    }

    @Override
    public boolean executeAskQuery(AskBuilder ask) throws SPARQLException {
        try {

            BooleanQuery askQuery = rdf4JConnection.prepareBooleanQuery(QueryLanguage.SPARQL, ask.buildString());
            askQuery.setMaxExecutionTime(TIMEOUT);
            return askQuery.evaluate();
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public List<SPARQLStatement> executeDescribeQuery(DescribeBuilder describe) throws SPARQLException {
        try {
            GraphQuery describeQuery = rdf4JConnection.prepareGraphQuery(QueryLanguage.SPARQL, describe.buildString());
            describeQuery.setMaxExecutionTime(TIMEOUT);
            GraphQueryResult results = describeQuery.evaluate();

            return statementsToSPARQLResultList(results);
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public List<SPARQLStatement> executeConstructQuery(ConstructBuilder construct) throws SPARQLException {
        try {
            GraphQuery constructQuery = rdf4JConnection.prepareGraphQuery(QueryLanguage.SPARQL, construct.buildString());
            constructQuery.setMaxExecutionTime(TIMEOUT);
            GraphQueryResult results = constructQuery.evaluate();

            return statementsToSPARQLResultList(results);
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLException {
        try {
            TupleQuery selectQuery = rdf4JConnection.prepareTupleQuery(QueryLanguage.SPARQL, select.buildString());
            selectQuery.setMaxExecutionTime(TIMEOUT);
            TupleQueryResult results = selectQuery.evaluate();

            return bindingSetsToSPARQLResultList(results, resultHandler);
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLException {
        try {
            Update updateQuery = rdf4JConnection.prepareUpdate(QueryLanguage.SPARQL, update.buildRequest().toString());
            updateQuery.setMaxExecutionTime(TIMEOUT);
            updateQuery.execute();
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void executeDeleteQuery(UpdateBuilder update) throws SPARQLException {
        try {
            Update updateQuery = rdf4JConnection.prepareUpdate(QueryLanguage.SPARQL, update.buildRequest().toString());
            updateQuery.setMaxExecutionTime(TIMEOUT);
            updateQuery.execute();
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void startTransaction() throws SPARQLException {
        try {
            rdf4JConnection.begin();
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void commitTransaction() throws SPARQLException {
        try {
            rdf4JConnection.commit();
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void rollbackTransaction() throws SPARQLException {
        try {
            rdf4JConnection.rollback();
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void clearGraph(URI graph) throws SPARQLException {
        try {
            rdf4JConnection.clear(SimpleValueFactory.getInstance().createIRI(graph.toString()));
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void renameGraph(URI oldGraphURI, URI newGraphURI) throws SPARQLException {

        try {
            String moveQuery = "MOVE <" + oldGraphURI + "> TO <" + newGraphURI + ">";
            Update renameQuery = rdf4JConnection.prepareUpdate(QueryLanguage.SPARQL, moveQuery);
            renameQuery.setMaxExecutionTime(TIMEOUT);
            renameQuery.execute();

        } catch (UpdateExecutionException | RepositoryException | MalformedQueryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public List<SPARQLStatement> getGraphStatement(URI graph) throws SPARQLException {
        try {
            RepositoryResult<Statement> results = rdf4JConnection.getStatements(null, null, null, SimpleValueFactory.getInstance().createIRI(graph.toString()));

            return repoStatementsToSPARQLResultList(results);
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    @Override
    public void clear() throws SPARQLException {
        try {
            rdf4JConnection.clear();
        } catch (RepositoryException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(ex.getMessage());
            }
        }
    }

    private List<SPARQLStatement> statementsToSPARQLResultList(QueryResult<Statement> queryResults) {
        List<SPARQLStatement> resultList = new ArrayList<>();

        while (queryResults.hasNext()) {
            Statement result = queryResults.next();
            resultList.add(new RDF4JStatement(result));
        }

        queryResults.close();
        return resultList;
    }

    private List<SPARQLStatement> repoStatementsToSPARQLResultList(RepositoryResult<Statement> queryResults) {
        List<SPARQLStatement> resultList = new ArrayList<>();

        while (queryResults.hasNext()) {
            Statement result = queryResults.next();
            resultList.add(new RDF4JStatement(result));
        }

        queryResults.close();
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

        queryResults.close();
        return resultList;
    }

    private SPARQLValidationException convertRDF4JSHACLException(ShaclSailValidationException validationEx) {
        SPARQLValidationException exception = new SPARQLValidationException();
        validationEx.getValidationReport().getValidationResult().forEach((validationResult) -> {
            URI invalidObject = null;
            URI invalidObjectProperty = null;
            URI brokenConstraint = null;
            for (Statement stmt : validationResult.asModel()) {
                try {
                    if (stmt.getPredicate().toString().equals(SHACL.focusNode.toString())) {
                        invalidObject = new URI(stmt.getObject().stringValue());
                    } else if (stmt.getPredicate().toString().equals(SHACL.resultPath.toString())) {
                        invalidObjectProperty = new URI(stmt.getObject().stringValue());
                    } else if (stmt.getPredicate().toString().equals(SHACL.sourceConstraintComponent.toString())) {
                        brokenConstraint = new URI(stmt.getObject().stringValue());
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Unexpected error while analyzing SHACL exception", ex);
                }

            }
            if (invalidObject != null && invalidObjectProperty != null && brokenConstraint != null) {
                exception.addValidationError(invalidObject, invalidObjectProperty, brokenConstraint);
            }

        });
        return exception;
    }

    @Override
    public URI getGraphSHACL() {
        try {
            return new URI(RDF4J.SHACL_SHAPE_GRAPH.toString());
        } catch (URISyntaxException ex) {
            LOGGER.error("Unexpected error that should never happend", ex);
            return null;
        }
    }

    @Deprecated
    public RepositoryConnection getRepositoryConnectionImpl() {
        return rdf4JConnection;
    }

    @Override
    public void disableSHACL() throws SPARQLException {
        URI shaclGraph = getGraphSHACL();
        if (shaclGraph != null) {
            clearGraph(shaclGraph);
        }
    }

    @Override
    public void enableSHACL() throws SPARQLException {
        URI shaclGraph = getGraphSHACL();
        if (shaclGraph != null) {
            clearGraph(shaclGraph);
            
            List<String> shaclList = new ArrayList<>();

            for (Class<?> c : SPARQLClassObjectMapper.getResourceClasses()) {
                shaclList.add(SHACL.generateSHACL(c));
            }

            String shaclString = StringUtils.join(shaclList, '\n');

            LOGGER.debug("Generated SHACL: \n" + shaclString);

            if (!shaclList.isEmpty()) {
                loadOntology(getGraphSHACL(), shaclString, Lang.TURTLE);
            }
        } else {
            LOGGER.warn("No SHACL graph specified, SHACL validation will be disabled");
        }

    }
}
