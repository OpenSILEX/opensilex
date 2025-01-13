//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.ORG;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.opensilex.service.BaseService;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.service.SPARQLConnection;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.sparql.utils.SHACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author vincent
 */
public class RDF4JConnection extends BaseService implements SPARQLConnection {

    private final static Logger LOGGER = LoggerFactory.getLogger(RDF4JConnection.class);

    private final RepositoryConnection rdf4JConnection;

    private static AtomicInteger connectionCount = new AtomicInteger(0);

    private Reasoner reasoner;

    public RDF4JConnection(RepositoryConnection rdf4JConnection) {
        super(null);
        this.rdf4JConnection = rdf4JConnection;
        LOGGER.debug("Acquire RDF4J sparql connection: " + this.rdf4JConnection.hashCode() + " (" + RDF4JConnection.connectionCount.incrementAndGet() + ")");

        LOGGER.debug("****************************************************************************************");
        long startTime = System.currentTimeMillis();
        Model rdf4jModel = new LinkedHashModel();

        Resource organizationGraph= rdf4JConnection.getValueFactory().createIRI("http://opensilex.test/set/scientific-object");
        try (RepositoryResult<Statement> result = rdf4JConnection.getStatements(null, null, null, organizationGraph)) {
            while (result.hasNext()) {
                Statement stmt = result.next();
                rdf4jModel.add(stmt);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        org.eclipse.rdf4j.rio.Rio.write(rdf4jModel, outputStream, RDFFormat.RDFXML);

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

// Lecture du modèle Jena à partir de l'InputStream
        var jenaModel = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        jenaModel.read(inputStream, "", "RDF/XML");
        var conversionTime = System.currentTimeMillis() - startTime;
        LOGGER.debug("RDF4J to Jena conversion time for Organization graph: " + conversionTime + "ms");
        LOGGER.debug("nb statements in Organization graph: " + jenaModel.size());
        /**
         * reasonner on the transitivity of the hasPart/hasSite property
         */
        LOGGER.debug("****************************************************************************************");

        String ruleSrc = "[rule1: (?o <http://www.w3.org/ns/org#hasSite> ?s), (?o <http://www.opensilex.org/vocabulary/oeso#hasPart> ?x) -> (?x <http://www.w3.org/ns/org#hasSite> ?s)]";
        List rules = Rule.parseRules(ruleSrc);

        reasoner = new GenericRuleReasoner(rules);
        InfModel inf = ModelFactory.createInfModel(reasoner, jenaModel);
        var totalTime = System.currentTimeMillis() - startTime;
        LOGGER.debug("reasonner creation time: " + (totalTime - conversionTime)  + "ms");
        LOGGER.debug("total time with reasoner creation: " + totalTime + "ms");
        LOGGER.debug("****************************************************************************************");
        Property hasPart = jenaModel.getProperty("http://www.opensilex.org/vocabulary/oeso#hasPart");
        Property hasSite = ORG.hasSite;
        org.apache.jena.rdf.model.Resource retrievedParent = inf.getResource("http://opensilex.test/id/organization/diascope");
        String sitesUris = inf.listStatements(retrievedParent, hasSite, (RDFNode) null).toList().stream().map(stmt -> stmt.getObject().toString()).reduce("", (a, b) -> a + ", " + b);
        String childrenUris = inf.listStatements(retrievedParent, hasPart, (RDFNode) null).toList().stream().map(stmt -> stmt.getObject().toString()).reduce("", (a, b) -> a + ", " + b);
        LOGGER.debug("Parent organization (Diascope) : ");
        LOGGER.debug("Sites: " + sitesUris);
        LOGGER.debug("organizations children: " + childrenUris);

        LOGGER.debug("");
        LOGGER.debug("Child organization (Diaphen) : ");
        org.apache.jena.rdf.model.Resource retrievedChild = inf.getResource("http://opensilex.test/id/organization/diaphen");
        sitesUris = inf.listStatements(retrievedChild, hasSite, (RDFNode) null).toList().stream().map(stmt -> stmt.getObject().toString()).reduce("", (a, b) -> a + ", " + b);
        LOGGER.debug("Sites: " + sitesUris);
        LOGGER.debug("****************************************************************************************");
    }

    private int timeout;

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
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
            if (getTimeout() > 0) {
                askQuery.setMaxExecutionTime(getTimeout());
            }
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
            if (getTimeout() > 0) {
                describeQuery.setMaxExecutionTime(getTimeout());
            }
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
            if (getTimeout() > 0) {
                constructQuery.setMaxExecutionTime(getTimeout());
            }
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
            if (getTimeout() > 0) {
                selectQuery.setMaxExecutionTime(getTimeout());
            }
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
    public Stream<SPARQLResult> executeSelectQueryAsStream(SelectBuilder select) throws SPARQLException {
        try {
            TupleQuery selectQuery = rdf4JConnection.prepareTupleQuery(QueryLanguage.SPARQL, select.buildString());
            if (getTimeout() > 0) {
                selectQuery.setMaxExecutionTime(getTimeout());
            }
            TupleQueryResult results = selectQuery.evaluate();
            if( ! results.hasNext()){
                return Stream.empty();
            }
            return results.stream().map(RDF4JResult::new);

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
        executeUpdateQuery(update.buildRequest().toString());
    }

    @Override
    public void executeUpdateQuery(String update) throws SPARQLException {
        try {
            Update updateQuery = rdf4JConnection.prepareUpdate(QueryLanguage.SPARQL, update);
            if (getTimeout() > 0) {
                updateQuery.setMaxExecutionTime(getTimeout());
            }
            updateQuery.execute();
        } catch (Exception ex) {
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
            if (getTimeout() > 0) {
                updateQuery.setMaxExecutionTime(getTimeout());
            }
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
    public boolean hasActiveTransaction() {
        return rdf4JConnection.isActive();
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
    public void rollbackTransaction(Exception ex) throws Exception {
        try {
            rdf4JConnection.rollback();
            if(ex != null){
                throw ex;
            }
        } catch (RepositoryException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ShaclSailValidationException) {
                throw convertRDF4JSHACLException((ShaclSailValidationException) cause);
            } else {
                throw new SPARQLException(e.getMessage());
            }
        }
    }

    @Override
    public void clearGraph(URI graph) throws SPARQLException {
        clearGraph(SimpleValueFactory.getInstance().createIRI(graph.toString()));
    }

    public void clearGraph(IRI graph) throws SPARQLException {
        try {
            rdf4JConnection.clear(graph);
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
            String moveQuery = "MOVE GRAPH <" + oldGraphURI + "> TO <" + newGraphURI + ">";
            Update renameQuery = rdf4JConnection.prepareUpdate(QueryLanguage.SPARQL, moveQuery);
            if (getTimeout() > 0) {
                renameQuery.setMaxExecutionTime(getTimeout());
            }
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

    private static SPARQLValidationException convertRDF4JSHACLException(ShaclSailValidationException validationEx) {
        SPARQLValidationException exception = new SPARQLValidationException();
        Model model = validationEx.validationReportAsModel();
        Map<Resource, URI> invalidObject = new HashMap<>();
        Map<Resource, URI> invalidObjectProperty = new HashMap<>();
        Map<Resource, URI> brokenConstraint = new HashMap<>();
        Map<Resource, String> invalidValue = new HashMap<>();
        for (Statement stmt : model) {
            Resource errorURI = stmt.getSubject();
            try {
                if (stmt.getPredicate().toString().equals(SHACL.focusNode.toString())) {
                    invalidObject.put(errorURI, new URI(stmt.getObject().stringValue()));
                } else if (stmt.getPredicate().toString().equals(SHACL.resultPath.toString())) {
                    invalidObjectProperty.put(errorURI, new URI(stmt.getObject().stringValue()));
                } else if (stmt.getPredicate().toString().equals(SHACL.sourceConstraintComponent.toString())) {
                    brokenConstraint.put(errorURI, new URI(stmt.getObject().stringValue()));
                } else if (stmt.getPredicate().toString().equals(SHACL.value.toString())) {
                    invalidValue.put(errorURI, stmt.getObject().stringValue());
                }

                if (invalidObject.containsKey(errorURI) && invalidObjectProperty.containsKey(errorURI) && brokenConstraint.containsKey(errorURI)) {
                    exception.addValidationError(invalidObject.get(errorURI), invalidObjectProperty.get(errorURI), brokenConstraint.get(errorURI), invalidValue.get(errorURI));

                }
            } catch (Exception ex) {
                LOGGER.warn("Unexpected error while analyzing SHACL exception", ex);
            }

        }

        return exception;
    }

    @Deprecated
    public RepositoryConnection getRepositoryConnectionImpl() {
        return rdf4JConnection;
    }

    private boolean shaclEnabled = false;

    @Override
    public void disableSHACL() throws SPARQLException {
        try {
            clearGraph(RDF4J.SHACL_SHAPE_GRAPH);
            shaclEnabled = false;
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
    public void enableSHACL() throws SPARQLException {
        try {
            rdf4JConnection.begin();
            clearGraph(RDF4J.SHACL_SHAPE_GRAPH);

            for (Class<?> c : getMapperIndex().getResourceClasses()) {
                try {
                    String shaclTTL = SHACL.generateSHACL(c, getMapperIndex());
                    if (shaclTTL != null) {
                        LOGGER.debug("Generated SHACL for: " + c.getCanonicalName() + "\n" + shaclTTL);
                        rdf4JConnection.add(new StringReader(shaclTTL), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
                    } else {
                        LOGGER.debug("No SHACL Validation for: " + c.getCanonicalName() + "\n" + shaclTTL);
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Error while loading SHACL for: " + c.getCanonicalName(), ex);
                }
            }

            rdf4JConnection.commit();
            shaclEnabled = true;
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
    public boolean isShaclEnabled() {
        return shaclEnabled;
    }

    private SPARQLClassObjectMapperIndex mapperIndex;

    @Override
    public SPARQLClassObjectMapperIndex getMapperIndex() {
        return mapperIndex;
    }

    @Override
    public void setMapperIndex(SPARQLClassObjectMapperIndex mapperIndex) {
        this.mapperIndex = mapperIndex;
    }

    /**
     * @apiNote Override for a better display of the connection type and identify
     * (Especially useful for traceability of connection inside logging)
     */
    @Override
    public String toString() {
        return rdf4JConnection.getClass().getSimpleName() + "@" + System.identityHashCode(this);
    }
}
