//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.OpenSilex;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfigDefault;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.exceptions.SPARQLTransactionException;
import org.opensilex.sparql.exceptions.SPARQLUnknownFieldException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLModel;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SPARQLService
 */
@ServiceConfigDefault(
        connection = RDF4JConnection.class,
        connectionConfig = RDF4JConfig.class,
        connectionConfigID = "rdf4j"
)
public class SPARQLService implements SPARQLConnection, Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLService.class);

    private final SPARQLConnection connection;

    private final URI baseURI;

    public SPARQLService(SPARQLConnection connection) throws URISyntaxException {
        this.connection = connection;
        baseURI = OpenSilex.getPlatformURI();
    }

    @Override
    public void startup() {
        connection.startup();
    }

    @Override
    public void shutdown() {
        connection.shutdown();
    }

    @Override
    public boolean executeAskQuery(AskBuilder ask) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL ASK\n" + ask.buildString());
        }
        return connection.executeAskQuery(ask);
    }

    @Override
    public List<SPARQLStatement> executeDescribeQuery(DescribeBuilder describe) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL DESCRIBE\n" + describe.buildString());
        }
        return connection.executeDescribeQuery(describe);
    }

    public List<SPARQLStatement> describe(URI uri) throws SPARQLQueryException {
        DescribeBuilder describe = new DescribeBuilder();
        Var uriVar = makeVar("uri");
        describe.addVar(uriVar);
        describe.addBind(new ExprFactory().iri(uri), uriVar);
        return executeDescribeQuery(describe);
    }

    @Override
    public List<SPARQLStatement> executeConstructQuery(ConstructBuilder construct) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL CONSTRUCT\n" + construct.buildString());
        }
        return connection.executeConstructQuery(construct);
    }

    @Override
    public List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL SELECT\n" + select.buildString());
        }
        return connection.executeSelectQuery(select, resultHandler);
    }

    @Override
    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL UPDATE\n" + update.build().toString());
        }
        connection.executeUpdateQuery(update);
    }

    @Override
    public void executeDeleteQuery(UpdateBuilder delete) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL DELETE\n" + delete.buildRequest().toString());
        }
        connection.executeDeleteQuery(delete);
    }

    @Override
    public void startTransaction() throws SPARQLTransactionException {
        LOGGER.debug("SPARQL TRANSACTION START");
        connection.startTransaction();
    }

    @Override
    public void commitTransaction() throws SPARQLTransactionException {
        LOGGER.debug("SPARQL TRANSACTION COMMIT");
        connection.commitTransaction();
    }

    @Override
    public void rollbackTransaction() throws SPARQLTransactionException {
        LOGGER.debug("SPARQL TRANSACTION ROLLBACK");
        connection.rollbackTransaction();
    }

    @Override
    public void clearGraph(URI graph) throws SPARQLQueryException {
        LOGGER.debug("SPARQL CLEAR GRAPH: " + graph);
        connection.clearGraph(graph);
    }

    @Override
    public void clear() throws SPARQLQueryException {
        LOGGER.debug("SPARQL CLEAR REPOSITORY");
        connection.clear();
    }

    public void loadOntologyStream(URI graph, InputStream ontology, Lang format) throws SPARQLQueryException {
        Node graphNode = NodeFactory.createURI(graph.toString());
        LOGGER.debug("SPARQL LOAD " + format.getName() + " FILE INTO GRAPH: " + graphNode);
        Model model = ModelFactory.createDefaultModel();
        model.read(ontology, null, format.getName());

        UpdateBuilder insertQuery = new UpdateBuilder();
        StmtIterator iterator = model.listStatements();

        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            insertQuery.addInsert(graphNode, statement.asTriple());
        }
        insertQuery.buildRequest().toString();
        executeUpdateQuery(insertQuery);
    }

    public <T extends SPARQLModel> T getByURI(Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        T instance = sparqlObjectMapper.createInstance(uri, this);
        return instance;
    }

    public <T extends SPARQLModel> T loadByURI(Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        SelectBuilder select = sparqlObjectMapper.getSelectBuilder();
        Node nodeURI = NodeFactory.createURI(uri.toString());
        select.setVar(sparqlObjectMapper.getURIFieldName(), nodeURI);
        select.addValueVar(sparqlObjectMapper.getURIFieldName(), "<" + nodeURI.getURI() + ">");

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.size() == 1) {
            return sparqlObjectMapper.createInstance(results.get(0), this);
        } else if (results.size() > 1) {
            throw new SPARQLException("Multiple objects for the same URI: " + uri.toString());
        } else {
            return null;
        }
    }

    public <T extends SPARQLModel> T getByUniquePropertyValue(Class<T> objectClass, Property property, Object propertyValue) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        SelectBuilder select = sparqlObjectMapper.getSelectBuilder();
        Field field = sparqlObjectMapper.getFieldFromUniqueProperty(property);

        SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForClass(propertyValue.getClass());
        select.addWhereValueVar(field.getName(), deserializer.getNode(propertyValue));

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.size() == 0) {
            return null;
        } else if (results.size() == 1) {
            return sparqlObjectMapper.createInstance(results.get(0), this);
        } else {
            throw new SPARQLException("Multiple objects for some unique property");
        }
    }

    public <T extends SPARQLModel> boolean existsByUniquePropertyValue(Class<T> objectClass, Property property, Object propertyValue) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        AskBuilder ask = sparqlObjectMapper.getAskBuilder();
        Field field = sparqlObjectMapper.getFieldFromUniqueProperty(property);
        SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForClass(propertyValue.getClass());
        ask.setVar(field.getName(), deserializer.getNode(propertyValue));

        return executeAskQuery(ask);
    }

    public <T extends SPARQLModel> List<T> search(Class<T> objectClass) throws Exception {
        return search(objectClass, null, null, null, null);
    }

    public <T extends SPARQLModel> List<T> search(Class<T> objectClass, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return search(objectClass, filterHandler, null, null, null);
    }

    public <T extends SPARQLModel> List<T> search(Class<T> objectClass, List<OrderBy> orderByList) throws Exception {
        return search(objectClass, null, orderByList, null, null);
    }

    public <T extends SPARQLModel> List<T> search(Class<T> objectClass, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList) throws Exception {
        return search(objectClass, filterHandler, orderByList, null, null);
    }

    public <T extends SPARQLModel> List<T> search(Class<T> objectClass, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        SelectBuilder select = sparqlObjectMapper.getSelectBuilder();

        if (filterHandler != null) {
            filterHandler.accept(select);
        }

        if (orderByList != null) {
            orderByList.forEach(ThrowingConsumer.wrap((OrderBy orderBy) -> {
                Expr fieldOrderExpr = sparqlObjectMapper.getFieldOrderExpr(orderBy.getFieldName());
                select.addOrderBy(fieldOrderExpr, orderBy.getOrder());
            }, SPARQLUnknownFieldException.class));
        }

        if (page == null || page < 0) {
            page = 0;
        }

        if (pageSize != null && pageSize > 0) {
            select.setOffset(page * pageSize);
            select.setLimit(pageSize);
        }

        List<T> resultList = new ArrayList<>();
        executeSelectQuery(select, ThrowingConsumer.wrap((SPARQLResult result) -> {
            resultList.add(sparqlObjectMapper.createInstance(result, this));
        }, Exception.class));

        return resultList;
    }

    public <T extends SPARQLModel> int count(Class<T> objectClass, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        SelectBuilder selectCount = sparqlObjectMapper.getCountBuilder("count");

        if (filterHandler != null) {
            filterHandler.accept(selectCount);
        }

        List<SPARQLResult> resultSet = executeSelectQuery(selectCount);

        if (resultSet.size() == 1) {
            return Integer.valueOf(resultSet.get(0).getStringValue("count"));
        } else {
            throw new SPARQLException("Invalid count query");
        }
    }

    public <T extends SPARQLModel> ListWithPagination<T> searchWithPagination(Class<T> objectClass, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        int total = count(objectClass, filterHandler);

        List<T> list;
        if (pageSize == null || pageSize == 0) {
            list = search(objectClass, filterHandler, orderByList);
        } else if (total > 0 && (page * pageSize) < total) {
            list = search(objectClass, filterHandler, orderByList, page, pageSize);
        } else {
            list = new ArrayList<T>();
        }

        return new ListWithPagination<T>(list, page, pageSize, total);
    }

    public <T extends SPARQLModel> void create(T instance) throws Exception {
        @SuppressWarnings("unchecked")
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass((Class<T>) instance.getClass());

        generateUniqueUriIfNullOrValidateCurrent(sparqlObjectMapper, instance);

        UpdateBuilder create = sparqlObjectMapper.getCreateBuilder(instance);

        executeUpdateQuery(create);
    }

    public <T extends SPARQLModel> void create(List<T> instances) throws Exception {
        UpdateBuilder create = new UpdateBuilder();
        for (T instance : instances) {
            @SuppressWarnings("unchecked")
            SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass((Class<T>) instance.getClass());
            generateUniqueUriIfNullOrValidateCurrent(sparqlObjectMapper, instance);
            sparqlObjectMapper.addCreateBuilder(instance, create);
        }

        executeUpdateQuery(create);
    }

    private <T extends SPARQLModel> void generateUniqueUriIfNullOrValidateCurrent(SPARQLClassObjectMapper<T> sparqlObjectMapper, T instance) throws Exception {
        URIGenerator<T> uriGenerator = sparqlObjectMapper.getUriGenerator(instance);
        URI uri = sparqlObjectMapper.getURI(instance);
        if (uri == null) {
            uri = uriGenerator.generateURI(baseURI, instance);
            int retry = 0;
            while (uriExists(uri)) {
                uri = uriGenerator.generateURI(baseURI, instance, ++retry);
            }

            sparqlObjectMapper.setUri(instance, uri);
        } else {
            if (uriExists(uri)) {
                throw new SPARQLAlreadyExistingUriException(uri);
            }
        }
    }

    public <T extends SPARQLModel> void update(T instance) throws Exception {
        @SuppressWarnings("unchecked")
        Class<T> objectClass = (Class<T>) instance.getClass();
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);

        UpdateBuilder update = new UpdateBuilder();
        sparqlObjectMapper.addUpdateBuilder(loadByURI(objectClass, sparqlObjectMapper.getURI(instance)), instance, update);

        executeUpdateQuery(update);
    }

    public <T extends SPARQLModel> void update(List<T> instances) throws Exception {
        UpdateBuilder update = new UpdateBuilder();
        for (T instance : instances) {
            @SuppressWarnings("unchecked")
            Class<T> objectClass = (Class<T>) instance.getClass();
            SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
            sparqlObjectMapper.addUpdateBuilder(loadByURI(objectClass, sparqlObjectMapper.getURI(instance)), instance, update);
        }

        executeUpdateQuery(update);
    }

    public <T extends SPARQLModel> void delete(Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);

        UpdateBuilder delete = sparqlObjectMapper.getDeleteBuilder(loadByURI(objectClass, uri));

        executeDeleteQuery(delete);
    }

    public <T extends SPARQLModel> void delete(Class<T> objectClass, List<URI> uris) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);

        UpdateBuilder delete = new UpdateBuilder();
        for (URI uri : uris) {
            sparqlObjectMapper.addDeleteBuilder(loadByURI(objectClass, uri), delete);
        }

        executeDeleteQuery(delete);
    }

    public boolean uriExists(URI uri) throws SPARQLQueryException {
        AskBuilder askQuery = new AskBuilder();
        Var s = makeVar("s");
        Var p = makeVar("p");
        Var o = makeVar("o");
        Node nodeUri = Ontology.nodeURI(uri);
        askQuery.addWhere(nodeUri, p, o);
        WhereBuilder reverseWhere = new WhereBuilder();
        reverseWhere.addWhere(s, p, nodeUri);
        askQuery.addUnion(reverseWhere);

        return executeAskQuery(askQuery);
    }

    public <T extends SPARQLModel> boolean uriExists(Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);

        AskBuilder askQuery = new AskBuilder();
        Var p = makeVar("p");
        Var o = makeVar("o");
        Var type = makeVar("type");
        Node nodeUri = Ontology.nodeURI(uri);
        askQuery.addWhere(nodeUri, RDF.type, type);

        Resource typeDef = sparqlObjectMapper.getRDFType();

        askQuery.addWhere(type, Ontology.subClassAny, typeDef);

        return executeAskQuery(askQuery);
    }

    public void deleteObjectRelation(Node g, URI s, Property p, URI o) throws SPARQLQueryException {
        UpdateBuilder delete = new UpdateBuilder();
        delete.addDelete(g, Ontology.nodeURI(s), p.asNode(), Ontology.nodeURI(o));

        executeDeleteQuery(delete);
    }
}
