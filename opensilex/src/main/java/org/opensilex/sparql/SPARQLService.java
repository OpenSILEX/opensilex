//******************************************************************************
//                          SPARQLService.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql;

import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Var;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.service.Service;
import org.opensilex.sparql.deserializer.Deserializers;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.exceptions.SPARQLEntityNotFoundException;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.exceptions.SPARQLTransactionException;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SPARQLService
 */
public class SPARQLService implements SPARQLConnection, Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLService.class);

    private final SPARQLConnection connection;

    public SPARQLService(SPARQLConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean executeAskQuery(AskBuilder ask) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL ASK\n" + ask.buildString());
        }
        return connection.executeAskQuery(ask);
    }

    @Override
    public List<SPARQLResult> executeDescribeQuery(DescribeBuilder describe) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL DESCRIBE\n" + describe.buildString());
        }
        return connection.executeDescribeQuery(describe);
    }

    @Override
    public List<SPARQLResult> executeConstructQuery(ConstructBuilder construct) throws SPARQLQueryException {
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
    public void executeDeleteQuery(UpdateBuilder update) throws SPARQLQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL UPDATE\n" + update.buildRequest().toString());
        }
        connection.executeDeleteQuery(update);
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
    public void clearGraph(Node graph) throws SPARQLQueryException {
        LOGGER.debug("SPARQL CLEAR GRAPH: " + graph.getURI());
        connection.clearGraph(graph);
    }

    @Override
    public void clear() throws SPARQLQueryException {
        LOGGER.debug("SPARQL CLEAR REPOSITORY");
        connection.clear();
    }

    public void loadOntologyStream(Node graph, InputStream ontology, Lang format) throws SPARQLQueryException {
        LOGGER.debug("SPARQL LOAD " + format.getName() + " FILE INTO GRAPH: " + graph.getURI());
        Model model = ModelFactory.createDefaultModel();
        model.read(ontology, null, format.getName());

        UpdateBuilder insertQuery = new UpdateBuilder();
        StmtIterator iterator = model.listStatements();

        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            insertQuery.addInsert(graph, statement.asTriple());
        }
        insertQuery.buildRequest().toString();
        executeUpdateQuery(insertQuery);
    }

    public <T> T getByURI(Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        T instance;
        if (sparqlObjectMapper.hasCacheInstance(uri)) {
            instance = sparqlObjectMapper.getCacheInstance(uri);
        } else {
            instance = sparqlObjectMapper.createInstance(uri, this);
        }
        return instance;
    }

    public <T> T loadByURI(Class<T> objectClass, URI uri) throws Exception {
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

    public <T> T getByUniquePropertyValue(Class<T> objectClass, Property property, Object propertyValue) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        SelectBuilder select = sparqlObjectMapper.getSelectBuilder();
        Field field = sparqlObjectMapper.getFieldFromUniqueProperty(property);

//        propertyValue
        SPARQLDeserializer<?> deserializer = Deserializers.getForClass(propertyValue.getClass());
//        select.addBind(, );
        select.setVar(field.getName(), deserializer.getNode(propertyValue));

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.size() == 0) {
            throw new SPARQLEntityNotFoundException(objectClass, property, propertyValue);
        } else if (results.size() == 1) {
            return sparqlObjectMapper.createInstance(results.get(0), this);
        } else {
            throw new SPARQLException("Multiple objects for some unique property");
        }
    }

    public <T> boolean existsByUniquePropertyValue(Class<T> objectClass, Property property, Object propertyValue) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        AskBuilder ask = sparqlObjectMapper.getAskBuilder();
        Field field = sparqlObjectMapper.getFieldFromUniqueProperty(property);
        SPARQLDeserializer<?> deserializer = Deserializers.getForClass(propertyValue.getClass());
        ask.setVar(field.getName(), deserializer.getNode(propertyValue));

        return executeAskQuery(ask);
    }

    public <T> List<T> search(Class<T> objectClass, Consumer<SelectBuilder> filterHandler) throws Exception {
        return search(objectClass, filterHandler, null, null, null);
    }

    public <T> List<T> search(Class<T> objectClass, Consumer<SelectBuilder> filterHandler, Map<String, Order> orderBy, Integer offset, Integer limit) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
        SelectBuilder select = sparqlObjectMapper.getSelectBuilder();

        if (filterHandler != null) {
            filterHandler.accept(select);
        }

        if (orderBy != null) {
            orderBy.forEach((String fieldName, Order order) -> {
                select.addOrderBy(fieldName, order);
            });
        }

        if (offset != null) {
            select.setOffset(offset);
        }

        if (limit != null) {
            select.setLimit(limit);
        }

        List<T> resultList = new ArrayList<>();
        executeSelectQuery(select, (SPARQLResult result) -> {
            try {
                resultList.add(sparqlObjectMapper.createInstance(result, this));
            } catch (Exception ex) {
                // TODO warn
                resultList.add(null);
            }
        });

        return resultList;
    }

    public <T> int count(Class<T> objectClass, Consumer<SelectBuilder> filterHandler) throws Exception {
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

    public <T> void create(T instance) throws Exception {
        @SuppressWarnings("unchecked")
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass((Class<T>) instance.getClass());

        UpdateBuilder create = sparqlObjectMapper.getCreateBuilder(instance);

        executeUpdateQuery(create);
    }

    public <T> void create(List<T> instances) throws Exception {
        UpdateBuilder create = new UpdateBuilder();
        for (T instance : instances) {
            @SuppressWarnings("unchecked")
            SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass((Class<T>) instance.getClass());
            sparqlObjectMapper.addCreateBuilder(instance, create);
        }

        executeUpdateQuery(create);
    }

    public <T> void update(T instance) throws Exception {
        @SuppressWarnings("unchecked")
        Class<T> objectClass = (Class<T>) instance.getClass();
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);

        UpdateBuilder update = new UpdateBuilder();
        sparqlObjectMapper.addUpdateBuilder(loadByURI(objectClass, sparqlObjectMapper.getURI(instance)), instance, update);

        executeUpdateQuery(update);
    }

    public <T> void update(List<T> instances) throws Exception {
        UpdateBuilder update = new UpdateBuilder();
        for (T instance : instances) {
            @SuppressWarnings("unchecked")
            Class<T> objectClass = (Class<T>) instance.getClass();
            SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);
            sparqlObjectMapper.addUpdateBuilder(loadByURI(objectClass, sparqlObjectMapper.getURI(instance)), instance, update);
        }

        executeUpdateQuery(update);
    }

    public <T> void delete(Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);

        UpdateBuilder delete = sparqlObjectMapper.getDeleteBuilder(loadByURI(objectClass, uri));

        executeDeleteQuery(delete);

        sparqlObjectMapper.removeCacheInstance(uri);
    }

    public <T> void delete(Class<T> objectClass, List<URI> uris) throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(objectClass);

        UpdateBuilder delete = new UpdateBuilder();
        for (URI uri : uris) {
            sparqlObjectMapper.addDeleteBuilder(loadByURI(objectClass, uri), delete);
        }

        executeDeleteQuery(delete);

        uris.forEach((uri) -> {
            sparqlObjectMapper.removeCacheInstance(uri);
        });
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
}
