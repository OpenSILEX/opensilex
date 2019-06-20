//******************************************************************************
//                          SPARQLService.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.core.service.sparql;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLException;
import org.opensilex.service.Service;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLQueryException;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLTransactionException;
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
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);
        SelectBuilder select = descriptor.getSelectBuilder();
        select.addBind(descriptor.getURIFieldName(), uri);

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.size() == 1) {
            return descriptor.createInstance(results.get(0), this);
        } else {
            throw new SPARQLException("Multiple objects for the same URI");
        }
    }

    public <T> T getByUniquePropertyValue(Class<T> objectClass, Property property, Object propertyValue) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);
        SelectBuilder select = descriptor.getSelectBuilder();
        Field field = descriptor.getFieldFromUniqueProperty(property);
        select.addBind(field.getName(), propertyValue);

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.size() == 1) {
            return descriptor.createInstance(results.get(0), this);
        } else {
            throw new SPARQLException("Multiple objects for some unique property");
        }
    }

    public <T> List<T> search(Class<T> objectClass, Consumer<SelectBuilder> filterHandler) throws Exception {
        return search(objectClass, filterHandler, null, null, null);
    }

    public <T> List<T> search(Class<T> objectClass, Consumer<SelectBuilder> filterHandler, Map<String, Boolean> orderBy, Integer offset, Integer limit) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);
        SelectBuilder select = descriptor.getSelectBuilder();

        if (filterHandler != null) {
            filterHandler.accept(select);
        }

        if (orderBy != null) {
            orderBy.forEach((String fieldName, Boolean isDesc) -> {
                if (isDesc) {
                    select.addOrderBy(fieldName, Order.DESCENDING);
                } else {
                    select.addOrderBy(fieldName, Order.ASCENDING);
                }
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
                resultList.add(descriptor.createInstance(result, this));
            } catch (Exception ex) {
                // TODO warn
                resultList.add(null);
            }
        });

        return resultList;
    }

    public <T> int count(Class<T> objectClass, Consumer<SelectBuilder> filterHandler) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);
        SelectBuilder selectCount = descriptor.getCountBuilder("count");

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

    public <T> void create(Class<T> objectClass, T instance) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);

        UpdateBuilder create = descriptor.getCreateBuilder(instance);

        executeUpdateQuery(create);
    }

    public <T> void create(Class<T> objectClass, List<T> instances) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);

        UpdateBuilder create = new UpdateBuilder();
        instances.forEach((T instance) -> {
            descriptor.addCreateBuilder(instance, create);
        });

        executeUpdateQuery(create);
    }

    public <T> void update(Class<T> objectClass, T instance) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);

        UpdateBuilder update = new UpdateBuilder();
        descriptor.addDeleteBuilder(instance, update);
        descriptor.addCreateBuilder(instance, update);

        executeUpdateQuery(update);
    }

    public <T> void update(Class<T> objectClass, List<T> instances) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);

        UpdateBuilder update = new UpdateBuilder();
        instances.forEach((T instance) -> {
            descriptor.addDeleteBuilder(instance, update);
            descriptor.addCreateBuilder(instance, update);
        });

        executeUpdateQuery(update);
    }

    public <T> void delete(Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);

        UpdateBuilder delete = descriptor.getDeleteBuilder(uri);

        executeUpdateQuery(delete);
    }

    public <T> void delete(Class<T> objectClass, List<URI> uris) throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(objectClass);

        UpdateBuilder delete = new UpdateBuilder();

        uris.forEach((URI uri) -> {
            descriptor.addDeleteBuilder(uri, delete);
        });

        executeUpdateQuery(delete);
    }

}
